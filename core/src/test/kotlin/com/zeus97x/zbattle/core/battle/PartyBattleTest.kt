package com.zeus97x.zbattle.core.battle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** CLAUDE-005 B2: party of 3, switching and fainted replacement (D-PARTY, decided 2026-10-10). */
class PartyBattleTest {
    private val encounter = Encounters.playable.single()

    /** Sparklit (uid 1), Inkling (uid 2), Cindlet (uid 3), all level 1. */
    private fun owner() = BattleProgress().withStarter("sparklit")
        .let { it.copy(creatures = it.creatures + OwnedCreature(2, "inkling", 0) + OwnedCreature(3, "cindlet", 0), nextUid = 4) }

    private fun battle() = owner().startBattle(encounter)

    @Test
    fun partyIsThreeLeadFirstAndAllStartAtFullHp() {
        val b = battle().active!!
        assertEquals(listOf(1L, 2L, 3L), b.team.map { it.uid })
        assertEquals(0, b.activeIndex)
        assertTrue(b.team.all { it.combatant.hp == it.combatant.maxHp })
        val four = owner().copy(creatures = owner().creatures + OwnedCreature(4, "voltmaw", 0), nextUid = 5)
        assertEquals(3, four.startBattle(encounter).active!!.team.size)
    }

    @Test
    fun switchingUsesTheTurnAndTheOpponentHitsOnlyOnce() {
        val start = battle().active!!
        val switched = BattleEngine.switch(start, 1)
        assertEquals(1, switched.turn)
        assertEquals(1, switched.activeIndex)
        // Retaliation on Inkling (guard 4): max(2, 5 + 4 - 4/2) = 7, exactly one normal hit.
        assertEquals(switched.player.maxHp - 7, switched.player.hp)
        assertEquals(start.enemy.hp, switched.enemy.hp, "no player damage on a switch turn")
        assertEquals(start.team[0], switched.team[0], "outgoing creature untouched")
        assertEquals(setOf(2L), switched.participants)
    }

    @Test
    fun switchOnTheThirdTurnTakesTheNormalHeavyStrikeOnly() {
        var b = battle().active!!
        repeat(2) { b = BattleEngine.act(b, BattleAction.Attack) }
        val before = b.team[2].combatant.hp
        b = BattleEngine.switch(b, 2)
        // Cindlet guard 4: 7 + 5 heavy = 12.
        assertEquals(before - 12, b.player.hp)
    }

    @Test
    fun illegalSwitchesAreRejected() {
        val b = battle().active!!
        assertFailsWith<IllegalStateException> { BattleEngine.switch(b, 0) }
        assertFailsWith<IllegalStateException> { BattleEngine.switch(b, 5) }
        val benchFainted = b.copy(team = b.team.mapIndexed { i, m -> if (i == 1) m.copy(combatant = m.combatant.copy(hp = 0)) else m })
        assertFailsWith<IllegalStateException> { BattleEngine.switch(benchFainted, 1) }
        assertFailsWith<IllegalStateException> { BattleEngine.replace(b, 1) }
        val solo = BattleProgress().withStarter("sparklit").startBattle(encounter).active!!
        assertFalse(solo.canSwitch)
    }

    @Test
    fun faintedReplacementIsFreeAndDefeatNeedsEveryoneDown() {
        var b = battle().active!!.withPlayer { it.copy(hp = 1) }
        b = BattleEngine.act(b, BattleAction.Attack) // Voltmaw is faster and knocks Sparklit out first
        assertTrue(b.awaitingReplacement)
        assertNull(b.outcome)
        assertFalse(1L in b.participants, "knocked out before acting = did not participate")
        assertFailsWith<IllegalStateException> { BattleEngine.act(b, BattleAction.Attack) }
        assertFailsWith<IllegalStateException> { BattleEngine.switch(b, 1) }
        val replaced = BattleEngine.replace(b, 2)
        assertEquals(b.turn, replaced.turn)
        assertEquals(b.enemy, replaced.enemy)
        assertEquals(replaced.team[2].combatant.maxHp, replaced.player.hp, "no free hit on the replacement")
        assertFalse(replaced.awaitingReplacement)

        val lastStanding = replaced.copy(team = replaced.team.mapIndexed { i, m -> if (i == 1) m.copy(combatant = m.combatant.copy(hp = 0)) else m })
            .withPlayer { it.copy(hp = 1) }
        val end = BattleEngine.act(lastStanding, BattleAction.Attack)
        assertEquals(Outcome.Defeat, end.outcome)
        assertFalse(end.awaitingReplacement)
    }

    @Test
    fun skillCooldownStaysWithTheCreatureWhileBenched() {
        var b = BattleEngine.act(battle().active!!, BattleAction.Skill)
        assertEquals(BattleEngine.SKILL_COOLDOWN, b.skillCooldown)
        b = BattleEngine.switch(b, 1)
        assertEquals(0, b.skillCooldown, "Inkling's own skill is ready")
        b = BattleEngine.switch(b, 0)
        assertEquals(BattleEngine.SKILL_COOLDOWN, b.skillCooldown, "Sparklit's cooldown did not tick while benched")
    }

    @Test
    fun firstWinXpIsSplitBetweenActualParticipantsOnly() {
        var p = battle()
        p = p.switchTo(1)
        while (p.active != null) p = p.act(if (p.active!!.skillReady) BattleAction.Skill else BattleAction.Attack)
        val result = p.lastResult!!
        assertEquals(Outcome.Victory, result.outcome)
        assertEquals(listOf(2L), result.gains.map { it.uid }, "Sparklit never acted, Cindlet never entered")
        assertEquals(60, result.xpGained)
        assertEquals(60, p.owned(2)!!.xp)
        assertEquals(0, p.owned(1)!!.xp)
        assertEquals(0, p.owned(3)!!.xp)
    }

    @Test
    fun xpShareRules() {
        assertEquals(mapOf(1L to 66L, 2L to 66L, 3L to 68L), PartyXp.share(200, listOf(1, 2, 3), finisher = 3))
        assertEquals(mapOf(1L to 30L, 2L to 30L), PartyXp.share(60, listOf(1, 2), finisher = 2))
        assertEquals(mapOf(5L to 60L), PartyXp.share(60, listOf(5), finisher = 5))
        assertEquals(mapOf(1L to 60L, 2L to 60L), PartyXp.share(60, listOf(1, 2), finisher = 2, rule = PartyXpRule.EachFull))
        assertEquals(emptyMap(), PartyXp.share(0, listOf(1), finisher = 1))
        assertEquals(PartyXpRule.SplitEvenly, PartyXp.rule, "proposal default until D-PARTY-XP is decided")
    }

    @Test
    fun partySelectionIsCappedNeverEmptyAndLockedInBattle() {
        val four = owner().copy(creatures = owner().creatures + OwnedCreature(4, "voltmaw", 0), nextUid = 5)
        assertEquals(listOf(1L, 2L, 3L), four.partyMembers.map { it.uid })
        assertEquals(listOf(1L, 2L, 3L), four.toggleParty(4).partyMembers.map { it.uid }, "full party: ignored")
        val swapped = four.toggleParty(2).toggleParty(4)
        assertEquals(listOf(1L, 3L, 4L), swapped.partyMembers.map { it.uid })
        assertEquals(listOf(4L, 1L, 3L), swapped.makeLead(4).partyMembers.map { it.uid })
        val solo = BattleProgress().withStarter("sparklit")
        assertEquals(solo.partyMembers, solo.toggleParty(1).partyMembers, "the last member can't be removed")
        assertFailsWith<IllegalStateException> { battle().toggleParty(2) }
    }

    @Test
    fun partyBattleSurvivesSaveAndReload() {
        var p = battle().switchTo(2)
        p = p.copy(active = p.active!!.withPlayer { it.copy(hp = 1) })
        p = p.act(BattleAction.Attack)
        assertTrue(p.active!!.awaitingReplacement)
        val reloaded = BattleProgressCodec.decode(BattleProgressCodec.encode(p))
        assertEquals(p, reloaded)
    }
}
