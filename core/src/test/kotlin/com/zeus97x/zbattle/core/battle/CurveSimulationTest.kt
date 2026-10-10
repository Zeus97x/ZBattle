package com.zeus97x.zbattle.core.battle

import com.zeus97x.zbattle.core.AssetPack
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * D-CURVE: writes the simulation report to `build/curve-simulation.md` (copied into
 * ai/proposals/CURVE-SIMULATION.md by hand). Set ZBATTLE_CURVE_SEARCH=1 to also run the grid search.
 */
class CurveSimulationTest {
    @Test
    fun simulationIsDeterministicAndCoversTheApprovedLayout() {
        val rows = CurveSimulation.rows()
        assertEquals(300, rows.size)
        assertEquals(16_240, rows.sumOf { it.firstWinXp })
        assertEquals(84, CurveSimulation.trios().size)
        val row = rows.first { it.kind == EncounterKind.RegionBoss }
        val trio = CurveSimulation.trios().first()
        assertEquals(
            CurveSimulation.fight(row, CurveSimulation.PROPOSAL, trio, 10, CurveSimulation.Style.Prepared),
            CurveSimulation.fight(row, CurveSimulation.PROPOSAL, trio, 10, CurveSimulation.Style.Prepared),
        )
    }

    @Test
    fun writeReport() {
        val out = File(AssetPack.root.parentFile, "core/build/curve-simulation.md").apply { parentFile.mkdirs() }
        val curves = listOf(CurveSimulation.PROPOSAL) + CANDIDATES
        out.writeText(buildString {
            for (curve in curves) {
                appendLine("### ${curve.name}")
                appendLine()
                for (style in CurveSimulation.Style.entries) append(CurveSimulation.table(style.label, CurveSimulation.run(curve, style)))
            }
            if (System.getenv("ZBATTLE_CURVE_SEARCH") == "1") append(search())
            if (System.getenv("ZBATTLE_CURVE_SEARCH") == "2") append(searchPerKind())
            if (System.getenv("ZBATTLE_CURVE_SEARCH") == "3") append(searchScaled())
        })
        assertTrue(out.length() > 0)
        println("Wrote ${out.path}")
    }

    /** Grid over ramp and boss-bonus scale; scored against the target bands for the Prepared style. */
    private fun search(): String = buildString {
        appendLine("### Grid search (Prepared style)")
        appendLine()
        appendLine("| ramp | boss scale | Wild | Mini | Stage | Location | Region | score |")
        appendLine("|---|---|---|---|---|---|---|---|")
        val scored = mutableListOf<Pair<Int, String>>()
        for ((num, den) in listOf(1 to 1, 5 to 4, 4 to 3, 3 to 2, 5 to 3, 2 to 1)) {
            for (scale in listOf(1, 2, 3, 4)) {
                val curve = scaled("ramp $num/$den, bosses ×$scale", num, den, scale)
                val k = CurveSimulation.byKind(CurveSimulation.run(curve, CurveSimulation.Style.Prepared))
                val pct = EncounterKind.entries.map { k.getValue(it).winPct }
                val score = pct.zip(TARGETS).sumOf { (p, t) -> if (p < t.first) t.first - p else if (p > t.last) p - t.last else 0 }
                scored += score to "| $num/$den | ×$scale | " + pct.joinToString(" | ") { "$it%" } + " | $score |"
            }
        }
        scored.sortedBy { it.first }.forEach { appendLine(it.second) }
        appendLine()
    }

    /**
     * Each kind's win rate depends only on its own bonus (levels come from the fixed XP), so each kind
     * is tuned on its own: bonus × h/2 for h = 2..12, reporting overall win % and the spread across groups.
     */
    private fun searchPerKind(): String = buildString {
        appendLine("### Per-kind search (Prepared style; bonus × h/2)")
        appendLine()
        val rows = CurveSimulation.rows()
        val trios = CurveSimulation.trios()
        for ((num, den) in listOf(1 to 1, 5 to 4, 4 to 3)) {
            appendLine("ramp $num/$den")
            appendLine()
            appendLine("| kind | h/2 | win % | min group % | max group % |")
            appendLine("|---|---|---|---|---|")
            for (kind in EncounterKind.entries.drop(1)) {
                val subset = rows.filter { it.kind == kind }
                for (h in 2..12) {
                    val curve = CurveSimulation.Curve("x", num, den, CurveSimulation.PROPOSAL_BONUS.mapValues { (k, b) ->
                        if (k != kind) b else com.zeus97x.zbattle.core.economy.StatBonus(b.power * h / 2, b.guard * h / 2, b.speed * h / 2, b.maxHp * h / 2)
                    })
                    // Levels must still follow the full campaign's XP, so run over all rows and read this kind.
                    val result = CurveSimulation.run(curve, CurveSimulation.Style.Prepared, rows, trios).filterKeys { it.second == kind }
                    val total = CurveSimulation.byKind(result).getValue(kind)
                    val groups = result.values.map { it.winPct }
                    appendLine("| ${kind.name} | $h | ${total.winPct}% | ${groups.min()}% | ${groups.max()}% |")
                    if (subset.isEmpty() || total.winPct == 0) break
                }
            }
            appendLine()
        }
    }

    /**
     * Multiplicative boss scaling (no flat bonus): boss = wild stats × (HP %, power/guard %). Reports
     * overall win % and the spread across groups for both prepared and auto play.
     */
    private fun searchScaled(): String = buildString {
        appendLine("### Multiplicative search (no flat bonus; HP % / power+guard %)")
        appendLine()
        val rows = CurveSimulation.rows()
        val trios = CurveSimulation.trios()
        val flat = EncounterKind.entries.associateWith { com.zeus97x.zbattle.core.economy.StatBonus() }
        appendLine("| kind | HP % | atk % | prepared win % (min–max group) | auto win % (min–max group) |")
        appendLine("|---|---|---|---|---|")
        for (kind in EncounterKind.entries.drop(1)) {
            for (hp in listOf(150, 200, 250, 300, 350)) for (atk in listOf(110, 125, 140, 160)) {
                val curve = CurveSimulation.Curve("x", bonus = flat, scalePct = mapOf(kind to (hp to atk)))
                fun cell(style: CurveSimulation.Style): String {
                    val r = CurveSimulation.run(curve, style, rows, trios).filterKeys { it.second == kind }
                    val g = r.values.map { it.winPct }
                    return "${CurveSimulation.byKind(r).getValue(kind).winPct}% (${g.min()}–${g.max()})"
                }
                appendLine("| ${kind.name} | $hp | $atk | ${cell(CurveSimulation.Style.Prepared)} | ${cell(CurveSimulation.Style.Auto)} |")
            }
        }
        appendLine()
    }

    companion object {
        /** Proposed target win-rate bands for a prepared, on-curve party (Wild, Mini, Stage, Location, Region). */
        val TARGETS = listOf(95..100, 85..95, 70..85, 55..75, 30..50)

        fun scaled(name: String, num: Int, den: Int, scale: Int) = CurveSimulation.Curve(
            name, num, den,
            CurveSimulation.PROPOSAL_BONUS.mapValues { (_, b) ->
                com.zeus97x.zbattle.core.economy.StatBonus(b.power * scale, b.guard * scale, b.speed * scale, b.maxHp * scale)
            },
        )

        private val FLAT = EncounterKind.entries.associateWith { com.zeus97x.zbattle.core.economy.StatBonus() }

        /** Multiplicative boss scaling candidates (HP %, power+guard %) on the unchanged wild ramp. */
        val CANDIDATES: List<CurveSimulation.Curve> = listOf(
            CurveSimulation.Curve(
                "candidate A: mini 200/125, stage 250/125, location 250/140, region 300/125", bonus = FLAT,
                scalePct = mapOf(
                    EncounterKind.MiniBoss to (200 to 125), EncounterKind.StageBoss to (250 to 125),
                    EncounterKind.LocationBoss to (250 to 140), EncounterKind.RegionBoss to (300 to 125),
                ),
            ),
            CurveSimulation.Curve(
                "candidate B: mini 200/110, stage 250/110, location 300/110, region 350/110", bonus = FLAT,
                scalePct = mapOf(
                    EncounterKind.MiniBoss to (200 to 110), EncounterKind.StageBoss to (250 to 110),
                    EncounterKind.LocationBoss to (300 to 110), EncounterKind.RegionBoss to (350 to 110),
                ),
            ),
        )
    }
}
