package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.FormStage
import com.zeus97x.zbattle.core.RegionCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsTest {
    @Test
    fun levelFollowsZPetCurve() {
        assertEquals(1, Leveling.levelFor(0))
        assertEquals(1, Leveling.levelFor(99))
        assertEquals(2, Leveling.levelFor(100))
        assertEquals(50, Leveling.levelFor(1_000_000))
        assertEquals(60L to 100L, Leveling.progressInLevel(160))
        assertNull(Leveling.progressInLevel(4_900))
    }

    @Test
    fun formBaseMatchesZPetProgressionStats() {
        assertEquals(Triple(4, 4, 4), CreatureStats.base(FormStage.Baby))
        assertEquals(Triple(6, 5, 7), CreatureStats.base(FormStage.Young))
        assertEquals(Triple(12, 10, 6), CreatureStats.base(FormStage.BranchAFinal))
        assertEquals(Triple(7, 7, 14), CreatureStats.base(FormStage.BranchBFinal))
    }

    @Test
    fun hpDerivesFromLevelAndGuard() {
        val sparklit = CreatureCatalog.require("sparklit")
        val lv1 = CreatureStats.forCreature(sparklit, 1)
        assertEquals(StatBlock(maxHp = 45 + 3 + 4, power = 4, guard = 4, speed = 4), lv1)
        val lv10 = CreatureStats.forCreature(sparklit, 10)
        assertEquals(4 + 3, lv10.power)
        assertEquals(4 + 2, lv10.guard)
        assertEquals(45 + 30 + lv10.guard, lv10.maxHp)
        assertTrue(lv10.maxHp > lv1.maxHp)
    }

    @Test
    fun opponentStatsFollowZPetAreaStage() {
        assertEquals(StatBlock(30, 5, 4, 5), CreatureStats.forOpponent(0, boss = false))
        assertEquals(StatBlock(60 + 30, 11 + 4, 7 + 3, 8), CreatureStats.forOpponent(3, boss = true))
    }

    @Test
    fun skillsAndAdvantageMatchZPet() {
        assertEquals("Thunder Fang", Skills.forFamily(0).name)
        assertEquals(SkillEffect.Weaken, Skills.forFamily(1).effect)
        assertEquals(SkillEffect.Burn, Skills.forFamily(2).effect)
        assertEquals(3, Skills.advantage(2, 0)) // (2+1)%3 == 0
        assertEquals(-2, Skills.advantage(0, 2))
        assertEquals(0, Skills.advantage(0, 0))
    }

    @Test
    fun sliceEncounterUsesZPetRegionFamilyRule() {
        val e = Encounters.playable.single()
        assertEquals(RegionCatalog.area(0), e.area)
        assertEquals("Voltmaw", e.creature.name) // FAMILIES[family(0)][1]
        assertEquals("area-00/slot-0", e.id)
        assertEquals(60, e.firstWinXp)
        assertTrue(e.creature.hasArtwork)
    }
}
