package com.zeus97x.zbattle.ui

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
import com.zeus97x.zbattle.core.battle.autoStep

/** UI state holder shared by every screen. Settings are persisted through [store] on change. */
@Stable
class AppState(private val store: SettingsStore, initialNav: NavState? = null) {
    var settings by mutableStateOf(store.load().withSeededStarter())
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

    fun updateSettings(transform: (PlayerSettings) -> PlayerSettings) {
        val next = transform(settings)
        if (next != settings) {
            settings = next
            store.save(next)
        }
    }

    private fun updateProgress(transform: (BattleProgress) -> BattleProgress) =
        updateSettings { it.copy(progress = transform(it.progress)) }

    /** Saves the first-run Pet Master, grants the starter and opens Home. */
    fun completeSetup(master: PetMaster) {
        updateSettings { it.copy(master = master).withSeededStarter() }
        nav = NavState()
    }

    /** Travel confirms an area, then returns Home showing it. */
    fun travelTo(area: Area) {
        updateSettings { it.travelTo(area) }
        nav = NavState(listOf(Route.Home))
    }

    /** Starts (or resumes) the real battle for [encounter] and opens the battle screen. */
    fun startBattle(encounter: Encounter) {
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

    fun toggleParty(uid: Long) { if (settings.progress.active == null) updateProgress { it.toggleParty(uid) } }

    fun makeLead(uid: Long) { if (settings.progress.active == null) updateProgress { it.makeLead(uid) } }

    fun startAutoFight() { if (settings.progress.active != null) autoFight = true }

    fun stopAutoFight() { autoFight = false }

    /**
     * One scheduled auto move for the battle exactly as it was when scheduled. Ignored when auto is
     * off, a dialog is open, or the battle has moved on (see [autoStep]); stops once it settles.
     */
    fun autoFightStep(expected: BattleState) {
        if (!autoFight || nav.overlay != null) return
        updateProgress { it.autoStep(expected) }
        if (settings.progress.active == null) autoFight = false
    }

    /** Retreat grants nothing and returns to the challenge list. */
    fun retreatBattle() {
        stopAutoFight()
        if (settings.progress.active != null) updateProgress { it.retreat().dismissResult() }
        nav = nav.popTo { it is Route.Challenges }
    }

    /** Leaves the results screen. Rewards were already applied when the battle ended. */
    fun finishBattle() {
        stopAutoFight()
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
