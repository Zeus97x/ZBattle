package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.economy.ItemCatalog
import com.zeus97x.zbattle.core.economy.Transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** CLAUDE-006 battle items (D-SHOP, ECONOMY §7.1). */
class BattleItemsTest {
    private val encounter = Encounters.playable.single()

    /** Starter kit (3 Potions) plus 2 of every other consumable, in a battle that has just started. */
    private fun inBattle(): BattleProgress {
        val p = BattleProgress().withStarter("cindlet")
        val stocked = p.inventory.applyOrThrow(Transaction("stock", mapOf("super-potion" to 2L, "ember-vial" to 2L, "sapping-dust" to 2L)))
        return p.copy(inventory = stocked).startBattle(encounter)
    }

    private fun BattleProgress.hurt(hp: Int) = copy(active = active!!.withPlayer { it.copy(hp = hp) })

    @Test
    fun aPotionUsesTheTurnHealsAndIsConsumedOnce() {
        val start = inBattle().act(BattleAction.Skill).hurt(10)
        val before = start.active!!
        val after = start.useItem(ItemCatalog.POTION)
        val b = after.active!!
        assertEquals(before.turn + 1, b.turn)
        assertEquals(before.enemy.hp - BattleEngine.EFFECT_AMOUNT, b.enemy.hp, "no item damage; only the running Burn ticks")
        assertEquals(before.skillCooldown - 1, b.skillCooldown, "cooldown ticks as on Attack")
        assertEquals(1, b.itemsUsed)
        // Voltmaw (speed 5) strikes before Cindlet heals: 10 - 7 + 25.
        assertEquals(10 - 7 + 25, b.player.hp)
        assertEquals(2, after.inventory[ItemCatalog.POTION])
        assertTrue("item-${b.battleId}-${b.turn}" in after.inventory.applied)
        assertTrue(b.playerUid in b.participants, "using an item counts as taking part")
    }

    @Test
    fun healingNeverExceedsMaxHpAndIsRefusedAtFullHp() {
        val p = inBattle()
        assertEquals("Already at full HP", p.itemRefusal(ItemCatalog.POTION))
        assertFailsWith<IllegalStateException> { p.useItem(ItemCatalog.POTION) }
        val healed = p.hurt(p.active!!.player.maxHp - 2).useItem("super-potion").active!!
        assertEquals(healed.player.maxHp - 7 + minOf(60, 7), healed.player.hp.coerceAtMost(healed.player.maxHp))
        assertTrue(healed.player.hp <= healed.player.maxHp)
    }

    @Test
    fun effectItemsRefreshRatherThanStack() {
        val burned = inBattle().useItem("ember-vial").active!!
        assertEquals(BattleEngine.EFFECT_TURNS - 1, burned.burnTurns, "applied, then ticks once this turn like the Skill")
        val again = inBattle().useItem("ember-vial").useItem("ember-vial").active!!
        assertEquals(BattleEngine.EFFECT_TURNS - 1, again.burnTurns)
        val weakened = inBattle().useItem("sapping-dust").active!!
        // Voltmaw's normal 7 is reduced by 3 on the same turn.
        assertEquals(weakened.player.maxHp - 4, weakened.player.hp)
    }

    @Test
    fun atMostFiveItemsPerBattleAndNoneWhenOutOfStock() {
        // A tough opponent so the battle outlasts five item turns.
        var p = inBattle().let { it.copy(inventory = it.inventory.applyOrThrow(Transaction("more", mapOf("ember-vial" to 10L)))) }
        p = p.copy(active = p.active!!.copy(enemy = p.active!!.enemy.copy(maxHp = 500, hp = 500)))
        repeat(BattleEngine.MAX_ITEMS_PER_BATTLE) { p = p.useItem("ember-vial") }
        assertEquals(BattleEngine.MAX_ITEMS_PER_BATTLE, p.active!!.itemsUsed)
        assertEquals("5 items already used this battle", p.itemRefusal("ember-vial"))
        assertEquals(12L - 5, p.inventory["ember-vial"])
        val empty = inBattle().let { it.copy(inventory = it.inventory.applyOrThrow(Transaction("drop", mapOf("sapping-dust" to -2L)))) }
        assertEquals("None left", empty.itemRefusal("sapping-dust"))
        assertEquals("Not a battle item", empty.itemRefusal(ItemCatalog.TICKET_RARE))
    }

    @Test
    fun anItemIsNotSpentWhenTheCreatureFaintsBeforeMoving() {
        val p = inBattle().hurt(1)
        val after = p.useItem(ItemCatalog.POTION)
        assertEquals(Outcome.Defeat, after.lastResult!!.outcome)
        assertEquals(3, after.inventory[ItemCatalog.POTION])
        assertNull(after.active)
    }

    @Test
    fun aBurnTickFromAnItemCanWinAndSettlesOnce() {
        val p = inBattle().let { it.copy(active = it.active!!.copy(enemy = it.active!!.enemy.copy(hp = 3))) }
        val won = p.useItem("ember-vial")
        assertEquals(Outcome.Victory, won.lastResult!!.outcome)
        assertEquals(1, won.inventory["ember-vial"])
        assertEquals(setOf(ItemCatalog.STARTER_KIT_TX, "stock", "item-${won.lastResult!!.battleId}-1", "battle-${won.lastResult!!.battleId}"), won.inventory.applied)
    }

    @Test
    fun itemsUsedSurvivesTheSaveAndAutoNeverUsesItems() {
        val p = inBattle().act(BattleAction.Skill).hurt(10).useItem(ItemCatalog.POTION)
        assertEquals(p, BattleProgressCodec.decode(BattleProgressCodec.encode(p)))
        assertEquals("zbattle-rules-3", BattleEngine.RULES_REVISION)
        // A whole auto battle leaves the inventory exactly as it was, apart from the reward transaction.
        var q = inBattle()
        val stock = q.inventory.balances - ItemCatalog.COINS
        while (q.active != null) q = q.autoStep(q.active!!)
        assertEquals(stock, q.inventory.balances - ItemCatalog.COINS)
    }
}
