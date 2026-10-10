package com.zeus97x.zbattle.core.battle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** CLAUDE-005 B5: replay tracking, duplicate-safe settlement and repeat sessions (D-REPLAY-REWARDS). */
class ReplayTest {
    private val encounter = Encounters.playable.single()

    private fun win(p: BattleProgress): BattleProgress {
        var q = p.dismissResult().startBattle(encounter)
        while (q.active != null) q = q.act(if (q.active!!.skillReady) BattleAction.Skill else BattleAction.Attack)
        return q
    }

    @Test
    fun replaysAreTrackedAndPayAQuarter() {
        val first = win(BattleProgress().withStarter("cindlet"))
        assertTrue(first.lastResult!!.firstVictory)
        assertFalse(first.lastResult!!.replay)
        val second = win(first)
        assertTrue(second.lastResult!!.replay)
        // D-REPLAY-RATE: 25% of the shipped encounter's 60 XP and 20 coins, rounded down.
        assertEquals(15, second.lastResult!!.xpGained)
        assertEquals(5, second.lastResult!!.coins)
        assertEquals(null, second.lastResult!!.ticket)
        assertEquals(2, second.wins[encounter.id])
        assertEquals(setOf(encounter.id), second.defeated, "first-clear claimed once")
    }

    @Test
    fun eachSeparateEncounterStartsAtFullHpAndAResumedBattleKeepsItsHp() {
        val first = win(BattleProgress().withStarter("cindlet"))
        val next = first.dismissResult().startBattle(encounter).active!!
        assertTrue(next.team.all { it.combatant.hp == it.combatant.maxHp })
        val mid = first.dismissResult().startBattle(encounter).act(BattleAction.Attack)
        val reopened = BattleProgressCodec.decode(BattleProgressCodec.encode(mid))
        assertEquals(mid.active!!.player.hp, reopened.active!!.player.hp)
        assertTrue(reopened.active!!.player.hp < reopened.active!!.player.maxHp)
    }

    @Test
    fun aSettledBattleIdCanNeverPayAgain() {
        val start = BattleProgress().withStarter("cindlet").startBattle(encounter)
        val old = start.active!!
        var p = start
        while (p.active != null) p = p.act(if (p.active!!.skillReady) BattleAction.Skill else BattleAction.Attack)
        assertEquals(old.battleId, p.settledThrough)
        // Restoring the old in-progress battle (e.g. from a backup copy) must not settle again.
        val restored = p.copy(active = old)
        assertFailsWith<IllegalStateException> {
            var q = restored
            while (q.active != null) q = q.act(if (q.active!!.skillReady) BattleAction.Skill else BattleAction.Attack)
        }
    }

    @Test
    fun settlementMarkerSurvivesSavesAndOldSavesDeriveIt() {
        val p = win(win(BattleProgress().withStarter("cindlet")))
        assertEquals(p, BattleProgressCodec.decode(BattleProgressCodec.encode(p)))
        assertEquals(p.nextBattleId - 1, p.settledThrough)
        val bad = p.copy(settledThrough = p.nextBattleId)
        assertFailsWith<IllegalStateException> { BattleProgressCodec.decode(BattleProgressCodec.encode(bad)) }
    }

    @Test
    fun repeatSessionStopsOnTargetDefeatOrInterruption() {
        fun result(outcome: Outcome, xp: Long = 0) =
            BattleResult(1, encounter.id, outcome, firstVictory = false, gains = if (xp > 0) listOf(XpGain(1, xp, 1, 1)) else emptyList())
        var s = RepeatSession(encounter.id, target = 3)
        s = s.record(result(Outcome.Victory, 5)).record(result(Outcome.Victory))
        assertTrue(s.running)
        s = s.record(result(Outcome.Victory))
        assertEquals(RepeatEnd.Completed, s.end)
        assertEquals(Triple(3, 3, 5L), Triple(s.played, s.wins, s.xp))
        assertEquals(s, s.record(result(Outcome.Victory)), "nothing recorded after the end")

        val lost = RepeatSession(encounter.id, 5).record(result(Outcome.Victory)).record(result(Outcome.Defeat))
        assertEquals(RepeatEnd.Defeat, lost.end)
        assertEquals(RepeatEnd.Defeat, RepeatSession(encounter.id, 5).record(result(Outcome.Retreat)).end)
        assertEquals(RepeatEnd.Interrupted, RepeatSession(encounter.id, 5).interrupted().end)
        assertEquals(RepeatSession(encounter.id, 5), RepeatSession(encounter.id, 5).record(result(Outcome.Victory).copy(encounterId = "area-01/slot-0")))
        assertFailsWith<IllegalArgumentException> { RepeatSession(encounter.id, RepeatSession.MAX_BATTLES + 1) }
        assertFailsWith<IllegalArgumentException> { RepeatSession(encounter.id, 0) }
    }
}
