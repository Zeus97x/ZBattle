# Shared integration contract — draft v0.2

- **Status:** PROPOSED. This is not a deployed API.
- **Revision:** `zb-zp-contract-0.2-draft`.
- **Supersedes:** [CONTRACT-v0.1.md](CONTRACT-v0.1.md), which is kept for history.
- **Written:** 2026-10-10 (America/Toronto) by Claude, as CLAUDE-004 Phase A sub-PR A2.
- **Amended:** 2026-10-10 with Zeus97x decision batch 1 (Q4, Q8, Q13–Q16; see [DECISIONS.md](DECISIONS.md)). This changed the bundle hash. The revision string is unchanged because no copy had been adopted yet.

v0.2 turns the v0.1 prose into machine-checkable artefacts, grounded in the evidence in [AUDIT-A1.md](AUDIT-A1.md):
- JSON Schemas (draft 2020-12): [schemas/](schemas/)
- Fixtures and sequence scenarios: [fixtures/](fixtures/) (`manifest.json` lists the expected outcome of each)
- Reference rule checker: `tools/validate_contract.py`, which runs in CI

The schemas and fixtures are the contract. This document explains them.

## 1. Changes from v0.1 (each resolves an audit gap)
| Gap | v0.2 resolution (proposal) |
|---|---|
| G1 identity | `companionId` is a UUID assigned **once** by the origin app's migration and persisted. It is never derived from `pet-N` or `uid`. The old id is kept as `legacyLocalId` (`pet-N` for ZPet, `zb-uid-N` for ZBattle), and its prefix must match `originApp`. |
| G2 species | `speciesId` = ZPet `family:rarity`, with the canonical rarity ids 0–3 unchanged. Display names (D-RARITY): 0 Common = Common, 1 Heroic = Rare, 2 Mythic = Epic, 3 Celestial = Legendary. ZBattle-native starters use their family's Common species `family:0` (D-NATIVE-SPECIES). Species stays separate from the instance `companionId` and from the form. |
| G3 form | `formIndex` 0–5 plus `branch` `none`/`A`/`B`. Forms 0–1 are `none`, 2–3 are `A`, 4–5 are `B`; the schema enforces this. Form names are display only. |
| G4 bond | `bondPercent` and `bondRevision` are nullable together. `null` means no ZPet bond producer exists yet. For the origin bonus it counts as **0%**, so the +10% base still applies (D-ORIGIN-ROUNDING). |
| G5 element | `elementId` is nullable and its value set is pending (**D-ELEMENT**). Battle advantage stays as implemented until elements are approved. |
| G6 origin bonus | **Decided (D-ORIGIN-ROUNDING).** Only a **verified** ZPet-origin companion qualifies: its CompanionSnapshot must have been accepted by the delivery authority. Bonus % = `10 + 2.5 × floor(bondPercent/25)`, capped at 20; unknown bond = 0%. In integers: `bonusBp = min(2000, 1000 + 250*floor(bond/25))`, `stat = floor(base * (10000 + bonusBp) / 10000)`. Applied once, to the unmodified derived HP/Power/Guard/Speed, with the bond snapshotted at fight start. It is recomputed from base stats each time and never stored on top of a previous bonus, so a re-import cannot compound it. Worked cases: [fixtures/origin-bonus-examples.json](fixtures/origin-bonus-examples.json), checked by the validator. |
| G7 verification | Every `BattleCompleted` carries `verification` = `unverified-client` or `server-settled`. `server-settled` requires a `settlementRef`. Client UUIDs deduplicate; they never prove a result. **D-OFFLINE-TRUST:** a server *receiving* a phone report is not verification. `server-settled` may only be used when the delivery authority has re-validated the result, and those checks must be documented (see the backend audit). Offline play keeps local progress, but cross-app rewards and progression stay pending until validated. |
| G8 account | `accountId` = an authenticated account UUID. The account system and backend for ZBattle are pending (**D-BACKEND**). |

## 2. Authority (who may produce what)
| Record | Producer (`sourceApp`) | Owner of truth | Consumer use |
|---|---|---|---|
| CompanionSnapshot | the companion's `originApp` | origin app (identity, species, form, nickname, bond) | Other app: read/display; battle input only |
| BattleCompleted (party ≤ 3, D-PARTY) | `zbattle` | ZBattle (combat rules and XP); trusted settlement for rewards | ZPet: expedition participation credit only |
| RewardEarned | `zpet` | ZPet (earning rules) | ZBattle: inbox → atomic redemption |
| RewardRedeemed | `zbattle` | ZBattle inventory transaction | ZPet: delivery history |
| ExpeditionSnapshot | `zpet` | ZPet | ZBattle: read-only display |
| BossState | `zbattle` | trusted settlement authority (Phase D) | display, timers |
| LineageSnapshot | `zpet` | ZPet | ZBattle: cosmetic display; the schema forbids combat fields |

Battle XP, battle level and combat inventory are ZBattle-owned and never overwrite ZPet training XP (ZPet XP is capped at 4900). Neither app writes the other's whole save.

## 3. Envelope
See [schemas/envelope.schema.json](schemas/envelope.schema.json).

- **Required fields:** `schemaVersion` (1), `contractRevision`, `eventId`, `accountId`, `sourceApp`, `sourceDeviceId` (pseudonymous install UUID), `recordType`, `recordId`, `recordRevision`, `createdAtUtc`, `payload`.
- **Server-only fields:** `acceptedAtUtc` and `authoritativeRevision`.
- **Format:** UUIDs are lower-case; timestamps are UTC and end in `Z`.

## 4. Acceptance rules and error codes
Rules are evaluated in this order. `tools/validate_contract.py` (`Authority.submit`) is the reference implementation, and the production authority must give the same answer for every sequence in `fixtures/manifest.json`.

| Rule | Check | Result code |
|---|---|---|
| R2 | `schemaVersion` known | `UPDATE_REQUIRED`: show "update needed"; nothing partially imported |
| R3 | Schema valid | `REJECT_SCHEMA` |
| R1 | `accountId` equals the authenticated principal | `REJECT_ACCOUNT_MISMATCH` |
| R4 | `sourceApp` is allowed to produce this `recordType` (table §2) | `REJECT_PRODUCER` |
| R5 | `recordId` equals the payload's primary id (`deliveryId` for RewardRedeemed) | `REJECT_RECORD_ID` |
| R9 | Catalogue: species/form exist; `areaId` ∈ `groupId`; `encounterId` area = `areaId` | `REJECT_CATALOGUE` |
| R6 | `(accountId, eventId)` already seen | `DUPLICATE_IGNORED` if the payload is byte-identical (canonical JSON); otherwise `REJECT_REPLAY_MISMATCH` |
| R7 | `recordRevision` greater than the stored revision for `(accountId, recordType, recordId)` | `REJECT_STALE_REVISION` |
| R10 | Companion `recordRevision` = `payload.sourceRevision` | `REJECT_REVISION_MISMATCH` |
| R11 | Companion `originApp`, `legacyLocalId` and `speciesId` are immutable; `formIndex` never decreases | `REJECT_IMMUTABLE_FIELD`, `REJECT_FORM_REGRESSION` (correction policy pending **D-EVOLUTION**) |
| R8 | `bondRevision` never decreases; a newer revision may lower the percent (a correction) | `REJECT_STALE_BOND` |
| R12 | Redemption references a known reward and redeems at most once. **D-PARTICIPATION:** participation credit only for actual participants (active for at least one turn) of a qualifying **victory**. Losses, retreats and rewardless `practice` battles are excluded, and battle events are deduplicated by `eventId`/`battleId`. | `REJECT_UNKNOWN_REWARD`, `REJECT_ALREADY_REDEEMED` |
| — | Everything passed | `ACCEPTED` |

The deduplication scope is `accountId + eventId`, plus domain claim ids (`rewardId`, `deliveryId`, settlement ids). The same `eventId` under a different account is a different event, and R1 already rejects foreign submissions.

## 5. Delivery and recovery (unchanged from v0.1, now testable)
- **Outbox:** the record is persisted before sending. A retry reuses the `eventId` and a frozen payload (fixture sequence S1).
- **Inbox states:**

  | State | Meaning |
  |---|---|
  | `pending` | Delivered, waiting for redemption |
  | `accepted` | Checks passed, not yet redeemed |
  | `rejected` | Refused; carries `failure.code` and `failure.retryable` |
  | `redeemed` | Applied once, atomically |
  | `acknowledged` | Producer confirmed; acknowledge only after a durable commit |

- **Redemption:** consumes the reward, settles exactly one roll, adds the inventory and stores the outcome in **one** transaction. A retry never re-rolls (S12).
- **Out-of-order delivery:** stale revisions are rejected (S2), evolution cannot revert (S8), and bond corrections are allowed only through a newer bond revision (S7).
- **Unknown data:** unknown schema versions give UPDATE_REQUIRED (S11). Unknown catalogue ids are rejected and kept for recovery, never dropped.

## 6. Compatibility matrix
| Producer → Consumer | Contract revision | ZBattle build | ZPet build | Status |
|---|---|---|---|---|
| any | `zb-zp-contract-0.2-draft` | none implements it yet (main `d2f938d`) | none implements it yet (main `1adcedb`) | Draft fixtures only |

Both repositories must hold byte-identical `schemas/` and `fixtures/` (checked by bundle hash, A3) before any implementation claims compatibility. Required-version negotiation: a consumer that does not know `schemaVersion` returns UPDATE_REQUIRED.

## 7. Migration and old-save fixtures
- **ZBattle v1 → v2 identity:** [fixtures/migration/](fixtures/migration/) contains a **real** `BattleProgress` v1 encoding (golden file, checked by `core/.../ContractMigrationFixtureTest.kt`) and the proposed `zb-uid-N` → `companionId` mapping. The v2 save itself is Phase D1 work and does not exist yet.
- **ZPet `pet-N` → `companionId`:** owned and implemented by the ZPet project. No ZPet save fixture exists here, so the ZPet project should add one in its counterpart copy (recorded as a gap).

## 8. Privacy and security (carried from v0.1)
- No service keys, session copies or publishable keys in contract documents.
- Owner-scoped transport and two-account isolation.
- No backend deployment is authorised by this document.

Pending decisions with owners are listed in [DECISIONS.md](DECISIONS.md).
