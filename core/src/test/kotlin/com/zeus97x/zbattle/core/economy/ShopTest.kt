package com.zeus97x.zbattle.core.economy

import com.zeus97x.zbattle.core.ShopCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** D-SHOP: coin purchases are single transactions; nothing unapproved or unavailable can be bought. */
class ShopTest {
    private val kit = Inventory().withStarterKit() // 100 coins, 3 Potions
    private val frame = "cosmetic-trainer-frame" // 300 coins, owned once

    private fun rich(coins: Long = 5_000) = kit.applyOrThrow(Transaction("grant", mapOf(ItemCatalog.COINS to coins)))

    @Test
    fun aPurchaseMovesCoinsAndTheItemTogether() {
        val bought = assertIs<BuyResult.Bought>(Shop.buy(rich(), frame)).inventory
        assertEquals(5_100L - 300, bought.coins)
        assertEquals(1, bought[frame])
        assertTrue("buy-1" in bought.applied)
        // Two purchases never share a transaction id.
        val again = assertIs<BuyResult.Bought>(Shop.buy(bought, "cosmetic-party-banner")).inventory
        assertTrue("buy-2" in again.applied)
    }

    @Test
    fun refusalsLeaveTheInventoryUnchanged() {
        assertEquals(BuyResult.Refused(BuyRefusal.NotEnoughCoins), Shop.buy(kit, frame))
        val owned = assertIs<BuyResult.Bought>(Shop.buy(rich(), frame)).inventory
        assertEquals(BuyResult.Refused(BuyRefusal.AtCap), Shop.buy(owned, frame))
        assertEquals(BuyResult.Refused(BuyRefusal.NotForSale), Shop.buy(rich(), ItemCatalog.TICKET_RARE))
        assertEquals(BuyResult.Refused(BuyRefusal.NotForSale), Shop.buy(rich(), "zcube-mythic"))
        assertEquals(BuyResult.Refused(BuyRefusal.Unavailable), Shop.buy(rich(), "zcube-basic"))
    }

    @Test
    fun everyListedItemIsApprovedAndSortedIntoOneCategory() {
        val listed = ShopCategory.entries.flatMap(Shop::items)
        assertEquals(listed.map { it.id }.toSet().size, listed.size)
        assertTrue(listed.all { it.price != null })
        assertTrue(listed.none { it.kind == ItemKind.Ticket || it.kind == ItemKind.Currency })
        assertEquals(4 + 3, Shop.items(ShopCategory.Consumables).size, "4 consumables + Basic/Great/Ultra ZCubes")
        assertEquals(8, Shop.items(ShopCategory.Equipment).size)
        assertEquals(4, Shop.items(ShopCategory.Cosmetics).size)
    }
}
