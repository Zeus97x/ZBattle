package com.zeus97x.zbattle.core.economy

import com.zeus97x.zbattle.core.battle.BattleProgress
import com.zeus97x.zbattle.core.battle.BattleProgressCodec
import com.zeus97x.zbattle.core.battle.Encounters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** CLAUDE-006 C3: cascading ticket tables, one persisted outcome per ticket. */
class TicketsTest {
    /** Test-only pool: never shipped (D-TICKET-POOL is open). */
    private val testPool = TicketPool { listOf("sparklit", "inkling") }

    private fun withTickets(n: Long = 2) = BattleProgress().withStarter("cindlet").let {
        it.copy(inventory = it.inventory.applyOrThrow(Transaction("t", mapOf(ItemCatalog.TICKET_RARE to n, ItemCatalog.TICKET_LEGENDARY to 1L))))
    }

    @Test
    fun tableBoundariesAreExact() {
        fun counts(tier: Int) = (0..99).groupingBy { TicketTables.rarity(tier, it) }.eachCount()
        assertEquals(mapOf(1 to 70, 0 to 30), counts(1))
        assertEquals(mapOf(2 to 50, 1 to 35, 0 to 15), counts(2))
        assertEquals(mapOf(3 to 40, 2 to 30, 1 to 21, 0 to 9), counts(3))
        // Boundaries: the last roll of each band and the first of the next.
        assertEquals(listOf(1, 0), listOf(69, 70).map { TicketTables.rarity(1, it) })
        assertEquals(listOf(2, 1, 1, 0), listOf(49, 50, 84, 85).map { TicketTables.rarity(2, it) })
        assertEquals(listOf(3, 2, 2, 1, 1, 0), listOf(39, 40, 69, 70, 90, 91).map { TicketTables.rarity(3, it) })
        assertFailsWith<IllegalArgumentException> { TicketTables.rarity(1, 100) }
    }

    @Test
    fun aRedemptionConsumesOneTicketAndGrantsOneNewIndividual() {
        val p = withTickets()
        val rolls = ArrayDeque(listOf(10, 0)) // Rare (10 < 70), first creature
        val after = p.redeemTicket(ItemCatalog.TICKET_RARE, testPool) { rolls.removeFirst() }
        val claim = after.ticketClaims.single()
        assertEquals(1, after.inventory[ItemCatalog.TICKET_RARE])
        assertEquals(1, claim.rarity)
        assertEquals("sparklit", claim.creatureId)
        val granted = after.owned(claim.uid)!!
        assertEquals(1, granted.rarity)
        assertEquals(0, granted.xp)
        assertTrue(claim.claimId in after.inventory.applied, "ticket debit and outcome share one claim id")

        // The same species again is a separate creature with its own uid.
        val again = after.redeemTicket(ItemCatalog.TICKET_RARE) { 0 }.let { it }
        assertEquals(2, again.ticketClaims.map { it.uid }.toSet().size)
        assertEquals(0, again.inventory[ItemCatalog.TICKET_RARE])
        assertFailsWith<IllegalStateException> { again.redeemTicket(ItemCatalog.TICKET_RARE, testPool) { 0 } }
    }

    private fun BattleProgress.redeemTicket(id: String, roll: (Int) -> Int) = redeemTicket(id, testPool, roll)

    @Test
    fun outcomesSurviveTheSaveAndAreNeverRolledAgain() {
        val p = withTickets().redeemTicket(ItemCatalog.TICKET_LEGENDARY, testPool) { 95 % it }
        val back = BattleProgressCodec.decode(BattleProgressCodec.encode(p))
        assertEquals(p, back)
        assertEquals(0, back.ticketClaims.single().rarity, "roll 95 on a Legendary ticket is Common (9%)")
        // A claim that doesn't match a ledger transaction is rejected as corrupt.
        val forged = p.copy(ticketClaims = p.ticketClaims + p.ticketClaims.single().copy(claimId = "ticket-999"))
        assertFailsWith<IllegalStateException> { BattleProgressCodec.decode(BattleProgressCodec.encode(forged)) }
    }

    @Test
    fun noProductionPoolYet() {
        assertNull(TicketPools.approved, "D-TICKET-POOL is open; tickets stay unspent")
        assertTrue(Encounters.playable.none { BattleRewardsTicket(it.kind) }, "no ticket-paying encounter is playable yet")
    }

    private fun BattleRewardsTicket(kind: com.zeus97x.zbattle.core.battle.EncounterKind) =
        com.zeus97x.zbattle.core.battle.BattleRewards.ticket[kind] != null
}
