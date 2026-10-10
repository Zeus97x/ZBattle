package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.AssetPack
import com.zeus97x.zbattle.core.CreatureCatalog
import com.zeus97x.zbattle.core.RegionCatalog
import com.zeus97x.zbattle.core.economy.ItemCatalog
import com.zeus97x.zbattle.core.economy.ItemEffect
import com.zeus97x.zbattle.core.economy.StatBonus
import java.io.File

/**
 * D-CURVE simulation (decision batch 2): the approved campaign layout and XP, played through the
 * real [BattleEngine] by a party of three. Not game content; used to choose opponent stats.
 *
 * - The player is "on curve": before each encounter every member holds a third of the first-win XP
 *   of all earlier encounters (D-PARTY-XP split, everyone participating).
 * - Parties: every trio of distinct families with artwork (84), all Baby form. Evolution is excluded
 *   because D-EVOLUTION-THRESHOLDS is open; results that depend on it are flagged in the report.
 * - Styles: [Style.Auto] is the B1 auto policy; [Style.Prepared] adds Potions (up to 5), switching out
 *   of trouble and a charm on every member.
 */
object CurveSimulation {
    data class Row(val id: String, val areaIndex: Int, val slot: Int, val kind: EncounterKind, val creatureId: String, val firstWinXp: Long)

    /** Opponent stat formula: the B4 proposal's shape with tunable ramp and boss bonuses. */
    data class Curve(
        val name: String,
        /** Area tier = areaIndex × rampNum / rampDen. */
        val rampNum: Int = 1,
        val rampDen: Int = 1,
        /** Per-kind flat bonus (power, guard, speed, HP), as in the B4 proposal. */
        val bonus: Map<EncounterKind, StatBonus> = PROPOSAL_BONUS,
        /** Per-kind multiplier in percent on (HP, power, guard) after the bonus; speed is unchanged. */
        val scalePct: Map<EncounterKind, Pair<Int, Int>> = emptyMap(),
    ) {
        fun stats(areaIndex: Int, kind: EncounterKind): StatBlock {
            val t = areaIndex * rampNum / rampDen
            val b = bonus.getValue(kind)
            val (hpPct, atkPct) = scalePct[kind] ?: (100 to 100)
            return StatBlock(
                maxHp = (30 + (7 * t) / 2 + b.maxHp) * hpPct / 100,
                power = (5 + t / 3 + b.power) * atkPct / 100,
                guard = (4 + t / 4 + b.guard) * atkPct / 100,
                speed = 5 + t / 5 + b.speed,
            )
        }
    }

    val PROPOSAL_BONUS = mapOf(
        EncounterKind.Wild to StatBonus(),
        EncounterKind.MiniBoss to StatBonus(power = 1, maxHp = 8),
        EncounterKind.StageBoss to StatBonus(power = 1, guard = 1, maxHp = 12),
        EncounterKind.LocationBoss to StatBonus(power = 2, guard = 1, maxHp = 18),
        EncounterKind.RegionBoss to StatBonus(power = 3, guard = 2, maxHp = 25),
    )
    /** `tools/campaign_proposal.py` opponent_stats, revision 2. */
    val PROPOSAL = Curve("proposal (campaign-proposal-2)")

    enum class Style(val label: String, val charm: String?) {
        Auto("auto, no charm", null),
        Prepared("Potions + switching + Fang Charm I", "fang-charm-1"),
        PreparedII("Potions + switching + Fang Charm II", "fang-charm-2"),
    }

    data class Fight(val won: Boolean, val hpLeftPercent: Int, val turns: Int)

    fun rows(): List<Row> {
        val json = File(AssetPack.root.parentFile, "ai/proposals/campaign-proposal.json").readText()
        val kinds = mapOf("wild" to EncounterKind.Wild, "mini_boss" to EncounterKind.MiniBoss, "stage_boss" to EncounterKind.StageBoss,
            "location_boss" to EncounterKind.LocationBoss, "region_boss" to EncounterKind.RegionBoss)
        return Regex("""\{"id": "([^"]+)", "areaIndex": (\d+), "slot": (\d+), "kind": "(\w+)", "creatureId": "(\w+)", "opponentLevel": \d+, "firstWinXp": (\d+)\}""")
            .findAll(json).map { m ->
                val g = m.groupValues
                Row(g[1], g[2].toInt(), g[3].toInt(), kinds.getValue(g[4]), g[5], g[6].toLong())
            }.toList()
    }

    /** All trios of distinct families that have artwork, Baby form. */
    fun trios(): List<List<String>> {
        val babies = CreatureCatalog.families.filter { f -> CreatureCatalog.require(f.forms[0].lowercase()).hasArtwork }.map { it.forms[0].lowercase() }
        return babies.indices.flatMap { a -> (a + 1 until babies.size).flatMap { b -> (b + 1 until babies.size).map { c -> listOf(babies[a], babies[b], babies[c]) } } }
    }

    fun fight(row: Row, curve: Curve, party: List<String>, level: Int, style: Style): Fight {
        val area = RegionCatalog.area(row.areaIndex)
        val encounter = Encounter(area, row.slot, CreatureCatalog.require(row.creatureId), row.kind != EncounterKind.Wild, row.firstWinXp, row.kind)
        val bonus = style.charm?.let { ItemCatalog.require(it).bonus }
        val e = curve.stats(row.areaIndex, row.kind)
        var s = BattleEngine.start(1, encounter, party.mapIndexed { i, id -> BattleEngine.Entrant(i + 1L, id, level, bonus) })
        s = s.copy(enemy = Combatant(row.creatureId, 1, e.maxHp, e.maxHp, e.power, e.guard, e.speed))
        val potion = ItemCatalog.require(ItemCatalog.POTION)
        while (!s.over) {
            s = when {
                s.awaitingReplacement -> BattleEngine.replace(s, s.benchIndices().maxBy { s.team[it].combatant.hp })
                style == Style.Auto -> BattleEngine.act(s, if (s.skillReady) BattleAction.Skill else BattleAction.Attack)
                s.player.hp * 100 < s.player.maxHp * 35 && BattleEngine.itemRefusal(s, potion.effect!!) == null ->
                    BattleEngine.useItem(s, potion.effect as ItemEffect, potion.name)
                s.player.hp * 100 < s.player.maxHp * 25 && s.canSwitch &&
                    s.benchIndices().any { s.team[it].combatant.hp * 100 >= s.team[it].combatant.maxHp * 60 } ->
                    BattleEngine.switch(s, s.benchIndices().maxBy { s.team[it].combatant.hp })
                else -> BattleEngine.act(s, if (s.skillReady) BattleAction.Skill else BattleAction.Attack)
            }
        }
        val hp = s.team.sumOf { it.combatant.hp } * 100 / s.team.sumOf { it.combatant.maxHp }
        return Fight(s.outcome == Outcome.Victory, hp, s.turn)
    }

    data class Cell(var n: Int = 0, var wins: Int = 0, var hp: Long = 0) {
        val winPct: Int get() = if (n == 0) 0 else wins * 100 / n
        val avgHp: Int get() = if (wins == 0) 0 else (hp / wins).toInt()
    }

    /** Win rate and average party HP left on wins, per (group, kind). */
    fun run(curve: Curve, style: Style, rows: List<Row> = rows(), trios: List<List<String>> = trios()): Map<Pair<Int, EncounterKind>, Cell> {
        val out = linkedMapOf<Pair<Int, EncounterKind>, Cell>()
        var xp = 0L
        for (row in rows) {
            val level = Leveling.levelFor(xp / BattleEngine.PARTY_SIZE)
            val cell = out.getOrPut(row.areaIndex / 4 to row.kind) { Cell() }
            for (trio in trios) {
                val f = fight(row, curve, trio, level, style)
                cell.n++
                if (f.won) { cell.wins++; cell.hp += f.hpLeftPercent }
            }
            xp += row.firstWinXp
        }
        return out
    }

    /** Overall win rate per kind. */
    fun byKind(result: Map<Pair<Int, EncounterKind>, Cell>): Map<EncounterKind, Cell> =
        EncounterKind.entries.associateWith { k ->
            result.filterKeys { it.second == k }.values.fold(Cell()) { a, c -> Cell(a.n + c.n, a.wins + c.wins, a.hp + c.hp) }
        }

    fun table(title: String, result: Map<Pair<Int, EncounterKind>, Cell>): String = buildString {
        appendLine("#### $title")
        appendLine()
        appendLine("| Group | Level at group start | " + EncounterKind.entries.joinToString(" | ") { it.name } + " |")
        appendLine("|---|---|" + "---|".repeat(EncounterKind.entries.size))
        val rows = rows()
        for (g in 0 until 12) {
            val xpBefore = rows.takeWhile { it.areaIndex < g * 4 }.sumOf { it.firstWinXp }
            val level = Leveling.levelFor(xpBefore / BattleEngine.PARTY_SIZE)
            append("| group-$g | $level | ")
            appendLine(EncounterKind.entries.joinToString(" | ") { k -> result[g to k]?.let { "${it.winPct}% (${it.avgHp}% HP)" } ?: "—" } + " |")
        }
        appendLine()
        appendLine("Overall: " + byKind(result).entries.joinToString(" · ") { (k, c) -> "${k.name} ${c.winPct}% (${c.avgHp}% HP)" })
        appendLine()
    }
}
