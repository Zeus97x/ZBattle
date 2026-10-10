package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.CreatureCatalog

enum class BattleAction { Attack, Skill }

enum class Outcome { Victory, Defeat, Retreat }

data class Combatant(
    val creatureId: String,
    val level: Int,
    val maxHp: Int,
    val hp: Int,
    val power: Int,
    val guard: Int,
    val speed: Int,
) {
    init {
        require(maxHp > 0 && hp in 0..maxHp) { "Invalid HP" }
    }

    val creature get() = CreatureCatalog.require(creatureId)
    val fainted: Boolean get() = hp <= 0
}

/**
 * A running battle. Pure data: every move returns a new state, so it can be saved after each
 * turn and resumed after the app is closed.
 */
data class BattleState(
    /** Unique per battle; settlement is accepted only once per id. */
    val battleId: Long,
    val encounterId: String,
    /** Owned-creature uid fighting for the player. */
    val playerUid: Long,
    val player: Combatant,
    val enemy: Combatant,
    val turn: Int = 0,
    /** Turns until Skill is ready again (0 = ready). */
    val skillCooldown: Int = 0,
    val burnTurns: Int = 0,
    val weakenTurns: Int = 0,
    val log: List<String> = emptyList(),
    val outcome: Outcome? = null,
) {
    val over: Boolean get() = outcome != null
    val skillReady: Boolean get() = skillCooldown == 0 && !over
    val skill: Skill get() = Skills.forFamily(player.creature.family.index)

    /** ZPet telegraph: every third turn the opponent lands a heavy strike. */
    val enemyIntent: String get() = if ((turn + 1) % 3 == 0) "Heavy strike incoming" else "Steady strike"
}

/**
 * Deterministic turn rules (ZPet `AdventureState.Battle.move`, minus Guard/Potion which ZBattle
 * has not approved yet):
 * - Attack: max(2, power + 5 − enemyGuard/2).
 * - Skill: max(3, power + 9 − enemyGuard/2 + family advantage), then 3-turn cooldown and Burn/Weaken.
 * - Opponent: max(2, power + 4 − guard/2), +5 on every third turn, −3 while weakened.
 * - The faster side acts first; ties go to the player. Burn ticks after the player's hit.
 * - 50-turn limit counts as a defeat.
 */
object BattleEngine {
    const val TURN_LIMIT = 50
    const val SKILL_COOLDOWN = 3
    const val EFFECT_TURNS = 3
    const val EFFECT_AMOUNT = 3

    fun start(battleId: Long, encounter: Encounter, playerUid: Long, playerCreatureId: String, playerLevel: Int): BattleState {
        val creature = CreatureCatalog.require(playerCreatureId)
        val p = CreatureStats.forCreature(creature, playerLevel)
        val e = encounter.stats
        return BattleState(
            battleId = battleId,
            encounterId = encounter.id,
            playerUid = playerUid,
            player = Combatant(creature.id, playerLevel, p.maxHp, p.maxHp, p.power, p.guard, p.speed),
            enemy = Combatant(encounter.creature.id, encounter.level, e.maxHp, e.maxHp, e.power, e.guard, e.speed),
            log = listOf("${encounter.label} appeared!"),
        )
    }

    fun act(state: BattleState, action: BattleAction): BattleState {
        check(!state.over) { "Battle is over" }
        if (action == BattleAction.Skill) check(state.skillCooldown == 0) { "Skill is cooling down" }

        val turn = state.turn + 1
        val player = state.player
        val enemy = state.enemy
        val skill = state.skill
        val lines = mutableListOf<String>()

        var burn = state.burnTurns
        var weaken = state.weakenTurns
        val hit: Int
        val cooldown: Int
        if (action == BattleAction.Skill) {
            val advantage = Skills.advantage(player.creature.family.index, enemy.creature.family.index)
            hit = maxOf(3, player.power + 9 - enemy.guard / 2 + advantage)
            cooldown = SKILL_COOLDOWN
            if (skill.effect == SkillEffect.Burn) burn = EFFECT_TURNS else weaken = EFFECT_TURNS
        } else {
            hit = maxOf(2, player.power + 5 - enemy.guard / 2)
            cooldown = maxOf(0, state.skillCooldown - 1)
        }

        var retaliation = maxOf(2, enemy.power + 4 - player.guard / 2)
        if (turn % 3 == 0) retaliation += 5
        if (weaken > 0) {
            retaliation = maxOf(1, retaliation - EFFECT_AMOUNT)
            weaken--
        }

        var playerHp = player.hp
        var enemyHp = enemy.hp
        val enemyFirst = enemy.speed > player.speed
        val moveName = if (action == BattleAction.Skill) skill.name else "Attack"

        fun enemyStrikes() {
            playerHp = maxOf(0, playerHp - retaliation)
            lines += "${enemy.creature.name} hits for $retaliation."
        }

        if (enemyFirst) enemyStrikes()
        if (playerHp > 0) {
            enemyHp = maxOf(0, enemyHp - hit)
            lines += "${player.creature.name} uses $moveName for $hit."
            if (action == BattleAction.Skill) lines += "${enemy.creature.name} is affected by ${skill.effect.label}."
            if (burn > 0 && enemyHp > 0) {
                enemyHp = maxOf(0, enemyHp - EFFECT_AMOUNT)
                burn--
                lines += "Burn deals $EFFECT_AMOUNT."
            }
            if (!enemyFirst && enemyHp > 0) enemyStrikes()
        }

        val outcome = when {
            enemyHp <= 0 -> Outcome.Victory
            playerHp <= 0 -> Outcome.Defeat
            turn >= TURN_LIMIT -> Outcome.Defeat.also { lines += "Turn limit reached." }
            else -> null
        }
        when (outcome) {
            Outcome.Victory -> lines += "${enemy.creature.name} fainted. You win!"
            Outcome.Defeat -> if (playerHp <= 0) lines += "${player.creature.name} fainted."
            else -> Unit
        }

        return state.copy(
            turn = turn,
            player = player.copy(hp = playerHp),
            enemy = enemy.copy(hp = enemyHp),
            skillCooldown = cooldown,
            burnTurns = burn,
            weakenTurns = weaken,
            log = (state.log + "Turn $turn").plus(lines).takeLast(LOG_LIMIT),
            outcome = outcome,
        )
    }

    fun retreat(state: BattleState): BattleState {
        check(!state.over) { "Battle is over" }
        return state.copy(outcome = Outcome.Retreat, log = (state.log + "You retreated.").takeLast(LOG_LIMIT))
    }

    private const val LOG_LIMIT = 12
}
