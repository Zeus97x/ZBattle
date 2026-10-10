# CLAUDE-001 — Android UI foundation
Status: IN PROGRESS — Claude resumed 2026-10-09 (evening) at the user's request (the ChatGPT handoff was withdrawn). Owner: Claude. Updated 2026-10-09 America/Toronto.
Claim: Claude Code session, started 2026-10-09 ~19:30 America/Toronto; branch claude/zbattle-ui-foundation; base main b1fa4c5414e45daeac5650aa1e96ac3b5dfb121e.
Parallel work found at claim time: open PR #1 (branch ai/codex/zbattle-foundation, base 758c43c) adds a separate Java Activity shell under app/ plus .github/ai/**. It is unmerged and conflicts with this task's app/ and Gradle files; not overwritten — reconciliation is the user's decision (see Progress log).
Repo: Zeus97x/ZBattle. Base: current main. Work branch: claude/zbattle-ui-foundation.

## Read first
1. Root AGENTS.md and DEVELOPMENT_LOG.md.
2. ai/README.md and ai/UI_LAYOUT_SPEC.md.
3. ZBattle-ZPet-Assets/README.md, manifest.json and reference catalogs/rendering code.
4. Current source and Git history; do not overwrite parallel work.

## Goal
Build the native Android UI foundation while ChatGPT creates final artwork. The repo inspection on 2026-10-09 found docs and the existing ZPet asset pack, not a runnable Android app. The user now requests implementation preparation; previous README development deferral no longer blocks this scoped task.

## Work
- If no app source exists, scaffold a minimal Kotlin/Compose Android application with reproducible Gradle configuration and wrapper. Keep package identity distinct from ZPet (proposed com.zeus97x.zbattle); record selected SDK/tool versions.
- Build central theme/component tokens and artwork resolver/fallback described in UI_LAYOUT_SPEC.md.
- Inventory all 54 existing monster PNGs and valid reference catalog mappings. Integrate existing pets unchanged; do not finish missing forms.
- Implement the nine screen layouts/routes and reusable components; route top-level navigation, sheets, back actions and creature detail coherently.
- Include all 48 location names with stable group/area IDs. Check current ZPet catalogue; retain duplicate-tradition groups separately.
- Make search, sort, filters, location selection and local profile/settings persistence function. Keep battle, economy, event and unlock engine previews explicitly demo/planned.
- Put screenshots and validation evidence in PR when available.
- Add meaningful checks for asset/catalog lookup and duplicate tradition group identity, plus UI navigation/state where supported. Do not write mirror tests for cosmetic constants.
- Update DEVELOPMENT_LOG.md and task status with each meaningful commit and at handoff. Preserve earlier history.

## Parallel ownership
Claude may modify new Android source, Gradle configuration, tests and its task status. ChatGPT owns generated artwork, branding and further design decisions.
Do not edit or replace source creature PNGs, generate images, alter ZPet, modify secrets/signing, add backend integrations, merge or publish releases. Use placeholders for missing final art.
Do not claim app features work based only on clickable mockups. Keep demo values separate from production models.
If another branch/source exists, reconcile before scaffolding; report conflicting scope rather than overwriting it.

## Milestones
A: buildable app + theme/navigation + Home/Collection using current assets.
B: Travel/Challenges/Battle preview + correct catalogue identities.
C: Shop/Double Battle/Profile + usable local settings + verification.
Finish this scoped UI task and return a PR; report milestone progress in the task log. No release build publication without the user's instruction.

## Acceptance
Use ai/UI_LAYOUT_SPEC.md definition of done. Build/compile succeeds or exact environmental blocker recorded; navigation/insets checked; existing pets reused; no unsupported production rewards/unlocks; no new generated creature art. Small phone and large font review performed if tools permit.
Return PR URL, commit, changes, checks, screenshots if available, remaining blockers and next task. Do not merge.

## Progress log
- 2026-10-09: claimed. Milestone A started: Gradle wrapper (8.11.1), :core platform-free catalogues/navigation/settings with tests, JVM layout harness in preview/.
- 2026-10-09: shared Compose screens for all nine layouts + JVM layout harness committed (WIP, paused by user). Android entry point and CI not yet added.
- 2026-10-09: handed off to ChatGPT at the user's request; remaining steps in [CLAUDE-001-HANDOFF.md](CLAUDE-001-HANDOFF.md).
- 2026-10-09 (evening): user withdrew the ChatGPT handoff; Claude resumed. Added Android entry point (manifest, MainActivity, AssetArtLoader, PrefsSettingsStore), CI workflow, art drop-folder notes, layout fixes (large font, light mode, placeholder labels) and committed renders under docs/screenshots/claude-001/.
- 2026-10-09 (evening): user asked not to build the app yet. CI run cancelled; workflow switched to manual-only (workflow_dispatch). Android compile/lint/APK remain unverified until the user approves a build.
- 2026-10-09 (evening): user clarified PR #1 (Codex ZB-002) is guidance for this task. Integrated its Pet Master foundation: first-run setup (name 1–24, 5 styles × male/female, starter Sparklit/Inkling/Cindlet), persisted locally; party = chosen starter; Home/Profile show appearance and Level 1; Profile lists planned skills/equipment, Pet Master tasks and one-way ZPet connection. Kept the newer layout spec's tabs (Collection/Shop/Home/Events/Profile) instead of PR #1's Camp/Adventure/Tasks/Master.
