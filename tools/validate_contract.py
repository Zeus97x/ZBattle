#!/usr/bin/env python3
"""Validate the ZBattle <-> ZPet integration contract bundle (CLAUDE-004 / A2).

What it checks:
1. Every fixture under ai/integration/fixtures/records against the JSON Schemas (2020-12).
2. Catalogue coverage: species/area/group/form ids exist in the ZPet reference catalogues
   committed in ZBattle-ZPet-Assets/reference (verified identical to ZPet main in AUDIT-A1).
3. Sequence fixtures against a *reference* implementation of the contract acceptance rules
   (R1-R12 in CONTRACT-v0.2.md). This is executable documentation for both projects, not
   production code: real authority lives in the Phase D server.
4. The ZBattle v1 save -> companion mapping migration fixture.
5. The bundle hash in ai/integration/CONTRACT-BUNDLE.sha256 (A3 handoff), when present.

Usage: python3 tools/validate_contract.py [--print-hash]
Requires: jsonschema>=4.18 (pip install jsonschema==4.26.0).
"""
import hashlib
import json
import pathlib
import re
import sys

from jsonschema import Draft202012Validator
from referencing import Registry, Resource

ROOT = pathlib.Path(__file__).resolve().parent.parent
INTEGRATION = ROOT / "ai/integration"
SCHEMAS = INTEGRATION / "schemas"
FIXTURES = INTEGRATION / "fixtures"
REFERENCE = ROOT / "ZBattle-ZPet-Assets/reference"
BUNDLE_FILES = ["ai/integration/CONTRACT-v0.2.md", "ai/integration/schemas", "ai/integration/fixtures"]
HASH_FILE = INTEGRATION / "CONTRACT-BUNDLE.sha256"

PRODUCERS = {
    "BattleCompleted": {"zbattle"},
    "RewardRedeemed": {"zbattle"},
    "BossState": {"zbattle"},
    "RewardEarned": {"zpet"},
    "ExpeditionSnapshot": {"zpet"},
    "LineageSnapshot": {"zpet"},
    # CompanionSnapshot: produced by its origin app (checked against payload.originApp).
}
PRIMARY_ID = {
    "CompanionSnapshot": "companionId", "BattleCompleted": "battleId", "RewardEarned": "rewardId",
    "RewardRedeemed": "deliveryId", "ExpeditionSnapshot": "expeditionId", "BossState": "bossId",
    "LineageSnapshot": "lineageId",
}

failures = []


def fail(message):
    failures.append(message)


# ---------------------------------------------------------------- catalogues
def java_strings(source, field):
    start = source.index(field + "=") if field + "=" in source else source.index(field + " =")
    depth, i, open_at = 0, source.index("{", start), source.index("{", start)
    while True:
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                break
        i += 1
    return re.findall(r'"([^"]*)"', source[open_at:i])


def load_catalogue():
    regions = (REFERENCE / "RegionCatalog.java").read_text()
    species = (REFERENCE / "SpeciesCatalog.java").read_text()
    monsters = (REFERENCE / "MonsterCatalog.java").read_text()
    areas = java_strings(regions, "AREAS")
    traditions = java_strings(regions, "TRADITIONS")
    names = java_strings(species, "NAMES")
    forms = java_strings(monsters, "FAMILIES")
    assert len(areas) == 48 and len(traditions) == 12 and len(names) == 48 and len(forms) == 72
    return {
        "species": {f"{i // 4}:{i % 4}": n for i, n in enumerate(names)},
        "areas": {f"area-{i:02d}": n for i, n in enumerate(areas)},
        "groups": {f"group-{i}": t for i, t in enumerate(traditions)},
        "forms": {(i // 6, i % 6): n for i, n in enumerate(forms)},
    }


def catalogue_errors(env, cat):
    p, t = env["payload"], env["recordType"]
    errs = []
    if t == "CompanionSnapshot":
        if p["speciesId"] not in cat["species"]:
            errs.append(f"unknown species {p['speciesId']}")
        family = int(p["speciesId"].split(":")[0])
        if (family, p["formIndex"]) not in cat["forms"]:
            errs.append("unknown form")
    if t == "BattleCompleted":
        area = int(p["areaId"].split("-")[1])
        if p["areaId"] not in cat["areas"] or p["groupId"] != f"group-{area // 4}":
            errs.append(f"{p['areaId']} is not in {p['groupId']}")
        if not p["encounterId"].startswith(p["areaId"] + "/"):
            errs.append("encounterId area differs from areaId")
    return errs


# ---------------------------------------------------------------- schemas
def build_validator():
    registry = Registry()
    for path in SCHEMAS.glob("*.schema.json"):
        schema = json.loads(path.read_text())
        Draft202012Validator.check_schema(schema)
        resource = Resource.from_contents(schema)
        registry = registry.with_resource(schema["$id"], resource)
    envelope = json.loads((SCHEMAS / "envelope.schema.json").read_text())
    return Draft202012Validator(envelope, registry=registry)


def canonical(obj):
    return json.dumps(obj, sort_keys=True, separators=(",", ":"))


# ---------------------------------------------------------------- reference rules
class Authority:
    """Reference implementation of acceptance rules R1-R12 (CONTRACT-v0.2.md section 4)."""

    def __init__(self, validator, cat):
        self.validator, self.cat = validator, cat
        self.events = {}     # (account, eventId) -> canonical envelope
        self.records = {}    # (account, type, recordId) -> envelope
        self.rewards = set()
        self.redeemed = {}

    def submit(self, env, principal):
        if env.get("schemaVersion") != 1:
            return "UPDATE_REQUIRED"                                                  # R2
        if any(True for _ in self.validator.iter_errors(env)):
            return "REJECT_SCHEMA"                                                    # R3
        if env["accountId"] != principal:
            return "REJECT_ACCOUNT_MISMATCH"                                          # R1
        t, p = env["recordType"], env["payload"]
        allowed = {p["originApp"]} if t == "CompanionSnapshot" else PRODUCERS[t]
        if env["sourceApp"] not in allowed:
            return "REJECT_PRODUCER"                                                  # R4
        if env["recordId"] != p[PRIMARY_ID[t]]:
            return "REJECT_RECORD_ID"                                                 # R5
        if catalogue_errors(env, self.cat):
            return "REJECT_CATALOGUE"                                                 # R9
        key = (principal, env["eventId"])
        if key in self.events:                                                        # R6
            return "DUPLICATE_IGNORED" if self.events[key] == canonical(env) else "REJECT_REPLAY_MISMATCH"
        rkey = (principal, t, env["recordId"])
        prior = self.records.get(rkey)
        if prior and env["recordRevision"] <= prior["recordRevision"]:
            return "REJECT_STALE_REVISION"                                            # R7
        if t == "CompanionSnapshot":
            if env["recordRevision"] != p["sourceRevision"]:
                return "REJECT_REVISION_MISMATCH"                                     # R10
            if prior:
                q = prior["payload"]
                if any(p[f] != q[f] for f in ("originApp", "legacyLocalId", "speciesId")):
                    return "REJECT_IMMUTABLE_FIELD"                                   # R11
                if p["formIndex"] < q["formIndex"]:
                    return "REJECT_FORM_REGRESSION"                                   # R11
                if q["bondRevision"] is not None and (p["bondRevision"] or 0) < q["bondRevision"]:
                    return "REJECT_STALE_BOND"                                        # R8
        if t == "RewardRedeemed":
            if p["rewardId"] not in self.rewards:
                return "REJECT_UNKNOWN_REWARD"                                        # R12
            if p["state"] == "redeemed":
                if self.redeemed.get(p["rewardId"], 0) >= 1:
                    return "REJECT_ALREADY_REDEEMED"                                  # R12
                self.redeemed[p["rewardId"]] = self.redeemed.get(p["rewardId"], 0) + 1
        if t == "RewardEarned":
            self.rewards.add(p["rewardId"])
        self.events[key] = canonical(env)
        self.records[rkey] = env
        return "ACCEPTED"

    def companions(self):
        return [e for (a, t, r), e in self.records.items() if t == "CompanionSnapshot"]


def eligible(battle_env, companion_id):
    """R12 participation (D-PARTICIPATION): only actual participants of a non-practice victory qualify."""
    p = battle_env["payload"]
    return p["outcome"] == "victory" and p["encounterKind"] != "practice" and companion_id in p["participantCompanionIds"]


def origin_bonus_bp(bond_percent):
    """D-ORIGIN-ROUNDING: 10% + 2.5% per full 25% bond, capped at 20%; unknown bond counts as 0%."""
    return min(2000, 1000 + 250 * ((bond_percent or 0) // 25))


def origin_stat(base, bond_percent):
    return base * (10000 + origin_bonus_bp(bond_percent)) // 10000


# ---------------------------------------------------------------- bundle hash
def bundle_files():
    files = []
    for entry in BUNDLE_FILES:
        path = ROOT / entry
        files += sorted(path.rglob("*")) if path.is_dir() else [path]
    return [f for f in files if f.is_file()]


def bundle_hash():
    h = hashlib.sha256()
    for f in bundle_files():
        rel = f.relative_to(ROOT).as_posix()
        h.update(rel.encode() + b"\0" + hashlib.sha256(f.read_bytes()).hexdigest().encode() + b"\n")
    return h.hexdigest()


# ---------------------------------------------------------------- main
def main():
    if "--print-hash" in sys.argv:
        print(bundle_hash())
        return 0
    cat = load_catalogue()
    validator = build_validator()
    manifest = json.loads((FIXTURES / "manifest.json").read_text())
    records = {p.stem: json.loads(p.read_text()) for p in sorted((FIXTURES / "records").glob("*.json"))}
    if set(records) != set(manifest["records"]):
        fail(f"manifest/records mismatch: {sorted(set(records) ^ set(manifest['records']))}")

    for name, expected in manifest["records"].items():
        env = records[name]
        if env.get("schemaVersion") != 1:
            got = "UPDATE_REQUIRED"
        else:
            errors = list(validator.iter_errors(env))
            got = "SCHEMA_VALID" if not errors else "REJECT_SCHEMA"
        if got != expected:
            fail(f"{name}: expected {expected}, got {got}")
        if got == "SCHEMA_VALID":
            for e in catalogue_errors(env, cat):
                if name != "battle-area-group-mismatch":
                    fail(f"{name}: catalogue: {e}")

    for seq in manifest["sequences"]:
        auth = Authority(validator, cat)
        for i, step in enumerate(seq["steps"]):
            got = auth.submit(records[step["record"]], step["as"])
            if got != step["expect"]:
                fail(f"{seq['id']} step {i + 1} ({step['record']}): expected {step['expect']}, got {got}")
        final = seq.get("final", {})
        if "companions" in final and len(auth.companions()) != final["companions"]:
            fail(f"{seq['id']}: expected {final['companions']} companions, got {len(auth.companions())}")
        if "companion" in final:
            want = final["companion"]
            env = next((e for e in auth.companions() if e["recordId"] == want["id"]), None)
            if env is None:
                fail(f"{seq['id']}: companion {want['id']} missing")
            else:
                for k, v in want.items():
                    actual = env["recordRevision"] if k == "recordRevision" else env["payload"].get("companionId" if k == "id" else k)
                    if actual != v:
                        fail(f"{seq['id']}: companion {k} expected {v}, got {actual}")
        if "rewardRedemptions" in final and sum(auth.redeemed.values()) != final["rewardRedemptions"]:
            fail(f"{seq['id']}: redemptions {sum(auth.redeemed.values())}")
        if "eligibility" in seq:
            battle = records[seq["eligibility"]["battle"]]
            for c in seq["eligibility"]["eligible"]:
                if not eligible(battle, c):
                    fail(f"{seq['id']}: {c} should be eligible")
            for c in seq["eligibility"]["notEligible"]:
                if eligible(battle, c):
                    fail(f"{seq['id']}: {c} must not be eligible")

    examples = json.loads((FIXTURES / "origin-bonus-examples.json").read_text())
    for case in examples["cases"]:
        bp, stat = origin_bonus_bp(case["bondPercent"]), origin_stat(case["base"], case["bondPercent"])
        if (bp, stat) != (case["bonusBp"], case["stat"]):
            fail(f"origin bonus {case}: computed bonusBp={bp} stat={stat}")

    mapping_path = FIXTURES / "migration/zbattle-v1-companion-mapping.json"
    if mapping_path.exists():
        mapping = json.loads(mapping_path.read_text())
        ids = [m["companionId"] for m in mapping["companions"]]
        if len(set(ids)) != len(ids):
            fail("migration: companionIds not unique")
        for m in mapping["companions"]:
            if m["speciesId"] not in cat["species"]:
                fail(f"migration: unknown species {m['speciesId']}")
            if not re.fullmatch(r"zb-uid-[1-9][0-9]*", m["legacyLocalId"]):
                fail(f"migration: bad legacy id {m['legacyLocalId']}")
    else:
        fail("migration fixture missing")

    if HASH_FILE.exists():
        recorded = HASH_FILE.read_text().split()[0]
        if recorded != bundle_hash():
            fail(f"bundle hash {bundle_hash()} != recorded {recorded}; re-run with --print-hash and update the handoff")

    valid = sum(1 for v in manifest["records"].values() if v == "SCHEMA_VALID")
    if failures:
        print("CONTRACT CHECK FAILED")
        for f in failures:
            print(" -", f)
        return 1
    print(f"contract OK: {len(records)} records ({valid} schema-valid), {len(manifest['sequences'])} sequences, bundle {bundle_hash()[:12]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
