# ZBattle — Development and recovery log

Repository: [Zeus97x/ZBattle](https://github.com/Zeus97x/ZBattle)  
Default branch at initialization: `main`

This is the running technical record for ChatGPT/Codex to recover context, understand how changes were made and resume work. Keep entries in chronological order and update with every meaningful change. See [AGENTS.md](AGENTS.md) for maintenance rules. The same standard applies to future Zeus websites in GitHub.

## Recovery starting point

- Inspect current Git source and history before acting on historical notes.
- Baseline before this log: [8730d3fed73b97df3cac81349254984a07d228f3](https://github.com/Zeus97x/ZBattle/commit/8730d3fed73b97df3cac81349254984a07d228f3). Recover committed files by checking out that commit or a later verified checkpoint; do not overwrite newer source with an old ZIP.
- README.md and ZBattle-ZPet-Assets/ were present. The README describes a future game with development deferred; do not treat the asset collection as a complete app.
- Earlier work is documented in the references below and Git history. This initialization does not claim a complete reconstruction of past changes; backfill only from verified source, diffs and existing handoffs, with provenance.

### Existing history and guidance

- [README.md](README.md)

## 2026-10-08 — Initialize shared recovery record

- Request: keep a running Markdown record for all apps and future GitHub websites so development can resume if chat context or files are lost.
- Changed: added `DEVELOPMENT_LOG.md`; created `AGENTS.md`.
- How: documented the current Git checkpoint, repository contents and existing history references; required future work to record implementation details and verification alongside source changes. Existing project instructions and history are preserved.
- Affected files: `DEVELOPMENT_LOG.md`, `AGENTS.md`.
- Verification: read the default-branch root contents, existing root agent instructions and baseline Git commit. Documentation only; no application tests or build were run. Confirm the committed documents by reading them back after publication.
- Remaining: prior implementation details have not been backfilled; use the existing history and commits as evidence. Record subsequent work here with exact affected paths.
- Next step: read this record together with the relevant handoff/phase files, inspect current source, and proceed with the user's next authorized task.

## Entry template for future work

Copy this template for a new dated entry; replace every placeholder with facts.

### YYYY-MM-DD — Change or phase name

- Status: implemented / in progress / blocked / planned.
- Request/problem and reason:
- Changes:
- Implementation (how and important decisions):
- Affected files and configuration names (no secret values):
- References: baseline/source commit, PR, build/release, related notes as applicable. A commit cannot contain its own final SHA; use the baseline or add the resulting reference in a later entry.
- Verification: commands/checks actually run and their results; state what was not tested.
- Remaining issues/blockers:
- Recovery/resume: exact next step and any compatibility or migration details needed to continue safely.

## 2026-10-09 — UI layout and Claude foundation handoff
- Status: documentation implemented; Android UI foundation planned, not started.
- Request: put the screenshot-inspired layout and Claude task into the repo so coding can proceed while ChatGPT designs artwork.
- Inspected: root contents, AGENTS.md, existing log/README, asset README/manifest; no Android source at root. Manifest contains 54 monster PNGs and five Java reference files. Fetched current ZPet RegionCatalog.java (blob cfb3b696838245a37c359622965ef9caabac41fa): 12 catalogue groups / 48 ordered areas, including repeated tradition labels.
- Changes: added ai/README.md task queue, ai/UI_LAYOUT_SPEC.md detailed nine-screen brief, ai/tasks/CLAUDE-001-UI-FOUNDATION.md scoped implementation handoff; updated root README.md to supersede stale blanket development deferral for this UI task.
- Implementation: specified responsive tokens, routing, reusable components, stable art lookup/fallbacks, all 48 exact areas, existing creature reuse, local vs preview interactions and explicit verification criteria. Assigned Claude a separate feature branch/PR; ChatGPT owns new artwork. Missing forms remain deferred. No app source, final generated assets, gameplay engine, import or release was added.
- References: layout commit 257bc447bef7169ba8d855aba60b9a92d84830b0; task commit 665fdb8a31d24cc74f5c57b92a6ea5b6ad7938da; workspace commit ef5a1c62876acde3ed60d3e2bf853631fd07eb44. Connector publishes one text file per commit, so documentation/log updates were sequential rather than a multi-file commit.
- Verification: fetched source docs and manifest; specification checked against current ZPet catalogue; published files to main. Readback verification follows this entry. No build or application tests run because this change is documentation only.
- Remaining: generated image boards remain in chat, not repo assets; branding preview approval/export pending; artwork phases 2-8 not started; Claude task Ready/unclaimed.
- Recovery/resume: read ai/README.md and Claude task, claim branch against fresh main, inspect source/parallel work, scaffold only if app source is still absent, reuse existing assets and keep missing art placeholders. Return a PR without merge/release.

## 2026-10-09 — CLAUDE-001 claimed; core catalogue module (Milestone A, in progress)
- Status: in progress on branch `claude/zbattle-ui-foundation` (base main `b1fa4c5`).
- Request: user asked Claude to claim CLAUDE-001 and implement the Android UI foundation per `ai/UI_LAYOUT_SPEC.md`.
- Parallel work found: open PR #1 (`ai/codex/zbattle-foundation`, base `758c43c`, unmerged) adds a Java Activity shell in `app/` (Camp/Adventure/Tasks/Master tabs) and `.github/ai/**`. Its build files conflict with this task's Kotlin/Compose `app/`. Not overwritten or merged; recorded for the user's decision. This branch adopts its idea of packaging `ZBattle-ZPet-Assets/assets` in place, and keeps package `com.zeus97x.zbattle`, minSdk 26, compileSdk/targetSdk 35.
- ZPet check: read-only fetch of ZPet main (`a67d0f6`) — `RegionCatalog.java` blob `cfb3b696` (unchanged from spec snapshot), `MonsterCatalog.java` identical to the reference copy, `app/src/main/assets/monsters` lists the same 54 PNGs. Nothing was written to ZPet.
- Changes: Gradle 8.11.1 wrapper; root Kotlin DSL build (AGP 8.7.3, Kotlin 2.0.21); `core/` platform-free module (creature/region catalogues, `ArtCatalog` art lookup, collection search/sort/filter, `NavState` navigation, local `PlayerSettings`, preview-only `PreviewContent`) with tests; `preview/` standalone JVM build for headless layout rendering.
- Environment blocker: this cloud session's network policy returns 403 for `dl.google.com` (Google Maven/Android SDK), so AGP, AndroidX and the Android SDK cannot be resolved here. Android compile/lint must be verified on GitHub Actions. The preview harness uses Compose Multiplatform desktop 1.5.12 from Maven Central (newer CMP releases pull AndroidX artifacts from Google Maven).
- Verification: `./gradlew -p preview test` — 21 core tests passed (catalogue names vs `reference/*.java`, duplicate-tradition group identity, 54 PNG mapping, manifest SHA-256 of all 54 PNGs, collection query, navigation back order, travel/profile rules). Android build not run (blocker above).
- Next step: shared Compose UI in `ui/`, Android entry point in `app/`, harness screenshots, CI workflow.

## 2026-10-09 — CLAUDE-001 shared Compose screens (work in progress, paused by user)
- Status: in progress; paused at the user's request. Not reviewed, no PR yet.
- Changes: `ui/src/main/kotlin/com/zeus97x/zbattle/ui/` — theme tokens, `ArtLoader`/`ArtworkSlot` placeholders, reusable components, `AppState`, root `ZBattleApp` with bottom bar and overlays, screens for Home, Collection, Travel, Challenges, Battle preview, Shop sheet, Item Shop, Double Battle, Profile, Achievements and Events, plus dialogs and the creature detail sheet. `preview/` — `FileArtLoader` and `LayoutRenderTest`, which renders every route/overlay at 412dp and 360dp, 1.3× font and light mode to `preview/build/screenshots/` (not committed).
- Verification: `./gradlew -p preview test` — 25 tests passed (21 core + 4 layout/state), 42 screenshots rendered and inspected. Android compile not run (Google Maven blocked by this session's network policy).
- Remaining: Android entry point (`app/src/main/AndroidManifest.xml`, `MainActivity`, asset `ArtLoader`, SharedPreferences `SettingsStore`), CI workflow for assembleDebug/lint, final task status, PR with screenshots. PR #1 conflict still needs the user's decision.
- Next step: add the Android entry point and CI, then verify on GitHub Actions.
