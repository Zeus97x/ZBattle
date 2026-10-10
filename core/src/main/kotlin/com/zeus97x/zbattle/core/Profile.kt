package com.zeus97x.zbattle.core

import com.zeus97x.zbattle.core.battle.BattleProgress
import com.zeus97x.zbattle.core.battle.OwnedCreature

/** Locally persisted player state. No cloud account, no ZPet write-back. */
data class PlayerSettings(
    /** Null until first-run setup ("Begin your journey") is completed. */
    val master: PetMaster? = null,
    val darkMode: Boolean = true,
    /** Saved preference only; audio is not implemented yet. */
    val music: Boolean = true,
    /** Controls the local preview animations (battle hit shake). */
    val battleAnimations: Boolean = true,
    val currentAreaIndex: Int = 0,
    val visitedAreas: Set<Int> = setOf(0),
    /** Owned creatures, XP/levels, encounter results and any active battle (CLAUDE-002). */
    val progress: BattleProgress = BattleProgress(),
) {
    val currentArea: Area get() = RegionCatalog.area(currentAreaIndex)
    val displayName: String get() = master?.name ?: "Pet Master"

    /** Battle party (up to 3, lead first; CLAUDE-005 B2). */
    val ownedParty: List<OwnedCreature> get() = progress.partyMembers
    val ownedCreatures: List<OwnedCreature> get() = progress.creatures
    val party: List<Creature> get() = ownedParty.map { it.creature }

    /** Makes sure a saved Pet Master owns their starter (new setups and pre-CLAUDE-002 saves). */
    fun withSeededStarter(): PlayerSettings =
        master?.let { copy(progress = progress.withStarter(it.starterId)) } ?: this

    fun travelTo(area: Area): PlayerSettings =
        copy(currentAreaIndex = area.index, visitedAreas = visitedAreas + area.index)
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
