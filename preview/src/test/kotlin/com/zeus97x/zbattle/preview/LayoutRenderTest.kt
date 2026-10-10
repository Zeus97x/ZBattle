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

    @Test
    fun rendersEveryRouteAndOverlay() {
        preloadCreatures()
        val written = mutableListOf<File>()
        for (phone in phones) for ((name, nav, settings) in cases) written += render(phone, name, nav, settings)
        for ((name, nav, settings) in cases.filter { it.first in setOf("00-setup", "01-home", "02-collection", "03-travel", "04-challenges", "05-battle", "05b-battle-result", "09-profile") }) {
            written += render(largeText, name, nav, settings)
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
        val hero = ArtCatalog.candidates(ArtKey.LocationHero(RegionCatalog.area(0)))
        assertTrue(hero.all { loader.load(it) == null }, "No scenery art is committed yet; placeholders must be used")
    }

    private fun preloadCreatures() {
        CreatureCatalog.created.forEach { loader.load(it.assetPath!!) }
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
