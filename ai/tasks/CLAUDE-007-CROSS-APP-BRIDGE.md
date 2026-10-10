# CLAUDE-007 — Phase D: Origin/bond updates and reward inbox
Updated 2026-10-10 America/Toronto.
Owner: Claude (unclaimed). Status: Blocked on A, ZPet counterpart and infrastructure approval.
Claim fields: implementation branch, base commit, start time, subtask owner and current PR — fill before edits.

## Read first
AGENTS.md, DEVELOPMENT_LOG.md, ai/README.md, ai/CROSS_APP_ROADMAP.md, ai/integration/CONTRACT-v0.1.md and relevant preceding task summaries. Inspect latest main and active PRs; never overwrite concurrent changes.

## Dependencies
A contract accepted in both repos; B/C consumers; ZPet producer; explicit backend deployment approval.

## Scope and ordered sub-PRs
Paths: current core owned creature/stats/save boundary; new typed integration adapters and inbox/outbox; app authentication/network adapters only after backend audit.
D1: offline fixture importer with persistent origin registry and old-UID migration. Reimport updates same individual; no full-save overwrite or duplicated bonuses.
D2: bonus 10%, +2.5 percentage points per completed 25% bond milestone, maximum total 20% on unbonused HP/Attack/Defense/Speed. Snapshot revision at battle start; later source updates affect subsequent fights. Approve rounding before coding.
D3: inspect ZPet CloudClient/zpet-api/schema, deployed compatibility through separately authorized read-only checks, account-link identity, backend reuse and transaction authority. Produce migration/auth/RLS plan before deployment. Never copy service credentials or ZPet session blobs.
D4: authenticated transport + durable reward inbox/outbox, transactional redemption/ack, server-side duplicate checks and producer validation. Shared battle events delivered only to ZPet expedition endpoint; no arbitrary companion save writes.
D5: two-device reconciliation, source corrections, guest migration and retry UI. Unverified local outcomes stay clearly pending under approved trust policy; client UUID alone is not proof of victory.
Acceptance: source owns bond/identity; ZBattle owns combat XP/inventory; stale updates cannot overwrite newer revisions; replay/forged foreign-account requests rejected; earned reward cannot be redeemed twice.
Validation: two-account isolation, two-device simultaneous claims, interrupted delivery/ack, stale/unknown schema, import duplicates and save migration. Live deployment and real phone account tests require approval/evidence.

## Delivery rules
Use focused feature branches and small PRs in the specified order. Record source paths, rule/config revision, actual validation, unavailable device checks, recovery/migration notes and next step in DEVELOPMENT_LOG.md and task status. Link the ZPet counterpart commit/hash where needed.
ChatGPT exclusively creates/edits artwork after user approval; Claude integrates existing approved assets only. No invented final gameplay data. No cross-repository writes, auto-merge, release or backend deployment without explicit authorization.
Pause for review at each phase boundary. Later tasks are queued plans, not permission to silently fill unresolved balancing decisions. Return a concise implementation summary for ChatGPT.

## Phase A outcome (CLAUDE-004, 2026-10-10)
- **Blocked on decisions** ([DECISIONS.md](../integration/DECISIONS.md)): D-NATIVE-SPECIES and D-CONTRACT-ACCEPT (D1), D-ORIGIN-ROUNDING (D2), D-BACKEND (D3/D4), D-OFFLINE-TRUST (D5).
- **Bounded scope:** D1 implements the BattleProgress v2 save with `companionId` (see `fixtures/migration/`) once D-CONTRACT-ACCEPT is met. D2 needs a ZPet bond producer (gap G4) and D-ORIGIN-ROUNDING. D3 is a read-only backend audit before any plan; no deployment.
- **Contract impact:** implements rules R1–R12 server-side; `tools/validate_contract.py` sequences are the acceptance tests. See [CONTRACT-v0.2.md](../integration/CONTRACT-v0.2.md).

## Decision batch 1 (Zeus97x, 2026-10-10)
Decisions affecting this task are recorded in [DECISIONS.md](../integration/DECISIONS.md) (Q2–Q21). Proposed values stay inactive until approved. Status is unchanged: this task still waits on Phase B and on its own approvals.
