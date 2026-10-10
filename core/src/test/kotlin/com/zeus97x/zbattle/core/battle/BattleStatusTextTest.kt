package com.zeus97x.zbattle.core.battle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** EXT-020: the plain-language battle status must agree with what the engine actually does next. */
class BattleStatusTextTest {
    private val encounter = Encounters.playable.single()

    /** A long fight: both sides have plenty of HP, so effects and cooldowns run their full course. */
    private fun longFight(starter: String): BattleState =
        BattleEngine.start(1, encounter, 1, starter, 1)
            .withPlayer { it.copy(maxHp = 999, hp = 999) }
            .copy(enemy = BattleEngine.start(1, encounter, 1, starter, 1).enemy.let { it.copy(maxHp = 999, hp = 999) })

    @Test
    fun skillStatusCountsTheAttackTurnsUntilTheSkillIsUsable() {
        var b = BattleEngine.act(longFight("sparklit"), BattleAction.Skill)
        assertEquals("Ready after ${BattleEngine.SKILL_COOLDOWN} more turns", b.skillStatus)
        var attacks = 0
        while (!b.skillReady) {
            val promised = Regex("""Ready after (\d+) more turns?""").matchEntire(b.skillStatus)!!.groupValues[1].toInt()
            assertEquals(BattleEngine.SKILL_COOLDOWN - attacks, promised)
            b = BattleEngine.act(b, BattleAction.Attack)
            attacks++
        }
        assertEquals(BattleEngine.SKILL_COOLDOWN, attacks, "the promised count is exactly how many turns it took")
        assertEquals("Ready", b.skillStatus)
    }

    @Test
    fun activeEffectsShowTurnsLeftAndClearWhenTheEffectEnds() {
        for (starter in listOf("sparklit", "inkling")) {
            val start = longFight(starter)
            val effect = start.skill.effect
            var b = BattleEngine.act(start, BattleAction.Skill)
            val seen = mutableListOf<String>()
            while (b.activeEffects.isNotEmpty()) {
                seen += b.activeEffects.single()
                b = BattleEngine.act(b, BattleAction.Attack)
            }
            assertTrue(seen.isNotEmpty(), "$starter's ${effect.label} should be shown")
            assertTrue(seen.all { it.startsWith("${effect.label} · ") })
            // Each turn removes exactly one: "2 turns left", "1 turn left", then nothing.
            val counts = seen.map { it.substringAfter(" · ").substringBefore(" ").toInt() }
            assertEquals((counts.first() downTo 1).toList(), counts)
            assertEquals("1 turn left", seen.last().substringAfter(" · "))
        }
    }

    @Test
    fun effectDescriptionsUseTheEngineValues() {
        val amount = BattleEngine.EFFECT_AMOUNT.toString()
        val turns = BattleEngine.EFFECT_TURNS.toString()
        SkillEffect.entries.forEach { assertTrue(amount in it.description && turns in it.description, it.description) }
    }
}
