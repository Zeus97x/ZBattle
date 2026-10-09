# ZBattle
Separate Android adventure/AFK game using original ZPet creatures. Development authorized 2026-10-09.
Foundation: persisted Pet Master name, five cosmetic styles with male/female choices, three starter companions using approved artwork, Camp/Adventure/Tasks/Master tabs. Future gameplay explicitly marked as planned.
## Build
JDK17, Android SDK35, Gradle8.11.1, AGP8.9.2. No wrapper yet; install pinned Gradle or use manual-only .github/workflows/verify-foundation.yml.
Run: `gradle :app:assembleDebug :app:lintDebug --no-daemon`.
Profile suite: compile MasterProfile.java and tests/MasterProfileTest.java with javac; run MasterProfileTest.
No APK/release or permanent signing configured. Debug workflow artifact is validation only. Permanent signing and APK Releases must precede production updates.
## Recovery
Read .github/ai/README.md, PROJECT_STATUS.md, ROADMAP.md, AGENTS.md and DEVELOPMENT_LOG.md. Assets/reference files preserved in ZBattle-ZPet-Assets; reference Java does not compile into this app.
