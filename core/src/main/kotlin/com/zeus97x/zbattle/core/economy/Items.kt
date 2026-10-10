package com.zeus97x.zbattle.core.economy

/**
 * Item catalogue (CLAUDE-006 C1, D-SHOP decided in batch 2, 2026-10-10).
 *
 * Ids are stable production ids: saves and transactions refer to them, so an id is never renamed or
 * reused. Names are working labels until ChatGPT supplies final names and art. Prices are the ones
 * approved in ECONOMY-PROPOSAL §7. Deferred items (Focus Tonic, Revive Seed) and crafting materials
 * are deliberately absent.
 */
enum class ItemKind { Currency, Consumable, Equipment, Cosmetic, Ticket, ZCube, Material }

/** What a consumable does in battle (wired by the item-action PR; listed here so ids and effects travel together). */
sealed interface ItemEffect {
    data class Heal(val hp: Int) : ItemEffect
    data object ApplyBurn : ItemEffect
    data object ApplyWeaken : ItemEffect
}

/** Flat stat bonus from one equipped item (applied by the equipment PR). */
data class StatBonus(val power: Int = 0, val guard: Int = 0, val speed: Int = 0, val maxHp: Int = 0)

data class ItemDef(
    val id: String,
    val kind: ItemKind,
    val name: String,
    /** Coins, or null when the item can't be bought. */
    val price: Long? = null,
    /** Most a player can hold. */
    val cap: Long,
    val effect: ItemEffect? = null,
    val bonus: StatBonus? = null,
    /** Ticket tier as a ZBattle rarity index (1 Rare, 2 Epic, 3 Legendary). */
    val ticketTier: Int? = null,
    /** Shown but not purchasable, with the reason (e.g. the system it needs isn't built yet). */
    val unavailableReason: String? = null,
) {
    val purchasable: Boolean get() = price != null && unavailableReason == null
}

object ItemCatalog {
    const val COINS = "coins"
    const val POTION = "potion"
    const val TICKET_RARE = "ticket-rare"
    const val TICKET_EPIC = "ticket-epic"
    const val TICKET_LEGENDARY = "ticket-legendary"

    /** ECONOMY §4 caps. */
    const val COIN_CAP = 999_999L
    const val STACK_CAP = 99L
    const val TICKET_CAP = 9_999L

    private const val CATCHING_PENDING = "Catching isn't in ZBattle yet"

    val all: List<ItemDef> = listOf(
        ItemDef(COINS, ItemKind.Currency, "Coins", cap = COIN_CAP),
        // Consumables (§7.1): one per turn, at most 5 per battle, never used by auto.
        ItemDef(POTION, ItemKind.Consumable, "Potion", price = 30, cap = STACK_CAP, effect = ItemEffect.Heal(25)),
        ItemDef("super-potion", ItemKind.Consumable, "Super Potion", price = 80, cap = STACK_CAP, effect = ItemEffect.Heal(60)),
        ItemDef("ember-vial", ItemKind.Consumable, "Ember Vial", price = 50, cap = STACK_CAP, effect = ItemEffect.ApplyBurn),
        ItemDef("sapping-dust", ItemKind.Consumable, "Sapping Dust", price = 50, cap = STACK_CAP, effect = ItemEffect.ApplyWeaken),
        // Equipment (§7.2): one slot per creature; equipping tier II puts tier I back in the bag (no trade-in).
        ItemDef("fang-charm-1", ItemKind.Equipment, "Fang Charm I", price = 200, cap = STACK_CAP, bonus = StatBonus(power = 1)),
        ItemDef("fang-charm-2", ItemKind.Equipment, "Fang Charm II", price = 600, cap = STACK_CAP, bonus = StatBonus(power = 2)),
        ItemDef("shell-charm-1", ItemKind.Equipment, "Shell Charm I", price = 200, cap = STACK_CAP, bonus = StatBonus(guard = 1)),
        ItemDef("shell-charm-2", ItemKind.Equipment, "Shell Charm II", price = 600, cap = STACK_CAP, bonus = StatBonus(guard = 2)),
        ItemDef("feather-charm-1", ItemKind.Equipment, "Feather Charm I", price = 200, cap = STACK_CAP, bonus = StatBonus(speed = 1)),
        ItemDef("feather-charm-2", ItemKind.Equipment, "Feather Charm II", price = 600, cap = STACK_CAP, bonus = StatBonus(speed = 2)),
        ItemDef("heart-charm-1", ItemKind.Equipment, "Heart Charm I", price = 250, cap = STACK_CAP, bonus = StatBonus(maxHp = 10)),
        ItemDef("heart-charm-2", ItemKind.Equipment, "Heart Charm II", price = 700, cap = STACK_CAP, bonus = StatBonus(maxHp = 20)),
        // Cosmetics (§7.3): no stats, owned once.
        ItemDef("cosmetic-trainer-frame", ItemKind.Cosmetic, "Trainer frame", price = 300, cap = 1),
        ItemDef("cosmetic-party-banner", ItemKind.Cosmetic, "Party banner", price = 500, cap = 1),
        ItemDef("cosmetic-backdrop-tint", ItemKind.Cosmetic, "Battle backdrop tint", price = 800, cap = 1),
        ItemDef("cosmetic-trainer-title", ItemKind.Cosmetic, "Trainer title", price = 1_000, cap = 1),
        // Tickets (Q9): first-clear boss rewards only; never sold.
        ItemDef(TICKET_RARE, ItemKind.Ticket, "Rare ticket", cap = TICKET_CAP, ticketTier = 1),
        ItemDef(TICKET_EPIC, ItemKind.Ticket, "Epic ticket", cap = TICKET_CAP, ticketTier = 2),
        ItemDef(TICKET_LEGENDARY, ItemKind.Ticket, "Legendary ticket", cap = TICKET_CAP, ticketTier = 3),
        // ZCubes (§7.4): Basic/Great/Ultra may be sold; Mythic is never sold. Held back until catching exists.
        ItemDef("zcube-basic", ItemKind.ZCube, "Basic ZCube", price = 25, cap = STACK_CAP, unavailableReason = CATCHING_PENDING),
        ItemDef("zcube-great", ItemKind.ZCube, "Great ZCube", price = 75, cap = STACK_CAP, unavailableReason = CATCHING_PENDING),
        ItemDef("zcube-ultra", ItemKind.ZCube, "Ultra ZCube", price = 200, cap = STACK_CAP, unavailableReason = CATCHING_PENDING),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): ItemDef? = byId[id]
    fun require(id: String): ItemDef = requireNotNull(byId[id]) { "Unknown item $id" }
    fun ofKind(kind: ItemKind): List<ItemDef> = all.filter { it.kind == kind }
    fun ticketFor(tier: Int): ItemDef = all.single { it.ticketTier == tier }

    /** D-SHOP: new players start with 100 coins and 3 Potions. */
    val starterKit: Map<String, Long> = mapOf(COINS to 100L, POTION to 3L)
    const val STARTER_KIT_TX = "starter-kit"
}
