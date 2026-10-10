package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.Creature
import com.zeus97x.zbattle.core.FormStage

/**
 * Provisional ZBattle stat model, baselined on ZPet (read-only reference, ZPet main 1adcedb):
 * - Form stats: `Progression.stats()` without training (Baby 4/4/4 … Branch A final 12/10/6,
 *   Branch B final 7/7/14 — the two finals trade power/guard against speed on an equal budget).
 * - Level: `Progression.level()` = 1 + xp/100, capped at 50.
 * - Max HP: `AdventureState.Battle` = 45 + level×3 + guard.
 * ZPet grows stats through step-funded training; ZBattle has no training yet, so it adds a small
 * per-level growth instead (documented, easy to rebalance). ZBattle progress never writes to ZPet.
 */
data class StatBlock(val maxHp: Int, val power: Int, val guard: Int, val speed: Int)

object Leveling {
    const val MAX_LEVEL = 50
    const val XP_PER_LEVEL = 100L

    fun levelFor(xp: Long): Int = (1 + xp.coerceAtLeast(0) / XP_PER_LEVEL).coerceAtMost(MAX_LEVEL.toLong()).toInt()

    /** XP earned inside the current level and needed for the next one (null at the cap). */
    fun progressInLevel(xp: Long): Pair<Long, Long>? =
        if (levelFor(xp) >= MAX_LEVEL) null else (xp.coerceAtLeast(0) % XP_PER_LEVEL) to XP_PER_LEVEL
}

object CreatureStats {
    /** ZPet `Progression.stats()` base values (power, guard, speed) by form index 0..5. */
    private val formBase: Map<FormStage, Triple<Int, Int, Int>> = mapOf(
        FormStage.Baby to Triple(4, 4, 4),
        FormStage.Young to Triple(6, 5, 7),
        FormStage.BranchAAdvanced to Triple(8, 7, 9),
        FormStage.BranchAFinal to Triple(12, 10, 6),
        FormStage.BranchBAdvanced to Triple(8, 7, 9),
        FormStage.BranchBFinal to Triple(7, 7, 14),
    )

    fun base(stage: FormStage): Triple<Int, Int, Int> = formBase.getValue(stage)

    /** Player creature stats at [level]. Growth: +1 power per 3 levels, +1 guard per 4, +1 speed per 5. */
    fun forCreature(creature: Creature, level: Int): StatBlock {
        val (p, g, s) = base(creature.stage)
        val l = level.coerceIn(1, Leveling.MAX_LEVEL)
        val power = p + (l - 1) / 3
        val guard = g + (l - 1) / 4
        val speed = s + (l - 1) / 5
        return StatBlock(maxHp = 45 + l * 3 + guard, power = power, guard = guard, speed = speed)
    }

    /**
     * Opponent stats from the area's stage within its region (ZPet `AdventureState.Battle`):
     * power 5+2·stage, guard 4+stage, speed 5+stage, HP 30+10·stage; bosses +4/+3/+0/+30.
     */
    fun forOpponent(areaStage: Int, boss: Boolean): StatBlock {
        val stage = areaStage.coerceIn(0, 3)
        return StatBlock(
            maxHp = 30 + stage * 10 + if (boss) 30 else 0,
            power = 5 + stage * 2 + if (boss) 4 else 0,
            guard = 4 + stage + if (boss) 3 else 0,
            speed = 5 + stage,
        )
    }

    /** Display level for opponents (ZPet has none); tracks area stage so the UI can compare. */
    fun opponentLevel(areaStage: Int, boss: Boolean): Int = 2 + areaStage.coerceIn(0, 3) * 3 + if (boss) 3 else 0
}

/** One signature skill per family, named as in ZPet `AdventureState.Battle.skillName()`. */
enum class SkillEffect(val label: String) {
    /** 3 extra damage at the end of each of the next 3 turns. */
    Burn("Burn"),
    /** Opponent hits for 3 less during the next 3 turns. */
    Weaken("Weaken"),
}

data class Skill(val name: String, val effect: SkillEffect)

object Skills {
    private val names = listOf(
        "Thunder Fang", "Rune Hex", "Celestial Flame", "Guardian Veil", "Foxfire", "Plume Gale",
        "Solar Pulse", "Tidal Crest", "Moon Barrier", "Dawn Rebirth", "Story Web", "Grove Thorns",
    )

    /** ZPet rule: family % 3 == 1 weakens, the others burn. */
    fun forFamily(familyIndex: Int): Skill =
        Skill(names[familyIndex], if (familyIndex % 3 == 1) SkillEffect.Weaken else SkillEffect.Burn)

    /** ZPet three-way family advantage: +3 when strong, −2 when weak, else 0. */
    fun advantage(attackerFamily: Int, defenderFamily: Int): Int = when {
        (attackerFamily % 3 + 1) % 3 == defenderFamily % 3 -> 3
        (defenderFamily % 3 + 1) % 3 == attackerFamily % 3 -> -2
        else -> 0
    }
}
