# ZPet handoff — contract v0.2 (CLAUDE-004 / A3)

Written 2026-10-10 (America/Toronto) by Claude, from ZBattle branch `claude/zbattle-phase-a-contract`. ZPet's repository was **not** modified; this note is for the ZPet project's owner and assistant. It extends [ZPET-COUNTERPART-HANDOFF.md](ZPET-COUNTERPART-HANDOFF.md).

## Contract bundle to mirror
| Item | Value |
|---|---|
| Contract revision | `zb-zp-contract-0.2-draft` |
| Bundle | `ai/integration/CONTRACT-v0.2.md`, `ai/integration/schemas/`, `ai/integration/fixtures/` (44 files) |
| Bundle SHA-256 | `afc3a8de1367e1ffa0d684463ed81ddb020ccc18cbbc51f36146e6202a83e2c4` |
| How it is computed | `python3 tools/validate_contract.py --print-hash`: SHA-256 over sorted `path\0sha256(file)\n` lines |
| ZBattle commit | the PR commit that adds this file (record it on merge) |

A matching title is not enough. The ZPet copy is accepted only when its bundle hash equals the value above, or when a newer revision is agreed and both repositories record the new hash.

## What the ZPet project needs to do (ZPet-owned; not done by Claude)
1. **Copy the bundle byte-for-byte** into ZPet, wherever ZPet's conventions put it. Run the validator, or a port of it, and confirm the hash.
2. **Check the audit findings about ZPet** in [AUDIT-A1.md](AUDIT-A1.md) §2 and §3 (G1–G8). Correct anything wrong; that is a contract change and needs a new hash.
3. **Add a ZPet save fixture** (`pet-N` → `companionId`), mirroring ZBattle's `fixtures/migration/`. It must be a real `WorldState` encoding, with a proposal for assigning UUIDs once that survives cloud restore, `PortableSave` import and the separate normal/test profiles.
4. **Answer the ZPet-owned decisions** in [DECISIONS.md](DECISIONS.md): D-RARITY, D-NATIVE-SPECIES, D-EVOLUTION, D-EXPEDITION, D-LEGACY and D-CONTRACT-ACCEPT.
5. **Report back** the ZPet commit SHA and the bundle hash it validated, so the ZBattle log can record it (D-CONTRACT-ACCEPT).

## Status
**Awaiting the ZPet project.** No ZPet copy exists yet. The Phase A gate ("shared contract accepted by both project owners") is open until step 5 is done.
