# Extended backlog — status and claims
Updated 2026-10-10 America/Toronto. Owner: Claude.

The 62 EXT items are defined in `ai/CLAUDE_EXTENDED_BACKLOG.md`, which is added by [PR #9](https://github.com/Zeus97x/ZBattle/pull/9) (open, not merged at the time of writing). This file tracks their status against the source code, so that PR #9's file stays unchanged.

Reconciled against `main` at `400d46b` (PRs #2–#18 merged). Each row was checked against the code or documents named in it. A merged PR alone does not make an item Done.

| Status | Meaning |
|---|---|
| **Done** | Acceptance criteria met by merged work; evidence named |
| **Partial** | Some criteria met; the remaining gap is named |
| **Claimed** | Being worked on now; owner and branch named |
| **Blocked** | Needs a named decision, artwork or a counterpart producer |
| **Queued** | Its phase is not yet approved for runtime work |

## Summary
| Phase | Done | Partial | Claimed | Blocked | Queued |
|---|---|---|---|---|---|
| A (EXT-001–008) | 7 | 1 | 0 | 0 | 0 |
| B (EXT-009–020) | 7 | 2 | 1 | 2 | 0 |
| C (EXT-021–030) | 0 | 0 | 0 | 0 | 10 |
| D (EXT-031–040) | 0 | 0 | 0 | 0 | 10 |
| E (EXT-041–046) | 0 | 0 | 0 | 0 | 6 |
| F (EXT-047–054) | 0 | 0 | 0 | 0 | 8 |
| G (EXT-055–062) | 0 | 0 | 0 | 0 | 8 |

Phase B is at its boundary: B1–B5 are merged. The remaining B items either need decisions or are the accessibility work claimed below. Phase C runtime work waits for user approval (see the checkpoint at the end).

## A — Coordination (CLAUDE-004)
| ID | Status | Evidence / gap |
|---|---|---|
| EXT-001 | Done | `ai/integration/AUDIT-A1.md` §1–2 (implemented vs missing, with commits); this file updates it to `400d46b` |
| EXT-002 | Done | AUDIT-A1 §1 (12 groups / 48 areas, verbatim ZPet copy) and G5 (no element data in either app, flagged) |
| EXT-003 | Done | AUDIT-A1 §2 / G1; `fixtures/migration/zbattle-v1-companion-mapping.json`; B3 assigns stable companion UUIDs (codec v3). The ZPet `pet-N` side is ZPet-owned |
| EXT-004 | Done | `fixtures/records/companion-*` (native, imported r1/r2, bonded, correction, form regression, duplicate species); bundle hash `77d69eb1…` in `CONTRACT-BUNDLE.sha256`. Review by the ZPet owner is tracked by D-CONTRACT-ACCEPT |
| EXT-005 | Partial | Fixtures cover earned, redeemed, redeem-rejected and retry-with-changed-payload (`REJECT_REPLAY_MISMATCH`). **Gap:** no fixture records the `accepted` or `acknowledged` inbox states. Adding one changes the bundle hash, so it is held until ZPet Claude reports on v0.2 (D-CONTRACT-ACCEPT) |
| EXT-006 | Done | `CONTRACT-v0.2.md` §2 authority, §4 R1–R12, §6 compatibility; fixtures `unknown-schema-version`, `invalid-unknown-record-type`, stale-revision sequences |
| EXT-007 | Done | `ai/proposals/BACKEND-AUDIT.md` (repository files only; the live project was not inspected); contract `verification: unverified-client` |
| EXT-008 | Done | `ai/PHASE-A-B1-SUMMARY-AND-QUESTIONS.md` and `ai/integration/DECISIONS.md` (batch 1 decided; four open items) |

## B — Battle automation and progression (CLAUDE-005)
| ID | Status | Evidence / gap |
|---|---|---|
| EXT-009 | Done | `core/.../battle/AutoFight.kt` (`AutoFight.choose`); `AutoFightTest` (legal moves, manual equivalence, deterministic). PR #11 |
| EXT-010 | Done | `BattleScreen` single `LaunchedEffect(battleId, turn)`; `BattleProgress.autoStep` ignores stale or duplicate steps. PR #11 |
| EXT-011 | Done | `MainActivity.onStop`, navigation, dialogs and settlement stop auto-fight; `RepeatSession.interrupted()` never resumes by itself. PRs #11, #17 |
| EXT-012 | Done | `RepeatSession` counts played, wins and XP from settled `BattleResult`s, with end reason `Completed/Defeat/Interrupted`. PR #17 |
| EXT-013 | Partial | Bounded repeat sessions (3/5/10), stop on defeat, replays only of cleared encounters; there is no playable boss yet. **Gap:** replay quantities pay 0 until ECONOMY Q-E1/Q-E2 are approved; D-REPEAT-SESSION (max 10) is unconfirmed |
| EXT-014 | Done | Party of 3, switching and the participant ledger (`PartyBattleTest`, 10 tests). PR #14. D-PARTY-XP and D-SWITCH-COOLDOWN still need confirmation |
| EXT-015 | Done | `BattleProgressCodec` v4 reads v1–v4; frozen v1 golden file (`ContractMigrationFixtureTest`). PRs #14, #15, #17 |
| EXT-016 | Blocked | Identity, form guard and unlock ledger done (`Evolution.kt`, PR #15). Activation needs **D-EVOLUTION-THRESHOLDS** (Zeus97x + ChatGPT + ZPet Claude) |
| EXT-017 | Partial | Stable `Encounter.id` and `EncounterKind`; `ai/proposals/campaign-proposal.json` validated but inactive. Runtime still has exactly one playable encounter, by design. **Gap:** registry loading waits for D-CAMPAIGN approval |
| EXT-018 | Blocked | Needs approval of `ai/proposals/CAMPAIGN-PROPOSAL.md`, including Q-F (difficulty versus Option B XP) |
| EXT-019 | Done | `BattleProgress.settle`: retreat and defeat pay 0; `settledThrough` and the active-id guard prevent double settlement (`ReplayTest`, `BattleProgressTest`) |
| EXT-020 | Claimed | Owner Claude; branch `ccr-79612a33-kjesjl`, base `400d46b`. Scope: in-battle explanations of Skill cooldown, active Burn/Weaken effects and the outcome; 48dp targets; renders at 360/412dp and 130% text. No rule or value changes. "Enemy intent" is left out: the engine has no announced intent, and adding one would be a gameplay change |

## C–G — Queued
Every item in phases C–G is runtime work in a phase that has not been approved. Each needs the phase approval plus the inputs below. Proposals are already written, so these items can start as soon as the inputs arrive.

| Phase | Items | Inputs needed before work starts |
|---|---|---|
| C (CLAUDE-006) | EXT-021–030 | Approval of `ECONOMY-PROPOSAL.md` (coins C1, replay Q-E1/Q-E2). Ticket tables are already recorded (Rare 70/30; Epic 50/35/15; Legendary 40/30/21/9) and D-WEEK-WINDOW is decided (EXT-028 is anchored, not rolling). EXT-029 exclusives are blocked by D-EXCLUSIVE-POOL (ChatGPT designs and art). EXT-025 uses existing art only |
| D (CLAUDE-007) | EXT-031–040 | D-CONTRACT-ACCEPT (ZPet adoption of v0.2), D-BACKEND choice from `BACKEND-AUDIT.md`, and a matching ZPet producer. EXT-036/037 also need deployment authorization. D-ORIGIN-ROUNDING (EXT-032) is decided |
| E (CLAUDE-008) | EXT-041–046 | Approval of `EXPEDITION-PROPOSAL.md` values with the ZPet owner; Phase D transport. D-PARTICIPATION is decided and the B2 ledger already records participants |
| F (CLAUDE-009) | EXT-047–054 | Approval of `ADVANCED-COMBAT-PROPOSAL.md` and `ELEMENTS-PROPOSAL.md`; ZPet personality/relationship producers (EXT-053/054); approved dialogue text (EXT-052) |
| G (CLAUDE-010) | EXT-055–062 | D-LEGACY details (successor rules), ChatGPT emblem/flare art (EXT-056), approved portal/corrupted/world-boss content, and a ZPet lineage producer. EXT-062 never publishes without an explicit request |

## Checkpoint (2026-10-10)
- **Completed by existing work:** EXT-001–004, 006–012, 014, 015, 019 (PRs #10–#18).
- **Claimed now:** EXT-020 (battle accessibility, no rule changes).
- **Decisions that unblock the most work:**
  1. Approve or adjust `ECONOMY-PROPOSAL.md`. This unblocks Phase C and EXT-013 replay quantities.
  2. Approve `CAMPAIGN-PROPOSAL.md` and pick a Q-F resolution. This unblocks EXT-017/018.
  3. Confirm the B2 open items D-PARTY-XP, D-SWITCH-COOLDOWN and D-REPEAT-SESSION.
  4. Supply D-EVOLUTION-THRESHOLDS, with ChatGPT and ZPet Claude. This unblocks EXT-016.
- **Next:** finish EXT-020, then stop at the Phase B boundary for approval before any Phase C runtime work.
