#!/usr/bin/env python3
"""Campaign PROPOSAL tool (CLAUDE-005 B4, D-CAMPAIGN). Not active game content.

  python3 tools/campaign_proposal.py              validate ai/proposals/campaign-proposal.json (CI)
  python3 tools/campaign_proposal.py --write      regenerate it from the formulas below
  python3 tools/campaign_proposal.py --simulate [--party N]
                                                  win-rate table: the current engine and the B1
                                                  auto policy, with a party of N splitting XP

Creature ids are parsed independently from Creatures.kt. Formulas were drafted in the proposal and
first-win XP follows D-ECONOMY-XP (decision batch 2): 300 XP per ordinary area, +150 per region boss.
Layout approved (D-CAMPAIGN); opponent stats stay provisional until the D-CURVE party simulation.
"""
import collections, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CREATURES = ROOT / "core/src/main/kotlin/com/zeus97x/zbattle/core/Creatures.kt"
ENCOUNTERS = ROOT / "core/src/main/kotlin/com/zeus97x/zbattle/core/battle/Encounters.kt"
OUT = ROOT / "ai/proposals/campaign-proposal.json"
REVISION = "campaign-proposal-2"
LEGACY = {"id": "area-00/slot-0", "areaIndex": 0, "slot": 0, "kind": "wild", "creatureId": "voltmaw", "opponentLevel": 2, "firstWinXp": 60}

# Art-blocked groups 9..11 use an interim family of the same family%3 class (advantage maths unchanged).
INTERIM = {9: 6, 10: 1, 11: 5}
# Slots per area stage (0..3): (kind, formIndex).
LAYOUT = {
    0: [("wild", 1), ("wild", 0), ("mini_boss", 1), ("wild", 0), ("stage_boss", 4), ("location_boss", 2)],
    1: [("wild", 1), ("wild", 0), ("mini_boss", 2), ("wild", 1), ("stage_boss", 4), ("location_boss", 2)],
    2: [("wild", 1), ("wild", 4), ("mini_boss", 2), ("wild", 4), ("stage_boss", 2), ("location_boss", 5)],
    3: [("wild", 1), ("wild", 4), ("mini_boss", 2), ("wild", 2), ("stage_boss", 4), ("location_boss", 5), ("region_boss", 3)],
}
XP = {"wild": 20, "mini_boss": 40, "stage_boss": 80, "location_boss": 120, "region_boss": 150}  # D-ECONOMY-XP (batch 2)
LEVEL_BONUS = {"wild": 0, "mini_boss": 1, "stage_boss": 2, "location_boss": 3, "region_boss": 4}
STAT_BONUS = {"wild": (0, 0, 0, 0), "mini_boss": (1, 0, 0, 8), "stage_boss": (1, 1, 0, 12), "location_boss": (2, 1, 0, 18), "region_boss": (3, 2, 0, 25)}
KINDS = list(XP)


def families():
    src = CREATURES.read_text()
    fams = {}
    for m in re.finditer(r'CreatureFamily\((\d+),.*?listOf\(([^)]*)\)\)', src, re.S):
        fams[int(m.group(1))] = [n.lower().replace(" ", "") for n in re.findall(r'"([^"]+)"', m.group(2))]
    art = int(re.search(r"FAMILIES_WITH_ARTWORK\s*=\s*(\d+)", src).group(1))
    assert sorted(fams) == list(range(12)) and all(len(f) == 6 for f in fams.values())
    return fams, art


def opponent_level(area, kind):
    return min(50, 2 + area + area // 16 + LEVEL_BONUS[kind])


def opponent_stats(area, kind):
    """Proposed replacement for CreatureStats.forOpponent: one ramp over areaIndex 0..47."""
    p, g, s, h = STAT_BONUS[kind]
    return dict(hp=30 + (7 * area) // 2 + h, p=5 + area // 3 + p, g=4 + area // 4 + g, s=5 + area // 5 + s)


def build():
    fams, _ = families()
    out = []
    for area in range(48):
        group, stage = divmod(area, 4)
        fam = fams[INTERIM.get(group, group)]
        for slot, (kind, form) in enumerate(LAYOUT[stage]):
            eid = "area-%02d/slot-%d" % (area, slot)
            out.append({"id": eid, "areaIndex": area, "slot": slot, "kind": kind, "creatureId": fam[form],
                        "opponentLevel": opponent_level(area, kind), "firstWinXp": 60 if eid == LEGACY["id"] else XP[kind]})
    return out


def write():
    note = ("Layout APPROVED (D-CAMPAIGN, batch 2); runtime still inactive. firstWinXp follows D-ECONOMY-XP except the "
            "shipped area-00/slot-0 (60, unchanged). Opponent stats are PROVISIONAL until the D-CURVE party simulation is reviewed.")
    lines = ["{", f'  "revision": "{REVISION}",', '  "status": "APPROVED_LAYOUT",', f"  \"note\": {json.dumps(note)},", '  "encounters": [']
    enc = build()
    lines += ["    " + json.dumps(e) + ("," if i < len(enc) - 1 else "") for i, e in enumerate(enc)]
    OUT.write_text("\n".join(lines + ["  ]", "}"]) + "\n")
    print(f"wrote {OUT.relative_to(ROOT)} ({len(enc)} encounters)")


def validate():
    fams, art = families()
    ids = {cid: (fi, k) for fi, forms in fams.items() for k, cid in enumerate(forms)}
    doc = json.loads(OUT.read_text())
    errs = []
    if doc.get("revision") != REVISION or doc.get("status") != "APPROVED_LAYOUT":
        errs.append(f"revision/status must be {REVISION} / APPROVED_LAYOUT (runtime stays inactive until D-CURVE)")
    enc = doc["encounters"]
    if enc != build():
        errs.append("JSON differs from the formulas in this tool; run --write")
    per_area = collections.defaultdict(list)
    for e in enc:
        if set(e) != set(LEGACY):
            errs.append(f"{e.get('id')}: wrong keys")
            continue
        if e["id"] != "area-%02d/slot-%d" % (e["areaIndex"], e["slot"]):
            errs.append(f"{e['id']}: id does not match area/slot")
        if e["creatureId"] not in ids:
            errs.append(f"{e['id']}: unknown creature {e['creatureId']}")
        elif ids[e["creatureId"]][0] >= art:
            errs.append(f"{e['id']}: {e['creatureId']} has no approved artwork")
        if e["kind"] not in KINDS:
            errs.append(f"{e['id']}: bad kind")
        if e["kind"] == "region_boss" and e["areaIndex"] % 4 != 3:
            errs.append(f"{e['id']}: region boss outside a stage-3 area")
        if not 1 <= e["opponentLevel"] <= 50:
            errs.append(f"{e['id']}: level out of range")
        per_area[e["areaIndex"]].append(e["slot"])
    if sorted(per_area) != list(range(48)):
        errs.append("every area-00..area-47 must have encounters")
    for a, slots in per_area.items():
        if sorted(slots) != list(range(len(slots))):
            errs.append(f"area-{a:02d}: slots not contiguous from 0")
    if enc and enc[0] != LEGACY:
        errs.append("area-00/slot-0 must stay exactly the shipped encounter")
    # The shipped encounter in code must still match the legacy row.
    src = ENCOUNTERS.read_text()
    # C2 moved the shipped encounter's 60 XP into LEGACY_FIRST_WIN_XP (D-ECONOMY-XP keeps it at 60).
    if "forZPetRule(RegionCatalog.area(0), slot = 0, boss = false)" not in src or "LEGACY_FIRST_WIN_XP = 60L" not in src:
        errs.append("Encounters.kt no longer ships the legacy area-00/slot-0 encounter this proposal assumes")
    if errs:
        print("CAMPAIGN PROPOSAL CHECK FAILED")
        print("\n".join(" - " + e for e in errs))
        return 1
    counts = collections.Counter(e["kind"] for e in enc)
    print(f"campaign proposal OK: {len(enc)} encounters " + ", ".join(f"{k} {counts[k]}" for k in KINDS) + f", {sum(e['firstWinXp'] for e in enc)} first-win XP")
    return 0


# ------------------------------------------------------------------ simulation (mirrors BattleEngine.act)
FORM = {0: (4, 4, 4), 1: (6, 5, 7), 2: (8, 7, 9), 3: (12, 10, 6), 4: (8, 7, 9), 5: (7, 7, 14)}


def player(form, level):
    p, g, s = FORM[form]
    p += (level - 1) // 3; g += (level - 1) // 4; s += (level - 1) // 5
    return dict(hp=45 + 3 * level + g, p=p, g=g, s=s)


def adv(a, d):
    if (a % 3 + 1) % 3 == d % 3:
        return 3
    if (d % 3 + 1) % 3 == a % 3:
        return -2
    return 0


def fight(form, pfam, level, e, efam):
    """One fighter, auto policy (Skill when ready). A party is approximated as the best single member."""
    P = player(form, level); php, ehp = P["hp"], e["hp"]; cd = burn = weak = 0
    for turn in range(1, 51):
        if cd == 0:
            hit = max(3, P["p"] + 9 - e["g"] // 2 + adv(pfam, efam)); cd = 3
            if pfam % 3 != 1: burn = 3
            else: weak = 3
        else:
            hit = max(2, P["p"] + 5 - e["g"] // 2); cd = max(0, cd - 1)
        ret = max(2, e["p"] + 4 - P["g"] // 2) + (5 if turn % 3 == 0 else 0)
        if weak > 0:
            ret = max(1, ret - 3); weak -= 1
        first = e["s"] > P["s"]
        if first: php = max(0, php - ret)
        if php > 0:
            ehp = max(0, ehp - hit)
            if burn > 0 and ehp > 0: ehp = max(0, ehp - 3); burn -= 1
            if not first and ehp > 0: php = max(0, php - ret)
        if ehp <= 0: return True
        if php <= 0: return False
    return False


def simulate(party):
    fams, _ = families()
    famof = {cid: fi for fi, forms in fams.items() for cid in forms}
    res = collections.defaultdict(collections.Counter)
    xp = 0
    for e in build():
        level = min(50, 1 + xp // party // 100)
        for label, form in (("Baby", 0), ("Young", 1), ("A-final", 3), ("B-final", 5)):
            wins = sum(fight(form, fam, level, opponent_stats(e["areaIndex"], e["kind"]), famof[e["creatureId"]]) for fam in (0, 1, 2))
            res[(e["areaIndex"] // 4, label)][e["kind"] + "_w"] += wins
            res[(e["areaIndex"] // 4, label)][e["kind"] + "_n"] += 3
        xp += e["firstWinXp"]
    print(f"Party of {party} splitting first-win XP; level at each fight = 1 + (xp / {party}) / 100")
    print("| Group | Form | " + " | ".join(KINDS) + " |")
    print("|---|---|" + "---|" * len(KINDS))
    for g in range(12):
        for label in ("Baby", "Young", "A-final", "B-final"):
            c = res[(g, label)]
            print(f"| group-{g} | {label} | " + " | ".join("%d%%" % round(100 * c[k + "_w"] / c[k + "_n"]) for k in KINDS) + " |")


if __name__ == "__main__":
    if "--write" in sys.argv:
        write()
    elif "--simulate" in sys.argv:
        simulate(int(sys.argv[sys.argv.index("--party") + 1]) if "--party" in sys.argv else 1)
    else:
        sys.exit(validate())
