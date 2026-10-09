package com.zeus97x.zbattle.core

import java.util.Locale

enum class CollectionSort(val label: String) {
    Catalog("Catalogue order"),
    NameAscending("Name A–Z"),
    NameDescending("Name Z–A"),
    Stage("Evolution stage"),
}

enum class StageFilter(val label: String, val matches: (FormStage) -> Boolean) {
    All("All stages", { true }),
    Baby("Baby", { it == FormStage.Baby }),
    Young("Young", { it == FormStage.Young }),
    Advanced("Advanced", { it == FormStage.BranchAAdvanced || it == FormStage.BranchBAdvanced }),
    Final("Final", { it == FormStage.BranchAFinal || it == FormStage.BranchBFinal }),
}

enum class ArtworkFilter(val label: String) {
    Created("Created artwork"),
    Pending("Artwork pending"),
    All("All forms"),
}

/** Search, sort and filter state for the Collection screen. */
data class CollectionQuery(
    val search: String = "",
    val sort: CollectionSort = CollectionSort.Catalog,
    /** Empty = every family. Family indices, never tradition labels. */
    val families: Set<Int> = emptySet(),
    val stage: StageFilter = StageFilter.All,
    val artwork: ArtworkFilter = ArtworkFilter.Created,
) {
    val activeFilterCount: Int
        get() = (if (families.isNotEmpty()) 1 else 0) + (if (stage != StageFilter.All) 1 else 0) +
            (if (artwork != ArtworkFilter.Created) 1 else 0)

    fun apply(source: List<Creature> = CreatureCatalog.all): List<Creature> {
        val needle = search.trim().lowercase(Locale.ROOT)
        val filtered = source.filter { creature ->
            (needle.isEmpty() || creature.name.lowercase(Locale.ROOT).contains(needle)) &&
                (families.isEmpty() || creature.family.index in families) &&
                stage.matches(creature.stage) &&
                when (artwork) {
                    ArtworkFilter.Created -> creature.hasArtwork
                    ArtworkFilter.Pending -> !creature.hasArtwork
                    ArtworkFilter.All -> true
                }
        }
        return when (sort) {
            CollectionSort.Catalog -> filtered.sortedBy { it.catalogOrder }
            CollectionSort.NameAscending -> filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
            CollectionSort.NameDescending -> filtered.sortedByDescending { it.name.lowercase(Locale.ROOT) }
            CollectionSort.Stage -> filtered.sortedWith(compareBy<Creature> { it.formIndex }.thenBy { it.catalogOrder })
        }
    }
}
