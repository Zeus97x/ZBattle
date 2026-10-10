package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.AssetPack
import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.RegionCatalog
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CLAUDE-005 B4: the campaign config (ai/proposals/campaign-proposal.json; layout approved, stats provisional) stays inactive, and every
 * row resolves against the real Kotlin catalogue. The Python validator checks it independently in CI.
 */
class CampaignProposalTest {
    private val json = File(AssetPack.root.parentFile, "ai/proposals/campaign-proposal.json").readText()
    private val rows = Regex("""\{"id": "(area-(\d\d)/slot-(\d+))", "areaIndex": (\d+), "slot": (\d+), "kind": "(\w+)", "creatureId": "(\w+)", "opponentLevel": (\d+), "firstWinXp": (\d+)\}""")
        .findAll(json).map { it.groupValues }.toList()

    @Test
    fun proposalResolvesAgainstTheCatalogue() {
        assertTrue(json.contains("\"status\": \"APPROVED_LAYOUT\""))
        assertEquals(300, rows.size)
        rows.forEach { r ->
            val creature = CreatureCatalog.byId(r[7])
            assertTrue(creature != null && creature.hasArtwork, "${r[1]}: ${r[7]} must be an existing creature with artwork")
            assertEquals(RegionCatalog.area(r[4].toInt()).id, "area-${r[2]}")
            assertTrue(r[8].toInt() in 1..50)
        }
    }

    @Test
    fun shippedEncounterIsUnchangedAndProposalIsNotActive() {
        val legacy = rows.first()
        val shipped = Encounters.playable.single()
        assertEquals(listOf(shipped.id, "wild", shipped.creature.id, shipped.level.toString(), shipped.firstWinXp.toString()), listOf(legacy[1], legacy[6], legacy[7], legacy[8], legacy[9]))
        assertEquals(1, Encounters.playable.size, "campaign content stays inactive until D-CURVE fixes opponent stats")
    }
}
