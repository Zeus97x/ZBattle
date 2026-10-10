package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.economy.Transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** CLAUDE-006 equipment (ECONOMY §7.2): one charm slot per creature, flat bonus, changed between battles. */
class EquipmentTest {
    private val encounter = Encounters.playable.single()

    private fun withCharms(): BattleProgress = BattleProgress().withStarter("cindlet").let {
        it.copy(inventory = it.inventory.applyOrThrow(Transaction("bag", mapOf("fang-charm-1" to 1L, "fang-charm-2" to 1L, "heart-charm-1" to 1L))))
    }

    @Test
    fun aCharmMovesFromTheBagToTheSlotAndBack() {
        val p = withCharms()
        val uid = p.lead!!.uid
        val one = p.equip(uid, "fang-charm-1")
        assertEquals("fang-charm-1", one.lead!!.equipment)
        assertEquals(0, one.inventory["fang-charm-1"])
        // Tier II replaces tier I; tier I goes back to the bag (no trade-in, no loss).
        val two = one.equip(uid, "fang-charm-2")
        assertEquals("fang-charm-2", two.lead!!.equipment)
        assertEquals(1, two.inventory["fang-charm-1"])
        assertEquals(0, two.inventory["fang-charm-2"])
        val none = two.equip(uid, null)
        assertNull(none.lead!!.equipment)
        assertEquals(1, none.inventory["fang-charm-2"])
        assertEquals(none, none.equip(uid, null), "removing from an empty slot changes nothing")
    }

    @Test
    fun theBonusIsAppliedOnceToBattleStats() {
        val p = withCharms()
        val uid = p.lead!!.uid
        val base = p.lead!!.stats
        val hearty = p.equip(uid, "heart-charm-1")
        assertEquals(base.maxHp + 10, hearty.lead!!.stats.maxHp)
        assertEquals(base.power, hearty.lead!!.stats.power)
        val fighter = hearty.startBattle(encounter).active!!.player
        assertEquals(base.maxHp + 10, fighter.maxHp)
        assertEquals(fighter.maxHp, fighter.hp, "starts at the boosted full HP")
        val strong = p.equip(uid, "fang-charm-2").startBattle(encounter).active!!.player
        assertEquals(base.power + 2, strong.power)
    }

    @Test
    fun charmsAreFixedDuringBattleAndNeedToBeOwned() {
        val p = withCharms()
        val uid = p.lead!!.uid
        assertFailsWith<IllegalStateException> { p.startBattle(encounter).equip(uid, "fang-charm-1") }
        assertFailsWith<IllegalStateException> { p.equip(uid, "shell-charm-1") }
        assertFailsWith<IllegalStateException> { p.equip(uid, "potion") }
    }

    @Test
    fun equipmentSurvivesTheSave() {
        val p = withCharms().let { it.equip(it.lead!!.uid, "heart-charm-1") }
        assertEquals(p, BattleProgressCodec.decode(BattleProgressCodec.encode(p)))
    }
}
