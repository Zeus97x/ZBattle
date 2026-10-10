package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.economy.ItemCatalog

/** What one settled victory pays before XP is shared. */
data class Payout(val xp: Long, val coins: Long, val ticketItemId: String?) {
    companion object {
        val NONE = Payout(0, 0, null)
    }
}

/**
 * Battle rewards (CLAUDE-006 C2; decision batch 2, 2026-10-10).
 *
 * - First win, by kind: XP 20/40/80/120/150 and coins 20/50/80/120/250 (D-ECONOMY-XP). The shipped
 *   `area-00/slot-0` keeps its 60 XP ([Encounter.firstWinXp]).
 * - First-clear tickets (Q9, D-LOCATION-TICKET): mini boss Rare, stage boss Epic, region boss
 *   Legendary; none for wild or location bosses. Never on replay.
 * - Replay (D-REPLAY-RATE): 25% of the first-win XP and coins, rounded down, before XP is shared.
 * - Each separate encounter starts at full HP; reopening an active battle keeps its saved HP.
 * - Settlement is duplicate-safe: battle ids only grow and each id settles at most once
 *   ([BattleProgress.settledThrough]).
 */
object BattleRewards {
    val firstWinXp: Map<EncounterKind, Long> = mapOf(
        EncounterKind.Wild to 20, EncounterKind.MiniBoss to 40, EncounterKind.StageBoss to 80,
        EncounterKind.LocationBoss to 120, EncounterKind.RegionBoss to 150,
    )
    val firstWinCoins: Map<EncounterKind, Long> = mapOf(
        EncounterKind.Wild to 20, EncounterKind.MiniBoss to 50, EncounterKind.StageBoss to 80,
        EncounterKind.LocationBoss to 120, EncounterKind.RegionBoss to 250,
    )
    val ticket: Map<EncounterKind, String> = mapOf(
        EncounterKind.MiniBoss to ItemCatalog.TICKET_RARE,
        EncounterKind.StageBoss to ItemCatalog.TICKET_EPIC,
        EncounterKind.RegionBoss to ItemCatalog.TICKET_LEGENDARY,
    )
    const val REPLAY_PERCENT = 25L

    /** ECONOMY Q-E4 (daily replay coin cap, 300) was not answered: kept inactive. */
    val dailyReplayCoinCap: Long? = null

    fun firstWin(encounter: Encounter): Payout =
        Payout(encounter.firstWinXp, firstWinCoins.getValue(encounter.kind), ticket[encounter.kind])

    fun replay(encounter: Encounter): Payout = firstWin(encounter).let {
        Payout(it.xp * REPLAY_PERCENT / 100, it.coins * REPLAY_PERCENT / 100, null)
    }
}

enum class RepeatEnd { Completed, Defeat, Interrupted }

/**
 * A bounded run of back-to-back replays of one already-cleared encounter, played by auto-fight in
 * the foreground. Not saved: after an interruption the player restarts it explicitly.
 */
data class RepeatSession(
    val encounterId: String,
    val target: Int,
    val played: Int = 0,
    val wins: Int = 0,
    val xp: Long = 0,
    val coins: Long = 0,
    val end: RepeatEnd? = null,
) {
    init {
        require(target in 1..MAX_BATTLES) { "Repeat count must be 1..$MAX_BATTLES" }
    }

    val running: Boolean get() = end == null

    /** Records one settled battle; a defeat or retreat ends the session, as does reaching [target]. */
    fun record(result: BattleResult): RepeatSession {
        if (!running || result.encounterId != encounterId) return this
        val won = result.outcome == Outcome.Victory
        val next = copy(played = played + 1, wins = wins + if (won) 1 else 0, xp = xp + result.xpGained, coins = coins + result.coins)
        return when {
            !won -> next.copy(end = RepeatEnd.Defeat)
            next.played >= target -> next.copy(end = RepeatEnd.Completed)
            else -> next
        }
    }

    fun interrupted(): RepeatSession = if (running) copy(end = RepeatEnd.Interrupted) else this

    companion object {
        /** D-REPEAT-SESSION proposal: a UI bound, not a reward value. */
        const val MAX_BATTLES = 10
        val CHOICES = listOf(3, 5, 10)
    }
}
