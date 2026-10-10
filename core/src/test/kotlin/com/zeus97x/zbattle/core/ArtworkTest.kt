package com.zeus97x.zbattle.core

import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** CLAUDE-003: the ChatGPT artwork drop is wired by stable ids, one runtime file per key. */
class ArtworkTest {
    private val repo = AssetPack.root.parentFile
    private val assets = File(repo, "app/src/main/assets")
    private val manifest = File(repo, "design/artwork/manifest.json").readText()
    private val exports = File(repo, "design/artwork/runtime-exports.json").readText()

    private fun existing(key: ArtKey): List<String> = ArtCatalog.candidates(key).filter { File(assets, it).isFile }

    @Test
    fun everyRegionMapAndHeroResolvesToExactlyOneWebp() {
        val keys = RegionCatalog.groups.map { ArtKey.RegionMap(it) } + RegionCatalog.areas.map { ArtKey.LocationHero(it) }
        assertEquals(60, keys.size)
        keys.forEach { key ->
            val files = existing(key)
            assertEquals(listOf("art/${key.path}.webp"), files, "runtime art for $key")
        }
        val pngs = File(assets, "art").walkTopDown().filter { it.extension == "png" }.toList()
        assertTrue(pngs.isEmpty(), "No duplicate PNG runtime copies: $pngs")
    }

    @Test
    fun duplicateTraditionGroupsGetTheirOwnMaps() {
        val egyptian = RegionCatalog.groups.filter { it.tradition == "Egyptian" }.map { existing(ArtKey.RegionMap(it)).single() }
        val greek = RegionCatalog.groups.filter { it.tradition == "Greek" }.map { existing(ArtKey.RegionMap(it)).single() }
        assertEquals(2, egyptian.toSet().size)
        assertEquals(2, greek.toSet().size)
        val digests = (egyptian + greek).map { sha256(File(assets, it)) }
        assertEquals(4, digests.toSet().size, "Each group shows distinct artwork")
    }

    @Test
    fun manifestHeroNamesMatchExactZPetLocations() {
        val heroes = Regex("\"path\":\\s*\"design/artwork/locations/(area-\\d{2})/hero.png\",\\s*\"art_key\":\\s*\"location/(area-\\d{2})/hero\",\\s*\"area_name\":\\s*\"([^\"]+)\"")
            .findAll(manifest).map { Triple(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }.toList()
        assertEquals(48, heroes.size)
        heroes.forEach { (dir, key, name) ->
            assertEquals(dir, key)
            val area = RegionCatalog.areas.single { it.id == key }
            assertEquals(area.name, name, "manifest name for $key")
        }
    }

    @Test
    fun sourcesMatchManifestChecksumsAndExportRecord() {
        val entries = Regex("\"path\":\\s*\"(design/artwork/[^\"]+)\"[^}]*?\"sha256\":\\s*\"([0-9a-f]{64})\"")
            .findAll(manifest).map { it.groupValues[1] to it.groupValues[2] }.toList()
        assertEquals(70, entries.size)
        entries.forEach { (path, sha) -> assertEquals(sha, sha256(File(repo, path)), path) }
        val exported = Regex("\"source\":\\s*\"([^\"]+)\",\\s*\"source_sha256\":\\s*\"([0-9a-f]{64})\"")
            .findAll(exports).map { it.groupValues[1] to it.groupValues[2] }.toList()
        assertEquals(60, exported.size)
        assertTrue(entries.containsAll(exported), "Every export comes from a verified source")
    }

    @Test
    fun battleSceneryStaysAPlaceholderPhase() {
        RegionCatalog.areas.forEach { assertTrue(existing(ArtKey.LocationBattle(it)).isEmpty(), "battle art for ${it.id} is a later phase") }
    }

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}
