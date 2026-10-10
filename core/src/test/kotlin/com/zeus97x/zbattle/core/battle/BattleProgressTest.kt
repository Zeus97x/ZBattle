package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.InMemorySettingsStore
import com.zeus97x.zbattle.core.MasterGender
import com.zeus97x.zbattle.core.MasterStyle
import com.zeus97x.zbattle.core.PetMaster
import com.zeus97x.zbattle.core.PlayerSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BattleProgressTest {
    private val encounter = Encounters.playable.single()
    private fun fresh() = BattleProgress().withStarter("sparklit")

    private fun winOnce(p: BattleProgress): BattleProgress {
        var progress = p.startBattle(encounter)
        while (progress.active != null) {
            val a = progress.active!!
            progress = progress.act(if (a.skillReady) BattleAction.Skill else BattleAction.Attack)
        }
        return progress
    }

    @Test
    fun starterIsGrantedOnce() {
        val p = fresh()
        assertEquals(1, p.creatures.size)
        assertEquals(p, p.withStarter("inkling"))
        val settings = PlayerSettings(master = PetMaster("Zeus", MasterStyle.Mystic, MasterGender.Female, "cindlet")).withSeededStarter()
        assertEquals(listOf("cindlet"), settings.ownedParty.map { it.creatureId })
        assertEquals(settings, settings.withSeededStarter())
    }

    @Test
    fun firstVictoryPaysFullRewardsOnceAndRematchesPayAQuarter() {
        val won = winOnce(fresh())
        val result = assertNotNull(won.lastResult)
        assertEquals(Outcome.Victory, result.outcome)
        assertTrue(result.firstVictory)
        assertEquals(60, result.xpGained)
        assertEquals(60, won.lead!!.xp)
        assertEquals(20, result.coins)
        assertEquals(120, won.inventory.coins, "starter kit 100 + first win 20")
        assertTrue(encounter.id in won.defeated)
        assertNull(won.active)

        val rematch = winOnce(won.dismissResult())
        assertEquals(15, rematch.lastResult!!.xpGained)
        assertEquals(75, rematch.lead!!.xp)
        assertEquals(125, rematch.inventory.coins)
        assertEquals(2, rematch.wins[encounter.id])
    }

    @Test
    fun settledBattleCannotPayTwice() {
        val won = winOnce(fresh())
        // Re-opening the result or acting again cannot re-settle: there is no active battle.
        assertFailsWith<IllegalStateException> { won.act(BattleAction.Attack) }
        assertFailsWith<IllegalStateException> { won.retreat() }
        assertEquals(won.lead, won.dismissResult().lead)
    }

    @Test
    fun defeatAndRetreatGrantNothing() {
        val started = fresh().startBattle(encounter)
        val retreated = started.retreat()
        assertEquals(Outcome.Retreat, retreated.lastResult!!.outcome)
        assertEquals(0, retreated.lead!!.xp)
        assertTrue(retreated.defeated.isEmpty())

        val doomed = started.copy(active = started.active!!.withPlayer { it.copy(hp = 1) })
        val lost = doomed.act(BattleAction.Attack)
        assertEquals(Outcome.Defeat, lost.lastResult!!.outcome)
        assertEquals(0, lost.lead!!.xp)
    }

    @Test
    fun onlyOneBattleAtATime() {
        val started = fresh().startBattle(encounter)
        assertFailsWith<IllegalStateException> { started.startBattle(encounter) }
        assertEquals(2, started.nextBattleId)
    }

    @Test
    fun levelUpIsReported() {
        val near = fresh().copy(creatures = listOf(OwnedCreature(1, "sparklit", 70)))
        val won = winOnce(near)
        assertEquals(1, won.lastResult!!.gainFor(1)!!.levelBefore)
        assertEquals(2, won.lastResult!!.gainFor(1)!!.levelAfter)
    }

    @Test
    fun codecRoundTripsIncludingMidBattle() {
        val mid = fresh().startBattle(encounter).act(BattleAction.Skill).act(BattleAction.Attack)
        assertNotNull(mid.active)
        assertEquals(mid, BattleProgressCodec.decode(BattleProgressCodec.encode(mid)))
        val done = winOnce(fresh())
        assertEquals(done, BattleProgressCodec.decode(BattleProgressCodec.encode(done)))
        assertEquals(BattleProgress(), BattleProgressCodec.decode(BattleProgressCodec.encode(BattleProgress())))
    }

    @Test
    fun resumedBattleContinuesFromSavedTurn() {
        val mid = fresh().startBattle(encounter).act(BattleAction.Attack)
        val resumed = BattleProgressCodec.decode(BattleProgressCodec.encode(mid))
        assertEquals(mid.act(BattleAction.Attack), resumed.act(BattleAction.Attack))
    }

    @Test
    fun codecRejectsCorruptData() {
        val good = BattleProgressCodec.encode(winOnce(fresh()))
        assertFailsWith<IllegalStateException> { BattleProgressCodec.decode("not base64!") }
        assertFailsWith<IllegalStateException> { BattleProgressCodec.decode(good.dropLast(8)) }
        val bytes = java.util.Base64.getDecoder().decode(good)
        bytes[3] = 9 // version
        assertFailsWith<IllegalStateException> { BattleProgressCodec.decode(java.util.Base64.getEncoder().encodeToString(bytes)) }
    }

    @Test
    fun settingsStoreKeepsProgress() {
        val store = InMemorySettingsStore()
        val settings = PlayerSettings(progress = winOnce(fresh()))
        store.save(settings)
        assertEquals(60, store.load().progress.lead!!.xp)
    }
}
