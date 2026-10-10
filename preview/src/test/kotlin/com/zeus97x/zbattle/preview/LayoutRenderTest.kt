package com.zeus97x.zbattle.preview

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.zeus97x.zbattle.core.ArtCatalog
import com.zeus97x.zbattle.core.ArtKey
import com.zeus97x.zbattle.core.CollectionQuery
import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.InMemorySettingsStore
import com.zeus97x.zbattle.core.NavState
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.MasterGender
import com.zeus97x.zbattle.core.MasterStyle
import com.zeus97x.zbattle.core.PetMaster
import com.zeus97x.zbattle.core.PlayerSettings
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.Route
import com.zeus97x.zbattle.core.ShopCategory
import com.zeus97x.zbattle.core.StageFilter
import com.zeus97x.zbattle.core.Tab
import com.zeus97x.zbattle.core.battle.BattleAction
import com.zeus97x.zbattle.core.battle.OwnedCreature
import com.zeus97x.zbattle.core.battle.RepeatEnd
import com.zeus97x.zbattle.core.battle.Encounters
import com.zeus97x.zbattle.core.battle.Outcome
import com.zeus97x.zbattle.ui.AppState
import com.zeus97x.zbattle.ui.LocalArtLoader
import com.zeus97x.zbattle.ui.ZBattleApp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Renders every route and overlay headlessly at 412dp and 360dp portrait (plus 1.3× font scale
 * and light mode samples) with Compose Multiplatform desktop. Output PNGs go to
 * preview/build/screenshots for review; the test fails if any composition throws.
 */
class LayoutRenderTest {
    private val assetPack = File(System.getProperty("zbattle.assetPack") ?: "../ZBattle-ZPet-Assets")
    private val outDir = File(System.getProperty("zbattle.screenshots") ?: "build/screenshots").apply { mkdirs() }
    private val loader = FileArtLoader(listOf(File(assetPack, "assets"), File(assetPack, "../app/src/main/assets")))

    private data class Phone(val name: String, val widthDp: Int, val heightDp: Int, val fontScale: Float = 1f)

    private val phones = listOf(Phone("412dp", 412, 915), Phone("360dp", 360, 780))
    private val largeText = Phone("360dp-font130", 360, 780, 1.3f)

    private val visitedSettings = PlayerSettings(
        master = PetMaster("Zeus", MasterStyle.DragonDisciple, MasterGender.Female, "cindlet"),
        currentAreaIndex = 13,
        visitedAreas = setOf(0, 12, 13),
    )

    private val sliceArea = PlayerSettings(
        master = PetMaster("Zeus", MasterStyle.DragonDisciple, MasterGender.Female, "cindlet"),
        currentAreaIndex = 0,
        visitedAreas = setOf(0),
    ).withSeededStarter()
    private val encounter = Encounters.playable.single()
    private val midBattle = sliceArea.copy(progress = sliceArea.progress.startBattle(encounter).act(BattleAction.Skill).act(BattleAction.Attack))
    private val afterWin = sliceArea.copy(progress = run {
        var p = sliceArea.progress.startBattle(encounter)
        while (p.active != null) p = p.act(if (p.active!!.skillReady) BattleAction.Skill else BattleAction.Attack)
        p
    })
    private val battleNav = NavState(listOf(Route.Home, Route.Challenges(0), Route.Battle(0, 0)))

    private val cases: List<Triple<String, NavState, PlayerSettings>> = listOf(
        Triple("00-setup", NavState(), PlayerSettings()),
        Triple("01-home", NavState(), visitedSettings),
        Triple("02-collection", NavState(listOf(Route.Collection)), visitedSettings),
        Triple("03-travel", NavState(listOf(Route.Home, Route.Travel(3))), visitedSettings),
        Triple("03b-travel-egyptian-group7", NavState(listOf(Route.Home, Route.Travel(6))), visitedSettings),
        Triple("03c-travel-greek-group1", NavState(listOf(Route.Home, Route.Travel(0))), visitedSettings),
        Triple("03d-travel-greek-group8", NavState(listOf(Route.Home, Route.Travel(7))), visitedSettings),
        Triple("04-challenges", NavState(listOf(Route.Home, Route.Challenges(0))), sliceArea),
        Triple("04b-challenges-preview-area", NavState(listOf(Route.Home, Route.Challenges(13))), visitedSettings),
        Triple("04c-challenges-defeated", NavState(listOf(Route.Home, Route.Challenges(0))), afterWin.copy(progress = afterWin.progress.dismissResult())),
        Triple("05-battle", battleNav, midBattle),
        Triple("05b-battle-result", battleNav, afterWin),
        Triple("06-shop-sheet", NavState(overlay = Overlay.ShopSheet), visitedSettings),
        Triple("07-item-shop", NavState(listOf(Route.Home, Route.ItemShop(ShopCategory.Equipment))), visitedSettings),
        Triple("08-double-battle", NavState(listOf(Route.Home, Route.DoubleBattle(13))), visitedSettings),
        Triple("09-profile", NavState(listOf(Route.Profile)), visitedSettings),
        Triple("10-achievements", NavState(listOf(Route.Profile, Route.Achievements)), visitedSettings),
        Triple("11-events", NavState(listOf(Route.Events)), visitedSettings),
        Triple("12-creature-detail", NavState(listOf(Route.Collection), Overlay.CreatureDetail("tombwarden")), visitedSettings),
        Triple("12b-owned-detail", NavState(listOf(Route.Home), Overlay.CreatureDetail("cindlet")), afterWin.copy(progress = afterWin.progress.dismissResult())),
        Triple("13-pending-detail", NavState(listOf(Route.Collection), Overlay.CreatureDetail("ashpeep")), visitedSettings),
        Triple("14-confirm-challenge", NavState(listOf(Route.Home, Route.Challenges(0)), Overlay.ConfirmChallenge(0, 0)), sliceArea),
        Triple("14b-preview-opponent", NavState(listOf(Route.Home, Route.Challenges(13)), Overlay.ConfirmChallenge(13, 3)), visitedSettings),
        Triple("15-locked-area", NavState(listOf(Route.Home, Route.Travel(6)), Overlay.LockedArea(26)), visitedSettings),
        Triple("16-retreat", battleNav.show(Overlay.ConfirmRetreat), midBattle),
        Triple("17-edit-master", NavState(listOf(Route.Profile), Overlay.EditMaster), visitedSettings),
    )

    /** CLAUDE-005 B5: a won replay (the results screen offers repeat sessions). */
    private val afterReplay = afterWin.copy(progress = afterWin.progress.dismissResult().startBattle(encounter).let { p ->
        var q = p
        while (q.active != null) q = q.act(if (q.active!!.skillReady) BattleAction.Skill else BattleAction.Attack)
        q
    })

    /** CLAUDE-005 B2: three owned creatures (all with created art). */
    private val trio = sliceArea.copy(progress = sliceArea.progress.let {
        it.copy(creatures = it.creatures + OwnedCreature(it.nextUid, "sparklit", 120) + OwnedCreature(it.nextUid + 1, "inkling", 40), nextUid = it.nextUid + 2)
    })
    private val trioMid = trio.copy(progress = trio.progress.startBattle(encounter).act(BattleAction.Skill).switchTo(1))
    private val trioFainted = trioMid.copy(progress = trioMid.progress.let { p ->
        val b = p.active!!
        p.copy(active = b.copy(team = b.team.mapIndexed { i, m -> if (i == b.activeIndex) m.copy(combatant = m.combatant.copy(hp = 1)) else m }))
    }.act(BattleAction.Attack))
    private val trioWon = trio.copy(progress = trio.progress.startBattle(encounter).act(BattleAction.Skill).switchTo(1).let { p ->
        var q = p
        while (q.active != null) q = q.act(if (q.active!!.skillReady) BattleAction.Skill else BattleAction.Attack)
        q
    })
    private val partyCases = listOf(
        Triple("21-party-home", NavState(), trio),
        Triple("22-party-battle", battleNav, trioMid),
        Triple("23-party-replacement", battleNav, trioFainted),
        Triple("24-party-result", battleNav, trioWon),
        Triple("25-party-detail", NavState(listOf(Route.Home), Overlay.CreatureDetail("inkling")), trio),
        Triple("26-replay-result", battleNav, afterReplay),
    )

    @Test
    fun companionIdsAreAssignedOnceAndPersisted() {
        val store = InMemorySettingsStore(sliceArea.copy(progress = sliceArea.progress.copy(creatures = sliceArea.progress.creatures.map { it.copy(companionId = null) })))
        var n = 0
        val ids = { "00000000-0000-4000-8000-%012d".format(++n) }
        val first = AppState(store, newCompanionId = ids)
        val id = first.settings.progress.lead!!.companionId
        assertEquals("00000000-0000-4000-8000-000000000001", id)
        assertEquals(id, store.load().progress.lead!!.companionId, "saved immediately")
        assertEquals(id, AppState(store, newCompanionId = ids).settings.progress.lead!!.companionId, "never regenerated")
        assertEquals(1, n)
        val fresh = AppState(InMemorySettingsStore(), newCompanionId = ids)
        fresh.completeSetup(PetMaster("Zeus", MasterStyle.Knight, MasterGender.Male, "inkling"))
        assertEquals("00000000-0000-4000-8000-000000000002", fresh.settings.progress.lead!!.companionId)
    }

    @Test
    fun repeatSessionRunsBoundedAutoReplaysAndStopsOnInterruption() {
        val store = InMemorySettingsStore(afterWin.copy(progress = afterWin.progress.dismissResult()))
        val state = AppState(store, NavState(listOf(Route.Home, Route.Challenges(0))))
        val xpBefore = store.load().progress.lead!!.xp
        state.startRepeat(encounter, 3)
        assertEquals(true, state.autoFight)
        assertEquals(Route.Battle(0, 0), state.nav.current)
        var guard = 0
        while (state.repeat!!.running && guard++ < 500) state.autoFightStep(state.settings.progress.active!!)
        val r = state.repeat!!
        assertEquals(RepeatEnd.Completed, r.end)
        assertEquals(3, r.played)
        assertEquals(3, r.wins)
        assertEquals(false, state.autoFight)
        assertEquals(4, store.load().progress.wins[encounter.id], "1 first clear + 3 replays")
        assertEquals(xpBefore + 3 * 15, store.load().progress.lead!!.xp, "three replays at 25% of 60 XP")
        assertEquals(15L * 3, r.xp)
        assertEquals(5L * 3, r.coins)

        state.finishBattle()
        assertEquals(null, state.repeat)
        state.startRepeat(encounter, 5)
        state.autoFightStep(state.settings.progress.active!!)
        state.battleAction(BattleAction.Attack) // the player takes control: the session ends
        assertEquals(RepeatEnd.Interrupted, state.repeat!!.end)
        assertEquals(false, state.autoFight)
        assertEquals(null, AppState(store).repeat, "not saved: restart explicitly")

        // Only already-cleared encounters can be repeated.
        val fresh = AppState(InMemorySettingsStore(sliceArea), NavState(listOf(Route.Home, Route.Challenges(0))))
        fresh.startRepeat(encounter, 3)
        assertEquals(null, fresh.repeat)
        assertEquals(null, fresh.settings.progress.active)
    }

    @Test
    fun partyFlowThroughAppState() {
        val store = InMemorySettingsStore(trio)
        val state = AppState(store, NavState(listOf(Route.Home, Route.Challenges(0))))
        val uids = trio.progress.creatures.map { it.uid }
        state.makeLead(uids[2])
        assertEquals(uids[2], store.load().progress.lead!!.uid)
        state.startBattle(encounter)
        state.toggleParty(uids[0])
        assertEquals(3, store.load().progress.partyMembers.size, "party locked during battle")
        state.startAutoFight()
        state.switchTo(1)
        assertEquals(false, state.autoFight, "switching takes control")
        assertEquals(1, store.load().progress.active!!.turn)
        assertEquals(1, AppState(store).settings.progress.active!!.activeIndex, "switch survives restart")
        state.retreatBattle()
        assertEquals(null, store.load().progress.active)
    }

    @Test
    fun rendersEveryRouteAndOverlay() {
        preloadCreatures()
        val written = mutableListOf<File>()
        for (phone in phones) for ((name, nav, settings) in cases) written += render(phone, name, nav, settings)
        for ((name, nav, settings) in cases.filter { it.first in setOf("00-setup", "01-home", "02-collection", "03-travel", "04-challenges", "05-battle", "05b-battle-result", "09-profile") }) {
            written += render(largeText, name, nav, settings)
        }
        for (phone in phones + largeText) written += render(phone, "05c-battle-auto", battleNav, midBattle) { it.startAutoFight() }
        for (phone in phones + largeText) for ((name, nav, settings) in partyCases) written += render(phone, name, nav, settings)
        for (phone in phones + largeText) written += render(phone, "27-repeat-running", battleNav, afterWin.copy(progress = afterWin.progress.dismissResult())) {
            it.startRepeat(encounter, 5)
        }
        written += render(phones[0], "18-home-light", NavState(), visitedSettings.copy(darkMode = false))
        written += render(phones[0], "19-profile-light", NavState(listOf(Route.Profile)), visitedSettings.copy(darkMode = false))
        written += render(
            phones[0], "20-collection-filtered", NavState(listOf(Route.Collection)), visitedSettings,
        ) { it.collectionQuery = CollectionQuery(search = "", families = setOf(3, 6), stage = StageFilter.Final) }
        assertTrue(written.all { it.length() > 10_000 }, "Every screenshot should contain rendered content")
        println("Wrote ${written.size} screenshots to $outDir")
    }

    @Test
    fun everyRouteTypeIsCovered() {
        val routes = cases.flatMap { it.second.stack }.map { it::class }.toSet()
        val expected = setOf(Route.Home::class, Route.Collection::class, Route.Travel::class, Route.Challenges::class, Route.Battle::class,
            Route.ItemShop::class, Route.DoubleBattle::class, Route.Profile::class, Route.Achievements::class, Route.Events::class)
        assertEquals(expected, routes)
        val overlays = cases.mapNotNull { it.second.overlay?.let { o -> o::class } }.toSet()
        assertTrue(overlays.containsAll(setOf(Overlay.ShopSheet::class, Overlay.CreatureDetail::class, Overlay.ConfirmChallenge::class,
            Overlay.ConfirmRetreat::class, Overlay.LockedArea::class, Overlay.EditMaster::class)))
    }

    @Test
    fun appStateFlowsTravelAndSettingsThroughStore() {
        val store = InMemorySettingsStore()
        val state = AppState(store)
        assertEquals(null, state.settings.master)
        state.completeSetup(PetMaster("Zeus", MasterStyle.Knight, MasterGender.Male, "inkling"))
        assertEquals("inkling", store.load().master?.starterId)
        assertEquals(listOf("Inkling"), state.settings.party.map { it.name })
        state.navigate(Route.Travel(5))
        state.travelTo(RegionCatalog.area(20))
        assertEquals(Route.Home, state.nav.current)
        assertEquals(20, store.load().currentAreaIndex)
        state.updateSettings { it.copy(darkMode = false) }
        assertEquals(false, store.load().darkMode)
        state.selectTab(Tab.Shop)
        assertEquals(Overlay.ShopSheet, state.nav.overlay)
        assertTrue(state.back())
        assertEquals(null, state.nav.overlay)
        assertEquals(false, state.back())
    }

    @Test
    fun autoFightThroughAppStateIsForegroundOnlyAndSettlesOnce() {
        val store = InMemorySettingsStore(sliceArea)
        val state = AppState(store, NavState(listOf(Route.Home, Route.Challenges(0))))
        state.startAutoFight()
        assertEquals(false, state.autoFight, "No auto without an active battle")
        state.startBattle(encounter)
        state.startAutoFight()
        val first = state.settings.progress.active!!
        state.autoFightStep(first)
        assertEquals(1, state.settings.progress.active!!.turn)
        state.autoFightStep(first) // duplicate timer for the same state
        assertEquals(1, state.settings.progress.active!!.turn)

        // A dialog pauses it; Back (retreat prompt) stops it.
        state.show(Overlay.ConfirmRetreat)
        state.autoFightStep(state.settings.progress.active!!)
        assertEquals(1, state.settings.progress.active!!.turn)
        state.dismissOverlay()
        assertTrue(state.back())
        assertEquals(false, state.autoFight)
        state.dismissOverlay()

        // Tapping a move takes control back.
        state.startAutoFight()
        state.battleAction(BattleAction.Attack)
        assertEquals(false, state.autoFight)

        // Not saved: a restart comes back with auto off.
        state.startAutoFight()
        assertEquals(false, AppState(store).autoFight)

        while (state.settings.progress.active != null) {
            val a = state.settings.progress.active!!
            state.autoFightStep(a)
        }
        assertEquals(false, state.autoFight, "Auto stops once the battle settles")
        assertEquals(Outcome.Victory, store.load().progress.lastResult!!.outcome)
        assertEquals(60, store.load().progress.lead!!.xp)
        state.autoFightStep(first)
        assertEquals(60, store.load().progress.lead!!.xp)
        assertEquals(1, store.load().progress.wins.values.sum())
    }

    @Test
    fun battleFlowThroughAppStateSavesOnceAndResumes() {
        val store = InMemorySettingsStore(sliceArea)
        val state = AppState(store, NavState(listOf(Route.Home, Route.Challenges(0))))
        state.startBattle(encounter)
        assertEquals(Route.Battle(0, 0), state.nav.current)
        state.battleAction(BattleAction.Attack)
        // Back during a battle asks to retreat instead of leaving.
        assertTrue(state.back())
        assertEquals(Overlay.ConfirmRetreat, state.nav.overlay)
        state.dismissOverlay()

        // App restart mid-battle resumes straight into the saved battle.
        val reopened = AppState(store)
        assertEquals(Route.Battle(0, 0), reopened.nav.current)
        assertEquals(1, reopened.settings.progress.active!!.turn)

        while (reopened.settings.progress.active != null) {
            val a = reopened.settings.progress.active!!
            reopened.battleAction(if (a.skillReady) BattleAction.Skill else BattleAction.Attack)
        }
        assertEquals(Outcome.Victory, store.load().progress.lastResult!!.outcome)
        assertEquals(60, store.load().progress.lead!!.xp)
        reopened.finishBattle()
        reopened.finishBattle()
        assertEquals(Route.Challenges(0), reopened.nav.current)
        assertEquals(60, store.load().progress.lead!!.xp)

        // Retreat grants nothing and returns to the list.
        reopened.startBattle(encounter)
        reopened.retreatBattle()
        assertEquals(null, store.load().progress.active)
        assertEquals(60, store.load().progress.lead!!.xp)
        assertEquals(Route.Challenges(0), reopened.nav.current)
    }

    @Test
    fun existingCreatureArtLoadsAndMissingArtFallsBack() {
        CreatureCatalog.created.forEach { creature ->
            val path = ArtCatalog.candidates(ArtKey.CreatureArt(creature)).single()
            assertNotNull(loader.load(path), path)
        }
        // CLAUDE-003: maps and heroes ship as WebP; battle scenery is a later art phase.
        assertNotNull(ArtCatalog.candidates(ArtKey.LocationHero(RegionCatalog.area(0))).firstNotNullOfOrNull { loader.load(it) })
        assertNotNull(ArtCatalog.candidates(ArtKey.RegionMap(RegionCatalog.groups[6])).firstNotNullOfOrNull { loader.load(it) })
        val battle = ArtCatalog.candidates(ArtKey.LocationBattle(RegionCatalog.area(0)))
        assertTrue(battle.all { loader.load(it) == null }, "Battle scenery placeholders remain until that phase")
    }

    private fun preloadCreatures() {
        CreatureCatalog.created.forEach { loader.load(it.assetPath!!) }
        (RegionCatalog.groups.map { ArtKey.RegionMap(it) } + RegionCatalog.areas.map { ArtKey.LocationHero(it) })
            .forEach { key -> ArtCatalog.candidates(key).firstNotNullOfOrNull { loader.load(it) } }
    }

    private fun render(phone: Phone, name: String, nav: NavState, settings: PlayerSettings, setup: (AppState) -> Unit = {}): File {
        val density = 2f
        val state = AppState(InMemorySettingsStore(settings), nav).also(setup)
        val scene = ImageComposeScene(
            width = (phone.widthDp * density).toInt(),
            height = (phone.heightDp * density).toInt(),
            density = Density(density, phone.fontScale),
        ) {
            CompositionLocalProvider(LocalArtLoader provides loader) { ZBattleApp(state) }
        }
        try {
            var image = scene.render(0)
            // A few frames let pagers, lazy lists and async art settle.
            for (frame in 1..6) {
                Thread.sleep(30)
                image = scene.render(frame * 100_000_000L)
            }
            val bytes = assertNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes
            return File(outDir, "$name-${phone.name}.png").apply { writeBytes(bytes) }
        } finally {
            scene.close()
        }
    }
}
