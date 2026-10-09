package com.zeus97x.zbattle.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectionQueryTest {
    @Test
    fun defaultShowsCreatedCreaturesInCatalogueOrder() {
        val result = CollectionQuery().apply()
        assertEquals(54, result.size)
        assertEquals("Sparklit", result.first().name)
        assertEquals("Moondancer", result.last().name)
    }

    @Test
    fun searchMatchesNamesCaseInsensitively() {
        assertEquals(listOf("Moonbun", "Moondancer"), CollectionQuery(search = "  MOON ").apply().map { it.name })
        assertTrue(CollectionQuery(search = "zzz").apply().isEmpty())
    }

    @Test
    fun sortsChangeOrder() {
        val asc = CollectionQuery(sort = CollectionSort.NameAscending).apply()
        assertEquals("Abysscourser", asc.first().name)
        assertEquals(asc.reversed(), CollectionQuery(sort = CollectionSort.NameDescending).apply())
        val byStage = CollectionQuery(sort = CollectionSort.Stage).apply()
        assertTrue(byStage.take(9).all { it.stage == FormStage.Baby })
    }

    @Test
    fun filtersCombine() {
        val result = CollectionQuery(families = setOf(3, 6), stage = StageFilter.Final).apply()
        assertEquals(listOf("Tombwarden", "Duskjudge", "Solcarapace", "Aurorabeetle"), result.map { it.name })
        assertEquals(2, CollectionQuery(families = setOf(3), stage = StageFilter.Final).activeFilterCount)
    }

    @Test
    fun pendingFilterListsDeferredFormsOnly() {
        val pending = CollectionQuery(artwork = ArtworkFilter.Pending).apply()
        assertEquals(18, pending.size)
        assertTrue(pending.none { it.hasArtwork })
        assertEquals(72, CollectionQuery(artwork = ArtworkFilter.All).apply().size)
    }
}
