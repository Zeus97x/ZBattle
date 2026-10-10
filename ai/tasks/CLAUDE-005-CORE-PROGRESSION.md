# CLAUDE-005 — Phase B: Core progression and auto battle
Updated 2026-10-10 America/Toronto.
Owner: Claude (unclaimed). Status: Blocked on Phase A and B design approval.
Claim fields: implementation branch, base commit, start time, subtask owner and current PR — fill before edits.

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
