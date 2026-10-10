# Claude extended backlog — ZBattle
Updated 2026-10-10 America/Toronto. Owner: Claude implementation; ChatGPT coordination/artwork.
Status: queued implementation backlog, not completed features.

## Why this is a separate PR
User expects ChatGPT usage to run out before Claude usage and wants a long persistent queue. Claude is already working on PR #8. This file is separate from PR #8 and does not replace its tasks/contracts or change its phase approval rules. Finish current work first; recheck main, open PRs and owner claims before taking any item. Only DEVELOPMENT_LOG.md is a shared append-only path; preserve both branches' entries on merge.

## Working rules
- Start from current PR #8 implementation/approved contract, or latest main after it merges. References to CLAUDE-004..010 resolve in PR #8 until merged; do not recreate or rename their task files.
- Claim an item with owner, branch/base SHA, status, dependency decision and expected changed paths. One owner per overlapping implementation surface.
- Phase A audit/documentation/fixture items can proceed within the current task. Runtime B-G items are queued and require the corresponding phase approval and unresolved decisions. This backlog is not blanket permission to deploy/merge or invent game values.
- While waiting, continue independent audit/fixtures/compatibility documentation within the approved phase. At its boundary stop for user review; no ChatGPT turn is required for Claude to report directly to the user.
- Split work into focused PRs, typically 1–3 related items, with meaningful tests. Do not create tests solely mirroring implementation. Skip completed items after source inspection.
- Log checks actually run and unavailable device checks. Never mark done just because a PR exists. Track blocked reason and next dependency.
- ChatGPT exclusively creates/edits artwork after approval. Claude may integrate supplied approved assets, but cannot generate/recolour replacement imagery or effects assets.
- No cross-repo writes: ZPet owns its counterpart work. No signing changes, service credentials, backend deployment, automatic merges or release publication.

## Common source areas
Battle: core/src/main/kotlin/com/zeus97x/zbattle/core/battle/{Stats,Encounters,BattleEngine,BattleProgress,BattleProgressCodec}.kt.
Catalogue/navigation/settings: core/src/main/kotlin/com/zeus97x/zbattle/core/.
UI/state: ui/src/main/kotlin/com/zeus97x/zbattle/ui/.
Android persistence/loader: app/src/main/kotlin/com/zeus97x/zbattle/.
Tests/render harness: core/src/test/, preview/; existing tasks record commands. Source art: design/artwork/; runtime art: app/src/main/assets/art/.
New module paths must align with current source; confirm before adding. ZPet source/backend may be inspected read-only for agreed contracts; project owner implements changes there.

## A — Finish current coordination work
Dependency: CLAUDE-004 / PR #8; documentation/fixtures now, runtime changes later.

| ID | Work | Concrete scope | Acceptance |
|---|---|---|---|
| EXT-001 | Verify current feature inventory | Compare actual source and active PRs to the roadmap; record exact commits. | Evidence table distinguishes implemented, placeholder, tested and device-unverified. |
| EXT-002 | Map all shared catalogue IDs | Compare both catalogues including repeated traditions, species, forms and elements. | 12 groups/48 areas uniquely mapped; absent element data explicitly flagged. |
| EXT-003 | Trace individual companion persistence | Locate actual ZPet storage and ZBattle Long UIDs; design stable UUID migration. | Duplicates of the same species remain separate; backup/reimport preserves identity. |
| EXT-004 | Define import/update fixtures | Native, imported, evolved, renamed and duplicate-species examples. | Both project owners review identical fixture hashes; no fabricated production data. |
| EXT-005 | Define reward inbox fixtures | Pending, accepted, redeemed, failed and acknowledged delivery examples. | Duplicate event with changed payload is an explicit error. |
| EXT-006 | Define event ownership and version negotiation | Source-owned fields, revisions, privacy, unsupported schemas and migration. | Compatibility table includes unknown version/ID and stale updates. |
| EXT-007 | Audit shared verification options | Read existing backend and local trust boundaries; propose architecture. | No client UUID falsely described as verified victory; no deployment. |
| EXT-008 | Prepare consolidated decision sheet | Recommend values with rationale but mark them provisional. | User can approve independent choices without blocking unrelated work. |

## B — Battle automation and progression
Dependency: CLAUDE-005; Phase A and gameplay approval as applicable.

| ID | Work | Concrete scope | Acceptance |
|---|---|---|---|
| EXT-009 | Extract legal-action policy for auto-fight | Reuse manual engine; deterministic choices and clear fallback. | No Skill on cooldown; identical actions produce identical results. |
| EXT-010 | Add foreground auto-fight controls | Start/stop/take control with one cancellable scheduler. | Rapid taps and recomposition cannot schedule duplicate turns. |
| EXT-011 | Handle auto-fight interruption | Pause on background, navigation, process recovery and end of battle. | No invisible farming or unexpected restart; manual battle still recoverable. |
| EXT-012 | Add auto session summary | Track battle count, wins, XP and stop reason. | Numbers derived from settled results, never predicted rewards. |
| EXT-013 | Add repeat battles after reward approval | Battle limit and stop on defeat; manual stop after current fight. | Respects current area/eligible encounter; never silently starts a boss. |
| EXT-014 | Define selected party and switching | Use individually owned companion IDs; approved party size/switch cost. | Only actual fighters added to participation ledger; fainting rules explicit. |
| EXT-015 | Extend save codec with migrations | Versioned party/automation/campaign state with recovery. | Old saves retain progress; unreadable data never silently reset. |
| EXT-016 | Implement approved evolution rules | Native battle evolution and imported form authority distinguished. | No old snapshot reversions or rewriting ZPet XP. |
| EXT-017 | Build configured encounter registry | Stable encounter IDs and reusable stage/boss kinds. | Missing content stays unavailable; no hardcoded invented roster. |
| EXT-018 | Implement approved campaign map/progress | Numbered stages, location/region gates and replay labels. | Legacy progress migration and first-clear/replay rewards tested. |
| EXT-019 | Add clear defeat/retreat/result routes | Consistent recovery and retry options. | Retreat cannot pay rewards; results cannot settle twice. |
| EXT-020 | Improve action/status accessibility | Explain cooldowns, enemy intent, active effects and outcome. | 48dp targets and 360/412dp large-font layouts remain usable. |

## C — Inventory, bosses and tickets
Dependency: CLAUDE-006; economy/rarity/content gates; D for shared authority.

| ID | Work | Concrete scope | Acceptance |
|---|---|---|---|
| EXT-021 | Create typed inventory ledger | Coins, items, materials and tickets; transaction IDs. | Underflow/overflow and duplicate transactions rejected. |
| EXT-022 | Implement first-clear/replay reward separation | Use approved reward configuration. | Repeat tickets and XP never inferred from placeholder copy. |
| EXT-023 | Implement cascading gacha rules | Rare 70/30; Epic 50/35/15; Legendary 40/30/21/9. | Exact boundary tests and one final creature outcome. |
| EXT-024 | Persist ticket roll transaction | Consume + outcome + grant + claim atomically. | Retry/crash cannot consume twice, reroll or grant twice. |
| EXT-025 | Wire approved gacha results UI | Use existing art; show source, rarity and new individual identity. | Missing art uses honest fallback; no new generated portraits. |
| EXT-026 | Add boss completion records | Mini/stage/location/region distinctions and configured first clears. | Region completion derived from approved campaign requirements. |
| EXT-027 | Add mystical eligibility state | Unlock at approved 3-region intervals; separate records per boss. | Unrelated boss cooldowns/counters never shared. |
| EXT-028 | Implement chosen weekly timer policy | User must pick anchored window or true rolling cap. | Four-hour and 15-victory boundaries, UTC and rollback fixtures. |
| EXT-029 | Implement approved mystical pools | 28/12/24/36 outcome categories. | Undefined exclusive pool blocks reward-bearing encounter, no substitution. |
| EXT-030 | Add cooldown/countdown and claim history | Display authoritative/pending status clearly. | No invented security guarantee for local clock implementation. |

## D — Cross-app bridge
Dependency: CLAUDE-007; accepted contract and matching ZPet producer.

| ID | Work | Concrete scope | Acceptance |
|---|---|---|---|
| EXT-031 | Implement companion import registry | One stable origin instance to one battle instance. | Repeated import updates same pet; duplicate species remain distinct. |
| EXT-032 | Apply origin/bond stat modifier | 10% plus 2.5 points each 25% bond, max total 20%. | Apply once to base stats with approved rounding; snapshot at battle start. |
| EXT-033 | Handle nickname/form/bond revisions | Source updates and correction policy. | Stale records cannot overwrite newer authoritative data. |
| EXT-034 | Build durable delivery outbox | Frozen event payload and stable retry identity. | Restart/retry preserves pending delivery exactly. |
| EXT-035 | Build reward inbox and reconciliation UI | Pending/accepted/redeemed/rejected/acknowledged statuses. | Retry action does not invent or multiply rewards. |
| EXT-036 | Implement authenticated transport after approval | Owner validation and narrow endpoints. | Two-account isolation, no shared session blobs/service keys. |
| EXT-037 | Implement atomic shared claims after deployment approval | Transaction-backed uniqueness and fixed outcome. | Simultaneous two-device claim pays once. |
| EXT-038 | Handle guest/account linking and imports | Explicit merge/ownership policy and non-destructive recovery. | No hidden whole-save overwrite; recovery copy retained. |
| EXT-039 | Implement participation event transport | Only approved battle-event writes back to ZPet. | No arbitrary ZPet companion-state mutations. |
| EXT-040 | Add sync compatibility/error presentation | Update-needed, unavailable backend and pending verification. | Offline users see truthful local/pending status. |

## E — Expeditions and activity bridge
Dependency: CLAUDE-008; ZPet owns earning/expeditions.

| ID | Work | Concrete scope | Acceptance |
|---|---|---|---|
| EXT-041 | Publish completed-battle participant records | Battle ID, actual participants, kind, outcome and rule revision. | Spectators/unused party members receive no acceleration. |
| EXT-042 | Implement expedition snapshot consumer | Read-only timed progress and completion status. | Expeditioning pet remains usable in battle. |
| EXT-043 | Deduplicate expedition acceleration events | Agreed accepted event IDs and acknowledgement. | Duplicate/out-of-order delivery credits once. |
| EXT-044 | Integrate agreed acceleration cap and durations | Only after values approved by user/ZPet owner. | Shared walking/battle cap uses original duration consistently. |
| EXT-045 | Show shared reward delivery history | Activity source, delivery state and claim status. | ZBattle cannot earn/forge ZPet walking rewards. |
| EXT-046 | Test cross-device expedition recovery | Completion/claims with interrupted delivery. | No double completion or reward; clock policy explicit. |

## F — Advanced combat and companion adapters
Dependency: CLAUDE-009; approved mechanics, stable core; duo separately activated.

| ID | Work | Concrete scope | Acceptance |
|---|---|---|---|
| EXT-047 | Implement approved duo battle rules | Turn order/targeting/participants/resources. | Saved duo fight resumes correctly; unused companions excluded. |
| EXT-048 | Implement configured duo attacks | Approved costs/triggers/cooldowns. | No hidden duplicate damage or resource spending. |
| EXT-049 | Implement Last Stand | Approved eligibility/probability/duration. | Deterministic injected randomness, no infinite revival loops. |
| EXT-050 | Implement battlefield weather effects | Approved weather/rules metadata. | Effect ordering/stacking and save recovery verified. |
| EXT-051 | Implement rivalry/revenge encounter history | Stable opponent/event records and cooldown policy. | No repeated unintended revenge trigger from restoration. |
| EXT-052 | Wire boss dialogue/trash talk | Approved text, readable pacing and settings. | No invented canon or obstruction of controls. |
| EXT-053 | Consume personality/friendship snapshots | ZPet owns source traits and progression. | Cosmetic changes do not silently alter combat balance. |
| EXT-054 | Implement approved Secret Handshake interactions | Source relationship data and approved duo effect rules. | No unauthorized stat bonus; missing relationship fallback. |

## G — Legacy and endgame
Dependency: CLAUDE-010; approved definitions/art and ZPet lineage producer.

| ID | Work | Concrete scope | Acceptance |
|---|---|---|---|
| EXT-055 | Implement lineage snapshot display | Founders, successor, generations and history. | ZBattle never creates unauthorized legacy records. |
| EXT-056 | Wire approved legacy emblems/flares | Only supplied ChatGPT assets, actual element metadata. | Reduced-effects/toggle honoured; no extra stats beyond bond cap. |
| EXT-057 | Build Hall of Legends from recorded events | Boss achievements, discoveries, favorites and lineage. | No fabricated achievements from mockup totals. |
| EXT-058 | Implement approved mystery portals | Eligibility, destinations and rewards. | Stable IDs, clear pending content and once-only claims. |
| EXT-059 | Implement approved corrupted encounters | Use explicitly approved creature variants. | No automatic recolouring or invented corruption assets. |
| EXT-060 | Implement approved solo world bosses | Approved roster/rules/rewards and saved progress. | No multiplayer networking added implicitly. |
| EXT-061 | Run endgame balance and recovery review | Approved simulations, save/import/timer/claim regression. | Record measured outcomes; no silent production rebalance. |
| EXT-062 | Prepare release-readiness report | CI/device evidence, compatibility, known issues and remaining art. | No release/publication until explicitly requested. |

## Useful work when gameplay values are pending
Document options and recommended tradeoffs; create non-production examples with explicit fixture labels; trace saves and recovery; validate catalogue/manifest coverage; enumerate meaningful boundary cases; reconcile open PR overlap; prepare device test steps; audit existing image loading/memory/accessibility problems with evidence. Only fix runtime issues within approved scope. Do not turn provisional fixtures into released content.

## Standard checkpoint to give the user/ChatGPT
Completed item IDs and PR/commit links; changed behavior and migration; tests/results; unavailable checks; decisions required; blocked IDs with dependencies; next approved item. Save checkpoint in GitHub before stopping so a different session can resume.

## Inventory
62 scoped items across A–G. Existing work is reused, not counted as new implementation. Record authoritative resolution of rarity mapping, replay rewards, stage counts, bond earning/evolution authority, timer interpretation, exclusives and expedition values before activating affected features.
