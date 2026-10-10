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
import com.zeus97x.zbattle.core.Route
import com.zeus97x.zbattle.core.SettingsStore
import com.zeus97x.zbattle.core.Tab

/** UI state holder shared by every screen. Settings are persisted through [store] on change. */
@Stable
class AppState(private val store: SettingsStore, initialNav: NavState = NavState()) {
    var nav by mutableStateOf(initialNav)
        private set
    var settings by mutableStateOf(store.load())
        private set
    var collectionQuery by mutableStateOf(CollectionQuery())

    fun navigate(route: Route) { nav = nav.push(route) }
    fun selectTab(tab: Tab) { nav = nav.selectTab(tab) }
    fun show(overlay: Overlay) { nav = nav.show(overlay) }
    fun dismissOverlay() { nav = nav.dismissOverlay() }
    fun popTo(predicate: (Route) -> Boolean) { nav = nav.popTo(predicate) }

    /** System back. Returns false when the app should close. */
    fun back(): Boolean {
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

    /** Saves the first-run Pet Master and opens Home. */
    fun completeSetup(master: PetMaster) {
        updateSettings { it.copy(master = master) }
        nav = NavState()
    }

    /** Travel confirms an area, then returns Home showing it. */
    fun travelTo(area: Area) {
        updateSettings { it.travelTo(area) }
        nav = NavState(listOf(Route.Home))
    }
}
