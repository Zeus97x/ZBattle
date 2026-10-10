package com.zeus97x.zbattle.core

/**
 * Pet Master identity from the ZB-002 foundation (PR #1): five cosmetic styles × male/female,
 * a 1–24 character name and one of three approved starters. Appearance is cosmetic only —
 * every style can learn every skill and use all equipment.
 */
enum class MasterStyle(val id: String, val label: String) {
    Ranger("ranger", "Ranger"),
    DragonDisciple("dragon-disciple", "Dragon Disciple"),
    Knight("knight", "Knight"),
    Mystic("mystic", "Mystic"),
    Artificer("artificer", "Artificer"),
}

enum class MasterGender(val id: String, val label: String) {
    Male("male", "Male"),
    Female("female", "Female"),
}

object Starters {
    /** Approved starter companions, in PR #1 order. */
    val creatures: List<Creature> = listOf("sparklit", "inkling", "cindlet").map(CreatureCatalog::require)
}

data class PetMaster(
    val name: String,
    val style: MasterStyle,
    val gender: MasterGender,
    val starterId: String,
) {
    init {
        require(validName(name) == name) { "Use a name with 1–$MAX_NAME_LENGTH characters." }
        require(Starters.creatures.any { it.id == starterId }) { "Choose a valid companion." }
    }

    val starter: Creature get() = CreatureCatalog.require(starterId)
    val appearanceLabel: String get() = "${style.label} · ${gender.label}"
    /** One of the ten pending Pet Master illustrations, e.g. `avatar/dragon-disciple-female`. */
    val avatarKey: ArtKey get() = ArtKey.Avatar("${style.id}-${gender.id}")

    companion object {
        const val MAX_NAME_LENGTH = 24

        /** Returns the trimmed name, or null when it is empty or too long. */
        fun validName(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_LENGTH }

        /** Builds a profile from raw input, or null when any part is invalid. */
        fun create(name: String?, style: MasterStyle?, gender: MasterGender?, starterId: String?): PetMaster? {
            val clean = validName(name) ?: return null
            if (style == null || gender == null || starterId == null) return null
            if (Starters.creatures.none { it.id == starterId }) return null
            return PetMaster(clean, style, gender, starterId)
        }
    }
}
