package com.zeus97x.zbattle.core.battle

/**
 * Replay rewards and repeat sessions (CLAUDE-005 B5, D-REPLAY-REWARDS decided 2026-10-10).
 *
 * - Repeat victories earn reduced XP/coins and never a repeat first-clear ticket.
 * - Quantities need approval, so the tables below are null (inactive) and replays pay 0 for now.
 *   ECONOMY-PROPOSAL §5 has the proposed values.
 * - Each separate encounter starts at full HP; reopening an active battle keeps its saved HP.
 * - Settlement is duplicate-safe: battle ids only grow and each id settles at most once
 *   ([BattleProgress.settledThrough]).
 */
object ReplayRewards {
    /** Replay XP per kind. Inactive until D-REPLAY-REWARDS quantities are approved. */
    val xpByKind: Map<EncounterKind, Long>? = null
    /** Replay coins per kind. Inactive; there is no coin balance in the save until Phase C1. */
    val coinsByKind: Map<EncounterKind, Long>? = null

    fun xpFor(encounter: Encounter?, table: Map<EncounterKind, Long>? = xpByKind): Long =
        encounter?.let { table?.get(it.kind) } ?: 0
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
        val next = copy(played = played + 1, wins = wins + if (won) 1 else 0, xp = xp + result.xpGained)
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
