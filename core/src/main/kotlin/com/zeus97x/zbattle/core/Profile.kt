package com.zeus97x.zbattle.core

/** Locally persisted player preferences. No cloud account, no ZPet write-back. */
data class PlayerSettings(
    val displayName: String = DEFAULT_NAME,
    val darkMode: Boolean = true,
    /** Saved preference only; audio is not implemented yet. */
    val music: Boolean = true,
    /** Controls the local preview animations (battle hit shake, creature idle bob). */
    val battleAnimations: Boolean = true,
    val currentAreaIndex: Int = 0,
    val visitedAreas: Set<Int> = setOf(0),
) {
    val currentArea: Area get() = RegionCatalog.area(currentAreaIndex)

    fun travelTo(area: Area): PlayerSettings =
        copy(currentAreaIndex = area.index, visitedAreas = visitedAreas + area.index)

    companion object {
        const val DEFAULT_NAME = "Trainer"
        const val MAX_NAME_LENGTH = 24

        /** Returns the trimmed name, or null when it is empty or too long. */
        fun validName(raw: String): String? = raw.trim().takeIf { it.isNotEmpty() && it.length <= MAX_NAME_LENGTH }
    }
}

interface SettingsStore {
    fun load(): PlayerSettings
    fun save(settings: PlayerSettings)
}

class InMemorySettingsStore(private var value: PlayerSettings = PlayerSettings()) : SettingsStore {
    override fun load(): PlayerSettings = value
    override fun save(settings: PlayerSettings) { value = settings }
}

/**
 * PREVIEW travel rule until the progression engine exists: the first area of every group is
 * open and later areas open after the previous area has been visited. Nothing here grants
 * rewards or reads creature families; location access is independent of creature mythology.
 */
object TravelRules {
    fun isUnlocked(area: Area, visited: Set<Int>): Boolean =
        area.stage == 0 || (area.index - 1) in visited || area.index in visited

    fun requirement(area: Area): String =
        if (area.stage == 0) "Open"
        else "Visit ${RegionCatalog.area(area.index - 1).name} first. Final unlock requirements arrive with the progression engine."
}
