# CLAUDE-001 — summary for ChatGPT (what Claude did)

Written by Claude, 2026-10-09 (America/Toronto). Read this first, then `DEVELOPMENT_LOG.md` (full history) and
`ai/tasks/CLAUDE-001-UI-FOUNDATION.md` (task status). `ai/tasks/CLAUDE-001-HANDOFF.md` is an older, superseded note.

- **Repository:** Zeus97x/ZBattle
- **Branch:** `claude/zbattle-ui-foundation`
- **PR:** [#2](https://github.com/Zeus97x/ZBattle/pull/2) — open, not merged; merging needs the user's approval.
- **Base:** main `b1fa4c5`.

## Status at a glance
| Area | State |
|---|---|
| All UI screens (setup + 9 spec layouts + Events/Achievements, sheets, dialogs) | Implemented |
| Tests (`./gradlew -p preview test`) | 28 passing |
| Layout renders at 360dp, 412dp, 1.3× font and light mode | Checked |
| Android compile, lint and debug APK | First build started on PR #2 when the user said "push the build". Check the PR's Actions run for the result. |
| Device/emulator testing | Not done |
| Final artwork | Not added (placeholders everywhere except the 54 existing ZPet creature PNGs) |

## What was built
**Stack.** Kotlin + Jetpack Compose (Compose BOM 2024.12.01, Material 3), AGP 8.7.3, Kotlin 2.0.21, Gradle wrapper 8.11.1, JDK 17.
- App id and package: `com.zeus97x.zbattle`
- SDK levels: minSdk 26, target/compileSdk 35

**Modules and folders:**
- `core/` — pure Kotlin, no Android code:
  - `Creatures.kt` — the 12 ZPet families and 72 forms, copied verbatim from ZPet. Families 0–8 (54 forms) have PNGs; families 9–11 (18 forms) do not.
  - `Regions.kt` — the 12 region groups and 48 areas with exact ZPet names. Group ids are numeric, because "Egyptian" (groups 3 and 6) and "Greek" (groups 0 and 7) repeat.
  - `ArtCatalog.kt` — single art lookup by stable keys (see Art below).
  - `PetMaster.kt` — Pet Master from Codex PR #1 / ZB-002:
    - name of 1–24 characters
    - styles Ranger, Dragon Disciple, Knight, Mystic, Artificer, each Male or Female
    - starters Sparklit, Inkling, Cindlet
  - `Profile.kt` — `PlayerSettings`: master, dark mode, music, battle animations, current area, visited areas. The party is the chosen starter. Also holds the preview travel-lock rule.
  - `Navigation.kt` — `NavState`: routes plus one overlay. Back closes the overlay, then the screen, then other tabs return to Home, then the app exits.
  - `CollectionQuery.kt` — search, sort and filters.
  - `PreviewContent.kt` — demo-only opponents, shop items and double-battle lineup. Not canon; delete it when real systems exist.
- `ui/` — shared Compose screens:
  - Screens: `SetupScreen`, `HomeScreen`, `CollectionScreen`, `TravelScreen`, `ChallengesScreen`, `BattleScreen`, `ShopScreens` (category sheet + Item Shop), `DoubleBattleScreen`, `ProfileScreens` (Profile, Achievements, Events), `Overlays` (creature detail, dialogs, Edit Pet Master).
  - Shared code: `Theme.kt` (spec colours and dp tokens), `Components.kt` (reusable cards and buttons), `Art.kt` (`ArtworkSlot` placeholders), `ZBattleApp.kt` (root, bottom bar with raised Home), `AppState.kt`.
  - This folder must contain no `android.*` imports.
- `app/` — Android only:
  - `MainActivity.kt` — ViewModel, edge-to-edge, Back routing
  - `AssetArtLoader.kt` — downsamples large PNGs
  - `PrefsSettingsStore.kt` — local SharedPreferences
  - `AndroidManifest.xml`
- `preview/` — a separate JVM Gradle build. It renders every screen to PNG with Compose Multiplatform desktop and runs all tests, with no Android SDK needed. Output goes to `preview/build/screenshots/`. Curated copies are in `docs/screenshots/claude-001/`.
- `.github/workflows/android-ui.yml` — on pull requests and manual dispatch:
  - runs `:core:test :app:assembleDebug :app:lintDebug` and `-p preview test`
  - uploads the APK, lint report, test reports and renders as the `zbattle-ui-validation` artifact
  - validation only: no signing, no release

## What works vs what is a preview
**Works locally:**
- First-run setup.
- Editing the Pet Master's name, style and gender (the starter stays fixed).
- All navigation and Back behaviour.
- Collection search, sort and filters.
- Travel selection, saved as the current location, with explanations for locked areas.
- Dark Mode, applied live.
- Battle Animations toggle (controls the preview hit-shake).
- Creature detail sheet.

**Marked Preview or Planned in the UI:**
- Battle engine and actions (no damage, results or rewards are recorded).
- Opponents and boss roster.
- Steps and boss-win thresholds.
- XP and coins (shown as 0).
- Shop items, prices and purchases (Buy explains that purchases are unavailable).
- Events and achievements.
- Music (the setting is saved but no audio plays).
- Skills and equipment, Pet Master tasks/AFK, and the one-way ZPet import.
- Travel locks follow a stated preview rule: the first location of each region is open, and the next opens after you visit the previous one.

## Art — how ChatGPT's images plug in
Put files at `app/src/main/assets/art/<key>.png` (or `.webp`). They are picked up automatically, with no layout code changes. Until a file exists, the screen draws a themed placeholder. Full list: `app/src/main/assets/art/README.md`.

| Art group | Key |
|---|---|
| Branding | `branding/logo`, `branding/icon`, `branding/splash` |
| Region maps | `region/group-<0..11>/map` |
| Location scenery cards | `location/area-<00..47>/hero` |
| Battle backgrounds | `location/area-<00..47>/battle` |
| Pet Master illustrations (ten) | `avatar/<ranger\|dragon-disciple\|knight\|mystic\|artificer>-<male\|female>` |
| Other | `boss/<id>`, `opponent/<id>`, `item/<id>`, `badge/area-<NN>`, `event/<id>` |

Example: `art/location/area-12/hero.png` is the Desert Crossing scenery card.

Creature art rules:
- Creature PNGs are packaged unchanged from `ZBattle-ZPet-Assets/assets/monsters/`. Do not copy or edit them; a test checks their SHA-256.
- Families 9–11 (the Ashpeep, Threadbit and Budfawn lines) show "Artwork pending". Do not substitute other art for them.

## Decisions to know
- **Tabs.** The bottom tabs follow `ai/UI_LAYOUT_SPEC.md` (Collection | Shop | Home | Events | Profile), not PR #1's Camp/Adventure/Tasks/Master. PR #1 was used as guidance for the Pet Master setup.
- **PR #1.** PR #1's Java app shell is replaced by this PR, so close it after PR #2 is accepted. Its `.github/ai/**` workspace docs were not copied.
- **Species variants.** The 48 species variants (`SpeciesCatalog`) are not shown, because ZPet's `BranchPalette` colour data is not in the reference pack.
- **No network or ZPet writes.** There is no network permission and no backend, and nothing is written back to ZPet.

## Remaining work (suggested order)
1. Read the PR #2 Actions result; if the build or lint fails, fix it on `claude/zbattle-ui-foundation`.
2. Test on a device:
   - insets with gesture and with 3-button navigation
   - Back and predictive back
   - rotation and process restore
   - 360dp screens and large fonts
   - TalkBack labels
3. Art phases 1–8 and the ten Pet Master illustrations, dropped into the `art/` keys above.
4. Gameplay from the PR #1 roadmap:
   - turn-based battle engine and save/recovery
   - skills, tools and equipment
   - AFK tasks
   - the one-way ZPet copy and Legacy Bonus
   - economy, events and achievements
   - permanent signing and release (only on the user's instruction)

## Rules to keep (from AGENTS.md)
- Update `DEVELOPMENT_LOG.md` in the same commit as each change.
- Never commit secrets.
- Never merge or release without the user's approval.
