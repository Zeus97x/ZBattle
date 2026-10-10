package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.Area
import com.zeus97x.zbattle.core.Creature
import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.RegionCatalog

/**
 * A real, playable opponent. ZPet's encounter rule picks the area's region family: the young
 * form for normal encounters and the Branch A advanced form for bosses
 * (`MonsterCatalog.FAMILIES[family(route)][boss ? 2 : 1]`).
 */
data class Encounter(
    val area: Area,
    val slot: Int,
    val creature: Creature,
    val boss: Boolean,
    /** XP for the first victory ([BattleRewards]); replays pay 25% of it. */
    val firstWinXp: Long,
    /** Campaign kind (B4 proposal kinds). Today's ZPet-rule boss is the area guardian role. */
    val kind: EncounterKind = if (boss) EncounterKind.LocationBoss else EncounterKind.Wild,
) {
    val id: String get() = "${area.id}/slot-$slot"
    val level: Int get() = CreatureStats.opponentLevel(area.stage, boss)
    val stats: StatBlock get() = CreatureStats.forOpponent(area.stage, boss)
    val label: String get() = if (boss) "Boss ${creature.name}" else "Wild ${creature.name}"
}

enum class EncounterKind { Wild, MiniBoss, StageBoss, LocationBoss, RegionBoss }

object Encounters {
    const val LEGACY_FIRST_WIN_XP = 60L

    /**
     * CLAUDE-002 vertical slice: exactly one real opponent, slot 0 of Olympian Foothills.
     * Every other slot keeps its preview card until rosters for all 48 areas are approved.
     */
    val playable: List<Encounter> = listOf(forZPetRule(RegionCatalog.area(0), slot = 0, boss = false))

    fun find(areaIndex: Int, slot: Int): Encounter? = playable.firstOrNull { it.area.index == areaIndex && it.slot == slot }
    fun byId(id: String): Encounter? = playable.firstOrNull { it.id == id }

    private fun forZPetRule(area: Area, slot: Int, boss: Boolean): Encounter {
        val family = CreatureCatalog.families[area.groupIndex]
        val creature = CreatureCatalog.require(family.forms[if (boss) 2 else 1].lowercase().replace(" ", ""))
        val kind = if (boss) EncounterKind.LocationBoss else EncounterKind.Wild
        // The shipped area-00/slot-0 keeps the 60 XP it has always paid (D-ECONOMY-XP).
        val xp = if (area.index == 0 && slot == 0 && !boss) LEGACY_FIRST_WIN_XP else BattleRewards.firstWinXp.getValue(kind)
        return Encounter(area, slot, creature, boss, firstWinXp = xp)
    }
}
