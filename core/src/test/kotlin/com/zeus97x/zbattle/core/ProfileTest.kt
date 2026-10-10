package com.zeus97x.zbattle.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileTest {
    @Test
    fun nameValidation() {
        assertEquals("Zeus", PetMaster.validName("  Zeus "))
        assertNull(PetMaster.validName("   "))
        assertNull(PetMaster.validName("x".repeat(25)))
    }

    @Test
    fun travelRecordsCurrentAndVisitedArea() {
        val area = RegionCatalog.area(25)
        val settings = PlayerSettings().travelTo(area)
        assertEquals(area, settings.currentArea)
        assertTrue(25 in settings.visitedAreas)
    }

    @Test
    fun previewTravelLocksFollowVisitsNotCreatureFamilies() {
        val visited = setOf(0)
        assertTrue(TravelRules.isUnlocked(RegionCatalog.area(0), visited))
        assertTrue(TravelRules.isUnlocked(RegionCatalog.area(1), visited))
        assertFalse(TravelRules.isUnlocked(RegionCatalog.area(2), visited))
        RegionCatalog.groups.forEach { assertTrue(TravelRules.isUnlocked(it.areas.first(), emptySet())) }
    }
}
