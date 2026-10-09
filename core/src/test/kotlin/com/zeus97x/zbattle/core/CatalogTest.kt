package com.zeus97x.zbattle.core

import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CatalogTest {
    @Test
    fun regionCatalogueMatchesZPetReferenceExactly() {
        val source = AssetPack.reference("RegionCatalog.java")
        assertEquals(AssetPack.javaStrings(source, "TRADITIONS"), RegionCatalog.traditions)
        assertEquals(AssetPack.javaStrings(source, "AREAS"), RegionCatalog.areaNames)
        assertEquals(48, RegionCatalog.areas.size)
        assertEquals(12, RegionCatalog.groups.size)
    }

    @Test
    fun repeatedTraditionsStayDistinctGroups() {
        val egyptian = RegionCatalog.groups.filter { it.tradition == "Egyptian" }
        val greek = RegionCatalog.groups.filter { it.tradition == "Greek" }
        assertEquals(listOf(3, 6), egyptian.map { it.index })
        assertEquals(listOf(0, 7), greek.map { it.index })
        assertNotEquals(egyptian[0].id, egyptian[1].id)
        assertEquals(listOf("Desert Crossing", "Golden Necropolis", "Veiled Dunes", "Guardian Horizon"), egyptian[0].areas.map { it.name })
        assertEquals(listOf("Dawn Sands", "Solar Orchard", "Halo Oasis", "Sunrise Vault"), egyptian[1].areas.map { it.name })
        assertEquals(RegionCatalog.groups.size, RegionCatalog.groups.map { it.id }.toSet().size)
        assertEquals(48, RegionCatalog.areas.map { it.id }.toSet().size)
        // Art keys are per group/area id, so duplicate labels never share artwork slots.
        assertNotEquals(ArtKey.RegionMap(egyptian[0]), ArtKey.RegionMap(egyptian[1]))
    }

    @Test
    fun areaIndexArithmeticMatchesZPet() {
        RegionCatalog.areas.forEach { area ->
            assertEquals(area.index / 4, area.groupIndex)
            assertEquals(area.index % 4, area.stage)
            assertTrue(area in area.group.areas)
        }
    }

    @Test
    fun creatureFamiliesMatchMonsterCatalogReference() {
        val names = AssetPack.javaStrings(AssetPack.reference("MonsterCatalog.java"), "FAMILIES")
        assertEquals(names, CreatureCatalog.all.map { it.name })
        val journal = AssetPack.javaStrings(AssetPack.reference("MonsterJournal.java"), "FAMILIES").chunked(9)
        CreatureCatalog.families.forEach { family ->
            val row = journal[family.index]
            assertEquals(row[0], family.label)
            assertEquals(row[1], family.tradition)
            assertEquals(row.subList(2, 8), family.forms)
            assertEquals(row[8], family.lore)
        }
    }

    @Test
    fun everyExistingPngIsMappedAndEveryMappingExists() {
        val dir = File(AssetPack.root, "assets")
        val pngs = File(dir, "monsters").listFiles { f -> f.extension == "png" }!!.map { "monsters/${it.name}" }.toSet()
        assertEquals(54, pngs.size)
        val mapped = CreatureCatalog.created.mapNotNull { it.assetPath }.toSet()
        assertEquals(pngs, mapped, "Created creatures must map 1:1 onto the existing PNGs")
        assertEquals(18, CreatureCatalog.pending.size)
        assertTrue(CreatureCatalog.pending.all { it.family.index >= CreatureCatalog.FAMILIES_WITH_ARTWORK })
        CreatureCatalog.pending.forEach { assertTrue(ArtCatalog.candidates(ArtKey.CreatureArt(it)).isEmpty(), "${it.name} must not get substitute art") }
    }

    @Test
    fun existingPngsAreUnchangedFromManifest() {
        val manifest = File(AssetPack.root, "manifest.json").readText()
        val entries = Regex("\"path\":\\s*\"(assets/monsters/[^\"]+)\"[^}]*?\"sha256\":\\s*\"([0-9a-f]{64})\"")
            .findAll(manifest).map { it.groupValues[1] to it.groupValues[2] }.toList()
        assertEquals(54, entries.size)
        entries.forEach { (path, sha) ->
            val digest = MessageDigest.getInstance("SHA-256").digest(File(AssetPack.root, path).readBytes())
            assertEquals(sha, digest.joinToString("") { "%02x".format(it) }, path)
        }
    }

    @Test
    fun nonCreatureArtResolvesByStableConventionNotFilenames() {
        val area = RegionCatalog.area(12)
        assertEquals(listOf("art/location/area-12/hero.png", "art/location/area-12/hero.webp"), ArtCatalog.candidates(ArtKey.LocationHero(area)))
        assertEquals("art/region/group-6/map.png", ArtCatalog.candidates(ArtKey.RegionMap(RegionCatalog.groups[6])).first())
        val sparklit = CreatureCatalog.require("sparklit")
        assertEquals(listOf("monsters/sparklit.png"), ArtCatalog.candidates(ArtKey.CreatureArt(sparklit)))
        assertEquals(ArtFit.Contain, ArtKey.CreatureArt(sparklit).fit)
        assertEquals(ArtFit.Cover, ArtKey.LocationHero(area).fit)
    }

    @Test
    fun previewContentOnlyUsesCreatedCreatures() {
        RegionCatalog.areas.forEach { area ->
            PreviewContent.opponents(area).flatMap { it.party }.forEach { assertTrue(it.hasArtwork) }
            assertEquals(6, PreviewContent.doubleLineup(area).size)
            assertTrue(PreviewContent.doubleLineup(area).all { it.hasArtwork })
        }
        assertTrue(PreviewContent.party.all { it.hasArtwork })
        assertTrue(PreviewContent.shopItems.all { it.label.startsWith("Demo ") })
    }
}
