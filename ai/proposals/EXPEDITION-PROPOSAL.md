# Expedition design proposal (Q17)

> **PROPOSAL — inactive and configurable; needs Zeus97x approval (D-EXPEDITION)**
> Co-owner: ZPet owner. Every number in this document is a *proposed, configurable* value. None of it is active in either app. Expedition configuration is **ZPet-owned**. ZBattle only displays expedition state and sends battle-participation events.

Written 2026-10-10 (America/Toronto) by Claude. Sources were read without changes: ZBattle main `9d09592`, ZPet main `1adcedb`.

---

## 1. Current state (evidence)

| Area | What exists | Evidence |
|---|---|---|
| ZPet expedition system | **Does not exist.** No class, field, SQL table or doc text mentions "expedition". | `grep -rniE expedition` across `/home/user/zpet` (Java, SQL, TS, md) returns no matches. |
| ZPet "Explore" | A UI screen only (Adventure hub → Explore/Arena/Capture). It is not a timed activity. | `PrototypeState.java:8,19,24`; `DEVELOPMENT_LOG.md` "Explore cleanup phases 1–4" |
| ZPet step economy | 500 accepted steps bank one battle encounter, 2,500 bank one capture attempt and 5,000 per route boss checkpoint. 250 steps give 1 training energy, at most 24 a day. Travel uses a global high-water mark, so steps are never counted twice. | `AdventureState.java:10-14` (`walk`, `queued = travel/500`), `WorldState.java:6` (`REGION_STEP_REQUIREMENT=5000`), `WorldState.java:90` (`steps/2500`), `BLUEPRINT.md:19-20` |
| Roadmap values | Durations 1/4/8/12 h. Battle acceleration 2/5/10/15/25/40 min for wild/campaign/mini/stage/region/mystical. Walking credit 1,000 steps = 10 min. Combined cap 50% of the original duration. All are marked "illustrative / unapproved". | `ai/CROSS_APP_ROADMAP.md:42`, `ai/tasks/CLAUDE-008-EXPEDITION-EVENTS.md:97` |
| Contract | `ExpeditionSnapshot` is produced by `zpet` and displayed read-only by ZBattle. Fields: `expeditionId, companionId, durationSeconds, startedAtUtc, eligibleEventIds[], creditedProgressSeconds, capRevision (^[a-z0-9-]+-r[0-9]+$), status (running/completed/claimed/cancelled), claimId`. `additionalProperties: false`. | `ai/integration/schemas/expedition-snapshot.schema.json`; fixture `fixtures/records/expedition-running.json` (`capRevision: "expedition-cap-r1"`) |
| Contract | `BattleCompleted` is produced by `zbattle`. Fields: `participantCompanionIds` (1–6), `encounterKind` ∈ wild/campaign/mini-boss/stage-boss/location-boss/region-boss/mystical/practice, `outcome` ∈ victory/defeat/retreat, `verification`, `settlementRef`. | `schemas/battle-completed.schema.json` |
| Contract rule | R12: participation credit goes only to actual participants of a non-practice victory. Sequence S10 checks that non-participants are excluded. | `CONTRACT-v0.2.md` §4; `fixtures/manifest.json` S10 |
| ZBattle engine | One active creature per battle (`BattleState.playerUid`). The 50-turn limit counts as a **defeat**. Rematches are practice and pay 0 XP. | `core/.../battle/BattleEngine.kt`; `core/.../battle/Encounters.kt` (`firstWinXp` "Rematches are practice") |
| Decided (Q16) | Acceleration needs a qualifying **victory** and **actual participation** by that companion. Losses, retreats and rewardless practice are excluded. Battle events are deduplicated. | Owner decision |
| Decided (Q4) | Party of 3, one active creature. Switching consumes the player's turn. Replacing a fainted creature is free. | Owner decision |

**Gap:** the roadmap list has no acceleration value for `location-boss`, although the schema includes that kind. This proposal fills the gap below and flags it in §9.

## 2. Goals
1. Expeditions reward walking and battling without making either one mandatory. A companion always finishes on its natural timer.
2. Each battle and each step is credited **at most once** per expedition. The result is the same whether events arrive out of order, are delivered twice or come from two devices.
3. ZPet stays the single authority for configuration and progress. ZBattle never computes authoritative expedition progress.
4. Everything sits behind a versioned config (`capRevision`), so values can change without changing the schema.
5. A companion on an expedition stays selectable in ZBattle (CLAUDE-008 E2).

## 3. Recommended design (all values proposed and configurable)

### 3.1 Expedition tiers
| Tier id | Duration | Proposed unlock | Notes |
|---|---|---|---|
| `short` | 1 h (3,600 s) | available at start | Matches the fixture `durationSeconds: 3600` |
| `medium` | 4 h (14,400 s) | companion has completed First bond (ZPet `Progression`) | |
| `long` | 8 h (28,800 s) | any area boss defeated in ZPet | |
| `overnight` | 12 h (43,200 s) | Area 2 unlocked in ZPet | |

| Setting | Proposed value |
|---|---|
| Concurrent expeditions per account | 1 (configurable, up to 3) |
| Expeditions per companion at a time | 1 |
| Cancellation | Allowed. Nothing is awarded and accumulated credit is lost (status `cancelled`). |
| Completion | Time is measured against the server clock (`acceptedAtUtc`). The local clock is used for display only. |

Expedition *rewards* (what a claim yields, for example `RewardEarned` with `eligibilityEvidence.kind = "expedition-claim"`) are ZPet's earning rules and are outside the scope of this document.

### 3.2 Battle acceleration per qualifying battle
| `encounterKind` | Proposed credit | Source |
|---|---|---|
| `wild` | 2 min (120 s) | roadmap |
| `campaign` | 5 min (300 s) | roadmap |
| `mini-boss` | 10 min (600 s) | roadmap |
| `stage-boss` | 15 min (900 s) | roadmap |
| `location-boss` | **20 min (1,200 s)** | *new, fills the gap between stage and region* |
| `region-boss` | 25 min (1,500 s) | roadmap |
| `mystical` | 40 min (2,400 s) | roadmap |
| `practice` | 0 | Q16 |

**A battle qualifies** for companion *C* on expedition *E* only if all of these hold:
1. `outcome == "victory"`. Defeat, retreat and the turn-limit defeat never qualify.
2. `encounterKind != "practice"`. ZBattle labels every rewardless battle as `practice`, including zero-reward rematches under the current rules (`Encounters.kt`). The "rewardless" rule therefore needs no new field.
3. *C* ∈ `participantCompanionIds` (see §3.4).
4. The battle was accepted after `E.startedAtUtc` and before *E* completes (server `acceptedAtUtc`).
5. The battle has not already been credited to *E*: deduplicate by **(expeditionId, battleId)**, in addition to the envelope's (accountId, eventId).
6. It meets the verification policy under D-OFFLINE-TRUST. Proposal: `unverified-client` results show as **pending** and only `server-settled` results add credit.

### 3.3 Walking acceleration
| Setting | Proposed value |
|---|---|
| Rate | 1,000 accepted steps = 10 min (600 s) |
| Granularity | Whole 1,000-step blocks; the remainder carries over within the same expedition |
| Baseline | ZPet's accepted-step high-water mark when the expedition starts, the same model as `AdventureState.walk` |
| Multiple running expeditions | Each expedition keeps its own baseline, so a step counts once *per expedition*. Whether one step may advance several expeditions is an open question (§9). |
| Step source | Accepted steps only. Sandbox/test steps are excluded, as in BLUEPRINT. |

### 3.4 Combined cap and the participation definition
| Setting | Proposed value |
|---|---|
| Combined cap (battles + walking) | 50% of `durationSeconds` |
| Cap order | Credit is applied in server acceptance order. Credit beyond the cap is discarded and recorded, never banked. |
| Minimum real time | `durationSeconds − cap` (for example, an 8 h expedition takes at least 4 h) |
| `capRevision` | `expedition-cap-r1` = this table |

**Actual participation (proposal for ZBattle E1 under the Q4 party rules).** A party member is a participant if it was the active creature when at least one player turn resolved. Free replacements count once they act or are hit. A member that was only selected or only on the bench is not a participant. If Advanced Combat is approved, a Duo Strike partner also counts (see ADVANCED-COMBAT-PROPOSAL.md). The schema's `maxItems: 6` already covers a party of 3.

### 3.5 Worked example (proposed values)
An 8 h `long` expedition, started at 10:00:
- 2 wild victories as participant: 4 min. One stage-boss victory: 15 min. One loss: 0. The same wild victory delivered twice: 0 the second time.
- 9,400 steps walked: 9 blocks = 90 min.
- Total 109 min of a 240 min cap (50% of 480). The expedition completes at 10:00 + 480 − 109 min = **16:11**.

## 4. How it plugs in

| Concern | Owner app | Record / field |
|---|---|---|
| Expedition config (tiers, rates, cap) | **ZPet** | ZPet-side config, versioned as `capRevision` (`expedition-cap-rN`) |
| Battle participation | ZBattle (producer) | `BattleCompleted.participantCompanionIds`, `encounterKind`, `outcome`, `verification`, `settlementRef` (no schema change) |
| Credit calculation and dedup | ZPet / trusted authority (Phase D backend) | Appends `eventId` to `ExpeditionSnapshot.eligibleEventIds` and updates `creditedProgressSeconds` |
| Display | ZBattle (E2, read-only) | Remaining time = `durationSeconds − creditedProgressSeconds − (now − startedAtUtc)`, shown as approximate. Battles still `pending` show "credit pending". |
| Reward on claim | ZPet earns, ZBattle redeems | `RewardEarned` with `eligibilityEvidence.kind="expedition-claim"`, `ref=claimId` |

**Optional contract v0.3 addition** (not required for v0.2): `battleCreditSeconds`, `walkingCreditSeconds` and `discardedOverCapSeconds` in `ExpeditionSnapshot`, so ZBattle can explain the totals. These would need a schema revision because `additionalProperties: false`.

**ZBattle code (Phase E only):** an outbox emitter after settlement in the battle settlement path, which already enforces "accepted only once per battleId" (`BattleState.battleId` doc). A per-battle participant set must be added to `BattleState` with B2. Today `playerUid` is a single value.

## 5. Migration and compatibility
- No ZPet save holds expedition data yet, so nothing needs migrating. ZPet adds a new persisted record with an `expeditionId` UUID.
- Changes to `capRevision` apply only to expeditions started under the new revision. A running expedition keeps the revision it started with.
- Old ZBattle builds that cannot read `ExpeditionSnapshot` get `UPDATE_REQUIRED` (R2). Battle events from old builds still qualify if they are schema-valid.
- Until this proposal is approved, ZBattle E1 may emit `BattleCompleted`, but ZPet credits nothing.

## 6. Test plan
| # | Case | Expected |
|---|---|---|
| T1 | Victory with C as participant | +kind credit once |
| T2 | Same `battleId` with a new `eventId` (bug or forgery) | 0 (dedup by expeditionId+battleId) |
| T3 | Same `eventId` replayed | `DUPLICATE_IGNORED` (R6) |
| T4 | Defeat, retreat, turn-limit defeat, `practice` victory | 0 |
| T5 | Party member that was never active | Not credited (S10 extended) |
| T6 | Free replacement after a faint, then victory | Credited |
| T7 | Battle accepted before `startedAtUtc` or after completion | 0 |
| T8 | Credit that crosses the 50% cap | Clamped, excess logged, cap never exceeded |
| T9 | Events arrive out of order across two devices | Same final `creditedProgressSeconds` |
| T10 | 999 steps, then +1 step | 0, then +600 s |
| T11 | A downward health correction of steps | No negative credit (high-water mark) |
| T12 | `unverified-client` victory | Shown as pending, no credit, under the D-OFFLINE-TRUST proposal |
| T13 | Clock skew (device 2 h ahead) | Completion follows the server time |
| T14 | `capRevision` change mid-expedition | The running expedition keeps r1 |
Add these as fixture sequences (S13+) in `tools/validate_contract.py` once the proposal is approved.

## 7. Rollout
1. ZPet implements the config at r1 behind a disabled flag.
2. ZBattle E1 emits events (already allowed once D-PARTICIPATION is approved).
3. ZPet credits only in sandbox accounts.
4. After approval, the flag is turned on for real accounts.

## 8. Not in scope
Expedition destinations, flavor events, lucky finds and personality effects (ZPet-owned, per CLAUDE-008). Lazy-companion penalties are not invented here.

## 9. Open questions
1. Approve 1/4/8/12 h and the unlock gates in §3.1, or start with `short` only?
2. `location-boss` = 20 min: accept or change?
3. May one step advance several concurrent expeditions (proposal: yes, each has its own baseline), or should steps be split?
4. Should unverified offline victories ever be credited later, once settled (proposal: yes, retroactively, if still within the window)?
5. Cancellation: lose all credit (proposal) or refund part of it?
6. Concurrent expedition limit per account: 1 (proposal) or 3?
7. Add the optional v0.3 breakdown fields?
