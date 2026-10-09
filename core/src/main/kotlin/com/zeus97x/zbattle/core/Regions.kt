package com.zeus97x.zbattle.core

/**
 * ZPet RegionCatalog snapshot (ZPet main, RegionCatalog.java blob cfb3b696, checked 2026-10-09).
 * areaIndex 0..47; groupIndex = areaIndex / 4; stage = areaIndex % 4.
 *
 * Tradition labels are NOT unique ("Egyptian" = groups 3 and 6, "Greek" = groups 0 and 7),
 * so every lookup uses the numeric group id, never the label.
 */
data class RegionGroup(
    val index: Int,
    val tradition: String,
    val areas: List<Area>,
) {
    val id: String get() = "group-$index"
    /** Disambiguates repeated tradition labels without renaming them. */
    val rangeLabel: String get() = "${areas.first().name} – ${areas.last().name}"
}

data class Area(
    val index: Int,
    val name: String,
) {
    val id: String get() = "area-%02d".format(index)
    val groupIndex: Int get() = index / 4
    val stage: Int get() = index % 4
    val group: RegionGroup get() = RegionCatalog.groups[groupIndex]
}

object RegionCatalog {
    val traditions: List<String> = listOf(
        "Greek", "Norse", "Chinese", "Egyptian", "Japanese", "Mesoamerican", "Egyptian", "Greek",
        "Chinese moon folklore", "Greek phoenix inspiration", "Akan storytelling inspiration", "Celtic-inspired fantasy",
    )

    val areaNames: List<String> = listOf(
        "Olympian Foothills", "Thunderpeak", "Underworld Gates", "Elysian Horizon",
        "Yggdrasil Roots", "Rune Ruins", "Frostbound Fjord", "Aurora Citadel",
        "Jade Forest", "Celestial Peaks", "Dragon Palace", "Heavenly Gate",
        "Desert Crossing", "Golden Necropolis", "Veiled Dunes", "Guardian Horizon",
        "Lantern Path", "Mirror Grove", "Foxfire Shrine", "Dawn Sanctuary",
        "Feathered Canopy", "Wind Terrace", "Jade Garden", "Skywoven Summit",
        "Dawn Sands", "Solar Orchard", "Halo Oasis", "Sunrise Vault",
        "Foaming Shore", "Coral Passage", "Abyssal Reef", "Crest Horizon",
        "Moonlit Meadow", "Crescent Garden", "Jade Moon Terrace", "Lunar Sanctuary",
        "Ash Nest", "Cinder Ridge", "Dawn Roost", "Rebirth Summit",
        "Story Grove", "Riddle Crossing", "Silk Canopy", "Taleweaver Haven",
        "Moss Trail", "Briar Grove", "Elder Woodland", "Bloom Sanctuary",
    )

    val areas: List<Area> = areaNames.mapIndexed { index, name -> Area(index, name) }

    val groups: List<RegionGroup> = traditions.mapIndexed { index, tradition ->
        RegionGroup(index, tradition, areas.subList(index * 4, index * 4 + 4))
    }

    fun area(index: Int): Area = areas.getOrNull(index) ?: throw IllegalArgumentException("Unknown area $index")

    /** ZPet scenery wording; used as the short location description. */
    fun scenery(area: Area): String = when (area.groupIndex) {
        0 -> "Marble terraces · olive groves · storm-lit peaks"
        1 -> "Ancient roots · runestones · frost and aurora"
        2 -> "Jade groves · cloud terraces · lantern-lit waters"
        else -> "${area.group.tradition} · ${area.name}"
    }
}
