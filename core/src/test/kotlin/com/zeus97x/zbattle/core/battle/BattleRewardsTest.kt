package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.economy.ItemCatalog
import com.zeus97x.zbattle.core.economy.Transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** CLAUDE-006 C2 (decision batch 2): what each kind pays, and how settlement credits it. */
class BattleRewardsTest {
    private val shipped = Encounters.playable.single()

    private fun encounter(kind: EncounterKind) = Encounter(
        RegionCatalog.area(3), 0, CreatureCatalog.require("voltmaw"), kind != EncounterKind.Wild,
        BattleRewards.firstWinXp.getValue(kind), kind,
    )

    private fun win(p: BattleProgress): BattleProgress {
        var q = p.dismissResult().startBattle(shipped)
        while (q.active != null) q = q.act(if (q.active!!.skillReady) BattleAction.Skill else BattleAction.Attack)
        return q
    }

    @Test
    fun ticketsComeOnlyFromFirstClearsOfMiniStageAndRegionBosses() {
        val tickets = EncounterKind.entries.associateWith { BattleRewards.firstWin(encounter(it)).ticketItemId }
        assertEquals(
            mapOf(
                EncounterKind.Wild to null, EncounterKind.MiniBoss to ItemCatalog.TICKET_RARE, EncounterKind.StageBoss to ItemCatalog.TICKET_EPIC,
                EncounterKind.LocationBoss to null, EncounterKind.RegionBoss to ItemCatalog.TICKET_LEGENDARY,
            ),
            tickets,
        )
        EncounterKind.entries.forEach { assertNull(BattleRewards.replay(encounter(it)).ticketItemId, "$it replay never pays a ticket") }
    }

    @Test
    fun replayIsAQuarterRoundedDownBeforeSharing() {
        // Region boss: 150 XP / 250 coins -> 37 / 62.
        assertEquals(Payout(37, 62, null), BattleRewards.replay(encounter(EncounterKind.RegionBoss)))
        // Shipped wild keeps 60 XP: replay 15 XP, 5 coins.
        assertEquals(Payout(60, 20, null), BattleRewards.firstWin(shipped))
        assertEquals(Payout(15, 5, null), BattleRewards.replay(shipped))
        // 7 replay XP shared by 3 participants: 2 / 2 / 3, the remainder to the creature on the field.
        assertEquals(mapOf(1L to 2L, 2L to 2L, 3L to 3L), PartyXp.share(7, listOf(1, 2, 3), finisher = 3))
    }

    @Test
    fun aFullWalletIsCreditedUpToItsCapAndTheBattleStillSettles() {
        val start = BattleProgress().withStarter("cindlet").let {
            it.copy(inventory = it.inventory.applyOrThrow(Transaction("fill", mapOf(ItemCatalog.COINS to ItemCatalog.COIN_CAP - 100 - 7))))
        }
        val won = win(start)
        assertEquals(7, won.lastResult!!.coins)
        assertEquals(ItemCatalog.COIN_CAP, won.inventory.coins)
        val again = win(won)
        assertEquals(0, again.lastResult!!.coins)
        assertEquals(15, again.lastResult!!.xpGained, "XP is still paid")
    }

    @Test
    fun rewardsAreOneTransactionPerBattleAndSurviveTheSave() {
        val won = win(BattleProgress().withStarter("cindlet"))
        val id = "battle-${won.lastResult!!.battleId}"
        assertEquals(setOf(ItemCatalog.STARTER_KIT_TX, id), won.inventory.applied)
        assertEquals(won, BattleProgressCodec.decode(BattleProgressCodec.encode(won)))
        // A defeat or retreat pays nothing and records no transaction.
        val retreated = won.dismissResult().startBattle(shipped).retreat()
        assertEquals(won.inventory, retreated.inventory)
        assertEquals(0, retreated.lastResult!!.coins)
    }
}
