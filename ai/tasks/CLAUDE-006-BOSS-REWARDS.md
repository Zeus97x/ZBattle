# CLAUDE-006 — Phase C: Boss economy, tickets and mystical bosses
Updated 2026-10-10 America/Toronto.
Owner: Claude (unclaimed). Status: Blocked on B and economy/content decisions.
Claim fields: implementation branch, base commit, start time, subtask owner and current PR — fill before edits.

## Read first
AGENTS.md, DEVELOPMENT_LOG.md, ai/README.md, ai/CROSS_APP_ROADMAP.md, ai/integration/CONTRACT-v0.1.md and relevant preceding task summaries. Inspect latest main and active PRs; never overwrite concurrent changes.

## Dependencies
B, rarity mapping, pool definitions, economy values, seven-day policy; D for verified multi-device guarantees.

## Scope and ordered sub-PRs
Paths: core/battle and new core rewards/inventory/boss modules; BattleProgressCodec migration; ui challenges/results/shop/collection; app save boundary.
C1: durable coin/item/material/ticket inventory and atomic claim transaction ledger, no purchases unless pricing approved.
C2: approved mini-boss Rare + Rare ticket, stage boss Epic + Epic ticket, region boss Legendary + Legendary ticket; XP/coins/equipment/consumables/materials. First-clear vs replay rewards configuration; final quantities and ticket repeatability await approval.
C3: one-creature cascading rolls. Rare 70% Rare/30% Common. Epic 50% Epic/35% Rare/15% Common. Legendary 40% Legendary/30% Epic/21% Rare/9% Common. Persist one outcome per consumed ticket; native/imported duplicates are distinct individual companions, never duplicate claim IDs. Confirm rarity mapping; no invented exclusive species.
C4: mystical unlock at 3/6/9/12 completed regions if current 12-group campaign approved. Each boss independently tracks four hours after victory and 15 wins per chosen seven-day rule. Resolve anchored reset vs trailing rolling-window wording before coding.
C5: mystical roll 28% exclusive ZPet/12% exclusive item/24% Legendary/36% Epic, plus configured regular XP/coins. If exclusive pool is undefined, keep reward-bearing encounter unavailable and explain pending content; never reroll or substitute.
Local timer/reward scaffold may be tested in C, but trustworthy cross-device settlement is blocked on D authority. Do not claim client-clock protection is complete.
Acceptance: one claim/one roll, valid pool membership, per-boss independence, no rewards below Epic for mystical wins, no timer reset through save reload.
Validation: exact probability tree/table boundaries with injected RNG, inventory atomicity/crash replay, first-clear/replay claims, 15th/16th win, four-hour boundary, week reset/time-zone/clock rollback fixtures.

## Delivery rules
Use focused feature branches and small PRs in the specified order. Record source paths, rule/config revision, actual validation, unavailable device checks, recovery/migration notes and next step in DEVELOPMENT_LOG.md and task status. Link the ZPet counterpart commit/hash where needed.
ChatGPT exclusively creates/edits artwork after user approval; Claude integrates existing approved assets only. No invented final gameplay data. No cross-repository writes, auto-merge, release or backend deployment without explicit authorization.
Pause for review at each phase boundary. Later tasks are queued plans, not permission to silently fill unresolved balancing decisions. Return a concise implementation summary for ChatGPT.

## Phase A outcome (CLAUDE-004, 2026-10-10)
- **Blocked on decisions** ([DECISIONS.md](../integration/DECISIONS.md)): D-ECONOMY (C1/C2), D-RARITY (C2/C3), D-WEEK-WINDOW (C4), D-EXCLUSIVE-POOL (C5), D-OFFLINE-TRUST (timers).
- **Bounded scope:** C1 inventory and ledger can be built without prices. C2/C3 need D-RARITY and D-ECONOMY. C4 needs D-WEEK-WINDOW. C5 stays unavailable until D-EXCLUSIVE-POOL is defined.
- **Contract impact:** BossState and RewardRedeemed schemas (v0.2). Rolls persisted once per `deliveryId` (rule R12); no re-roll on retry. See [CONTRACT-v0.2.md](../integration/CONTRACT-v0.2.md).
