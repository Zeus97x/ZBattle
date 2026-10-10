# ZBattle

Adventure and battle game using the existing ZPet creature designs.

The user has authorized preparation of the Android UI foundation while ChatGPT finishes design and artwork. Start with [the AI workspace](ai/README.md), [UI layout specification](ai/UI_LAYOUT_SPEC.md), and [Claude's implementation task](ai/tasks/CLAUDE-001-UI-FOUNDATION.md).

The existing creature collection is in `ZBattle-ZPet-Assets/`. Reuse created pets; finishing missing forms is deferred. Its historical README describes the earlier deferred-development checkpoint. The root brief above is the current scoped instruction.

Player imports remain planned one-way from ZPet to ZBattle; ZBattle progress must never write back to ZPet.

Status: Android UI foundation (CLAUDE-001) is in review on branch `claude/zbattle-ui-foundation` — Kotlin + Jetpack Compose shell with all nine layouts, existing ZPet creatures and placeholders for pending art. Battle, economy, events and unlocks are explicitly previews. See [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md) for verified progress.

## Build (CLAUDE-001 branch)

- Requirements: JDK 17, Android SDK with platform 35. Gradle 8.11.1 via the wrapper; AGP 8.7.3; Kotlin 2.0.21; Compose BOM 2024.12.01.
- Debug APK, lint and core tests: `./gradlew :core:test :app:assembleDebug :app:lintDebug`
- Layout renders without the Android SDK: `./gradlew -p preview test` (see [preview/README.md](preview/README.md)).
- CI: `.github/workflows/android-ui.yml` runs both on pull requests (validation only; no signing or releases).
- Final artwork drop folder and naming: [app/src/main/assets/art/README.md](app/src/main/assets/art/README.md).
