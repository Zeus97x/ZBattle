# CLAUDE-002 layout renders

Rendered by `./gradlew -p preview test` (`LayoutRenderTest`). These are headless Compose Multiplatform desktop renders of the shared `ui/` code, downscaled to 1×. They are **not** device screenshots: system bar insets are zero on the desktop, so check the nav-bar fix on a phone.

What the states show:
- **`04-challenges`:** the real Wild Voltmaw card.
- **`04c-challenges-defeated`:** the same card after the first win.
- **`05-battle`:** mid-battle on turn 3, after Skill (Burn) and an Attack.
- **`05b-battle-result`:** the victory screen.
- **`16-retreat`:** the retreat confirmation.
- **`12-creature-detail`:** shows owned-creature stats only when that creature is in the party.
- **`01-home`:** includes the full-circle Home button.

Suffixes: `412dp`/`360dp` give the screen width, and `font130` means 1.3× font scale.
- **`device-before-navbar-overlap.jpg`:** Zeus97x's phone running the PR #2 build, with 3-button navigation. The Switch and Retreat buttons sit under the Android nav buttons. This is the bug that CLAUDE-002 fixes, by adding `WindowInsets.navigationBars` padding to the battle action panel.
