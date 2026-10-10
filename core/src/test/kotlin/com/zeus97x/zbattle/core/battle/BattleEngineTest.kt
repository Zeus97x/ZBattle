package com.zeus97x.zbattle.core.battle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BattleEngineTest {
    private val encounter = Encounters.playable.single()

    private fun start(starter: String = "sparklit", level: Int = 1) = BattleEngine.start(1, encounter, 1, starter, level)

    @Test
    fun startUsesRealStats() {
        val b = start()
        assertEquals(52, b.player.maxHp)
        assertEquals(52, b.player.hp)
        assertEquals(30, b.enemy.maxHp)
        assertNull(b.outcome)
    }

    @Test
    fun attackIsDeterministicAndSlowerSideActsSecond() {
        val b = BattleEngine.act(start(), BattleAction.Attack)
        // Sparklit speed 4 < Voltmaw speed 5: opponent first. Hit = max(2, 4+5-4/2) = 7; retaliation = max(2, 5+4-4/2) = 7.
        assertEquals(30 - 7, b.enemy.hp)
        assertEquals(52 - 7, b.player.hp)
        assertEquals(1, b.turn)
        assertEquals(b, BattleEngine.act(start(), BattleAction.Attack))
    }

    @Test
    fun everyThirdTurnIsAHeavyStrike() {
        var b = start()
        repeat(2) { b = BattleEngine.act(b, BattleAction.Attack) }
        assertEquals("Heavy strike incoming", b.enemyIntent)
        val before = b.player.hp
        b = BattleEngine.act(b, BattleAction.Attack)
        assertEquals(7 + 5, before - b.player.hp)
    }

    @Test
    fun skillAppliesDamageEffectAndCooldown() {
        val b = BattleEngine.act(start(), BattleAction.Skill)
        // Thunder Fang: max(3, 4+9-2+0) = 11, then Burn 3.
        assertEquals(30 - 11 - 3, b.enemy.hp)
        assertEquals(BattleEngine.SKILL_COOLDOWN, b.skillCooldown)
        assertFalse(b.skillReady)
        assertFailsWith<IllegalStateException> { BattleEngine.act(b, BattleAction.Skill) }
        val after = BattleEngine.act(b, BattleAction.Attack)
        assertEquals(BattleEngine.SKILL_COOLDOWN - 1, after.skillCooldown)
    }

    @Test
    fun weakenReducesIncomingDamage() {
        val b = BattleEngine.act(start("inkling"), BattleAction.Skill)
        assertEquals(52 - (7 - 3), b.player.hp)
        assertEquals(BattleEngine.EFFECT_TURNS - 1, b.weakenTurns)
    }

    @Test
    fun familyAdvantageBoostsSkill() {
        val b = BattleEngine.act(start("cindlet"), BattleAction.Skill)
        // Flame family 2 beats family 0: 4+9-2+3 = 14, burn 3.
        assertEquals(30 - 14 - 3, b.enemy.hp)
    }

    @Test
    fun starterCanWinTheSliceFight() {
        for (starter in listOf("sparklit", "inkling", "cindlet")) {
            var b = start(starter)
            while (!b.over) b = BattleEngine.act(b, if (b.skillReady) BattleAction.Skill else BattleAction.Attack)
            assertEquals(Outcome.Victory, b.outcome, starter)
            assertTrue(b.player.hp > 0)
            assertFailsWith<IllegalStateException> { BattleEngine.act(b, BattleAction.Attack) }
        }
    }

    @Test
    fun faintingEndsInDefeat() {
        var b = start().let { it.copy(player = it.player.copy(hp = 3)) }
        b = BattleEngine.act(b, BattleAction.Attack)
        assertEquals(Outcome.Defeat, b.outcome)
        assertEquals(0, b.player.hp)
        assertEquals(30, b.enemy.hp) // the opponent struck first, so no hit landed
    }

    @Test
    fun turnLimitIsADefeat() {
        val b = start().let { it.copy(turn = BattleEngine.TURN_LIMIT - 1, enemy = it.enemy.copy(hp = 30, maxHp = 30), player = it.player.copy(hp = 52)) }
        assertEquals(Outcome.Defeat, BattleEngine.act(b, BattleAction.Attack).outcome)
    }

    @Test
    fun retreatEndsWithoutVictory() {
        val b = BattleEngine.retreat(start())
        assertEquals(Outcome.Retreat, b.outcome)
        assertFailsWith<IllegalStateException> { BattleEngine.retreat(b) }
    }
}
