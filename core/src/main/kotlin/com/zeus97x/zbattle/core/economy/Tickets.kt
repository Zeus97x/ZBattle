package com.zeus97x.zbattle.core.economy

/**
 * Ticket rolls (CLAUDE-006 C3). One ticket gives exactly one creature.
 *
 * Rarity tables (approved, CLAUDE-006 C3; ZBattle names per D-RARITY):
 * - Rare ticket: 70% Rare, 30% Common.
 * - Epic ticket: 50% Epic, 35% Rare, 15% Common.
 * - Legendary ticket: 40% Legendary, 30% Epic, 21% Rare, 9% Common.
 *
 * Which creature (the family pool and starting form) is **not decided** (D-TICKET-POOL), so
 * [TicketPools.approved] is null and redemption stays unavailable. The roll, the persisted outcome
 * and the atomic grant are complete and tested with a test-only pool.
 */
object TicketTables {
    /** Ticket tier → (rarity, percent), highest rarity first. Each table sums to 100. */
    val tables: Map<Int, List<Pair<Int, Int>>> = mapOf(
        1 to listOf(1 to 70, 0 to 30),
        2 to listOf(2 to 50, 1 to 35, 0 to 15),
        3 to listOf(3 to 40, 2 to 30, 1 to 21, 0 to 9),
    )

    /** Rarity for a roll [r] in 0..99: the first entry whose running total exceeds [r]. */
    fun rarity(tier: Int, r: Int): Int {
        require(r in 0..99) { "Roll out of range" }
        var total = 0
        for ((rarity, pct) in tables.getValue(tier)) {
            total += pct
            if (r < total) return rarity
        }
        error("Table for tier $tier does not sum to 100")
    }
}

/** Which creature a rolled rarity becomes. */
fun interface TicketPool {
    /** Creature form ids (catalogue `Creature.id`) a ticket of this [rarity] may give; never empty. */
    fun creatures(rarity: Int): List<String>
}

object TicketPools {
    /** D-TICKET-POOL is open: no production pool, so tickets can't be redeemed yet. */
    val approved: TicketPool? = null
}

/** The stored result of one redeemed ticket. Retries show this; it is never rolled again. */
data class TicketClaim(
    val claimId: String,
    val ticketItemId: String,
    val rarityRoll: Int,
    val rarity: Int,
    val creatureId: String,
    /** Local uid of the creature granted. */
    val uid: Long,
)
