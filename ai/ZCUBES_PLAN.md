# ZCubes — catching tool plan

Updated 2026-10-09 (America/Toronto). Status: **planned, not implemented.** Name approved by Zeus97x: **ZCubes**.

ZCubes are the future tool for adding creatures to a ZBattle party. CLAUDE-002 does not implement catching, inventory or purchases. This file records the plan so later tasks start from the same baseline.

## Tiers (placeholder names, art by ChatGPT)
| Tier | Placeholder name | Art key (drop file at `app/src/main/assets/art/<key>.png`) | Notes |
|---|---|---|---|
| 1 | Basic ZCube | `item/zcube-basic` | Starting tier |
| 2 | Great ZCube | `item/zcube-great` | Better odds |
| 3 | Ultra ZCube | `item/zcube-ultra` | High odds |
| 4 | Mythic ZCube (or Legendary) | `item/zcube-mythic` | Rare; final name to be chosen with the art |

ChatGPT owns the visual design. The design should read as one family of cubes, with each tier clearly distinguishable at 48dp.

## ZPet baseline (read-only reference: ZPet `SpeciesCatalog.java`)
- **Rarities:** Common, Heroic, Mythic, Celestial (four species variants per family).
- **Catch chance by rarity:** 80% / 65% / 50% / 35% (`catchChance`).
- **Rarity rolls by area stage (`roll`), out of 100:**

  | Area stage | Common | Heroic | Mythic | Celestial |
  |---|---|---|---|---|
  | 0 | 85% | 15% | — | — |
  | 1 | 65% | 27% | 8% | — |
  | 2–3 | 50% | 32% | 15% | 3% |

- **Visuals:** the variants reuse family art with a colour matrix. ZPet's `BranchPalette.java` now exists in ZPet `main`; it is not yet copied into `ZBattle-ZPet-Assets/reference/`.

## Proposed rules (for approval; not implemented)
- Catching happens from a won or weakened wild encounter. Bosses cannot be caught.
- The ZCube tier adds to the base chance: Basic +0, Great +10, Ultra +20, Mythic +35 percentage points. The total is capped at 95%, except Mythic, which is capped at 100%.
- Each ZCube is consumed on use. Every attempt is recorded once per battle id, so retries cannot duplicate creatures.
- Caught creatures become `OwnedCreature` entries in ZBattle's own save (`BattleProgress`). Nothing is written to ZPet.

## Open decisions for Zeus97x / ChatGPT
1. The final name of tier 4.
2. How ZCubes are obtained: shop price, battle drops, or both. This depends on the economy task.
3. Whether a failed catch ends the encounter.
4. Whether to use ZPet's species-variant rarity system, which needs `BranchPalette` copied in as a reference.
