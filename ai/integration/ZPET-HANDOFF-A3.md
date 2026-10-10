# ZPet handoff — contract v0.2 (CLAUDE-004 / A3)

Written 2026-10-10 (America/Toronto) by Claude, from ZBattle branch `claude/zbattle-phase-a-contract`. ZPet's repository was **not** modified; this note is for the ZPet project's owner and assistant. It extends [ZPET-COUNTERPART-HANDOFF.md](ZPET-COUNTERPART-HANDOFF.md).

## Contract bundle to mirror
| Item | Value |
|---|---|
| Contract revision | `zb-zp-contract-0.2-draft` |
| Bundle | `ai/integration/CONTRACT-v0.2.md`, `ai/integration/schemas/`, `ai/integration/fixtures/` (46 files) |
| Bundle SHA-256 | `77d69eb17081a088bd66a2f34ac4fe6350730b3749def59b9c2d914d8b6fa41a` (batch-1 amendment; replaces Phase A's `afc3a8de…`) |
| How it is computed | `python3 tools/validate_contract.py --print-hash`: SHA-256 over sorted `path\0sha256(file)\n` lines |
| ZBattle commit | the PR commit that adds this file (record it on merge) |

A matching title is not enough. The ZPet copy is accepted only when its bundle hash equals the value above, or when a newer revision is agreed and both repositories record the new hash.

## Owner decision Q3 (2026-10-10)
Zeus97x assigned contract adoption to **the Claude session working in ZPet** (ZPet Claude). ZPet Claude:
- reviews v0.2 against ZPet's real saves
- copies schemas and fixtures unchanged
- verifies the full bundle hash
- supplies a real migration fixture
- reports any incompatibilities **before** accepting

Neither copy may be silently altered. The ZBattle session (this repo) never writes to ZPet.

Batch 1 amended the contract. The hash above is the amended one; adopt only the hash recorded in the latest merged version of this file. What changed:

| Change | Decision | Files |
|---|---|---|
| `participantCompanionIds` max 6 → 3; new invalid fixture `invalid-party-of-four` | D-PARTY | `schemas/battle-completed.schema.json`, `fixtures/records/invalid-party-of-four.json`, `fixtures/manifest.json` |
| `practice` defined as a rewardless battle, excluded from participation | D-PARTICIPATION | `schemas/battle-completed.schema.json`, contract R12 |
| Origin bonus: +10% at 0% bond, unknown bond = 0%, never compounds; worked cases | D-ORIGIN-ROUNDING | contract G4/G6, `fixtures/origin-bonus-examples.json` |
| Rarity display mapping; native species `family:0` | D-RARITY, D-NATIVE-SPECIES | contract G2 |
| Receiving a report is not verification | D-OFFLINE-TRUST | contract G7 |

## What the ZPet project needs to do (ZPet-owned; not done by ZBattle Claude)
1. **Copy the bundle byte-for-byte** into ZPet, wherever ZPet's conventions put it. Run the validator, or a port of it, and confirm the hash.
2. **Check the audit findings about ZPet** in [AUDIT-A1.md](AUDIT-A1.md) §2 and §3 (G1–G8). Correct anything wrong; that is a contract change and needs a new hash.
3. **Add a ZPet save fixture** (`pet-N` → `companionId`), mirroring ZBattle's `fixtures/migration/`. It must be a real `WorldState` encoding, with a proposal for assigning UUIDs once that survives cloud restore, `PortableSave` import and the separate normal/test profiles.
4. **Apply the decided rules** in [DECISIONS.md](DECISIONS.md) (batch 1): D-RARITY, D-NATIVE-SPECIES, D-EVOLUTION, D-ORIGIN-ROUNDING, D-PARTICIPATION and D-LEGACY. D-EXPEDITION is a proposal that needs approval.
5. **Report back** the ZPet commit SHA and the bundle hash it validated, so the ZBattle log can record it (D-CONTRACT-ACCEPT).

## Status
**Awaiting the ZPet project.** No ZPet copy exists yet. The Phase A gate ("shared contract accepted by both project owners") is open until step 5 is done.
