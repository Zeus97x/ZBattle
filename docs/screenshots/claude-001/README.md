# CLAUDE-001 layout renders

Rendered 2026-10-09 by `./gradlew -p preview test` (`preview/src/test/.../LayoutRenderTest.kt`), downscaled to 1×.
These are headless Compose Multiplatform desktop renders of the shared `ui/` code — not device or emulator
screenshots. They show the same composables the Android app uses; fonts and some Material details can
differ slightly on a phone. Suffixes: `412dp`/`360dp` portrait width, `font130` = 1.3× font scale, `light` = light mode.
All scenery, map, opponent, item, badge and avatar art is placeholder; creature art is the unchanged ZPet PNGs.
The harness writes 47 renders; the manual CI workflow uploads the full set as the `zbattle-ui-validation` artifact.

`00-setup` is the first-run Pet Master setup (style/gender/starter from PR #1's ZB-002 foundation).
