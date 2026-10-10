# ZBattle AI workspace
Updated 2026-10-09 America/Toronto.
Read root AGENTS.md and DEVELOPMENT_LOG.md first.

## Current work
| Task | Owner | Status | Scope |
|---|---|---|---|
| [CLAUDE-001](tasks/CLAUDE-001-UI-FOUNDATION.md) | Claude | Merged — PR #2 | Android UI foundation using existing ZPet assets and new-art placeholders |
| [CLAUDE-002](tasks/CLAUDE-002-BATTLE-VERTICAL-SLICE.md) | Claude | Ready | Battle vertical slice: stats, real battle engine, saved progress, Android bottom-inset fix, full-circle lightning nav, ZCubes planning; double battles deferred |
| Artwork phase 1 | ChatGPT | Branding preview made; approval/export pending | Logo, app icon, splash |
| Artwork phases 2-8 | ChatGPT | Planned, not started | Maps, location scenery, battle scenery, opponents, shop, progress, finishing |

[Layout specification](UI_LAYOUT_SPEC.md) is the implementation brief. Claude's summary of CLAUDE-001 for ChatGPT: [CLAUDE-001-SUMMARY-FOR-CHATGPT.md](CLAUDE-001-SUMMARY-FOR-CHATGPT.md). Generated design boards remain in chat; no new image files have been committed.

## Collaboration
Claim a task by recording owner, branch, base commit, status and start time in its task file. One owner per implementation task. Work in a feature branch and return a PR, not direct code writes to main. Check main and open work before starting.
Maintain recovery log, exact validation evidence, blockers and next steps. Move task status through Ready -> In progress -> Review -> Done; do not mark Done before review.
When the user reports low ChatGPT usage, prepare additional bounded Claude tasks with files, acceptance criteria and isolated ownership. No automatic visibility into the user's usage meter is assumed.
ChatGPT handles image generation and design. Claude handles this foundation and can suggest architecture improvements within scope. Codex may handle isolated testing/debugging tasks when assigned. Do not overlap edits without coordination.

## Artwork phases
1. Branding — preview ready, individual exports pending approval.
2. Region maps — ZPet catalogue groups, exact locations.
3. Location scenery — grouped by region, shared hero crops.
4. Battle backgrounds — matching each location.
5. Approved bosses and opponent portraits.
6. Approved equipment/consumable/cosmetic illustrations.
7. Progress medallions, achievements, profile art.
8. Event/empty-state illustrations and consistency review.
Check in with the user after EVERY artwork phase. Use all existing created ZPets; completing unfinished creature forms is deferred. No new image generation by Claude.

## Recovery
On restart, read task statuses, root log, current source and latest commits. Design examples are not implemented gameplay or approved economy.
