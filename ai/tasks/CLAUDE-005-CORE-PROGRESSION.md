# CLAUDE-005 — Phase B: Core progression and auto battle
Updated 2026-10-10 America/Toronto.
Owner: Claude. Status: IN PROGRESS — B1 in review (#11); B2–B5 authorized by Zeus97x decision batch 1 (2026-10-10).
Claim fields:
- Implementation branch: `claude/zbattle-b1-auto-fight` (stacked on `claude/zbattle-phase-a-contract`, Phase A PR not yet merged)
- Base commit: d24ade8
- Started: 2026-10-10 America/Toronto, after Zeus97x said "keep going" past the Phase A review stop
- Subtask owner: Claude (B1)
- Current PR: not opened (GitHub connector session error); compare link in DEVELOPMENT_LOG.md

## Read first
AGENTS.md, DEVELOPMENT_LOG.md, ai/README.md, ai/CROSS_APP_ROADMAP.md, ai/integration/CONTRACT-v0.1.md and relevant preceding task summaries. Inspect latest main and active PRs; never overwrite concurrent changes.

## Dependencies
A; approved B gameplay configuration. Do not wait for final art to implement valid placeholders.

## Scope and ordered sub-PRs
Paths: core/.../battle/{BattleEngine,Stats,Encounters,BattleProgress,BattleProgressCodec}.kt; core/Profile.kt and Regions.kt; ui/{AppState,BattleScreen,ChallengesScreen,HomeScreen,Overlays}.kt; app/PrefsSettingsStore.kt; core tests and preview/LayoutRenderTest.kt.
B1: optional auto-fight using the same manual engine with pure legal-action policy (skill cooldown/effect awareness, future switch readiness). Stop/take-control, one scheduler, lifecycle cancellation, no actions after victory or duplicate settlement. Foreground-only is proposed; confirm scope first. Keep manual flow usable. No automatic item use by default.
B2: selected party and switching with participant ledger; preserve owned identity, save migration and turn recovery. Approve party size and switch turn cost before coding.
B3: evolution integration, separate battle XP from source companion form authority; retain existing artwork/forms only.
B4: configuration-driven wild encounters + numbered campaign + mini/stage/location/region bosses across exact areas. Approve counts, rosters, gates and difficulty curves; no invented final content. Legacy cleared progress retained through migration.
B5: repeat-battle session controls, win/XP summary, stop on defeat, bounded count and explicit restart after interruption. Approve replay reward quantities first: current rematches pay zero XP and each battle starts at full HP, so low-HP-between-fights controls would be misleading without approved persistence changes.
Acceptance: manual/auto use identical engine/rewards; no overlapping turns; all approved slots reachable; local saves preserved; first/replay rewards explicit; no resource/item depletion without configured consent.
Validation: deterministic manual/auto equivalence, interruption/restart, turn cap/defeat/retreat, switch participants, cooldowns, evolution migration, gates and duplicate settlement. Android compile/lint + preview tests; 360/412dp/large-text renders; device lifecycle checks if available.

## Delivery rules
Use focused feature branches and small PRs in the specified order. Record source paths, rule/config revision, actual validation, unavailable device checks, recovery/migration notes and next step in DEVELOPMENT_LOG.md and task status. Link the ZPet counterpart commit/hash where needed.
ChatGPT exclusively creates/edits artwork after user approval; Claude integrates existing approved assets only. No invented final gameplay data. No cross-repository writes, auto-merge, release or backend deployment without explicit authorization.
Pause for review at each phase boundary. Later tasks are queued plans, not permission to silently fill unresolved balancing decisions. Return a concise implementation summary for ChatGPT.

## Phase A outcome (CLAUDE-004, 2026-10-10)
- **Blocked on decisions** ([DECISIONS.md](../integration/DECISIONS.md)): D-AUTO-FIGHT (B1), D-PARTY (B2), D-EVOLUTION (B3), D-CAMPAIGN (B4), D-REPLAY-REWARDS (B5).
- **Bounded scope:** B1 can start once D-AUTO-FIGHT is approved; it reuses `BattleEngine` unchanged. B2 needs D-PARTY. B3 needs D-EVOLUTION. B4 needs approved campaign content (D-CAMPAIGN); no invented rosters. B5 needs D-REPLAY-REWARDS.
- **Contract impact:** party battles must emit up to 6 `participantCompanionIds` (BattleCompleted schema). Keep `rulesRevision` as `zbattle-rules-N` and bump it whenever combat rules change. See [CONTRACT-v0.2.md](../integration/CONTRACT-v0.2.md).

## Progress
| Step | Status | Notes |
|---|---|---|
| B1 auto-fight | REVIEW | Built on the proposed D-AUTO-FIGHT scope (foreground only, no items, stops on interruption). Zeus97x still has to confirm that scope. |
| B2 party/switching | REVIEW | D-PARTY decided: 3, one active, switching costs a turn, fainted replacement is free. Open: D-PARTY-XP, D-SWITCH-COOLDOWN (proposals implemented as config). |
| B3 evolution | REVIEW | D-EVOLUTION decided. Build identity, form guard and unlock ledger; thresholds inactive (D-EVOLUTION-THRESHOLDS). |
| B4 campaign | REVIEW (proposal) | D-CAMPAIGN: proposal + inactive validated config only. |
| B5 repeat battles | READY | D-REPLAY-REWARDS: tracking + duplicate-safe settlement first; quantities inactive. |

### B1 implementation
- `core/.../battle/AutoFight.kt`: `AutoFight.choose`, a pure, legal policy: Skill when ready, otherwise Attack. It never uses items and never retreats. `BattleProgress.autoStep(battleId, turn)` acts only while that battle is active and still on that turn.
- `ui/AppState.kt`: `autoFight` lives in memory only and is never saved. `startAutoFight`, `stopAutoFight` and `autoFightStep` are guarded. A dialog pauses auto. Back, Retreat, a tapped move, settlement and leaving the screen each stop it.
- `ui/BattleScreen.kt`: one `LaunchedEffect(battleId, turn)` scheduler with a 900 ms pause (400 ms when animations are off), an "Auto battle / Stop auto" button and an "auto on" notice.
- `app/MainActivity.kt`: `onStop` stops auto, which makes it foreground-only.
- No engine, reward or save-format change. `rulesRevision` is unchanged.
- Tests:
  - `core/.../AutoFightTest.kt` (6): legal moves only, the same result and rewards as manual play, deterministic, ends within the turn limit, stale or duplicate steps ignored, no action after settlement, manual take-over.
  - `LayoutRenderTest.autoFightThroughAppStateIsForegroundOnlyAndSettlesOnce`.
  - `05c-battle-auto` renders at 412dp, 360dp and 360dp with 130% text.

### B2 implementation (branch `claude/zbattle-b2-party`)
- **Rules (D-PARTY), in `core/.../battle/BattleEngine.kt`:**
  - The party is up to 3, and the lead fights first; everyone starts at full HP.
  - `switch` uses the turn: the opponent then takes one normal turn against the incoming creature, and Burn ticks at the end of that turn.
  - `replace` after a faint is free: no turn passes and the opponent does not act.
  - Defeat comes only when the whole party is down.
  - A creature knocked out before it moves spends no cooldown and applies no effect.
  - `RULES_REVISION` = `zbattle-rules-2`.
- **Proposal behaviours, flagged:**
  - D-SWITCH-COOLDOWN: Skill cooldown is per creature and frozen while benched.
  - D-PARTY-XP: `PartyXp.share` splits first-win XP evenly between actual participants, with the remainder to the finisher. A single fighter still gets the full 60.
- **Participants (D-PARTICIPATION):** creatures that attacked, used a Skill or were switched in during a resolved turn. These are the only ones paid, and they are listed in `BattleResult.gains`.
- **Party selection:** `BattleProgress.party` with `toggleParty` and `makeLead`, locked during a battle. The creature detail sheet has Make lead / Remove / Add buttons, and Home shows "My Party n/3".
- **Battle UI:** team HP strip, switch picker, replacement prompt, and per-member XP on the results screen.
- **Auto-fight:** it now guards on the exact battle state and sends in the next standing member after a faint. It never switches voluntarily.
- **Save v2:**
  - `BattleProgressCodec` v2 stores the party, team battles and per-participant results.
  - v1 saves migrate on read. The frozen v1 golden file decodes to the same progress the current engine produces (`ContractMigrationFixtureTest`).
- **Tests:**
  - `PartyBattleTest` (10).
  - `AutoFightTest` (7, adds the replacement case).
  - `LayoutRenderTest.partyFlowThroughAppState`.
  - Renders 21–25 at 412dp, 360dp and 360dp with 130% text.
  - Total: 81 tests, 0 failures (`./gradlew -p preview test`).

### B3 implementation (branch `claude/zbattle-b3-evolution`)
- **Identity:** `OwnedCreature` now carries:
  - `companionId`, a UUID assigned once by `AppState` and saved immediately (`withCompanionIds`)
  - `rarity`, the canonical id 0–3 with D-RARITY display names
  - `nickname`, `origin` and `sourceRevision`
  - derived `speciesId` (`family:rarity`), `formIndex` and `branch`

  Species stays separate from the instance and the form (D-NATIVE-SPECIES: native starters are `family:0`).
- **`core/.../battle/Evolution.kt`:**
  - `FormGraph` (ZPet branches; once taken, a branch is kept).
  - `applyUnlock` accepts only validated, new events and never downgrades. It rejects wrong-authority sources and enforces the challenge-gated stage-4 claim (one game only).
  - ZBattle unlocks for imported companions are queued in `outbound` as pending delivery, not applied.
  - `applyFormSnapshot` ignores stale or older revisions and never lowers a form.
  - `EvolutionRules.combatLevelThresholds = null`: inactive until D-EVOLUTION-THRESHOLDS.
- **Save v3:** identity fields and the evolution ledger. v1 and v2 saves migrate (no id until the app assigns one, rarity 0, ZBattle origin).
- **UI:** the creature detail sheet shows species/rarity and evolution status. There is no evolve button while thresholds are inactive.
- **Not built:**
  - Nickname editing UI (the data is stored and preserved).
  - Founder definitions and thresholds (pending).
  - Cross-app delivery of `outbound` unlocks (Phase D).
- **Tests:** `EvolutionTest` (9) and `LayoutRenderTest.companionIdsAreAssignedOnceAndPersisted`. Total 91 tests, 0 failures.

### B4 proposal (branch `claude/zbattle-b4-campaign-proposal`)
- **`ai/proposals/CAMPAIGN-PROPOSAL.md` + `campaign-proposal.json`:**
  - 300 encounters over `area-00`…`area-47`: 6 per area, plus a region boss in each stage-3 area.
  - Only existing creatures with artwork; groups 9–11 use same-`family % 3` interim families until their art is approved.
  - Ordered unlocks; a proposed opponent ramp; the shipped `area-00/slot-0` unchanged.
- **`tools/campaign_proposal.py`:** generate, validate (in CI) and simulate.
- **`CampaignProposalTest`:** every row resolves against `CreatureCatalog` and `RegionCatalog`, the shipped encounter is unchanged, and runtime still has exactly 1 playable encounter (the proposal is inactive).
- **Reconciled with the economy proposal:** the JSON uses Option B XP. The resulting curve mismatch for a party of 3 is documented in §7.6 and left for Zeus97x (Q-F).
- **Not built:** no runtime campaign, no unlock gates, no new opponent stats until D-CAMPAIGN is approved.
