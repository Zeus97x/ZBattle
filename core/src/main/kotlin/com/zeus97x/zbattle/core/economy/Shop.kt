package com.zeus97x.zbattle.core.economy

import com.zeus97x.zbattle.core.ShopCategory

/** Why a purchase can't go ahead. */
enum class BuyRefusal(val message: String) {
    NotForSale("This item isn't sold."),
    Unavailable("Not available yet."),
    NotEnoughCoins("Not enough coins."),
    AtCap("You already hold the most you can."),
}

sealed interface BuyResult {
    data class Bought(val inventory: Inventory) : BuyResult
    data class Refused(val reason: BuyRefusal) : BuyResult
}

/**
 * Coin purchases (D-SHOP, batch 2). Each purchase is one ledger transaction `buy-<n>`: coins out and
 * the item in, together or not at all. There is no real money, no selling and no buying of tickets.
 */
object Shop {
    fun items(category: ShopCategory): List<ItemDef> = when (category) {
        ShopCategory.Equipment -> ItemCatalog.ofKind(ItemKind.Equipment)
        ShopCategory.Consumables -> ItemCatalog.ofKind(ItemKind.Consumable) + ItemCatalog.ofKind(ItemKind.ZCube)
        ShopCategory.Cosmetics -> ItemCatalog.ofKind(ItemKind.Cosmetic)
    }

    fun check(inventory: Inventory, item: ItemDef): BuyRefusal? = when {
        item.price == null -> BuyRefusal.NotForSale
        item.unavailableReason != null -> BuyRefusal.Unavailable
        inventory[item.id] >= item.cap -> BuyRefusal.AtCap
        inventory.coins < item.price -> BuyRefusal.NotEnoughCoins
        else -> null
    }

    fun buy(inventory: Inventory, itemId: String): BuyResult {
        val item = ItemCatalog.get(itemId) ?: return BuyResult.Refused(BuyRefusal.NotForSale)
        check(inventory, item)?.let { return BuyResult.Refused(it) }
        val (id, reserved) = inventory.reserveId("buy")
        val tx = Transaction(id, mapOf(ItemCatalog.COINS to -item.price!!, item.id to 1L))
        return when (val r = reserved.apply(tx)) {
            is TxResult.Applied -> BuyResult.Bought(r.inventory)
            is TxResult.Rejected -> BuyResult.Refused(if (r.error == TxError.OverCap) BuyRefusal.AtCap else BuyRefusal.NotEnoughCoins)
        }
    }
}
