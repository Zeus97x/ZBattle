# CLAUDE-004 — Phase A: Audit and finalize shared contract
Updated 2026-10-10 America/Toronto.
Owner: Claude (unclaimed). Status: Ready — documentation/fixtures only.
Claim fields: implementation branch, base commit, start time, subtask owner and current PR — fill before edits.

## Read first
AGENTS.md, DEVELOPMENT_LOG.md, ai/README.md, ai/CROSS_APP_ROADMAP.md, ai/integration/CONTRACT-v0.1.md and relevant preceding task summaries. Inspect latest main and active PRs; never overwrite concurrent changes.

## Dependencies
None; contract proposals require review before implementation.

## Scope and ordered sub-PRs
Read current ZBattle core/battle, Profile.kt, Regions.kt, CreatureCatalog.kt, ArtCatalog.kt, BattleProgressCodec.kt; app/PrefsSettingsStore.kt, ui/AppState.kt and current tasks/log. Read ZPet RegionCatalog.java, SpeciesCatalog.java, MonsterCatalog.java, Progression.java, PrototypeState.java, AdventureState.java, CloudClient.java and backend definitions read-only. Discover actual companion storage before proposing UUID migration; do not assume filenames.
Sub-PR A1: reconcile implemented vs missing features and active PR owners; fill evidence commit/source table.
Sub-PR A2: produce typed JSON schemas, fixture examples and authority/error/compatibility tables based on CONTRACT-v0.1.md. Include imported vs native companions, duplicate species, nickname/form updates, reward retry and participation events.
Sub-PR A3: reconcile with ZPet project copy through commit/hash handoff; record user design decisions and bounded B-G tasks.
Acceptance: both owners review identical fixtures; no fabricated IDs/names, no accidental evolution/XP authority sharing; IDs survive reimport; every pending decision has an owner. No gameplay or deployed backend changes. Existing no-reverse-writes rule amended only for approved battle-event transport.
Validation: schema fixture validation, sample old/new migration roundtrips where fixtures exist, catalogue coverage and changed-doc link checks. Record gaps.

## Delivery rules
Use focused feature branches and small PRs in the specified order. Record source paths, rule/config revision, actual validation, unavailable device checks, recovery/migration notes and next step in DEVELOPMENT_LOG.md and task status. Link the ZPet counterpart commit/hash where needed.
ChatGPT exclusively creates/edits artwork after user approval; Claude integrates existing approved assets only. No invented final gameplay data. No cross-repository writes, auto-merge, release or backend deployment without explicit authorization.
Pause for review at each phase boundary. Later tasks are queued plans, not permission to silently fill unresolved balancing decisions. Return a concise implementation summary for ChatGPT.
