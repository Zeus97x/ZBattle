package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.AssetPack
import com.zeus97x.zbattle.core.CreatureCatalog
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CLAUDE-004 A2: the old-save side of the companion-identity migration fixture is a real
 * BattleProgress v1 encoding, and the proposed mapping covers every owned creature exactly once.
 * The new (v2, UUID-bearing) save does not exist yet (Phase D1), so this checks old -> mapping only.
 */
class ContractMigrationFixtureTest {
    private val dir = File(AssetPack.root.parentFile, "ai/integration/fixtures/migration")
    private val golden = File(dir, "zbattle-battleprogress-v1.b64")
    private val mapping = File(dir, "zbattle-v1-companion-mapping.json")

    /** Deterministic sample: Cindlet starter after one first victory over Wild Voltmaw. */
    private fun sample(): BattleProgress {
        var p = BattleProgress().withStarter("cindlet").startBattle(Encounters.playable.single())
        while (p.active != null) p = p.act(if (p.active!!.skillReady) BattleAction.Skill else BattleAction.Attack)
        return p.dismissResult()
    }

    @Test
    fun goldenIsTheCurrentCodecOutput() {
        val encoded = BattleProgressCodec.encode(sample())
        if (System.getenv("ZBATTLE_UPDATE_GOLDEN") == "1") golden.writeText(encoded + "\n")
        assertEquals(encoded, golden.readText().trim(), "Regenerate with ZBATTLE_UPDATE_GOLDEN=1 only if the v1 codec intentionally changed")
        assertEquals(1, java.util.Base64.getDecoder().decode(encoded).let { it[3].toInt() }, "schema v1")
    }

    @Test
    fun mappingCoversEveryOwnedCreatureOnce() {
        val decoded = BattleProgressCodec.decode(golden.readText().trim())
        val text = mapping.readText()
        val entries = Regex("\"legacyLocalId\":\\s*\"zb-uid-(\\d+)\",\\s*\"companionId\":\\s*\"([0-9a-f-]{36})\",\\s*\"speciesId\":\\s*\"(\\d+):(\\d)\",\\s*\"formIndex\":\\s*(\\d),\\s*\"xp\":\\s*(\\d+)")
            .findAll(text).map { it.groupValues }.toList()
        assertEquals(decoded.creatures.size, entries.size)
        assertEquals(entries.size, entries.map { it[2] }.toSet().size, "companionIds unique")
        decoded.creatures.forEach { owned ->
            val e = entries.single { it[1].toLong() == owned.uid }
            val creature = CreatureCatalog.require(owned.creatureId)
            // D-NATIVE-SPECIES proposal: a ZBattle-owned form maps to its family's Common species (rarity 0).
            assertEquals(creature.family.index, e[3].toInt())
            assertEquals(0, e[4].toInt())
            assertEquals(creature.formIndex, e[5].toInt())
            assertEquals(owned.xp, e[6].toLong(), "battle XP stays ZBattle-owned and is carried over")
        }
        assertTrue(decoded.defeated.isNotEmpty(), "sample includes settled progress")
    }
}
