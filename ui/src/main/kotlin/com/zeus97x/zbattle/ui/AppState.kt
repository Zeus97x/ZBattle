package com.zeus97x.zbattle.ui

import com.zeus97x.zbattle.core.economy.TicketClaim
import com.zeus97x.zbattle.core.economy.TicketPool
import com.zeus97x.zbattle.core.economy.TicketPools
import com.zeus97x.zbattle.core.economy.BuyRefusal
import com.zeus97x.zbattle.core.economy.BuyResult
import com.zeus97x.zbattle.core.economy.Shop
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zeus97x.zbattle.core.Area
import com.zeus97x.zbattle.core.CollectionQuery
import com.zeus97x.zbattle.core.NavState
import com.zeus97x.zbattle.core.Overlay
import com.zeus97x.zbattle.core.PetMaster
import com.zeus97x.zbattle.core.PlayerSettings
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.Route
import com.zeus97x.zbattle.core.SettingsStore
import com.zeus97x.zbattle.core.Tab
import com.zeus97x.zbattle.core.battle.BattleAction
import com.zeus97x.zbattle.core.battle.BattleProgress
import com.zeus97x.zbattle.core.battle.BattleState
import com.zeus97x.zbattle.core.battle.Encounter
import com.zeus97x.zbattle.core.battle.Encounters
import com.zeus97x.zbattle.core.battle.RepeatSession
import com.zeus97x.zbattle.core.battle.autoStep
import com.zeus97x.zbattle.core.battle.withCompanionIds

/** UI state holder shared by every screen. Settings are persisted through [store] on change. */
@Stable
class AppState(
    private val store: SettingsStore,
    initialNav: NavState? = null,
    /** Source of new companion UUIDs (CLAUDE-005 B3); injectable for tests. */
    private val newCompanionId: () -> String = { java.util.UUID.randomUUID().toString() },
    /** Ticket creature pool (C3); null while D-TICKET-POOL is open. Injectable for tests. */
    val ticketPool: TicketPool? = TicketPools.approved,
    /** Ticket roll source: returns 0 until n. Injectable for tests. */
    private val ticketRoll: (Int) -> Int = { n -> java.security.SecureRandom().nextInt(n) },
) {
    var settings by mutableStateOf(normalized(store.load()))
        private set
    var nav by mutableStateOf(initialNav ?: resumeNav(settings))
        private set
    var collectionQuery by mutableStateOf(CollectionQuery())

    /**
     * Auto-fight (CLAUDE-005 B1) is foreground-only and never saved: closing, backgrounding or
     * leaving the battle turns it off, and the player restarts it explicitly.
     */
    var autoFight by mutableStateOf(false)
        private set

    /** Repeat session (CLAUDE-005 B5): in memory only, like auto-fight. Kept after it ends for the summary. */
    var repeat by mutableStateOf<RepeatSession?>(null)
        private set

    fun navigate(route: Route) { nav = nav.push(route) }
    fun selectTab(tab: Tab) { nav = nav.selectTab(tab) }
    fun show(overlay: Overlay) { nav = nav.show(overlay) }
    fun dismissOverlay() { nav = nav.dismissOverlay() }
    fun popTo(predicate: (Route) -> Boolean) { nav = nav.popTo(predicate) }

    /**
     * System back. Inside a running battle it asks to retreat instead of leaving silently;
     * on a results screen it acknowledges the result. Returns false when the app should close.
     */
    fun back(): Boolean {
        if (nav.overlay == null && nav.current is Route.Battle) {
            stopAutoFight()
            when {
                settings.progress.active != null -> { show(Overlay.ConfirmRetreat); return true }
                settings.progress.lastResult != null -> { finishBattle(); return true }
            }
        }
        val next = nav.back() ?: return false
        nav = next
        return true
    }

    /**
     * Seeds the starter and assigns each companion its UUID exactly once. Called on load and after
     * every change; the init block saves straight away, so an id is never regenerated on a later launch.
     */
    private fun normalized(s: PlayerSettings): PlayerSettings =
        s.withSeededStarter().let { it.copy(progress = it.progress.withCompanionIds(newCompanionId)) }

    init {
        if (settings != store.load()) store.save(settings)
    }

    fun updateSettings(transform: (PlayerSettings) -> PlayerSettings) {
        val next = normalized(transform(settings))
        if (next != settings) {
            settings = next
            store.save(next)
        }
    }

    private fun updateProgress(transform: (BattleProgress) -> BattleProgress) =
        updateSettings { it.copy(progress = transform(it.progress)) }

    /**
     * Buys one [itemId] with coins (one ledger transaction). Returns the refusal, if any, so the
     * caller can explain it; the overlay is closed either way.
     */
    fun buy(itemId: String): BuyRefusal? {
        dismissOverlay()
        return when (val r = Shop.buy(settings.progress.inventory, itemId)) {
            is BuyResult.Bought -> { updateProgress { it.copy(inventory = r.inventory) }; null }
            is BuyResult.Refused -> r.reason
        }
    }

    /**
     * Redeems one ticket (C3). The outcome is written to disk before it is returned, so a crash can't
     * lead to a second roll. Returns null when redemption isn't possible (no pool approved yet, no
     * ticket, or a battle in progress).
     */
    fun redeemTicket(itemId: String): TicketClaim? {
        val pool = ticketPool ?: return null
        val p = settings.progress
        if (p.active != null || p.inventory[itemId] <= 0) return null
        val next = normalized(settings.copy(progress = p.redeemTicket(itemId, pool, ticketRoll)))
        store.saveDurably(next)
        settings = next
        return next.progress.ticketClaims.last()
    }

    /** Saves the first-run Pet Master, grants the starter and opens Home. */
    fun completeSetup(master: PetMaster) {
        updateSettings { it.copy(master = master) }
        nav = NavState()
    }

    /** Travel confirms an area, then returns Home showing it. */
    fun travelTo(area: Area) {
        updateSettings { it.travelTo(area) }
        nav = NavState(listOf(Route.Home))
    }

    /** Starts (or resumes) the real battle for [encounter] and opens the battle screen. */
    fun startBattle(encounter: Encounter) {
        if (repeat?.running != true) repeat = null
        val active = settings.progress.active
        if (active == null) updateProgress { it.startBattle(encounter) }
        else if (active.encounterId != encounter.id) return
        nav = nav.popTo { it is Route.Challenges }.push(Route.Battle(encounter.area.index, encounter.slot))
    }

    /** A tapped move. Tapping while auto-fight runs takes control back (auto stops first). */
    fun battleAction(action: BattleAction) {
        stopAutoFight()
        if (settings.progress.active != null) updateProgress { it.act(action) }
    }

    /** Uses a battle item (costs the turn). Manual only: auto stops first and never uses items. */
    fun useItem(itemId: String) {
        stopAutoFight()
        if (settings.progress.itemRefusal(itemId) == null) updateProgress { it.useItem(itemId) }
    }

    /** Manual switch (costs the turn). Like any tapped move it takes control back from auto. */
    fun switchTo(index: Int) {
        stopAutoFight()
        if (settings.progress.active?.canSwitch == true) updateProgress { it.switchTo(index) }
    }

    /** Free replacement after a faint. */
    fun replaceWith(index: Int) {
        stopAutoFight()
        if (settings.progress.active?.awaitingReplacement == true) updateProgress { it.replaceWith(index) }
    }

    /** Equips charm [itemId] on [uid] (null removes it); ignored during a battle or without the charm. */
    fun equip(uid: Long, itemId: String?) {
        val p = settings.progress
        if (p.active != null || p.owned(uid) == null) return
        if (itemId != null && p.inventory[itemId] <= 0) return
        updateProgress { it.equip(uid, itemId) }
    }

    fun toggleParty(uid: Long) { if (settings.progress.active == null) updateProgress { it.toggleParty(uid) } }

    fun makeLead(uid: Long) { if (settings.progress.active == null) updateProgress { it.makeLead(uid) } }

    fun startAutoFight() { if (settings.progress.active != null) autoFight = true }

    /** Stops auto-fight; a running repeat session ends as interrupted (the player restarts it explicitly). */
    fun stopAutoFight() {
        autoFight = false
        repeat = repeat?.interrupted()
    }

    /**
     * Starts [count] back-to-back replays of an already-cleared encounter, played by auto-fight.
     * Each battle is a separate encounter at full HP; a defeat, retreat or any interruption ends it.
     */
    fun startRepeat(encounter: Encounter, count: Int) {
        val progress = settings.progress
        if (progress.active != null || encounter.id !in progress.defeated || count !in 1..RepeatSession.MAX_BATTLES) return
        repeat = RepeatSession(encounter.id, count)
        updateProgress { it.dismissResult().startBattle(encounter) }
        nav = nav.popTo { it is Route.Challenges }.push(Route.Battle(encounter.area.index, encounter.slot))
        autoFight = true
    }

    /**
     * One scheduled auto move for the battle exactly as it was when scheduled. Ignored when auto is
     * off, a dialog is open, or the battle has moved on (see [autoStep]); stops once it settles.
     */
    fun autoFightStep(expected: BattleState) {
        if (!autoFight || nav.overlay != null) return
        updateProgress { it.autoStep(expected) }
        if (settings.progress.active != null) return
        // The battle settled. A running repeat session records it and starts the next one.
        val session = repeat?.takeIf { it.running }
        val result = settings.progress.lastResult
        if (session == null || result == null) { autoFight = false; return }
        val next = session.record(result)
        repeat = next
        val encounter = Encounters.byId(session.encounterId)
        if (next.running && encounter != null) updateProgress { it.dismissResult().startBattle(encounter) } else autoFight = false
    }

    /** Retreat grants nothing and returns to the challenge list. */
    fun retreatBattle() {
        stopAutoFight()
        repeat = null
        if (settings.progress.active != null) updateProgress { it.retreat().dismissResult() }
        nav = nav.popTo { it is Route.Challenges }
    }

    /** Leaves the results screen. Rewards were already applied when the battle ended. */
    fun finishBattle() {
        stopAutoFight()
        repeat = null
        updateProgress { it.dismissResult() }
        nav = nav.popTo { it is Route.Challenges }
    }

    private companion object {
        /** Mid-battle recovery: reopen straight into a saved active battle. */
        fun resumeNav(settings: PlayerSettings): NavState {
            val battle = settings.progress.active ?: return NavState()
            val encounter = Encounters.byId(battle.encounterId) ?: return NavState()
            val area = RegionCatalog.area(encounter.area.index)
            return NavState(listOf(Route.Home, Route.Challenges(area.index), Route.Battle(area.index, encounter.slot)))
        }
    }
}
