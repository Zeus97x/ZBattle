# Shared integration contract — draft v0.1
> Superseded by [CONTRACT-v0.2.md](CONTRACT-v0.2.md) (CLAUDE-004 A2, 2026-10-10). Kept for history.

Status: PROPOSED, not a deployed API. ZBattle coordinator copy; ZPet project reviews and publishes its matching copy.
Revision: zb-zp-contract-0.1. Updated 2026-10-10 America/Toronto.
Changes: initial ownership, identity and event envelope proposal. No compatibility claim with existing saves.

## Identity and authority
| Field/record | Authority | Requirements |
|---|---|---|
| accountId | Validated auth service | Match authenticated principal; never trust body account IDs |
| companionId | Origin app with agreed migration | Immutable individual UUID; not name, species, ordinal or nickname |
| speciesId | ZPet canonical catalogue | Preserve family:rarity; never conflate form names with species |
| formId/evolutionBranch | ZPet for imported companions | Catalogue-backed; source revision; independent ZBattle evolution pending |
| groupId/areaId | Shared catalogue snapshot | group-0..11, area-00..47; labels not keys |
| nickname | ZPet imported source | Revision-controlled; defined local display override if wanted |
| origin | Validated import registry | zp-origin identity once; retransfers update, never clone |
| bondPercent/bondRevision | ZPet validated producer | 0..100; stale updates rejected; legitimate corrections supported |
| battle XP/level/inventory | ZBattle | Separate from ZPet training/care; no whole-save reverse writes |
| expedition/personality/lineage | ZPet | ZBattle read/display and battle participation input only |
| combat reward claims/timers | Trusted settlement authority | Atomic once-only claims across devices |

Use integer precision for stats and documented rounding. Origin total multiplier is 1 + (1000 + floor(bondPercent/25)*250)/10000, capped at 1.20; non-ZPet origins 1.00. Apply to unbonused HP/Attack/Defense/Speed exactly once. Snapshot authoritative bond revision at fight start; updates affect future battles. Element comes from approved metadata, not family index modulo three.

## Envelope proposal
All records/events include schemaVersion, contractRevision, eventId (UUID), accountId, sourceApp, sourceDeviceId (pseudonymous), recordId, recordRevision, createdAtUtc, payload. Server adds acceptedAtUtc and authoritative revision. Examples/JSON schemas are Claude-004 deliverables, not this prose.
Typed payloads:
- CompanionSnapshot: companionId, speciesId, formId, branch, nickname, elementId, provenance, sourceRevision, bondPercent, bondRevision.
- BattleCompleted: battleId, encounterId, encounterKind, groupId, areaId, participantCompanionIds, outcome, rulesRevision, completion evidence/reference. Only actual participants qualify.
- RewardEarned: rewardId, sourceActivityId, earningRuleRevision, fixed reward payload and eligibility evidence.
- RewardRedeemed: rewardId, deliveryId, inventoryTransactionId, acknowledgement; destination ZBattle.
- ExpeditionSnapshot: expeditionId, companionId, durationSeconds, startedAtUtc, eligible event IDs, credited progress, cap revision, status, claimId.
- BossState: bossId, nextEligibleAtUtc, windowStartAtUtc, victory timestamps/count, settlement IDs and reward state.
- LineageSnapshot: lineageId, generation, founders, successor, element, cosmeticAssetKey, sourceRevision. No extra combat stats.

## Delivery and recovery
Outbox persists before sending. Retry reuses eventId and frozen payload. Inbox states pending/accepted/rejected/redeemed/acknowledged with failure detail and retry action. Acknowledge only after durable commit.
Unique scope: accountId + eventId and domain claim ID. Server validates producer authority and payload consistency; replay of same ID with changed payload rejected. Unique IDs provide deduplication, not authenticity.
Atomic redemption consumes ticket/reward, settles one roll, adds inventory/owned creature and stores outcome together. Crash cannot consume without result or pay twice. All weighted rolls occur once with persisted outcomes; retries never reroll.
Out-of-order revisions rejected/reconciled; evolution cannot revert from stale snapshots. Offline progress separate from verified transferable entitlements. Final offline trust policy pending; UI must expose pending status.
Guest/account linking and old local Long creature UID migration retain stable mappings across backup/restore. No destructive reset. Unknown schema and unknown catalogue IDs retained/quarantined with recovery path.

## Compatibility and privacy
Both repos retain identical versioned fixtures and a compatibility matrix. Required revision negotiation; unknown required version produces an update-needed state, not partial unsafe import.
No service keys or session copying between APKs. Owner-scoped transport, two-account isolation, minimal shared profile fields. Audit existing backend before proposing migrations; no deployment authorized by this document.
Signing/build pipeline unchanged. Production verification, secure timers and cross-device consistency remain unimplemented until D infrastructure passes acceptance.

## Cross-project handoff
ZPet project owns matching publication and earning/bond/expedition implementations. ZBattle task only produces reviewable draft and adapters. Exchange Git commit references and contract hashes, never rely on chat visibility. Divergence blocks integration.
