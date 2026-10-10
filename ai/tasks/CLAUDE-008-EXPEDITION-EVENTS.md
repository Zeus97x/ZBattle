# CLAUDE-008 — Phase E: Expedition and activity integration
Updated 2026-10-10 America/Toronto.
Owner: Claude (unclaimed). Status: Blocked on D and ZPet activity implementation.
Claim fields: implementation branch, base commit, start time, subtask owner and current PR — fill before edits.

## Read first
AGENTS.md, DEVELOPMENT_LOG.md, ai/README.md, ai/CROSS_APP_ROADMAP.md, ai/integration/CONTRACT-v0.1.md and relevant preceding task summaries. Inspect latest main and active PRs; never overwrite concurrent changes.

## Dependencies
D; ZPet approved earning rules, expedition APIs and acceleration configuration.

## Scope and ordered sub-PRs
ZPet owns daily care/check-in, accepted steps, streaks, bond/lifetime achievements, reward earning and timed expeditions. Do not implement those in ZBattle or write ZPet repo from this task.
E1: publish stable completed-battle events with actual participating companion IDs, kind, outcome and verified settlement reference. Auto/manual equivalent; spectators and merely selected party members do not qualify.
E2: consume ZPet expedition snapshots into read-only progress display and shared reward delivery status. Expeditioning pets remain selectable in ZBattle.
E3: integrate accepted event acknowledgements/reconciliation; deduplicate by expedition + event under agreed authority.
Proposed durations 1/4/8/12 hours, battle acceleration 2/5/10/15/25/40 minutes by wild/campaign/mini/stage/region/mystical, walking 1000 steps => 10 minutes and combined 50% original-duration reduction remain unapproved. Define them as config proposals, not active rewards.
Acceptance: battle participation earned once, losses/practice eligibility explicitly decided; no duplicate walking credit; consistent capped progress/restoration; ZPet open/resume step refresh preserved by its owner.
Validation: duplicate and reordered events, multiple participants, nonparticipant exclusion, boundaries/completion/claim, clock corrections and two-device recovery. Lazy flavor/lucky finds are ZPet-owned and cannot impose invented penalties.

## Delivery rules
Use focused feature branches and small PRs in the specified order. Record source paths, rule/config revision, actual validation, unavailable device checks, recovery/migration notes and next step in DEVELOPMENT_LOG.md and task status. Link the ZPet counterpart commit/hash where needed.
ChatGPT exclusively creates/edits artwork after user approval; Claude integrates existing approved assets only. No invented final gameplay data. No cross-repository writes, auto-merge, release or backend deployment without explicit authorization.
Pause for review at each phase boundary. Later tasks are queued plans, not permission to silently fill unresolved balancing decisions. Return a concise implementation summary for ChatGPT.

## Phase A outcome (CLAUDE-004, 2026-10-10)
- **Blocked on decisions** ([DECISIONS.md](../integration/DECISIONS.md)): D-PARTICIPATION (E1), D-EXPEDITION (E2/E3).
- **Bounded scope:** E1 emits BattleCompleted with actual participants only; loss/practice credit needs D-PARTICIPATION. E2/E3 need ZPet's ExpeditionSnapshot producer and D-EXPEDITION values.
- **Contract impact:** ExpeditionSnapshot schema; participation check per R12 / sequence S10. See [CONTRACT-v0.2.md](../integration/CONTRACT-v0.2.md).

## Decision batch 1 (Zeus97x, 2026-10-10)
Decisions affecting this task are recorded in [DECISIONS.md](../integration/DECISIONS.md) (Q2–Q21). Proposed values stay inactive until approved. Status is unchanged: this task still waits on Phase B and on its own approvals.
