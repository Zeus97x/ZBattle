package com.zeus97x.zbattle.core.economy

import com.zeus97x.zbattle.core.economy.MysticalTimer.Companion.COOLDOWN_MS
import com.zeus97x.zbattle.core.economy.MysticalTimer.Companion.CYCLE_MS
import com.zeus97x.zbattle.core.economy.MysticalTimer.Companion.HOUR_MS
import com.zeus97x.zbattle.core.economy.MysticalTimer.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** EXT-028 / D-WEEK-WINDOW boundary fixtures (UTC milliseconds). */
class MysticalTimerTest {
    private val t0 = 1_791_000_000_000L // a fixed UTC instant

    @Test
    fun fourHourCooldownBoundary() {
        val t = MysticalTimer().recordVictory(t0)
        assertEquals(Status.CoolingDown(t0 + COOLDOWN_MS), t.status(t0 + COOLDOWN_MS - 1))
        assertEquals(Status.Available, t.status(t0 + COOLDOWN_MS))
        assertFailsWith<IllegalStateException> { t.recordVictory(t0 + COOLDOWN_MS - 1) }
    }

    @Test
    fun fifteenWinsPerCycleAndTheSixteenthWaits() {
        var t = MysticalTimer()
        var now = t0
        repeat(15) { t = t.recordVictory(now); now += COOLDOWN_MS }
        assertEquals(15, t.winsInCycle)
        assertEquals(Status.CapReached(t0 + CYCLE_MS), t.status(now))
        assertEquals(Status.CapReached(t0 + CYCLE_MS), t.status(t0 + CYCLE_MS - 1))
        // The cycle ends exactly 168 h after the first victory; the next win anchors a new cycle.
        assertEquals(Status.Available, t.status(t0 + CYCLE_MS))
        val later = t0 + CYCLE_MS + 30 * HOUR_MS
        val next = t.recordVictory(later)
        assertEquals(MysticalTimer(cycleStartUtc = later, winsInCycle = 1, lastVictoryUtc = later), next)
    }

    @Test
    fun unusedWinsDoNotCarryOver() {
        val t = MysticalTimer().recordVictory(t0).recordVictory(t0 + COOLDOWN_MS)
        val next = t.recordVictory(t0 + CYCLE_MS)
        assertEquals(1, next.winsInCycle, "a fresh cycle starts at 1 win, not 15 - 2 left over")
    }

    @Test
    fun clockMovedBackBlocksInsteadOfResetting() {
        val t = MysticalTimer().recordVictory(t0)
        assertEquals(Status.ClockBehind, t.status(t0 - 1))
        assertFailsWith<IllegalStateException> { t.recordVictory(t0 - HOUR_MS) }
    }

    @Test
    fun bossesAreIndependent() {
        val timers = MysticalTimers().recordVictory("mystical-a", t0)
        assertEquals(Status.CoolingDown(t0 + COOLDOWN_MS), timers.of("mystical-a").status(t0 + 1))
        assertEquals(Status.Available, timers.of("mystical-b").status(t0 + 1))
    }
}
