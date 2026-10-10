# ZPet × ZBattle — complete A–G implementation roadmap
Updated 2026-10-10 America/Toronto. Owner: ChatGPT coordination; Claude implementation.
Status: PLANNING; no feature implementation or backend deployment in this PR.
User asked to prepare every phase, with most coding assigned to Claude. Each phase ends with review and approval before the next. Artwork always requires separate explicit approval.

## Evidence baseline
ZBattle main includes merged CLAUDE-001/002/003 (PRs #2/#5/#6): Compose shell, Pet Master, approved creature assets, 12 maps/48 hero landscapes, deterministic Attack/Skill engine, cooldown/status effects, XP/levels, versioned saved battle recovery and once-only settlement.
Encounters.kt exposes exactly one real encounter; switching, catching, campaign, economy and evolution remain missing. Rematches pay zero XP. Source-only audit; no new build or device validation.
ZPet main has Java gameplay, accepted-step/training ledger, species IDs family:rarity, evolution quests, local companion progress, encrypted CloudClient sessions and a zpet-api boundary. Cloud backup is not proof of shared live inventory or verified battle events.
ZBattle open-PR check returned none. ZPet PR #2 AI workspace and #3 artwork handoff were open at audit; keep ZPet changes with its project owner. Recheck before claiming.

## Execution queue
| Phase | Claude task | Deliverable | Gate |
|---|---|---|---|
| A | CLAUDE-004 | Audited contracts, fixture examples and recovery design | Shared contract accepted by both project owners |
| B | CLAUDE-005 | Auto-fight, multi-companion battle, evolution, hybrid encounters/campaign | Approve stage counts, roster, unlock and replay rules |
| C | CLAUDE-006 | Inventory, bosses, tickets, mystical encounters | Rarity/economy approval and exclusive content |
| D | CLAUDE-007 | Origin/bond bridge, reward inbox, verified event transport | Matching ZPet producer; backend approval |
| E | CLAUDE-008 | Expedition participation events and integration views | ZPet activity/expedition producer |
| F | CLAUDE-009 | Advanced combat and companion interaction adapters | Stable core, approved mechanic values |
| G | CLAUDE-010 | Legacy displays, portals, corruption, world bosses and Hall | ZPet legacy definitions and approved artwork/content |

Do not rebuild completed work. Each task contains ordered sub-PRs, so a phase is never one giant implementation PR. No automatic merge or release. Treat technical choices as proposals until verified against current source. Double battles were previously deferred; this roadmap places them in F, not the first core phase.

## Project ownership
ZBattle: battles, stats, campaign, boss rules, gacha, combat inventories, reward redemption.
ZPet: care, accepted walking activity, bond percentage, personalities, expeditions, sanctuary, treasure, legacy creation and reward earning.
Integration introduces narrowly scoped battle event delivery to ZPet; it supersedes the older blanket no-reverse-writes design only for approved events. Never overwrite ZPet companion saves from ZBattle.
ChatGPT: all image/art creation or editing, design and review. Claude: source implementation and integration of approved art. Codex: optional separately assigned testing/debugging, not automatic parallel ownership.

## Decisions register
> Phase A turned this list into an owned register: [integration/DECISIONS.md](integration/DECISIONS.md). The contract is now [integration/CONTRACT-v0.2.md](integration/CONTRACT-v0.2.md).

- Rarity mapping proposed: Common/Common, Heroic/Rare, Mythic/Epic, Celestial/Legendary. Preserve IDs; not approved yet.
- Campaign counts, exact roster per location, unlock rules and repeat XP/coins/tickets remain unresolved.
- Bond earning and percentage migration belong to ZPet; the existing First bond quest is not percentage bonding.
- Evolution authority: ZPet owns imported companion form updates; define any independent ZBattle evolution and conflict policy before coding. Separate app XP must not overwrite ZPet XP.
- Clock definition: command says rolling seven-day cycle AND first-victory-anchored resets. These imply different rules. Recommend anchored 168-hour windows per mystical boss; true trailing-seven-day limits need a victory timestamp ledger. Ask user to choose before C timers.
- Four-hour cooldown starts after accepted victory, per boss; server time/atomic settlement required for cross-device guarantees.
- Exclusive pools empty until approved definitions; do not substitute ordinary rewards into the exclusive branch.
- Expedition durations, acceleration and 50% cap are illustrative until approved, as are daily/streak step thresholds.
- Account linking, guest migration, backend reuse, verification of local battle/step events and offline redemption policy need inspection. No trust guarantees from client IDs alone.
- Existing ZCube plan proposals are not final approved catch odds/prices.
- Auto-fight is optional; repeat automation policy, item consumption and foreground/background scope need explicit decisions. Proposed first version foreground only, pause on interruption, no automatic resume into farming.

## Every phase checkpoint
Report source/PRs, tests actually run, device checks unavailable, pending choices, ZPet dependencies and next task. Pause at phase boundary. Keep ai/README.md and DEVELOPMENT_LOG.md current; no secrets in docs.
