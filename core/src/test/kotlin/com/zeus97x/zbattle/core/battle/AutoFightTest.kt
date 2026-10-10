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
    private fun runAuto(start: BattleProgress): Pair<BattleProgress, List<AutoMove>> {
        var p = start
        val moves = mutableListOf<AutoMove>()
        while (p.active != null) {
            val battle = p.active!!
            moves += AutoFight.choose(battle)!!
            p = p.autoStep(battle)
        }
        return p to moves
    }

    private fun BattleProgress.play(move: AutoMove) = when (move) {
        is AutoMove.Act -> act(move.action)
        is AutoMove.Replace -> replaceWith(move.index)
    }

    @Test
    fun policyOnlyPicksLegalMoves() {
        var b = fresh().active!!
        while (!b.over) {
            val move = AutoFight.choose(b) as AutoMove.Act
            if (move.action == BattleAction.Skill) assertTrue(b.skillReady)
            if (!b.skillReady) assertEquals(BattleAction.Attack, move.action)
            b = BattleEngine.act(b, move.action)
        }
        assertNull(AutoFight.choose(b))
    }

    @Test
    fun autoAndManualWithSameMovesGiveIdenticalResultAndRewards() {
        for (starter in listOf("sparklit", "inkling", "cindlet")) {
            val (auto, moves) = runAuto(fresh(starter))
            var manual = fresh(starter)
            moves.forEach { manual = manual.play(it) }
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
        val once = start.autoStep(battle)
        // Same timer firing again for the old state: no second action.
        assertSame(once, once.autoStep(battle))
        // A different battle: ignored.
        assertSame(start, start.autoStep(battle.copy(battleId = battle.battleId + 1)))
    }

    @Test
    fun noActionAfterSettlement() {
        val start = fresh()
        val firstState = start.active!!
        val (done, _) = runAuto(start)
        assertNull(done.active)
        assertSame(done, done.autoStep(firstState))
    }

    @Test
    fun manualTakeOverMidFightContinuesSameBattle() {
        var p = fresh()
        val turn0 = p.active!!
        p = p.autoStep(turn0)
        val turn1 = p.active!!
        p = p.act(BattleAction.Attack) // the player takes control
        assertEquals(2, p.active!!.turn)
        // A late auto timer for turn 1 must not act on top of the manual move.
        assertSame(p, p.autoStep(turn1))
    }

    @Test
    fun autoSendsInTheNextStandingMemberAfterAFaint() {
        val party = BattleProgress().withStarter("sparklit")
            .let { it.copy(creatures = it.creatures + OwnedCreature(2, "inkling", 0) + OwnedCreature(3, "cindlet", 0), nextUid = 4) }
            .startBattle(encounter)
        val knocked = party.copy(active = party.active!!.withPlayer { it.copy(hp = 1) })
        var p = knocked.autoStep(knocked.active!!)
        val b = p.active!!
        assertTrue(b.awaitingReplacement)
        assertEquals(AutoMove.Replace(1), AutoFight.choose(b))
        p = p.autoStep(b)
        assertEquals(1, p.active!!.activeIndex)
        assertEquals(b.turn, p.active!!.turn, "replacement costs no turn")
    }
}
