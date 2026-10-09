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
