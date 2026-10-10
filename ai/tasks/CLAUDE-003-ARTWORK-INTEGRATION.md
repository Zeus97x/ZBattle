# CLAUDE-003 — Integrate the artwork drop

Status: READY. Owner: Claude. Source branch: ai/chatgpt/artwork-handoff. Base: main 2051a4b. Updated 2026-10-09 America/Toronto.

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
