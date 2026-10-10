# CLAUDE-010 — Phase G: Legacy cosmetics, exploration and endgame
Updated 2026-10-10 America/Toronto.
Owner: Claude (unclaimed). Status: Blocked on F, ZPet legacy data and approved artwork.
Claim fields: implementation branch, base commit, start time, subtask owner and current PR — fill before edits.

## Read first
AGENTS.md, DEVELOPMENT_LOG.md, ai/README.md, ai/CROSS_APP_ROADMAP.md, ai/integration/CONTRACT-v0.1.md and relevant preceding task summaries. Inspect latest main and active PRs; never overwrite concurrent changes.

## Dependencies
F; ZPet fully bonded/legacy systems; approved exclusives, portal/corruption/world-boss content and artwork.

## Scope and ordered sub-PRs
G1: lineage snapshot/Hall of Legends display from ZPet (founders, fully bonded eligibility, successor, generation). ZPet creates legacy; ZBattle displays.
G2: wire ChatGPT-approved unique emblems and elemental flare assets with gold accents into team/profile/battle intros; actual element metadata drives selection. Effects toggle/reduced-effects; no combat bonus beyond 20% bond cap. Do not procedurally invent replacement artwork/effects assets.
G3: mystery portals and corrupted encounters using approved definitions/assets and clear eligibility/reward rules. Existing variants aren't automatically approved corrupted forms.
G4: initially solo world bosses, with approved roster/mechanics/rewards; no multiplayer service without separate approval.
G5: achievements, hardest boss victories, discoveries, favorite companions and lineage history in Hall of Legends; final balancing and compatibility QA.
Acceptance: safe lineage history, cosmetic-only prestige, no unsupported asset substitutions, independent timers/claim authority retained and user settings respected.
Validation: migration, missing asset fallback, old lineage revisions, element mismatch, reduced-effects accessibility, boss replay/claim/timer regression, full cross-app recovery and device checks. No release until separately requested.

## Delivery rules
Use focused feature branches and small PRs in the specified order. Record source paths, rule/config revision, actual validation, unavailable device checks, recovery/migration notes and next step in DEVELOPMENT_LOG.md and task status. Link the ZPet counterpart commit/hash where needed.
ChatGPT exclusively creates/edits artwork after user approval; Claude integrates existing approved assets only. No invented final gameplay data. No cross-repository writes, auto-merge, release or backend deployment without explicit authorization.
Pause for review at each phase boundary. Later tasks are queued plans, not permission to silently fill unresolved balancing decisions. Return a concise implementation summary for ChatGPT.

## Phase A outcome (CLAUDE-004, 2026-10-10)
- **Blocked on decisions** ([DECISIONS.md](../integration/DECISIONS.md)): D-LEGACY (G1/G5), D-ELEMENT (G2), D-EXCLUSIVE-POOL and approved art (G3/G4).
- **Bounded scope:** G1 displays LineageSnapshot once ZPet produces it. G2 needs element metadata and ChatGPT-approved assets. G3/G4 need approved definitions.
- **Contract impact:** LineageSnapshot forbids combat fields (schema `additionalProperties: false`). See [CONTRACT-v0.2.md](../integration/CONTRACT-v0.2.md).
