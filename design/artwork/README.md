# ZBattle artwork placement guide

70 original PNGs: 12 region maps, 48 primary location heroes, 7 landscape variants, 3 reference boards. Original bytes preserved.

The source images live under `design/artwork/`; Claude should export runtime images to the corresponding `app/src/main/assets/art/<art_key>.webp` paths. The manifest provides dimensions and SHA-256 checksums.

## Region maps

| Group | Source path | Runtime key |
|---|---|---|
| group-0 | `design/artwork/regions/group-0/map.png` | `region/group-0/map` |
| group-1 | `design/artwork/regions/group-1/map.png` | `region/group-1/map` |
| group-2 | `design/artwork/regions/group-2/map.png` | `region/group-2/map` |
| group-3 | `design/artwork/regions/group-3/map.png` | `region/group-3/map` |
| group-4 | `design/artwork/regions/group-4/map.png` | `region/group-4/map` |
| group-5 | `design/artwork/regions/group-5/map.png` | `region/group-5/map` |
| group-6 | `design/artwork/regions/group-6/map.png` | `region/group-6/map` |
| group-7 | `design/artwork/regions/group-7/map.png` | `region/group-7/map` |
| group-8 | `design/artwork/regions/group-8/map.png` | `region/group-8/map` |
| group-9 | `design/artwork/regions/group-9/map.png` | `region/group-9/map` |
| group-10 | `design/artwork/regions/group-10/map.png` | `region/group-10/map` |
| group-11 | `design/artwork/regions/group-11/map.png` | `region/group-11/map` |

## Location heroes

| Area | Exact ZPet location | Source path |
|---|---|---|
| area-00 | Olympian Foothills | `design/artwork/locations/area-00/hero.png` |
| area-01 | Thunderpeak | `design/artwork/locations/area-01/hero.png` |
| area-02 | Underworld Gates | `design/artwork/locations/area-02/hero.png` |
| area-03 | Elysian Horizon | `design/artwork/locations/area-03/hero.png` |
| area-04 | Yggdrasil Roots | `design/artwork/locations/area-04/hero.png` |
| area-05 | Rune Ruins | `design/artwork/locations/area-05/hero.png` |
| area-06 | Frostbound Fjord | `design/artwork/locations/area-06/hero.png` |
| area-07 | Aurora Citadel | `design/artwork/locations/area-07/hero.png` |
| area-08 | Jade Forest | `design/artwork/locations/area-08/hero.png` |
| area-09 | Celestial Peaks | `design/artwork/locations/area-09/hero.png` |
| area-10 | Dragon Palace | `design/artwork/locations/area-10/hero.png` |
| area-11 | Heavenly Gate | `design/artwork/locations/area-11/hero.png` |
| area-12 | Desert Crossing | `design/artwork/locations/area-12/hero.png` |
| area-13 | Golden Necropolis | `design/artwork/locations/area-13/hero.png` |
| area-14 | Veiled Dunes | `design/artwork/locations/area-14/hero.png` |
| area-15 | Guardian Horizon | `design/artwork/locations/area-15/hero.png` |
| area-16 | Lantern Path | `design/artwork/locations/area-16/hero.png` |
| area-17 | Mirror Grove | `design/artwork/locations/area-17/hero.png` |
| area-18 | Foxfire Shrine | `design/artwork/locations/area-18/hero.png` |
| area-19 | Dawn Sanctuary | `design/artwork/locations/area-19/hero.png` |
| area-20 | Feathered Canopy | `design/artwork/locations/area-20/hero.png` |
| area-21 | Wind Terrace | `design/artwork/locations/area-21/hero.png` |
| area-22 | Jade Garden | `design/artwork/locations/area-22/hero.png` |
| area-23 | Skywoven Summit | `design/artwork/locations/area-23/hero.png` |
| area-24 | Dawn Sands | `design/artwork/locations/area-24/hero.png` |
| area-25 | Solar Orchard | `design/artwork/locations/area-25/hero.png` |
| area-26 | Halo Oasis | `design/artwork/locations/area-26/hero.png` |
| area-27 | Sunrise Vault | `design/artwork/locations/area-27/hero.png` |
| area-28 | Foaming Shore | `design/artwork/locations/area-28/hero.png` |
| area-29 | Coral Passage | `design/artwork/locations/area-29/hero.png` |
| area-30 | Abyssal Reef | `design/artwork/locations/area-30/hero.png` |
| area-31 | Crest Horizon | `design/artwork/locations/area-31/hero.png` |
| area-32 | Moonlit Meadow | `design/artwork/locations/area-32/hero.png` |
| area-33 | Crescent Garden | `design/artwork/locations/area-33/hero.png` |
| area-34 | Jade Moon Terrace | `design/artwork/locations/area-34/hero.png` |
| area-35 | Lunar Sanctuary | `design/artwork/locations/area-35/hero.png` |
| area-36 | Ash Nest | `design/artwork/locations/area-36/hero.png` |
| area-37 | Cinder Ridge | `design/artwork/locations/area-37/hero.png` |
| area-38 | Dawn Roost | `design/artwork/locations/area-38/hero.png` |
| area-39 | Rebirth Summit | `design/artwork/locations/area-39/hero.png` |
| area-40 | Story Grove | `design/artwork/locations/area-40/hero.png` |
| area-41 | Riddle Crossing | `design/artwork/locations/area-41/hero.png` |
| area-42 | Silk Canopy | `design/artwork/locations/area-42/hero.png` |
| area-43 | Taleweaver Haven | `design/artwork/locations/area-43/hero.png` |
| area-44 | Moss Trail | `design/artwork/locations/area-44/hero.png` |
| area-45 | Briar Grove | `design/artwork/locations/area-45/hero.png` |
| area-46 | Elder Woodland | `design/artwork/locations/area-46/hero.png` |
| area-47 | Bloom Sanctuary | `design/artwork/locations/area-47/hero.png` |

## Variants and references

Variants are preserved for visual review; do not replace primary selections blindly. Branding is a three-panel concept board, not three standalone production assets. UI boards are references, not screenshots to ship as UI. No separate battle backgrounds, bosses, items, avatars or badges have been exported in this drop. Keep their existing honest placeholders. Do not extract art from boards or generate new artwork.

- location_variant: `design/artwork/variants/dawn-sanctuary-on-cherry-blossom-hill.png`
- location_variant: `design/artwork/variants/emerald-pools-beneath-yggdrasil.png`
- location_variant: `design/artwork/variants/heavenly-jade-paifang-above-the-clouds.png`
- location_variant: `design/artwork/variants/jade-garden-with-turquoise-pools.png`
- location_variant: `design/artwork/variants/moonlit-mirror-grove-1.png`
- location_variant: `design/artwork/variants/skywoven-pyramid-at-sunset.png`
- location_variant: `design/artwork/variants/windswept-mayan-limestone-terrace.png`
- ui_board: `design/artwork/references/zbattle-shops-battle-and-profile.png`
- branding_board: `design/artwork/references/zbattle-three-panel-branding-concept-board.png`
- ui_board: `design/artwork/references/zbattle-five-screen-fantasy-battle-ui.png`
