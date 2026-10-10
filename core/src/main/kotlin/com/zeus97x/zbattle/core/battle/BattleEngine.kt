package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.economy.ItemEffect
import com.zeus97x.zbattle.core.economy.StatBonus

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

/** One party member inside a battle. Skill cooldown belongs to the creature (D-SWITCH-COOLDOWN proposal). */
data class TeamMember(val uid: Long, val combatant: Combatant, val skillCooldown: Int = 0)

/**
 * A running battle. Pure data: every move returns a new state, so it can be saved after each
 * turn and resumed after the app is closed.
 *
 * Party battles (CLAUDE-005 B2, D-PARTY): up to [BattleEngine.PARTY_SIZE] members, one active.
 */
data class BattleState(
    /** Unique per battle; settlement is accepted only once per id. */
    val battleId: Long,
    val encounterId: String,
    val team: List<TeamMember>,
    val activeIndex: Int,
    val enemy: Combatant,
    val turn: Int = 0,
    val burnTurns: Int = 0,
    val weakenTurns: Int = 0,
    /** Uids that attacked, used a skill or were switched in during a resolved turn (D-PARTICIPATION). */
    val participants: Set<Long> = emptySet(),
    /** The active creature fainted and others can still fight: the player must pick a replacement (free). */
    val awaitingReplacement: Boolean = false,
    /** Battle items used (CLAUDE-006, D-SHOP: at most 5 per battle). */
    val itemsUsed: Int = 0,
    val log: List<String> = emptyList(),
    val outcome: Outcome? = null,
) {
    init {
        require(team.size in 1..BattleEngine.PARTY_SIZE) { "Party must have 1..${BattleEngine.PARTY_SIZE} members" }
        require(activeIndex in team.indices) { "Invalid active member" }
        require(team.map { it.uid }.toSet().size == team.size) { "Duplicate party member" }
    }

    val active: TeamMember get() = team[activeIndex]
    val player: Combatant get() = active.combatant
    val playerUid: Long get() = active.uid
    val skillCooldown: Int get() = active.skillCooldown
    val over: Boolean get() = outcome != null
    val skillReady: Boolean get() = skillCooldown == 0 && !over && !awaitingReplacement
    val skill: Skill get() = Skills.forFamily(player.creature.family.index)

    /** Members that may be brought in now (alive and not already active). */
    fun benchIndices(): List<Int> = team.indices.filter { it != activeIndex && !team[it].combatant.fainted }
    val canSwitch: Boolean get() = !over && !awaitingReplacement && benchIndices().isNotEmpty()

    /** ZPet telegraph: every third turn the opponent lands a heavy strike. */
    val enemyIntent: String get() = if ((turn + 1) % 3 == 0) "Heavy strike incoming" else "Steady strike"

    /** Skill readiness in plain words, so the cooldown number is never the only cue (EXT-020). */
    val skillStatus: String get() = when (skillCooldown) {
        0 -> "Ready"
        1 -> "Ready after 1 more turn"
        else -> "Ready after $skillCooldown more turns"
    }

    /** Battle items used so far; at most [BattleEngine.MAX_ITEMS_PER_BATTLE] (D-SHOP). */
    val itemsLeft: Int get() = BattleEngine.MAX_ITEMS_PER_BATTLE - itemsUsed

    /** Effects still running on the opponent, with the turns left; empty when none (EXT-020). */
    val activeEffects: List<String> get() = buildList {
        if (burnTurns > 0) add("${SkillEffect.Burn.label} · ${turnsLeft(burnTurns)}")
        if (weakenTurns > 0) add("${SkillEffect.Weaken.label} · ${turnsLeft(weakenTurns)}")
    }

    private fun turnsLeft(n: Int) = if (n == 1) "1 turn left" else "$n turns left"
}

/** What a Skill effect does, worded from the engine's own constants (EXT-020). */
val SkillEffect.description: String get() = when (this) {
    SkillEffect.Burn -> "${BattleEngine.EFFECT_AMOUNT} damage at the end of each of the next ${BattleEngine.EFFECT_TURNS} turns"
    SkillEffect.Weaken -> "the opponent hits for ${BattleEngine.EFFECT_AMOUNT} less on its next ${BattleEngine.EFFECT_TURNS} strikes"
}

/**
 * Deterministic turn rules (ZPet `AdventureState.Battle.move`, minus Guard):
 * - Attack: max(2, power + 5 − enemyGuard/2).
 * - Skill: max(3, power + 9 − enemyGuard/2 + family advantage), then 3-turn cooldown and Burn/Weaken.
 * - Opponent: max(2, power + 4 − guard/2), +5 on every third turn, −3 while weakened.
 * - The faster side acts first; ties go to the player. Burn ticks after the player's hit.
 * - 50-turn limit counts as a defeat.
 * Party rules (D-PARTY, decided 2026-10-10):
 * - Switch: uses the player's turn; the opponent then takes its normal turn against the incoming
 *   creature (no extra free hit). Burn still ticks at the end of that turn.
 * - When the active creature faints and another can fight, the player picks a replacement. That
 *   choice costs no turn and the opponent does not act. Defeat only when every member has fainted.
 * - Skill cooldown is per creature and only counts down on that creature's own Attack turns
 *   (D-SWITCH-COOLDOWN, confirmed in batch 2; the single-creature behaviour is unchanged).
 * Battle items (D-SHOP, batch 2; ECONOMY §7.1):
 * - Using an item takes the player's turn and deals no damage; the opponent still strikes and the
 *   Skill cooldown ticks down as on Attack (ZPet Potion parity). At most 5 items per battle.
 * - Heal restores up to max HP and can't be used at full HP. Burn/Weaken items set the effect to its
 *   full length (refresh, never stack), exactly like the Skill effect.
 * - A creature knocked out before it moves uses no item ([BattleState.itemsUsed] doesn't change).
 * - Auto-fight never uses items.
 */
object BattleEngine {
    const val TURN_LIMIT = 50
    const val SKILL_COOLDOWN = 3
    const val EFFECT_TURNS = 3
    const val EFFECT_AMOUNT = 3
    const val PARTY_SIZE = 3
    const val MAX_ITEMS_PER_BATTLE = 5

    /**
     * Contract `rulesRevision` for BattleCompleted records (CONTRACT-v0.2). Bump whenever combat rules
     * change: 1 = CLAUDE-002 single fighter, 2 = CLAUDE-005 B2 party of 3 with switching,
     * 3 = CLAUDE-006 battle items.
     */
    const val RULES_REVISION = "zbattle-rules-3"

    /** A fighter entering battle at full HP (every separate encounter starts fresh, D-REPLAY-REWARDS). */
    data class Entrant(val uid: Long, val creatureId: String, val level: Int, val bonus: StatBonus? = null)

    fun start(battleId: Long, encounter: Encounter, playerUid: Long, playerCreatureId: String, playerLevel: Int): BattleState =
        start(battleId, encounter, listOf(Entrant(playerUid, playerCreatureId, playerLevel)))

    fun start(battleId: Long, encounter: Encounter, party: List<Entrant>): BattleState {
        val team = party.map { m ->
            val creature = CreatureCatalog.require(m.creatureId)
            val p = CreatureStats.forCreature(creature, m.level) + m.bonus
            TeamMember(m.uid, Combatant(creature.id, m.level, p.maxHp, p.maxHp, p.power, p.guard, p.speed))
        }
        val e = encounter.stats
        return BattleState(
            battleId = battleId,
            encounterId = encounter.id,
            team = team,
            activeIndex = 0,
            enemy = Combatant(encounter.creature.id, encounter.level, e.maxHp, e.maxHp, e.power, e.guard, e.speed),
            log = listOf("${encounter.label} appeared!"),
        )
    }

    fun act(state: BattleState, action: BattleAction): BattleState {
        check(!state.over) { "Battle is over" }
        if (action == BattleAction.Skill) check(state.skillCooldown == 0) { "Skill is cooling down" }
        return resolve(state, action, null)
    }

    /** Why [effect] can't be used right now, or null when it can. */
    fun itemRefusal(state: BattleState, effect: ItemEffect): String? = when {
        state.over || state.awaitingReplacement -> "Not now"
        state.itemsUsed >= MAX_ITEMS_PER_BATTLE -> "$MAX_ITEMS_PER_BATTLE items already used this battle"
        effect is ItemEffect.Heal && state.player.hp >= state.player.maxHp -> "Already at full HP"
        else -> null
    }

    /** Uses one battle item named [itemName]. Costs the turn; see the class notes. */
    fun useItem(state: BattleState, effect: ItemEffect, itemName: String): BattleState {
        itemRefusal(state, effect)?.let { error(it) }
        return resolve(state, BattleAction.Attack, effect to itemName)
    }

    /** One player turn: Attack, Skill, or (when [item] is set) a battle item. */
    private fun resolve(state: BattleState, action: BattleAction, item: Pair<ItemEffect, String>?): BattleState {
        check(!state.over) { "Battle is over" }
        check(!state.awaitingReplacement) { "Choose a replacement first" }

        val turn = state.turn + 1
        val player = state.player
        val enemy = state.enemy
        val skill = state.skill
        val lines = mutableListOf<String>()

        var burn = state.burnTurns
        var weaken = state.weakenTurns
        val hit: Int
        val cooldown: Int
        if (item != null) {
            hit = 0
            cooldown = maxOf(0, state.skillCooldown - 1)
            when (item.first) {
                ItemEffect.ApplyBurn -> burn = EFFECT_TURNS
                ItemEffect.ApplyWeaken -> weaken = EFFECT_TURNS
                is ItemEffect.Heal -> Unit
            }
        } else if (action == BattleAction.Skill) {
            val advantage = Skills.advantage(player.creature.family.index, enemy.creature.family.index)
            hit = maxOf(3, player.power + 9 - enemy.guard / 2 + advantage)
            cooldown = SKILL_COOLDOWN
            if (skill.effect == SkillEffect.Burn) burn = EFFECT_TURNS else weaken = EFFECT_TURNS
        } else {
            hit = maxOf(2, player.power + 5 - enemy.guard / 2)
            cooldown = maxOf(0, state.skillCooldown - 1)
        }

        val (retaliation, weakenAfter) = retaliation(enemy, player, turn, weaken)
        weaken = weakenAfter

        var playerHp = player.hp
        var enemyHp = enemy.hp
        val enemyFirst = enemy.speed > player.speed
        val moveName = if (action == BattleAction.Skill) skill.name else "Attack"

        fun enemyStrikes() {
            playerHp = maxOf(0, playerHp - retaliation)
            lines += "${enemy.creature.name} hits for $retaliation."
        }

        if (enemyFirst) enemyStrikes()
        val acted = playerHp > 0
        if (acted && item != null) {
            val (effect, name) = item
            when (effect) {
                is ItemEffect.Heal -> {
                    val healed = minOf(effect.hp, player.maxHp - playerHp)
                    playerHp += healed
                    lines += "${player.creature.name} uses $name: +$healed HP."
                }
                ItemEffect.ApplyBurn -> lines += "${player.creature.name} uses $name. ${enemy.creature.name} is affected by ${SkillEffect.Burn.label}."
                ItemEffect.ApplyWeaken -> lines += "${player.creature.name} uses $name. ${enemy.creature.name} is affected by ${SkillEffect.Weaken.label}."
            }
        } else if (acted) {
            enemyHp = maxOf(0, enemyHp - hit)
            lines += "${player.creature.name} uses $moveName for $hit."
            if (action == BattleAction.Skill) lines += "${enemy.creature.name} is affected by ${skill.effect.label}."
        }
        if (acted) {
            if (burn > 0 && enemyHp > 0) {
                enemyHp = maxOf(0, enemyHp - EFFECT_AMOUNT)
                burn--
                lines += "Burn deals $EFFECT_AMOUNT."
            }
            if (!enemyFirst && enemyHp > 0) enemyStrikes()
        }

        // A creature knocked out before it moves spends no cooldown and applies no effect (matters
        // once a replacement keeps the battle going; single-creature battles end here anyway).
        val finalCooldown = if (acted) cooldown else state.skillCooldown
        if (!acted) {
            burn = state.burnTurns
            weaken = maxOf(0, state.weakenTurns - 1)
        }
        val team = state.team.toMutableList()
        team[state.activeIndex] = state.active.copy(combatant = player.copy(hp = playerHp), skillCooldown = finalCooldown)
        val used = if (acted && item != null) state.copy(itemsUsed = state.itemsUsed + 1) else state
        return finishTurn(used, team, turn, enemy.copy(hp = enemyHp), burn, weaken, lines, if (acted) state.participants + state.playerUid else state.participants)
    }

    /** Brings in bench member [toIndex]. Costs the player's turn; the opponent takes its normal turn. */
    fun switch(state: BattleState, toIndex: Int): BattleState {
        check(!state.over) { "Battle is over" }
        check(!state.awaitingReplacement) { "Choose a replacement instead" }
        check(toIndex in state.benchIndices()) { "That creature cannot switch in" }
        val turn = state.turn + 1
        val incoming = state.team[toIndex]
        val lines = mutableListOf("${state.player.creature.name} swaps out for ${incoming.combatant.creature.name}.")
        val (hit, weaken) = retaliation(state.enemy, incoming.combatant, turn, state.weakenTurns)
        val incomingHp = maxOf(0, incoming.combatant.hp - hit)
        lines += "${state.enemy.creature.name} hits for $hit."
        var enemyHp = state.enemy.hp
        var burn = state.burnTurns
        if (burn > 0) {
            enemyHp = maxOf(0, enemyHp - EFFECT_AMOUNT)
            burn--
            lines += "Burn deals $EFFECT_AMOUNT."
        }
        val team = state.team.toMutableList()
        team[toIndex] = incoming.copy(combatant = incoming.combatant.copy(hp = incomingHp))
        val switched = state.copy(activeIndex = toIndex)
        return finishTurn(switched, team, turn, state.enemy.copy(hp = enemyHp), burn, weaken, lines, state.participants + incoming.uid)
    }

    /** Replaces a fainted active creature. No turn passes and the opponent does not act. */
    fun replace(state: BattleState, toIndex: Int): BattleState {
        check(!state.over) { "Battle is over" }
        check(state.awaitingReplacement) { "Nothing to replace" }
        check(toIndex in state.benchIndices()) { "That creature cannot come in" }
        val name = state.team[toIndex].combatant.creature.name
        return state.copy(activeIndex = toIndex, awaitingReplacement = false, log = (state.log + "Go, $name!").takeLast(LOG_LIMIT))
    }

    private fun retaliation(enemy: Combatant, target: Combatant, turn: Int, weakenTurns: Int): Pair<Int, Int> {
        var hit = maxOf(2, enemy.power + 4 - target.guard / 2)
        if (turn % 3 == 0) hit += 5
        var weaken = weakenTurns
        if (weaken > 0) {
            hit = maxOf(1, hit - EFFECT_AMOUNT)
            weaken--
        }
        return hit to weaken
    }

    private fun finishTurn(
        state: BattleState,
        team: List<TeamMember>,
        turn: Int,
        enemy: Combatant,
        burn: Int,
        weaken: Int,
        lines: MutableList<String>,
        participants: Set<Long>,
    ): BattleState {
        val active = team[state.activeIndex].combatant
        val anyStanding = team.any { !it.combatant.fainted }
        val outcome = when {
            enemy.hp <= 0 -> Outcome.Victory
            !anyStanding -> Outcome.Defeat
            turn >= TURN_LIMIT -> Outcome.Defeat.also { lines += "Turn limit reached." }
            else -> null
        }
        if (active.fainted) lines += "${active.creature.name} fainted."
        if (outcome == Outcome.Victory) lines += "${enemy.creature.name} fainted. You win!"
        val needsReplacement = outcome == null && active.fainted
        if (needsReplacement) lines += "Choose who fights next."
        return state.copy(
            team = team,
            turn = turn,
            enemy = enemy,
            burnTurns = burn,
            weakenTurns = weaken,
            participants = participants,
            awaitingReplacement = needsReplacement,
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
