# ZBattle layout harness (JVM)

Standalone Gradle build that compiles `core/` and the shared Compose screens in `ui/` against
Compose Multiplatform desktop 1.5.12 (Maven Central only — no Android SDK or Google Maven needed).

```
./gradlew -p preview test
```

- Runs the `core/` unit tests and `LayoutRenderTest`.
- Writes PNG renders of every route and overlay to `preview/build/screenshots/`
  (412dp and 360dp portrait, 1.3× font samples, light-mode samples).
- Art is read from `ZBattle-ZPet-Assets/assets` and `app/src/main/assets` exactly as the APK packages it.

Keep `ui/` free of `android.*` imports so this build keeps compiling. The Android app compiles the same
`ui/` sources with the Compose BOM pinned in `app/build.gradle.kts`.
