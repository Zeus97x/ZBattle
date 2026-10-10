package com.zeus97x.zbattle.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mirrors PR #1's MasterProfileTest: 30 valid combinations, invalid inputs rejected. */
class PetMasterTest {
    @Test
    fun everyStyleGenderStarterCombinationIsValid() {
        var count = 0
        for (style in MasterStyle.entries) for (gender in MasterGender.entries) for (starter in Starters.creatures) {
            val master = assertNotNull(PetMaster.create(" Zeus97x ", style, gender, starter.id))
            assertEquals("Zeus97x", master.name)
            assertEquals(listOf(starter), PlayerSettings(master = master).party)
            count++
        }
        assertEquals(30, count)
        assertEquals(10, MasterStyle.entries.flatMap { s -> MasterGender.entries.map { g -> PetMaster("A", s, g, "sparklit").avatarKey } }.toSet().size)
    }

    @Test
    fun invalidInputsAreRejected() {
        assertNull(PetMaster.create(null, MasterStyle.Ranger, MasterGender.Male, "sparklit"))
        assertNull(PetMaster.create("   ", MasterStyle.Ranger, MasterGender.Male, "sparklit"))
        assertNull(PetMaster.create("a".repeat(25), MasterStyle.Ranger, MasterGender.Male, "sparklit"))
        assertNull(PetMaster.create("Zeus", null, MasterGender.Male, "sparklit"))
        assertNull(PetMaster.create("Zeus", MasterStyle.Ranger, null, "sparklit"))
        assertNull(PetMaster.create("Zeus", MasterStyle.Ranger, MasterGender.Male, null))
        assertNull(PetMaster.create("Zeus", MasterStyle.Ranger, MasterGender.Male, "wispkit"))
        assertNull(PetMaster.create("Zeus", MasterStyle.Ranger, MasterGender.Male, "ashpeep"))
        assertFailsWith<IllegalArgumentException> { PetMaster(" Zeus", MasterStyle.Ranger, MasterGender.Male, "sparklit") }
    }

    @Test
    fun startersAreTheApprovedCreatedArtwork() {
        assertEquals(listOf("Sparklit", "Inkling", "Cindlet"), Starters.creatures.map { it.name })
        assertTrue(Starters.creatures.all { it.hasArtwork })
        assertTrue(PlayerSettings().party.isEmpty())
    }
}
