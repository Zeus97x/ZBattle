# CLAUDE-009 — Phase F: Advanced combat and companion interactions
Updated 2026-10-10 America/Toronto.
Owner: Claude (unclaimed). Status: Blocked on B-E and mechanic approval.
Claim fields: implementation branch, base commit, start time, subtask owner and current PR — fill before edits.

## Read first
AGENTS.md, DEVELOPMENT_LOG.md, ai/README.md, ai/CROSS_APP_ROADMAP.md, ai/integration/CONTRACT-v0.1.md and relevant preceding task summaries. Inspect latest main and active PRs; never overwrite concurrent changes.

## Dependencies
B core; D/E contracts; approved effect formulas/content; duo activation.

## Scope and ordered sub-PRs
Paths: pure battle engine/effects, saved battle state/rules revision, UI battle controls/results/settings, typed interaction snapshot adapters.
F1: duo battles and duo attacks after explicit activation approval (previously deferred); participant attribution and shared skill resources.
F2: Last Stand and battlefield weather with approved trigger/chance/duration/stacking configuration; no hidden stat inflation.
F3: rivalry/revenge encounter logic, safe persistence and boss dialogue/trash talk keyed to approved content.
F4: consume ZPet personality/friendship/Secret Handshake data; approved cosmetic/interaction effects. ZPet owns Brave/Curious/Loyal/Mischievous/Lazy/Lucky Idiot and sanctuary/treasure progression. Do not clone care systems into ZBattle.
Acceptance: every effect resolves through deterministic rules, recoverable state and clear UI; normal battles unchanged when features disabled; no invented opponents/art. Multiplayer remains out of scope.
Validation: status stacking, turn order, duo attribution, restart/resume, effect toggles, disabled-mode regression, small screen/large text and reduced-effects views. Use approved existing assets or honest placeholders; no image generation or palette alterations.

## Delivery rules
Use focused feature branches and small PRs in the specified order. Record source paths, rule/config revision, actual validation, unavailable device checks, recovery/migration notes and next step in DEVELOPMENT_LOG.md and task status. Link the ZPet counterpart commit/hash where needed.
ChatGPT exclusively creates/edits artwork after user approval; Claude integrates existing approved assets only. No invented final gameplay data. No cross-repository writes, auto-merge, release or backend deployment without explicit authorization.
Pause for review at each phase boundary. Later tasks are queued plans, not permission to silently fill unresolved balancing decisions. Return a concise implementation summary for ChatGPT.
