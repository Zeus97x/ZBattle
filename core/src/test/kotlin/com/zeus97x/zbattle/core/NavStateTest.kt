package com.zeus97x.zbattle.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavStateTest {
    @Test
    fun homeIsInitialAndBackExits() {
        val start = NavState()
        assertEquals(Route.Home, start.current)
        assertEquals(Tab.Home, start.selectedTab)
        assertNull(start.back())
    }

    @Test
    fun backDismissesOverlayBeforePoppingScreen() {
        val state = NavState().push(Route.Challenges(4)).show(Overlay.ConfirmChallenge(4, 0))
        val afterFirst = state.back()!!
        assertNull(afterFirst.overlay)
        assertEquals(Route.Challenges(4), afterFirst.current)
        assertEquals(Route.Home, afterFirst.back()!!.current)
    }

    @Test
    fun shopTabOpensSheetWithoutLeavingScreen() {
        val state = NavState().push(Route.Travel(0)).selectTab(Tab.Shop)
        assertEquals(Overlay.ShopSheet, state.overlay)
        assertEquals(Route.Travel(0), state.current)
        val shop = state.push(Route.ItemShop(ShopCategory.Cosmetics))
        assertNull(shop.overlay)
        assertEquals(Tab.Shop, shop.selectedTab)
    }

    @Test
    fun otherTabsReturnHomeOnBack() {
        val profile = NavState().selectTab(Tab.Profile).push(Route.Achievements)
        assertEquals(Route.Profile, profile.back()!!.current)
        assertEquals(Route.Home, profile.back()!!.back()!!.current)
    }

    @Test
    fun retreatPopsToChallenges() {
        val battle = NavState().push(Route.Challenges(2)).push(Route.Battle(2, 1)).show(Overlay.ConfirmRetreat)
        val after = battle.popTo { it is Route.Challenges }
        assertEquals(Route.Challenges(2), after.current)
        assertNull(after.overlay)
    }
}
