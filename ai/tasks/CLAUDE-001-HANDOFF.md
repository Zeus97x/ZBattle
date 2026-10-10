# CLAUDE-001 handoff — remaining work

> **Superseded 2026-10-10:** the user withdrew this handoff and Claude resumed and completed the steps below on the same branch. Kept for history; see the task file and DEVELOPMENT_LOG.md for current status.

Updated 2026-10-09 America/Toronto. Branch: `claude/zbattle-ui-foundation` (base main `b1fa4c5`). No PR opened yet.
Claude paused at the user's request; the user asked ChatGPT to continue. Read AGENTS.md, DEVELOPMENT_LOG.md and ai/UI_LAYOUT_SPEC.md first.

## Already done (committed)
- Gradle 8.11.1 wrapper; root `settings.gradle.kts`, `build.gradle.kts` (AGP 8.7.3, Kotlin 2.0.21), `gradle.properties`, `.gitignore`.
- `app/build.gradle.kts` — Compose app config, package `com.zeus97x.zbattle`, min 26 / target 35. Compiles `app/src/main/kotlin` + shared `../ui/src/main/kotlin`; packages `../ZBattle-ZPet-Assets/assets` unchanged.
- `core/` — catalogues (exact ZPet names), `ArtCatalog` lookup, `CollectionQuery`, `NavState`, `PlayerSettings`/`SettingsStore`, `TravelRules`, preview-only `PreviewContent`, and 21 tests.
- `ui/` — theme, components, `ArtLoader`/`ArtworkSlot`, `AppState`, `ZBattleApp` root, all nine screens + Events/Achievements, dialogs and sheets. Uses only platform-free Compose APIs.
- `preview/` — standalone JVM build that renders every screen to PNG. Run `./gradlew -p preview test`; screenshots land in `preview/build/screenshots/` (not committed). 25 tests pass.

## Not done (next steps, in order)
1. `app/src/main/AndroidManifest.xml`: launcher activity `.MainActivity`, `android:windowSoftInputMode="adjustResize"`, `allowBackup=false`, theme `@android:style/Theme.Material.NoActionBar`, label ZBattle.
2. `app/src/main/kotlin/com/zeus97x/zbattle/MainActivity.kt` (ComponentActivity):
   - `enableEdgeToEdge()`; create `AppState(PrefsSettingsStore(this))` once (e.g. `remember`), `setContent { CompositionLocalProvider(LocalArtLoader provides AssetArtLoader(assets)) { ZBattleApp(state) } }`.
   - Back: `onBackPressedDispatcher.addCallback(this) { if (!state.back()) { isEnabled = false; onBackPressedDispatcher.onBackPressed() } }`.
3. `AssetArtLoader` implementing `ui.ArtLoader`: open `assets.open(path)`, decode with `BitmapFactory` using `inSampleSize` so the longest side is ≤ 768 px (pattern in ZBattle-ZPet-Assets/reference/CreatureView.java), cache in an `LruCache`, return null on `IOException` (missing art → placeholder).
4. `PrefsSettingsStore` implementing `core.SettingsStore` with SharedPreferences `zbattle.settings.v1`: displayName, darkMode, music, battleAnimations, currentAreaIndex, visitedAreas (comma-joined ints). Validate on load (area index 0..47, `PlayerSettings.validName`), fall back to defaults.
5. CI: `.github/workflows/android-ui.yml` on pull_request + workflow_dispatch — JDK 17, `android-actions/setup-android@v3`, run `./gradlew :core:test :app:assembleDebug :app:lintDebug` and `./gradlew -p preview test`; upload the debug APK and `preview/build/screenshots` as artifacts. Validation only, no release.
6. Open the PR against main (do not merge). Note the conflict with open PR #1 (`ai/codex/zbattle-foundation`, Java shell in `app/`); the user decides which foundation to keep.
7. Update DEVELOPMENT_LOG.md and the CLAUDE-001 task status (In progress → Review) with real check results.

## Known limits / cautions
- The Android module has never been compiled (Claude's environment blocked dl.google.com). Expect to fix small API differences: `ui/` was verified against Compose Multiplatform 1.5.12 (Material3 1.1); the app uses Compose BOM 2024.12.01. Deprecation warnings (e.g. `Icons.Filled.ArrowBack`, `LinearProgressIndicator(progress = Float)`) are expected and harmless.
- Keep `ui/` free of `android.*` imports so `preview/` keeps compiling.
- Do not add generated art to `ui/`; final art goes in `app/src/main/assets/art/<key>.png` (keys in `core/.../ArtCatalog.kt`), e.g. `art/location/area-12/hero.png`, `art/region/group-3/map.png`.
- Families 9–11 (Ashpeep, Threadbit, Budfawn lines) have no artwork; they show as "Artwork pending". Do not substitute art.
- Opponents, shop items, prices, rewards, XP/coins, step/boss thresholds and travel locks are preview placeholders, not canon.
