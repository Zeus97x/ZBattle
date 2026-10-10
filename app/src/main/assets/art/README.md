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
