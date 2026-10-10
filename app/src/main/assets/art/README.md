# Final artwork drop folder

ChatGPT-approved illustrations go here, named by the stable keys in
`core/src/main/kotlin/com/zeus97x/zbattle/core/ArtCatalog.kt` (`.png` or `.webp`):

- `branding/logo`, `branding/icon`, `branding/splash`
- `region/group-<0..11>/map`
- `location/area-<00..47>/hero`, `location/area-<00..47>/battle`
- `boss/<id>`, `opponent/<id>`, `item/<id>`, `badge/<id>`, `avatar/<id>`, `event/<id>`
- ZCubes (planned, see `ai/ZCUBES_PLAN.md`): `item/zcube-basic`, `item/zcube-great`, `item/zcube-ultra`, `item/zcube-mythic`

Example: `art/location/area-12/hero.png` is the Desert Crossing scenery card.
Screens pick files up automatically; until a file exists they draw a placeholder.
Existing ZPet creature PNGs are not copied here — they are packaged unchanged from
`ZBattle-ZPet-Assets/assets/monsters/`.

## Current contents (CLAUDE-003)
- `region/group-0..11/map.webp`: the 12 region maps.
- `location/area-00..47/hero.webp`: the 48 primary location heroes.

These are exported from `design/artwork/` by `tools/export_artwork.py` (1280px long side, WebP quality 82). Battle scenery, branding, opponents, items, badges, avatars and ZCubes are still placeholders.
