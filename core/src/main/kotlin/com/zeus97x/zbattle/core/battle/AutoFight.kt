package com.zeus97x.zbattle.core.battle

/** One automatic move: a turn action, or the free replacement after a faint. */
sealed interface AutoMove {
    data class Act(val action: BattleAction) : AutoMove
    data class Replace(val index: Int) : AutoMove
}

/**
 * Auto-fight policy (CLAUDE-005 B1). It only picks a move; the move is played through the same
 * [BattleProgress] path as a tapped button, so manual and auto battles share one rule set and one
 * settlement. Never uses items, never retreats and never switches voluntarily.
 *
 * Skill is always at least as strong as Attack (+9 vs +5, worst family modifier −2) and adds
 * Burn/Weaken, so the policy plays Skill whenever it is ready and Attack otherwise. After a faint it
 * sends in the next standing party member in party order (D-PARTY: this costs no turn).
 */
object AutoFight {
    fun choose(state: BattleState): AutoMove? = when {
        state.over -> null
        state.awaitingReplacement -> state.benchIndices().firstOrNull()?.let { AutoMove.Replace(it) }
        state.skillReady -> AutoMove.Act(BattleAction.Skill)
        else -> AutoMove.Act(BattleAction.Attack)
    }
}

/**
 * One auto-fight step, guarded so a late or duplicated timer can never act twice: it plays only
 * while the active battle is still exactly [expected] (same battle, turn, HP and active member).
 * Otherwise it returns the progress unchanged (no action after victory, no duplicate settlement).
 */
fun BattleProgress.autoStep(expected: BattleState): BattleProgress {
    val battle = active ?: return this
    if (battle != expected) return this
    return when (val move = AutoFight.choose(battle) ?: return this) {
        is AutoMove.Act -> act(move.action)
        is AutoMove.Replace -> replaceWith(move.index)
    }
}
