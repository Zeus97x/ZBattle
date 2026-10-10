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
 * The golden file is frozen at v1 (CLAUDE-005 B2 moved the codec to v2): it must keep decoding,
 * through the v1 -> v2 migration, to exactly the progress the current engine produces.
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
    fun frozenV1GoldenMigratesToCurrentProgress() {
        val raw = golden.readText().trim()
        assertEquals(1, java.util.Base64.getDecoder().decode(raw).let { it[3].toInt() }, "golden stays a v1 save")
        // The app applies withStarter on every load (AppState), which grants the C1 starter kit once.
        val migrated = BattleProgressCodec.decode(raw).withStarter("cindlet")
        // Battles won before C2 existed are not paid coins retroactively (D-RETRO-TICKETS / no back-pay).
        assertEquals(sample().copy(inventory = migrated.inventory), migrated)
        assertEquals(com.zeus97x.zbattle.core.economy.Inventory().withStarterKit(), migrated.inventory)
        assertEquals(migrated, BattleProgressCodec.decode(BattleProgressCodec.encode(migrated)), "re-saved as v2 without loss")
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
