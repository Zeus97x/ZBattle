package com.zeus97x.zbattle.core

/** Bottom-navigation tabs in display order. Home is the raised centre action. */
enum class Tab(val label: String) { Collection("Collection"), Shop("Shop"), Home("Home"), Events("Events"), Profile("Profile") }

sealed interface Route {
    val tab: Tab

    data object Home : Route { override val tab = Tab.Home }
    data object Collection : Route { override val tab = Tab.Collection }
    data object Events : Route { override val tab = Tab.Events }
    data object Profile : Route { override val tab = Tab.Profile }
    data object Achievements : Route { override val tab = Tab.Profile }
    data class Travel(val focusGroup: Int) : Route { override val tab = Tab.Home }
    data class Challenges(val areaIndex: Int) : Route { override val tab = Tab.Home }
    data class Battle(val areaIndex: Int, val opponentSlot: Int) : Route { override val tab = Tab.Home }
    data class DoubleBattle(val areaIndex: Int) : Route { override val tab = Tab.Home }
    data class ItemShop(val category: ShopCategory) : Route { override val tab = Tab.Shop }
}

/** Sheets and dialogs drawn above the current route. Back always dismisses these first. */
sealed interface Overlay {
    data object ShopSheet : Overlay
    data class CreatureDetail(val creatureId: String) : Overlay
    data class ConfirmChallenge(val areaIndex: Int, val opponentSlot: Int) : Overlay
    data object ConfirmRetreat : Overlay
    data class LockedArea(val areaIndex: Int) : Overlay
    data class Notice(val title: String, val message: String) : Overlay
    /** Confirm spending coins on one item (CLAUDE-006 shop). */
    data class ConfirmPurchase(val itemId: String) : Overlay
    /** Edit name and cosmetic appearance; the starter choice is fixed after setup. */
    data object EditMaster : Overlay
}

/**
 * Immutable navigation state: a back stack whose root is a tab destination plus one optional
 * overlay. Pure so it can be unit-tested without a UI.
 */
data class NavState(
    val stack: List<Route> = listOf(Route.Home),
    val overlay: Overlay? = null,
) {
    init { require(stack.isNotEmpty()) }

    val current: Route get() = stack.last()
    val selectedTab: Tab get() = current.tab
    val canGoBack: Boolean get() = overlay != null || stack.size > 1 || stack.first() != Route.Home

    fun push(route: Route): NavState = copy(stack = stack + route, overlay = null)

    /** Replaces everything above the tab root. */
    fun selectTab(tab: Tab): NavState = when (tab) {
        // Shop has no standalone page: it opens the category sheet over the current screen.
        Tab.Shop -> copy(overlay = Overlay.ShopSheet)
        Tab.Home -> NavState(listOf(Route.Home))
        Tab.Collection -> NavState(listOf(Route.Collection))
        Tab.Events -> NavState(listOf(Route.Events))
        Tab.Profile -> NavState(listOf(Route.Profile))
    }

    fun show(overlay: Overlay): NavState = copy(overlay = overlay)
    fun dismissOverlay(): NavState = copy(overlay = null)

    /** Pops back to the nearest route matching [predicate], or to the tab root. */
    fun popTo(predicate: (Route) -> Boolean): NavState {
        val index = stack.indexOfLast(predicate)
        return copy(stack = if (index >= 0) stack.subList(0, index + 1) else stack.take(1), overlay = null)
    }

    /**
     * System back: overlay first, then screen stack, then other tabs return to Home.
     * Returns null when the app should exit.
     */
    fun back(): NavState? = when {
        overlay != null -> dismissOverlay()
        stack.size > 1 -> copy(stack = stack.dropLast(1))
        stack.first() != Route.Home -> NavState(listOf(Route.Home))
        else -> null
    }
}
