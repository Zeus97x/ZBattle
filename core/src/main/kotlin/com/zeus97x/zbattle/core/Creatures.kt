package com.zeus97x.zbattle.core

import java.util.Locale

/**
 * ZPet creature families and forms, copied verbatim from ZPet MonsterCatalog/MonsterJournal
 * (see ZBattle-ZPet-Assets/reference). Order is stable: familyIndex 0..11, formIndex 0..5.
 *
 * Only families 0..8 have created artwork (54 PNGs). Families 9..11 are listed so the
 * catalogue stays complete, but their forms are deferred and must never get substitute art.
 */
data class CreatureFamily(
    val index: Int,
    val label: String,
    val tradition: String,
    val lore: String,
    val forms: List<String>,
)

enum class FormStage(val label: String, val shortLabel: String) {
    Baby("Baby", "Baby"),
    Young("Young", "Young"),
    BranchAAdvanced("Branch A · advanced", "A · advanced"),
    BranchAFinal("Branch A · final", "A · final"),
    BranchBAdvanced("Branch B · advanced", "B · advanced"),
    BranchBFinal("Branch B · final", "B · final");

    companion object {
        fun ofForm(formIndex: Int): FormStage = entries[formIndex]
    }
}

data class Creature(
    val name: String,
    val family: CreatureFamily,
    val formIndex: Int,
    /** Path inside the asset pack (`monsters/<name>.png`) or null when artwork is not created yet. */
    val assetPath: String?,
) {
    /** Stable id; matches ZPet's lowercase asset naming. */
    val id: String get() = name.lowercase(Locale.ROOT).replace(" ", "")
    val stage: FormStage get() = FormStage.ofForm(formIndex)
    val hasArtwork: Boolean get() = assetPath != null
    /** Catalogue order used by ZPet saves. */
    val catalogOrder: Int get() = family.index * 6 + formIndex
}

object CreatureCatalog {
    /** Families whose six forms all have created PNGs in ZBattle-ZPet-Assets/assets/monsters. */
    const val FAMILIES_WITH_ARTWORK = 9

    val families: List<CreatureFamily> = listOf(
        CreatureFamily(0, "Storm wolf", "Greek · Zeus imagery", "Thunder gathers in its cloudlike fur. Its paths embody royal strength or storm speed.",
            listOf("Sparklit", "Voltmaw", "Crownstorm", "Astrapex", "Galestride", "Tempestral")),
        CreatureFamily(1, "Rune raven", "Norse · Odin imagery", "It collects lost runes; insight and armoured protection are its two original fantasy paths.",
            listOf("Inkling", "Runebeak", "Glyphwing", "Oracrow", "Ironquill", "Wargraven")),
        CreatureFamily(2, "Fire dragon", "Chinese dragon imagery", "An ember pearl guides its journey. The celestial final form has pearl-white scales and crimson accents.",
            listOf("Cindlet", "Kilnback", "Forgehide", "Vulcarion", "Flarecrest", "Pyrelisk")),
        CreatureFamily(3, "Guardian jackal", "Egyptian · Anubis imagery", "A watchful guardian of desert crossings, growing toward armour or spectral judgement.",
            listOf("Dunepup", "Sandward", "Giltguard", "Tombwarden", "Veilfang", "Duskjudge")),
        CreatureFamily(4, "Spirit fox", "Japanese · kitsune folklore", "Its lantern flames lead lost travellers; evolution favours illusion or protective fire.",
            listOf("Wispkit", "Emberveil", "Mirrortail", "Veilnine", "Lanternfox", "Dawnflare")),
        CreatureFamily(5, "Feathered serpent", "Mesoamerican · Quetzalcoatl imagery", "Feathers catch the wind; its fantasy paths weave sky currents or cultivate jade gardens.",
            listOf("Plumeling", "Plumeserp", "Galeplume", "Skyweaver", "Jadecoil", "Verdantcrest")),
        CreatureFamily(6, "Sun scarab", "Egyptian · Khepri imagery", "A dawn collector evolving toward solar armour or radiant support.",
            listOf("Glintgrub", "Dawnscarab", "Sunplate", "Solcarapace", "Halohover", "Aurorabeetle")),
        CreatureFamily(7, "Sea horse", "Greek · Poseidon imagery", "It can grow into a deep-water guardian or a swift crest-riding companion.",
            listOf("Foalfoam", "Tidecanter", "Reefmane", "Abysscourser", "Mistgallop", "Crestcharger")),
        CreatureFamily(8, "Moon rabbit", "East Asian moon-rabbit traditions · distinct lore briefs pending", "Its original fantasy paths use moon barriers or evasive dream magic.",
            listOf("Moonbun", "Crescenthop", "Jadebound", "Lunarwarden", "Dreamskip", "Moondancer")),
        CreatureFamily(9, "Rebirth bird", "Phoenix imagery · tradition brief pending", "Ash gives way to flame; choose powerful fire or restorative dawn magic.",
            listOf("Ashpeep", "Cinderwing", "Blazepinion", "Pyre Sovereign", "Dawnfeather", "Aurorise")),
        CreatureFamily(10, "Web spider", "Akan · Anansi storytelling inspiration", "A curious storyteller whose fantasy webs become decoys or protective shields.",
            listOf("Threadbit", "Taleweaver", "Riddleweb", "Mythspinner", "Silkguard", "Loomkeeper")),
        CreatureFamily(11, "Forest deer", "Celtic-inspired forest imagery", "Living antlers grow toward woodland protection or agile thorn attacks.",
            listOf("Budfawn", "Mossantler", "Grovecrest", "Elderbloom", "Briarstep", "Thornhart")),
    )

    val all: List<Creature> = families.flatMap { family ->
        family.forms.mapIndexed { formIndex, name ->
            val path = if (family.index < FAMILIES_WITH_ARTWORK) "monsters/${name.lowercase(Locale.ROOT)}.png" else null
            Creature(name, family, formIndex, path)
        }
    }

    val created: List<Creature> = all.filter { it.hasArtwork }
    val pending: List<Creature> = all.filterNot { it.hasArtwork }

    private val byId = all.associateBy { it.id }

    fun byId(id: String): Creature? = byId[id]
    fun require(id: String): Creature = byId[id] ?: throw IllegalArgumentException("Unknown creature $id")
}
