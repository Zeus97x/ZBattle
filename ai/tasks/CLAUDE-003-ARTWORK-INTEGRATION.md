# CLAUDE-003 — Integrate the artwork drop

Status: DONE — merged in PR #6 (merge 08a59c3; it also carries ChatGPT's artwork drop from PR #4). Owner: Claude.

## Read first
Read AGENTS.md, DEVELOPMENT_LOG.md, ai/UI_LAYOUT_SPEC.md, ai/CLAUDE-001-SUMMARY-FOR-CHATGPT.md, design/artwork/README.md and design/artwork/manifest.json. Check current main and open work; avoid overlapping the battle vertical-slice task. Claim this task with branch/base/status/time before editing.

## Goal and exact scope
Wire the supplied 12 maps and 48 primary landscapes into the existing Home location carousel and Travel map slots. All 70 source PNGs are preserved in design/artwork, including seven variants and three reference boards. This is a complete inventory of this ZBattle artwork drop, not a claim that every artwork phase is finished.

1. Use manifest art_key values and existing ArtCatalog: numeric group-0..11, area-00..47. Never key by tradition label because Egyptian and Greek repeat. Preserve exact RegionCatalog names/order; verify against ZPet's current catalogue if available and flag changes rather than guessing.
2. Export appropriately sized WebP runtime copies to app/src/main/assets/art/region/group-N/map.webp and location/area-NN/hero.webp. Preserve original PNGs. Inspect source dimensions and loader bounds, choose quality after visual comparison, and avoid bundling duplicate PNG/WebP runtime copies. No colour/style changes or generated replacements. Record sizes/quality.
3. Existing AssetArtLoader / ArtCatalog should pick these keys up; confirm actual screen usage, scrolling and cropping. Map landmarks must remain readable, hero titles legible with scrims, and existing creature art unchanged. Use native UI/icons for controls.
4. The seven variants are optional alternatives, not extra locations. The branding board and two UI boards remain references. Do not flatten boards into UI, extract production branding from the board or replace the launcher/splash until standalone exports exist. ChatGPT handles all image generation.
5. Battle art is a separate phase. Keep its placeholders unless the user explicitly approves reusing a hero. No gameplay engine/stat/economy/backend changes in this task. Preserve unfinished creature forms as deferred.

## Acceptance and evidence
- 12 region keys and all 48 hero keys resolve; check missing files and exact ID coverage against RegionCatalog, plus PNG integrity/checksums in the source manifest.
- Home/Travel show corresponding artwork, including both Egyptian and both Greek groups distinctly. Check long labels, cropping, contrast and memory-safe image loading.
- Run appropriate existing core/preview checks and Android compilation/lint where available. Inspect 360dp/412dp and large text layouts; attach screenshots and record unavailable device checks honestly.
- Update DEVELOPMENT_LOG.md, ai/README.md and this handoff with changed paths, validation, pending art and next steps.
- Return a focused implementation PR and a concise summary for ChatGPT. Do not merge, publish a release or trigger an APK release workflow. This handoff PR contains assets/docs only; user reviews artwork phases separately.

## Working branch
If artwork PR is still open, branch from ai/chatgpt/artwork-handoff so images are present; target the artwork branch to isolate implementation, then retarget main after artwork merge. If merged, branch from current main. Do not work directly on main or edit another owner's battle task.

## Completion report (Claude, 2026-10-09)

### What changed
- **Runtime exports:** `tools/export_artwork.py` verifies all 70 source PNGs against `design/artwork/manifest.json` (SHA-256), then writes 60 WebP files to `app/src/main/assets/art/region/group-N/map.webp` and `.../location/area-NN/hero.webp`.
  - Long side 1280px, quality 82, total 12.7 MiB.
  - Quality was chosen from 1:1 crops at q75/82/88: q75 softened skies, and q82 matched the source.
  - Every export is recorded in `design/artwork/runtime-exports.json` (key, source, checksum, size, bytes).
  - Source PNGs are untouched, and no PNG runtime copies are bundled.
- **Not exported:** the 7 variants (optional alternatives) and the 3 reference boards (branding and UI concepts). Launcher icon and splash are unchanged until standalone branding exports exist.
- **Battle scenery** (`location/area-NN/battle`) stays a placeholder; it is a separate art phase.
- **Wiring:** the existing `ArtCatalog` keys pick the files up with no screen code naming files.
  - Home carousel cards, Travel map, Travel area rows and the Challenges header all show the scenery.
  - "Scenery pending" chips hide automatically.
- **Travel map:** maps are portrait, about 2:3. Travel now shows the whole map (Contain) over a dimmed cover backdrop, so no landmark is cropped. The region title moved above the map. `ArtKey.RegionMap` fit changed to Contain.
- **Loader** (`app/.../AssetArtLoader.kt`): it now picks the largest power-of-two downsample that keeps the long side at or above 768px. 1280px scenery decodes at full size, and 1536px creature PNGs still decode at 768px as before.
- **Harness:** `preview/.../FileArtLoader.kt` now decodes with Skia, which supports WebP, like Android.

### Verification
- `core/.../ArtworkTest.kt`:
  - all 60 keys resolve to exactly one `.webp`; there are no PNG runtime copies
  - the two Egyptian and two Greek groups show distinct maps
  - all 48 manifest names equal `RegionCatalog` exactly
  - all 70 source checksums match, and all 60 exports trace back to verified sources
  - no battle art yet
- Render checks: Home and Travel at 412dp, 360dp and 1.3× font. Travel is rendered for groups 1, 4, 7 and 8 (both Greek and both Egyptian groups).
- Android CI: see the PR. There is no device run by Claude.

### For ChatGPT
- **Next art phases:**
  - battle backgrounds (`location/area-NN/battle`)
  - standalone branding exports (logo, icon, splash)
  - ZCube tiers (`ai/ZCUBES_PLAN.md`)
- **Re-exporting after art changes:** replace the PNG under `design/artwork/`, update its manifest checksum, then run `python3 tools/export_artwork.py`.
- **Variant picks:** to use a variant instead of a primary image, give it a primary `art_key` in the manifest.
