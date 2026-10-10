# ZBattle AI workspace
Updated 2026-10-09 America/Toronto.
Read root AGENTS.md and DEVELOPMENT_LOG.md first.

## Current work
| Task | Owner | Status | Scope |
|---|---|---|---|
| [CLAUDE-001](tasks/CLAUDE-001-UI-FOUNDATION.md) | Claude | Merged — PR #2 | Android UI foundation using existing ZPet assets and new-art placeholders |
| [CLAUDE-002](tasks/CLAUDE-002-BATTLE-VERTICAL-SLICE.md) | Claude | Merged — PR #5 | Battle vertical slice: stats, real battle engine, saved progress, Android bottom-inset fix, full-circle lightning nav, ZCubes planning; double battles deferred |
| [CLAUDE-003](tasks/CLAUDE-003-ARTWORK-INTEGRATION.md) | Claude | In progress — branch claude/zbattle-artwork-integration | Package and wire maps/hero scenery |
| Artwork phase 1 | ChatGPT | Reference board uploaded; standalone exports pending | Logo, app icon, splash |
| Artwork phase 2 | ChatGPT | 12 maps uploaded for review | Exact ZPet region groups |
| Artwork phase 3 | ChatGPT | 48 primary landscapes + 7 variants uploaded for review | Exact ZPet locations |
| Artwork phases 4-8 | ChatGPT | Pending | Battle scenery, opponents, shop, progress, finishing |

[Layout specification](UI_LAYOUT_SPEC.md) is the implementation brief. ZCubes catching plan: [ZCUBES_PLAN.md](ZCUBES_PLAN.md). Claude's summary of CLAUDE-001 for ChatGPT: [CLAUDE-001-SUMMARY-FOR-CHATGPT.md](CLAUDE-001-SUMMARY-FOR-CHATGPT.md). All 70 recovered ZBattle images are now in [the artwork drop](../design/artwork/README.md), with stable-ID mappings and checksums. Runtime integration is CLAUDE-003.

## Collaboration
Claim a task by recording owner, branch, base commit, status and start time in its task file. One owner per implementation task. Work in a feature branch and return a PR, not direct code writes to main. Check main and open work before starting.
Maintain recovery log, exact validation evidence, blockers and next steps. Move task status through Ready -> In progress -> Review -> Done; do not mark Done before review.
When the user reports low ChatGPT usage, prepare additional bounded Claude tasks with files, acceptance criteria and isolated ownership. No automatic visibility into the user's usage meter is assumed.
ChatGPT handles image generation and design. Claude handles this foundation and can suggest architecture improvements within scope. Codex may handle isolated testing/debugging tasks when assigned. Do not overlap edits without coordination.

## Artwork phases
1. Branding — preview ready, individual exports pending approval.
2. Region maps — 12 source maps uploaded, integration pending.
3. Location scenery — 48 primary sources and seven variants uploaded, integration pending.
4. Battle backgrounds — matching each location.
5. Approved bosses and opponent portraits.
6. Approved equipment/consumable/cosmetic illustrations.
7. Progress medallions, achievements, profile art.
8. Event/empty-state illustrations and consistency review.
Check in with the user after EVERY artwork phase. Use all existing created ZPets; completing unfinished creature forms is deferred. No new image generation by Claude.

## Recovery
On restart, read task statuses, root log, current source and latest commits. Design examples are not implemented gameplay or approved economy.
