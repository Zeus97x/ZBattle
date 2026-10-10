package com.zeus97x.zbattle.core.battle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AutoFightTest {
    private val encounter = Encounters.playable.single()

    private fun fresh(starter: String = "sparklit") = BattleProgress().withStarter(starter).startBattle(encounter)

    /** Runs auto steps until the battle settles; returns the final progress and the moves played. */
    private fun runAuto(start: BattleProgress): Pair<BattleProgress, List<BattleAction>> {
        var p = start
        val moves = mutableListOf<BattleAction>()
        while (p.active != null) {
            val battle = p.active!!
            moves += AutoFight.choose(battle)!!
            p = p.autoStep(battle.battleId, battle.turn)
        }
        return p to moves
    }

    @Test
    fun policyOnlyPicksLegalMoves() {
        var b = fresh().active!!
        while (!b.over) {
            val move = AutoFight.choose(b)!!
            if (move == BattleAction.Skill) assertTrue(b.skillReady)
            if (!b.skillReady) assertEquals(BattleAction.Attack, move)
            b = BattleEngine.act(b, move)
        }
        assertNull(AutoFight.choose(b))
    }

    @Test
    fun autoAndManualWithSameMovesGiveIdenticalResultAndRewards() {
        for (starter in listOf("sparklit", "inkling", "cindlet")) {
            val (auto, moves) = runAuto(fresh(starter))
            var manual = fresh(starter)
            moves.forEach { manual = manual.act(it) }
            assertEquals(manual, auto, "auto/manual diverged for $starter")
            assertNotNull(auto.lastResult)
        }
    }

    @Test
    fun autoFightIsDeterministicAndEndsWithinTurnLimit() {
        val a = runAuto(fresh()).first
        val b = runAuto(fresh()).first
        assertEquals(a, b)
        assertTrue(a.lastResult!!.outcome != Outcome.Retreat)
    }

    @Test
    fun staleOrDuplicateStepsAreIgnored() {
        val start = fresh()
        val battle = start.active!!
        val once = start.autoStep(battle.battleId, battle.turn)
        // Same timer firing again for the old turn: no second action.
        assertSame(once, once.autoStep(battle.battleId, battle.turn))
        // Wrong battle id: ignored.
        assertSame(start, start.autoStep(battle.battleId + 1, battle.turn))
    }

    @Test
    fun noActionAfterSettlement() {
        val (done, _) = runAuto(fresh())
        val settledId = done.lastResult!!.battleId
        assertNull(done.active)
        assertSame(done, done.autoStep(settledId, 0))
        assertEquals(done.wins, done.autoStep(settledId, 5).wins)
    }

    @Test
    fun manualTakeOverMidFightContinuesSameBattle() {
        var p = fresh()
        val id = p.active!!.battleId
        p = p.autoStep(id, 0)
        p = p.act(BattleAction.Attack) // the player takes control
        assertEquals(2, p.active!!.turn)
        // A late auto timer for turn 1 must not act on top of the manual move.
        assertSame(p, p.autoStep(id, 1))
    }
}
