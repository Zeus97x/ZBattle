package com.zeus97x.zbattle.core.battle

/**
 * Auto-fight policy (CLAUDE-005 B1). It only picks a move; the move is played through the same
 * [BattleEngine.act] / [BattleProgress.act] path as a tapped button, so manual and auto battles
 * share one rule set and one settlement. Never uses items, never retreats.
 *
 * Skill is always at least as strong as Attack (+9 vs +5, worst family modifier −2) and adds
 * Burn/Weaken, so the policy plays Skill whenever it is ready and Attack otherwise. A future
 * switch-aware policy plugs in here once party switching (B2) exists.
 */
object AutoFight {
    fun choose(state: BattleState): BattleAction? = when {
        state.over -> null
        state.skillReady -> BattleAction.Skill
        else -> BattleAction.Attack
    }
}

/**
 * One auto-fight step, guarded so a late or duplicated timer can never act twice: it plays only
 * while [battleId] is still the active battle and still on [turn]. Otherwise it returns the
 * progress unchanged (no action after victory, no duplicate settlement).
 */
fun BattleProgress.autoStep(battleId: Long, turn: Int): BattleProgress {
    val battle = active ?: return this
    if (battle.battleId != battleId || battle.turn != turn) return this
    val action = AutoFight.choose(battle) ?: return this
    return act(action)
}
