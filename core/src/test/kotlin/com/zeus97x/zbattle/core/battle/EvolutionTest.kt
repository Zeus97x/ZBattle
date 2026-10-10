package com.zeus97x.zbattle.core.battle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** CLAUDE-005 B3: evolution identity and unlock ledger (D-EVOLUTION, decided 2026-10-10). */
class EvolutionTest {
    private val nativeId = "00000000-0000-4000-8000-0000000c0002"
    private val importedId = "00000000-0000-4000-8000-0000000c0001"

    /** uid 1: native Cindlet (form 0). uid 2: imported ZPet Runebeak (Rune raven form 1, Heroic), nicknamed. */
    private fun owner() = BattleProgress(
        creatures = listOf(
            OwnedCreature(1, "cindlet", 150, companionId = nativeId),
            OwnedCreature(2, "runebeak", 40, companionId = importedId, rarity = 1, nickname = "Hugin", origin = CompanionOrigin.ZPet, sourceRevision = 3),
        ),
        nextUid = 3,
    )

    private fun unlock(id: String, companion: String, form: Int, source: UnlockSource, validated: Boolean = true) =
        EvolutionUnlock(id, companion, form, source, validated)

    @Test
    fun formGraphFollowsZPetBranches() {
        assertEquals(listOf(Branch.None, Branch.None, Branch.A, Branch.A, Branch.B, Branch.B), (0..5).map(FormGraph::branchOf))
        assertEquals(listOf(1, 2, 3, 4, 3, 4), (0..5).map(FormGraph::stage))
        assertTrue(FormGraph.reachable(0, 5))
        assertTrue(FormGraph.reachable(1, 3))
        assertFalse(FormGraph.reachable(2, 5), "branch A cannot become branch B")
        assertFalse(FormGraph.reachable(3, 4))
        assertFalse(FormGraph.reachable(2, 2))
    }

    @Test
    fun evolvingKeepsIdentityRarityNicknameAndXp() {
        val (p, r) = owner().applyUnlock(unlock("e1", importedId, 2, UnlockSource.ZPetCare))
        assertEquals(UnlockResult.Applied, r)
        val c = p.owned(2)!!
        assertEquals("glyphwing", c.creatureId)
        assertEquals(Branch.A, c.branch)
        assertEquals(listOf(importedId, "1:1", "Hugin", 40L, CompanionOrigin.ZPet), listOf(c.companionId, c.speciesId, c.nickname, c.xp, c.origin))
        assertEquals("1:1", owner().owned(2)!!.speciesId, "species unchanged by evolution")
        assertTrue(c.stats.power > owner().owned(2)!!.stats.power, "battle stats follow the new form")
    }

    @Test
    fun onlyValidatedNewEventsApplyAndNothingDowngrades() {
        val base = owner()
        assertEquals(UnlockResult.NotValidated, base.applyUnlock(unlock("e1", nativeId, 1, UnlockSource.ZBattleCombat, validated = false)).second)
        val (once, r1) = base.applyUnlock(unlock("e1", nativeId, 1, UnlockSource.ZBattleCombat))
        assertEquals(UnlockResult.Applied, r1)
        assertEquals(UnlockResult.Duplicate, once.applyUnlock(unlock("e1", nativeId, 1, UnlockSource.ZBattleCombat)).second)
        assertEquals(UnlockResult.NoDowngrade, once.applyUnlock(unlock("e2", nativeId, 0, UnlockSource.ZBattleCombat)).second)
        assertEquals(UnlockResult.UnknownCompanion, once.applyUnlock(unlock("e3", "00000000-0000-4000-8000-0000000c0009", 1, UnlockSource.ZBattleCombat)).second)
        val branchA = once.applyUnlock(unlock("e4", nativeId, 2, UnlockSource.ZBattleCombat)).first
        assertEquals(UnlockResult.IllegalForm, branchA.applyUnlock(unlock("e5", nativeId, 5, UnlockSource.ZBattleCombat)).second)
        assertEquals(UnlockResult.WrongAuthority, base.applyUnlock(unlock("e6", nativeId, 1, UnlockSource.ZPetCare)).second)
    }

    @Test
    fun staleSnapshotsNeverDowngradeImportedForms() {
        val evolved = owner().applyFormSnapshot(importedId, 4, sourceRevision = 4)
        assertEquals("ironquill", evolved.owned(2)!!.creatureId)
        assertEquals(evolved, evolved.applyFormSnapshot(importedId, 1, sourceRevision = 4), "same revision ignored")
        assertEquals(evolved, evolved.applyFormSnapshot(importedId, 5, sourceRevision = 2), "older revision ignored")
        val lower = evolved.applyFormSnapshot(importedId, 1, sourceRevision = 9)
        assertEquals("ironquill", lower.owned(2)!!.creatureId, "a newer snapshot with a lower form keeps the form")
        assertEquals(9, lower.owned(2)!!.sourceRevision)
        assertEquals(owner(), owner().applyFormSnapshot(nativeId, 3, sourceRevision = 99), "native forms are not ZPet's to set")
    }

    @Test
    fun zbattleUnlockForAnImportedCompanionIsQueuedNotApplied() {
        val (p, r) = owner().applyUnlock(unlock("e1", importedId, 2, UnlockSource.ZBattleChallenge))
        assertEquals(UnlockResult.PendingDelivery, r)
        assertEquals("runebeak", p.owned(2)!!.creatureId, "imported form preserved until delivery exists")
        assertEquals(listOf("e1"), p.evolution.outbound.map { it.eventId })
        assertEquals(UnlockResult.Duplicate, p.applyUnlock(unlock("e1", importedId, 2, UnlockSource.ZBattleChallenge)).second)
    }

    @Test
    fun challengeGatedFinalStageIsClaimedByOnlyOneGame() {
        val atAdvanced = owner().applyUnlock(unlock("a", importedId, 2, UnlockSource.ZPetCare)).first
        val (zbattleClaim, r1) = atAdvanced.applyUnlock(unlock("b", importedId, 3, UnlockSource.ZBattleChallenge))
        assertEquals(UnlockResult.PendingDelivery, r1)
        assertEquals(UnlockResult.ChallengeAlreadyClaimed, zbattleClaim.applyUnlock(unlock("c", importedId, 3, UnlockSource.ZPetChallenge)).second)

        val (zpetClaim, r2) = atAdvanced.applyUnlock(unlock("d", importedId, 3, UnlockSource.ZPetChallenge))
        assertEquals(UnlockResult.Applied, r2)
        assertEquals("oracrow", zpetClaim.owned(2)!!.creatureId)
        assertEquals(listOf(ChallengeClaim(importedId, 4, UnlockSource.ZPetChallenge)), zpetClaim.evolution.claims)
    }

    @Test
    fun combatThresholdsAreInactiveUntilApproved() {
        val native = owner().owned(1)!!
        assertEquals(null, EvolutionRules.combatLevelThresholds)
        assertEquals(emptyList(), EvolutionRules.availableCombatEvolutions(native))
        // The mechanism works once values are supplied (these numbers are test-only, not proposals).
        assertEquals(listOf(1), EvolutionRules.availableCombatEvolutions(native, thresholds = mapOf(1 to 2)))
        assertEquals(emptyList(), EvolutionRules.availableCombatEvolutions(owner().owned(2)!!, thresholds = mapOf(2 to 1)), "imported forms stay ZPet's")
    }

    @Test
    fun companionIdsAreAssignedOnceAndIdentityIsValidated() {
        val bare = BattleProgress().withStarter("cindlet")
        var n = 0
        val ids = { "00000000-0000-4000-8000-%012d".format(++n) }
        val assigned = bare.withCompanionIds(ids)
        assertEquals("00000000-0000-4000-8000-000000000001", assigned.owned(1)!!.companionId)
        assertEquals(assigned, assigned.withCompanionIds(ids), "never reassigned")
        assertEquals("2:0", assigned.owned(1)!!.speciesId, "D-NATIVE-SPECIES: native starter = family Common")
        assertFailsWith<IllegalArgumentException> { OwnedCreature(1, "cindlet", 0, companionId = "NOT-A-UUID") }
        assertFailsWith<IllegalArgumentException> { OwnedCreature(1, "cindlet", 0, rarity = 4) }
        assertFailsWith<IllegalArgumentException> { OwnedCreature(1, "cindlet", 0, nickname = "") }
    }

    @Test
    fun identityAndLedgerSurviveTheSave() {
        val p = owner().applyUnlock(unlock("e1", importedId, 2, UnlockSource.ZBattleChallenge)).first
            .applyUnlock(unlock("e2", nativeId, 1, UnlockSource.ZBattleCombat)).first
        assertEquals(p, BattleProgressCodec.decode(BattleProgressCodec.encode(p)))
        val dup = p.copy(creatures = p.creatures.map { it.copy(companionId = nativeId) })
        assertFailsWith<IllegalStateException> { BattleProgressCodec.decode(BattleProgressCodec.encode(dup)) }
    }
}
