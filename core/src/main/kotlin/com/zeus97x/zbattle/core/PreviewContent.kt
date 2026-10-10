package com.zeus97x.zbattle.core

/**
 * DEMO/PREVIEW content for screens whose systems are not built yet (battle engine, opponents,
 * economy, events). Nothing here is canon: no names, prices, rewards or rosters are approved.
 * Kept apart from catalogue models so it can be deleted when real systems land.
 */
/** Shop sections (real since CLAUDE-006; items live in `economy.ItemCatalog`). */
enum class ShopCategory(val label: String, val subtitle: String) {
    Equipment("Equipment", "Charms for your party"),
    Consumables("Consumables", "Battle items and ZCubes"),
    Cosmetics("Cosmetics", "Trainer and party styles"),
}

data class PreviewOpponent(
    val slot: Int,
    val label: String,
    val isBoss: Boolean,
    /** Existing ZPet creatures only; chosen by rotation, not by area mythology. */
    val party: List<Creature>,
) {
    val artKey: ArtKey get() = if (isBoss) ArtKey.Boss("pending-${slot}") else ArtKey.Opponent("pending-${slot}")
}

object PreviewContent {
    const val BOSS_SLOT = 3

    fun opponents(area: Area): List<PreviewOpponent> = (0..BOSS_SLOT).map { slot ->
        val isBoss = slot == BOSS_SLOT
        PreviewOpponent(
            slot = slot,
            label = if (isBoss) "Region Boss · roster pending" else "Preview opponent ${slot + 1}",
            isBoss = isBoss,
            party = rotation(area.index * 7 + slot * 3, if (isBoss) 3 else 2),
        )
    }

    fun doubleLineup(area: Area): List<Creature> = rotation(area.index * 5 + 11, 6)

    private fun rotation(start: Int, count: Int): List<Creature> {
        val pool = CreatureCatalog.created
        return (0 until count).map { pool[(start + it * 5) % pool.size] }
    }
}
