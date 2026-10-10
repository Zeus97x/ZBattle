package com.zeus97x.zbattle.core.economy

/**
 * Mystical boss availability (CLAUDE-006 C4, D-WEEK-WINDOW decided in batch 1, Q10):
 * - Each mystical boss has its own fixed 168-hour cycle that starts at its first victory, capped at
 *   15 victories. Unused wins don't carry over.
 * - The first victory after a cycle has expired anchors the next cycle (not the expiry moment).
 * - After every victory the boss cools down for 4 hours.
 *
 * Times are UTC epoch milliseconds from the device clock. This is a local scaffold: per
 * D-OFFLINE-TRUST it is **not** tamper-proof, and cross-device enforcement belongs to Phase D. A clock
 * that moved back before the last recorded victory blocks the boss instead of resetting anything.
 * Which bosses exist and when they unlock (3/6/9/12 completed regions) is separate work.
 */
data class MysticalTimer(
    val cycleStartUtc: Long? = null,
    val winsInCycle: Int = 0,
    val lastVictoryUtc: Long? = null,
) {
    init {
        require(winsInCycle in 0..MAX_WINS) { "Invalid win count" }
        require((cycleStartUtc == null) == (lastVictoryUtc == null)) { "Inconsistent timer" }
    }

    sealed interface Status {
        data object Available : Status
        data class CoolingDown(val untilUtc: Long) : Status
        data class CapReached(val untilUtc: Long) : Status
        /** The device clock is earlier than the last victory; the boss stays blocked until it catches up. */
        data object ClockBehind : Status
    }

    private fun cycleActive(nowUtc: Long) = cycleStartUtc != null && nowUtc < cycleStartUtc + CYCLE_MS

    fun status(nowUtc: Long): Status {
        val last = lastVictoryUtc ?: return Status.Available
        if (nowUtc < last) return Status.ClockBehind
        if (nowUtc < last + COOLDOWN_MS) return Status.CoolingDown(last + COOLDOWN_MS)
        if (cycleActive(nowUtc) && winsInCycle >= MAX_WINS) return Status.CapReached(cycleStartUtc!! + CYCLE_MS)
        return Status.Available
    }

    /** Records a victory at [nowUtc]; only allowed while [status] is Available. */
    fun recordVictory(nowUtc: Long): MysticalTimer {
        check(status(nowUtc) == Status.Available) { "Mystical boss not available" }
        return if (cycleActive(nowUtc)) copy(winsInCycle = winsInCycle + 1, lastVictoryUtc = nowUtc)
        else MysticalTimer(cycleStartUtc = nowUtc, winsInCycle = 1, lastVictoryUtc = nowUtc)
    }

    companion object {
        const val HOUR_MS = 3_600_000L
        const val CYCLE_MS = 168 * HOUR_MS
        const val COOLDOWN_MS = 4 * HOUR_MS
        const val MAX_WINS = 15
    }
}

/** Independent timers per mystical boss id: one boss's cooldown or cap never affects another. */
data class MysticalTimers(val byBoss: Map<String, MysticalTimer> = emptyMap()) {
    fun of(bossId: String): MysticalTimer = byBoss[bossId] ?: MysticalTimer()
    fun recordVictory(bossId: String, nowUtc: Long) = copy(byBoss = byBoss + (bossId to of(bossId).recordVictory(nowUtc)))
}
