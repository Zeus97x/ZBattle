# CLAUDE-005 — Phase B: Core progression and auto battle
Updated 2026-10-10 America/Toronto.
Owner: Claude. Status: IN PROGRESS — B1 in review; B2–B5 blocked on decisions.
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
| B2 party/switching | BLOCKED | D-PARTY: party size and switch turn cost |
| B3 evolution | BLOCKED | D-EVOLUTION |
| B4 campaign | BLOCKED | D-CAMPAIGN: stage counts, rosters, gates, difficulty |
| B5 repeat battles | BLOCKED | D-REPLAY-REWARDS |

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
