package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.Area
import com.zeus97x.zbattle.core.CreatureCatalog

/** A creature the player owns in ZBattle. Separate from ZPet; never written back. */
data class OwnedCreature(val uid: Long, val creatureId: String, val xp: Long) {
    init {
        require(uid > 0 && xp >= 0) { "Invalid owned creature" }
        require(CreatureCatalog.byId(creatureId) != null) { "Unknown creature $creatureId" }
    }

    val creature get() = CreatureCatalog.require(creatureId)
    val level: Int get() = Leveling.levelFor(xp)
    val stats: StatBlock get() = CreatureStats.forCreature(creature, level)
}

data class BattleResult(
    val battleId: Long,
    val encounterId: String,
    val outcome: Outcome,
    val xpGained: Long,
    val levelBefore: Int,
    val levelAfter: Int,
    /** True when this victory was the first against the encounter (the only one that pays XP). */
    val firstVictory: Boolean,
)

/**
 * Saved battle progress (schema v1, see [BattleProgressCodec]).
 *
 * Mid-battle recovery: the active battle is saved after every turn and RESUMED when the app
 * reopens. Rewards are applied in the same state change that ends the battle and only while
 * [active] still holds that battle id, so reopening a result can never pay twice.
 */
data class BattleProgress(
    val creatures: List<OwnedCreature> = emptyList(),
    val nextUid: Long = 1,
    val nextBattleId: Long = 1,
    /** Encounters whose first-victory reward has been claimed. */
    val defeated: Set<String> = emptySet(),
    /** Total victories per encounter, rematches included. */
    val wins: Map<String, Int> = emptyMap(),
    val active: BattleState? = null,
    /** Last settled battle, shown on the results screen until dismissed. */
    val lastResult: BattleResult? = null,
) {
    val lead: OwnedCreature? get() = creatures.firstOrNull()

    fun owned(uid: Long): OwnedCreature? = creatures.firstOrNull { it.uid == uid }

    fun ownsSpecies(creatureId: String): OwnedCreature? = creatures.firstOrNull { it.creatureId == creatureId }

    fun winsIn(area: Area): Int = wins.filterKeys { it.startsWith("${area.id}/") }.values.sum()

    /** Grants the chosen starter once; existing saves keep their creatures. */
    fun withStarter(creatureId: String): BattleProgress =
        if (creatures.isNotEmpty()) this
        else copy(creatures = listOf(OwnedCreature(nextUid, creatureId, 0)), nextUid = nextUid + 1)

    fun startBattle(encounter: Encounter): BattleProgress {
        check(active == null) { "Finish or retreat from the current battle first" }
        val fighter = checkNotNull(lead) { "No companion to battle with" }
        val battle = BattleEngine.start(nextBattleId, encounter, fighter.uid, fighter.creatureId, fighter.level)
        return copy(active = battle, nextBattleId = nextBattleId + 1, lastResult = null)
    }

    fun act(action: BattleAction): BattleProgress {
        val battle = checkNotNull(active) { "No active battle" }
        val next = BattleEngine.act(battle, action)
        return if (next.over) settle(next) else copy(active = next)
    }

    fun retreat(): BattleProgress = settle(BattleEngine.retreat(checkNotNull(active) { "No active battle" }))

    fun dismissResult(): BattleProgress = copy(lastResult = null)

    private fun settle(finished: BattleState): BattleProgress {
        val outcome = checkNotNull(finished.outcome)
        // Idempotency guard: only the battle currently held as active can be settled.
        check(active?.battleId == finished.battleId) { "Battle already settled" }
        val encounter = Encounters.byId(finished.encounterId)
        val fighter = owned(finished.playerUid)
        val firstVictory = outcome == Outcome.Victory && finished.encounterId !in defeated
        val xp = if (firstVictory) encounter?.firstWinXp ?: 0 else 0
        val before = fighter?.level ?: finished.player.level
        val updated = creatures.map { if (it.uid == finished.playerUid) it.copy(xp = it.xp + xp) else it }
        val after = updated.firstOrNull { it.uid == finished.playerUid }?.level ?: before
        return copy(
            creatures = updated,
            defeated = if (firstVictory) defeated + finished.encounterId else defeated,
            wins = if (outcome == Outcome.Victory) wins + (finished.encounterId to (wins[finished.encounterId] ?: 0) + 1) else wins,
            active = null,
            lastResult = BattleResult(finished.battleId, finished.encounterId, outcome, xp, before, after, firstVictory),
        )
    }
}
