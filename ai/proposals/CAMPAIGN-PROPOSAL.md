# ZBattle campaign proposal (campaign-proposal-1)

> **PROPOSAL — not active; needs Zeus97x approval (D-CAMPAIGN).**
> Nothing here is final content. It answers owner decision Q6: "Prepare a campaign proposal using existing catalogue creatures and exact region/location IDs. Recommend stage counts, bosses, unlocks and difficulty progression. Do not invent creatures or finalize campaign content yet."
> Scope: CLAUDE-005 B4 (configuration-driven wild encounters, a numbered campaign, and mini, stage, location and region bosses across exact areas, with legacy cleared progress kept through migration). This document does not change source code, saves or `rulesRevision`.

Companion files:
- [`campaign-proposal.json`](campaign-proposal.json): the machine-readable roster, 300 encounters.
- `tools/campaign_proposal.py`:
  - holds the formulas
  - `--write` regenerates the JSON
  - with no flag, validates it against `Creatures.kt` and the shipped encounter (this runs in CI)
  - `--simulate --party N` mirrors `BattleEngine` and the auto policy to estimate win rates

> **Decision batch 2 (Zeus97x, 2026-10-10): layout approved (D-CAMPAIGN), Q-F answered with option (c) (D-CURVE).** First-win XP now follows D-ECONOMY-XP (300 per ordinary area + 150 per region boss; the shipped encounter keeps 60). Opponent stats stay **provisional** until a simulation of the real three-member party, switching, evolution and equipment is reviewed; bosses must be a meaningful challenge. `campaign-proposal.json` is revision 2, status `APPROVED_LAYOUT`, and the runtime still ships only `area-00/slot-0`.

> **Claude review note (2026-10-10).** Claude reconciled this draft with [ECONOMY-PROPOSAL.md](ECONOMY-PROPOSAL.md) before committing it. The JSON now uses the economy's **Option B** first-win XP; the shipped `area-00/slot-0` keeps 60. The draft's XP of 10/15/20/30/50 was tuned for a lone starter, and Zeus97x has since decided on a party of 3 with split XP. Section 7.6 shows the effect: the opponent curve and the XP values can't both stay as drafted. That choice is open question **Q-F**.

## 1. Summary

| | Proposal |
|---|---|
| Areas covered | All 48 (`area-00`…`area-47`) in 12 groups (`group-0`…`group-11`) |
| Encounters per area | 6 (wild ×3, mini boss, stage boss, location boss). The last area of each group adds a region boss, so it has 7. |
| Total encounters | **300**: 144 wild, 48 mini_boss, 48 stage_boss, 48 location_boss, 12 region_boss |
| Creatures used | 54 distinct ids, all from families 0–8 (the ones with artwork). **No art-blocked creature is used. No creature is invented.** |
| Legacy | `area-00/slot-0` stays exactly as it is: wild `voltmaw`, opponent level 2, 60 first-win XP, same stats (30/5/4/5) |
| Player curve | First-win XP adds up to 5 210, so a player who wins every encounter once reaches about level 50 in the last group (level = 1 + xp/100, capped at 50) |
| Opponent curve | One tier T = areaIndex for the whole campaign. The ramp per area follows the player's growth per level (power/3, guard/4, speed/5). Simulated against the current engine, a Baby starter at the expected level beats every wild, mini-boss and stage-boss tier, beats most location bosses, and finds region bosses a deliberate wall. |
| Region completion | Clearing a group's `region_boss` completes that region. A count of completed regions is kept for Phase C mystical unlocks (3/6/9/12, Q10); this proposal does not design those bosses. |

## 2. Principles

1. **Exact ids only.** Areas are `area-NN` (`Regions.kt`, `Area.id`), groups are `group-G` with `G = areaIndex / 4`, and an area's stage is `areaIndex % 4`. Encounter ids keep the existing format `<areaId>/slot-<n>`, with slots 0-based and contiguous in each area. Tradition labels repeat (Egyptian is groups 3 and 6, Greek is groups 0 and 7), so the proposal never uses a label as a key.
2. **Existing catalogue creatures only.** Every `creatureId` is a lowercase name without spaces from `CreatureCatalog.families[*].forms`. `tools/campaign_proposal.py` parses `Creatures.kt` on its own and rejects any unknown or art-blocked id.
3. **Art availability.** Only families 0–8 have PNGs (`FAMILIES_WITH_ARTWORK = 9`). Families 9–11 (Rebirth bird, Web spider, Forest deer) are listed with no art and must never get substitute art. Groups 9–11 therefore use an interim roster for now (section 6).
4. **The ZPet rule is the baseline.** ZPet `AdventureState.Battle` (ZPet `1adcedb`, `AdventureState.java` lines 60–69) picks the enemy as `MonsterCatalog.FAMILIES[RegionCatalog.family(route)][boss ? 2 : 1]`, where `RegionCatalog.family(area) = area/4`, and sets `boss = !practice && number % 10 == 0` (a boss every 10th encounter on a route). First-win XP is `boss ? 200 : 60` (`AdventureState.java:26`). The enemy stat curve is power 5+2·stage, guard 4+stage, speed 5+stage, HP 30+10·stage, and bosses get +4/+3/+0/+30. ZPet also gates the next area: `WorldState.refreshRegionUnlocks()` unlocks area n+1 once area n is unlocked, its boss is beaten (`cleared >= 10`) and it has 5 000 route steps (`REGION_STEP_REQUIREMENT`). Stage-0 areas are always selectable. This proposal keeps the family-per-area rule and uses the young form (1) for slot 0 and the Branch A advanced form (2) for the ZPet-style boss role. It adds the other forms of the same family so a roster is not one creature repeated.
5. **Configuration-driven.** Every encounter is a data row: `id`, `areaIndex`, `slot`, `kind`, `creatureId`, `opponentLevel`, `firstWinXp`. Stats come from a formula keyed on `(areaIndex, kind)`, so rebalancing changes the formula and not 300 rows.
6. **No silent rebalancing of shipped content.** The only playable encounter today keeps its id, meaning, level, stats and XP.

## 3. Recommended structure per area

The proposal treats an area as one numbered campaign location. Its slots are played in order:

| Slot | Kind | Role | Form used (by area stage 0 / 1 / 2 / 3) |
|---|---|---|---|
| 0 | `wild` | Opening wild encounter (ZPet "wild" role) | Young (1) at every stage |
| 1 | `wild` | Wild | Baby (0) / Baby (0) / B-adv (4) / B-adv (4) |
| 2 | `mini_boss` | Mid-area checkpoint | Young (1) / A-adv (2) / A-adv (2) / A-adv (2) |
| 3 | `wild` | Wild | Baby (0) / Young (1) / B-adv (4) / A-adv (2) |
| 4 | `stage_boss` | Closes the area's numbered stage track; the counterpart of ZPet's "every 10th" boss | B-adv (4) / B-adv (4) / A-adv (2) / B-adv (4) |
| 5 | `location_boss` | Area guardian. A first win clears the area and opens the next one. | A-adv (2) / A-adv (2) / B-final (5) / B-final (5) |
| 6 | `region_boss` | Only in stage-3 areas (`area-03`, `-07`, … `-47`). A first win completes the region (group). | A-final (3) |

Definitions to confirm (open question Q-A):
- A **stage** is the numbered campaign track inside an area, so the **stage boss** closes that track.
- A **location boss** clears the area.
- A **region boss** clears the group.

Another reading is that "stage" means `Area.stage` (0–3), so a stage boss would equal a location boss. In that case slot 4 becomes a fourth wild and the kinds collapse to four.

The **campaign number** shown to players can be `areaIndex·10 + slot + 1`, for example "Stage 1-1" up to "48-7". It is display only and never stored, so ids stay stable.

Alternative sizes if 6–7 encounters per area is too many:
- Lean (5 per area, 252 total): drop slot 3.
- Rich (8 per area): add a wild before the stage boss.

## 4. Roster: all 12 groups and 48 areas

Abbreviations: W = wild, MB = mini_boss, SB = stage_boss, LB = location_boss, RB = region_boss. L is the opponent level. The last number is first-win XP.

#### group-0 · Greek

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-00` | Olympian Foothills | 0 | 0 · W · `voltmaw` · L2 · 60<br>1 · W · `sparklit` · L2 · 10<br>2 · MB · `voltmaw` · L3 · 15<br>3 · W · `sparklit` · L2 · 10<br>4 · SB · `galestride` · L4 · 20<br>5 · LB · `crownstorm` · L5 · 30 |
| `area-01` | Thunderpeak | 1 | 0 · W · `voltmaw` · L3 · 10<br>1 · W · `sparklit` · L3 · 10<br>2 · MB · `crownstorm` · L4 · 15<br>3 · W · `voltmaw` · L3 · 10<br>4 · SB · `galestride` · L5 · 20<br>5 · LB · `crownstorm` · L6 · 30 |
| `area-02` | Underworld Gates | 2 | 0 · W · `voltmaw` · L4 · 10<br>1 · W · `galestride` · L4 · 10<br>2 · MB · `crownstorm` · L5 · 15<br>3 · W · `galestride` · L4 · 10<br>4 · SB · `crownstorm` · L6 · 20<br>5 · LB · `tempestral` · L7 · 30 |
| `area-03` | Elysian Horizon | 3 | 0 · W · `voltmaw` · L5 · 10<br>1 · W · `galestride` · L5 · 10<br>2 · MB · `crownstorm` · L6 · 15<br>3 · W · `crownstorm` · L5 · 10<br>4 · SB · `galestride` · L7 · 20<br>5 · LB · `tempestral` · L8 · 30<br>6 · RB · `astrapex` · L9 · 50 |

#### group-1 · Norse

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-04` | Yggdrasil Roots | 0 | 0 · W · `runebeak` · L6 · 10<br>1 · W · `inkling` · L6 · 10<br>2 · MB · `runebeak` · L7 · 15<br>3 · W · `inkling` · L6 · 10<br>4 · SB · `ironquill` · L8 · 20<br>5 · LB · `glyphwing` · L9 · 30 |
| `area-05` | Rune Ruins | 1 | 0 · W · `runebeak` · L7 · 10<br>1 · W · `inkling` · L7 · 10<br>2 · MB · `glyphwing` · L8 · 15<br>3 · W · `runebeak` · L7 · 10<br>4 · SB · `ironquill` · L9 · 20<br>5 · LB · `glyphwing` · L10 · 30 |
| `area-06` | Frostbound Fjord | 2 | 0 · W · `runebeak` · L8 · 10<br>1 · W · `ironquill` · L8 · 10<br>2 · MB · `glyphwing` · L9 · 15<br>3 · W · `ironquill` · L8 · 10<br>4 · SB · `glyphwing` · L10 · 20<br>5 · LB · `wargraven` · L11 · 30 |
| `area-07` | Aurora Citadel | 3 | 0 · W · `runebeak` · L9 · 10<br>1 · W · `ironquill` · L9 · 10<br>2 · MB · `glyphwing` · L10 · 15<br>3 · W · `glyphwing` · L9 · 10<br>4 · SB · `ironquill` · L11 · 20<br>5 · LB · `wargraven` · L12 · 30<br>6 · RB · `oracrow` · L13 · 50 |

#### group-2 · Chinese

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-08` | Jade Forest | 0 | 0 · W · `kilnback` · L10 · 10<br>1 · W · `cindlet` · L10 · 10<br>2 · MB · `kilnback` · L11 · 15<br>3 · W · `cindlet` · L10 · 10<br>4 · SB · `flarecrest` · L12 · 20<br>5 · LB · `forgehide` · L13 · 30 |
| `area-09` | Celestial Peaks | 1 | 0 · W · `kilnback` · L11 · 10<br>1 · W · `cindlet` · L11 · 10<br>2 · MB · `forgehide` · L12 · 15<br>3 · W · `kilnback` · L11 · 10<br>4 · SB · `flarecrest` · L13 · 20<br>5 · LB · `forgehide` · L14 · 30 |
| `area-10` | Dragon Palace | 2 | 0 · W · `kilnback` · L12 · 10<br>1 · W · `flarecrest` · L12 · 10<br>2 · MB · `forgehide` · L13 · 15<br>3 · W · `flarecrest` · L12 · 10<br>4 · SB · `forgehide` · L14 · 20<br>5 · LB · `pyrelisk` · L15 · 30 |
| `area-11` | Heavenly Gate | 3 | 0 · W · `kilnback` · L13 · 10<br>1 · W · `flarecrest` · L13 · 10<br>2 · MB · `forgehide` · L14 · 15<br>3 · W · `forgehide` · L13 · 10<br>4 · SB · `flarecrest` · L15 · 20<br>5 · LB · `pyrelisk` · L16 · 30<br>6 · RB · `vulcarion` · L17 · 50 |

#### group-3 · Egyptian

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-12` | Desert Crossing | 0 | 0 · W · `sandward` · L14 · 10<br>1 · W · `dunepup` · L14 · 10<br>2 · MB · `sandward` · L15 · 15<br>3 · W · `dunepup` · L14 · 10<br>4 · SB · `veilfang` · L16 · 20<br>5 · LB · `giltguard` · L17 · 30 |
| `area-13` | Golden Necropolis | 1 | 0 · W · `sandward` · L15 · 10<br>1 · W · `dunepup` · L15 · 10<br>2 · MB · `giltguard` · L16 · 15<br>3 · W · `sandward` · L15 · 10<br>4 · SB · `veilfang` · L17 · 20<br>5 · LB · `giltguard` · L18 · 30 |
| `area-14` | Veiled Dunes | 2 | 0 · W · `sandward` · L16 · 10<br>1 · W · `veilfang` · L16 · 10<br>2 · MB · `giltguard` · L17 · 15<br>3 · W · `veilfang` · L16 · 10<br>4 · SB · `giltguard` · L18 · 20<br>5 · LB · `duskjudge` · L19 · 30 |
| `area-15` | Guardian Horizon | 3 | 0 · W · `sandward` · L17 · 10<br>1 · W · `veilfang` · L17 · 10<br>2 · MB · `giltguard` · L18 · 15<br>3 · W · `giltguard` · L17 · 10<br>4 · SB · `veilfang` · L19 · 20<br>5 · LB · `duskjudge` · L20 · 30<br>6 · RB · `tombwarden` · L21 · 50 |

#### group-4 · Japanese

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-16` | Lantern Path | 0 | 0 · W · `emberveil` · L19 · 10<br>1 · W · `wispkit` · L19 · 10<br>2 · MB · `emberveil` · L20 · 15<br>3 · W · `wispkit` · L19 · 10<br>4 · SB · `lanternfox` · L21 · 20<br>5 · LB · `mirrortail` · L22 · 30 |
| `area-17` | Mirror Grove | 1 | 0 · W · `emberveil` · L20 · 10<br>1 · W · `wispkit` · L20 · 10<br>2 · MB · `mirrortail` · L21 · 15<br>3 · W · `emberveil` · L20 · 10<br>4 · SB · `lanternfox` · L22 · 20<br>5 · LB · `mirrortail` · L23 · 30 |
| `area-18` | Foxfire Shrine | 2 | 0 · W · `emberveil` · L21 · 10<br>1 · W · `lanternfox` · L21 · 10<br>2 · MB · `mirrortail` · L22 · 15<br>3 · W · `lanternfox` · L21 · 10<br>4 · SB · `mirrortail` · L23 · 20<br>5 · LB · `dawnflare` · L24 · 30 |
| `area-19` | Dawn Sanctuary | 3 | 0 · W · `emberveil` · L22 · 10<br>1 · W · `lanternfox` · L22 · 10<br>2 · MB · `mirrortail` · L23 · 15<br>3 · W · `mirrortail` · L22 · 10<br>4 · SB · `lanternfox` · L24 · 20<br>5 · LB · `dawnflare` · L25 · 30<br>6 · RB · `veilnine` · L26 · 50 |

#### group-5 · Mesoamerican

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-20` | Feathered Canopy | 0 | 0 · W · `plumeserp` · L23 · 10<br>1 · W · `plumeling` · L23 · 10<br>2 · MB · `plumeserp` · L24 · 15<br>3 · W · `plumeling` · L23 · 10<br>4 · SB · `jadecoil` · L25 · 20<br>5 · LB · `galeplume` · L26 · 30 |
| `area-21` | Wind Terrace | 1 | 0 · W · `plumeserp` · L24 · 10<br>1 · W · `plumeling` · L24 · 10<br>2 · MB · `galeplume` · L25 · 15<br>3 · W · `plumeserp` · L24 · 10<br>4 · SB · `jadecoil` · L26 · 20<br>5 · LB · `galeplume` · L27 · 30 |
| `area-22` | Jade Garden | 2 | 0 · W · `plumeserp` · L25 · 10<br>1 · W · `jadecoil` · L25 · 10<br>2 · MB · `galeplume` · L26 · 15<br>3 · W · `jadecoil` · L25 · 10<br>4 · SB · `galeplume` · L27 · 20<br>5 · LB · `verdantcrest` · L28 · 30 |
| `area-23` | Skywoven Summit | 3 | 0 · W · `plumeserp` · L26 · 10<br>1 · W · `jadecoil` · L26 · 10<br>2 · MB · `galeplume` · L27 · 15<br>3 · W · `galeplume` · L26 · 10<br>4 · SB · `jadecoil` · L28 · 20<br>5 · LB · `verdantcrest` · L29 · 30<br>6 · RB · `skyweaver` · L30 · 50 |

#### group-6 · Egyptian

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-24` | Dawn Sands | 0 | 0 · W · `dawnscarab` · L27 · 10<br>1 · W · `glintgrub` · L27 · 10<br>2 · MB · `dawnscarab` · L28 · 15<br>3 · W · `glintgrub` · L27 · 10<br>4 · SB · `halohover` · L29 · 20<br>5 · LB · `sunplate` · L30 · 30 |
| `area-25` | Solar Orchard | 1 | 0 · W · `dawnscarab` · L28 · 10<br>1 · W · `glintgrub` · L28 · 10<br>2 · MB · `sunplate` · L29 · 15<br>3 · W · `dawnscarab` · L28 · 10<br>4 · SB · `halohover` · L30 · 20<br>5 · LB · `sunplate` · L31 · 30 |
| `area-26` | Halo Oasis | 2 | 0 · W · `dawnscarab` · L29 · 10<br>1 · W · `halohover` · L29 · 10<br>2 · MB · `sunplate` · L30 · 15<br>3 · W · `halohover` · L29 · 10<br>4 · SB · `sunplate` · L31 · 20<br>5 · LB · `aurorabeetle` · L32 · 30 |
| `area-27` | Sunrise Vault | 3 | 0 · W · `dawnscarab` · L30 · 10<br>1 · W · `halohover` · L30 · 10<br>2 · MB · `sunplate` · L31 · 15<br>3 · W · `sunplate` · L30 · 10<br>4 · SB · `halohover` · L32 · 20<br>5 · LB · `aurorabeetle` · L33 · 30<br>6 · RB · `solcarapace` · L34 · 50 |

#### group-7 · Greek

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-28` | Foaming Shore | 0 | 0 · W · `tidecanter` · L31 · 10<br>1 · W · `foalfoam` · L31 · 10<br>2 · MB · `tidecanter` · L32 · 15<br>3 · W · `foalfoam` · L31 · 10<br>4 · SB · `mistgallop` · L33 · 20<br>5 · LB · `reefmane` · L34 · 30 |
| `area-29` | Coral Passage | 1 | 0 · W · `tidecanter` · L32 · 10<br>1 · W · `foalfoam` · L32 · 10<br>2 · MB · `reefmane` · L33 · 15<br>3 · W · `tidecanter` · L32 · 10<br>4 · SB · `mistgallop` · L34 · 20<br>5 · LB · `reefmane` · L35 · 30 |
| `area-30` | Abyssal Reef | 2 | 0 · W · `tidecanter` · L33 · 10<br>1 · W · `mistgallop` · L33 · 10<br>2 · MB · `reefmane` · L34 · 15<br>3 · W · `mistgallop` · L33 · 10<br>4 · SB · `reefmane` · L35 · 20<br>5 · LB · `crestcharger` · L36 · 30 |
| `area-31` | Crest Horizon | 3 | 0 · W · `tidecanter` · L34 · 10<br>1 · W · `mistgallop` · L34 · 10<br>2 · MB · `reefmane` · L35 · 15<br>3 · W · `reefmane` · L34 · 10<br>4 · SB · `mistgallop` · L36 · 20<br>5 · LB · `crestcharger` · L37 · 30<br>6 · RB · `abysscourser` · L38 · 50 |

#### group-8 · Chinese moon folklore

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-32` | Moonlit Meadow | 0 | 0 · W · `crescenthop` · L36 · 10<br>1 · W · `moonbun` · L36 · 10<br>2 · MB · `crescenthop` · L37 · 15<br>3 · W · `moonbun` · L36 · 10<br>4 · SB · `dreamskip` · L38 · 20<br>5 · LB · `jadebound` · L39 · 30 |
| `area-33` | Crescent Garden | 1 | 0 · W · `crescenthop` · L37 · 10<br>1 · W · `moonbun` · L37 · 10<br>2 · MB · `jadebound` · L38 · 15<br>3 · W · `crescenthop` · L37 · 10<br>4 · SB · `dreamskip` · L39 · 20<br>5 · LB · `jadebound` · L40 · 30 |
| `area-34` | Jade Moon Terrace | 2 | 0 · W · `crescenthop` · L38 · 10<br>1 · W · `dreamskip` · L38 · 10<br>2 · MB · `jadebound` · L39 · 15<br>3 · W · `dreamskip` · L38 · 10<br>4 · SB · `jadebound` · L40 · 20<br>5 · LB · `moondancer` · L41 · 30 |
| `area-35` | Lunar Sanctuary | 3 | 0 · W · `crescenthop` · L39 · 10<br>1 · W · `dreamskip` · L39 · 10<br>2 · MB · `jadebound` · L40 · 15<br>3 · W · `jadebound` · L39 · 10<br>4 · SB · `dreamskip` · L41 · 20<br>5 · LB · `moondancer` · L42 · 30<br>6 · RB · `lunarwarden` · L43 · 50 |

#### group-9 · Greek phoenix inspiration — interim roster, see §6 (own family art-blocked)

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-36` | Ash Nest | 0 | 0 · W · `dawnscarab` · L40 · 10<br>1 · W · `glintgrub` · L40 · 10<br>2 · MB · `dawnscarab` · L41 · 15<br>3 · W · `glintgrub` · L40 · 10<br>4 · SB · `halohover` · L42 · 20<br>5 · LB · `sunplate` · L43 · 30 |
| `area-37` | Cinder Ridge | 1 | 0 · W · `dawnscarab` · L41 · 10<br>1 · W · `glintgrub` · L41 · 10<br>2 · MB · `sunplate` · L42 · 15<br>3 · W · `dawnscarab` · L41 · 10<br>4 · SB · `halohover` · L43 · 20<br>5 · LB · `sunplate` · L44 · 30 |
| `area-38` | Dawn Roost | 2 | 0 · W · `dawnscarab` · L42 · 10<br>1 · W · `halohover` · L42 · 10<br>2 · MB · `sunplate` · L43 · 15<br>3 · W · `halohover` · L42 · 10<br>4 · SB · `sunplate` · L44 · 20<br>5 · LB · `aurorabeetle` · L45 · 30 |
| `area-39` | Rebirth Summit | 3 | 0 · W · `dawnscarab` · L43 · 10<br>1 · W · `halohover` · L43 · 10<br>2 · MB · `sunplate` · L44 · 15<br>3 · W · `sunplate` · L43 · 10<br>4 · SB · `halohover` · L45 · 20<br>5 · LB · `aurorabeetle` · L46 · 30<br>6 · RB · `solcarapace` · L47 · 50 |

#### group-10 · Akan storytelling inspiration — interim roster, see §6 (own family art-blocked)

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-40` | Story Grove | 0 | 0 · W · `runebeak` · L44 · 10<br>1 · W · `inkling` · L44 · 10<br>2 · MB · `runebeak` · L45 · 15<br>3 · W · `inkling` · L44 · 10<br>4 · SB · `ironquill` · L46 · 20<br>5 · LB · `glyphwing` · L47 · 30 |
| `area-41` | Riddle Crossing | 1 | 0 · W · `runebeak` · L45 · 10<br>1 · W · `inkling` · L45 · 10<br>2 · MB · `glyphwing` · L46 · 15<br>3 · W · `runebeak` · L45 · 10<br>4 · SB · `ironquill` · L47 · 20<br>5 · LB · `glyphwing` · L48 · 30 |
| `area-42` | Silk Canopy | 2 | 0 · W · `runebeak` · L46 · 10<br>1 · W · `ironquill` · L46 · 10<br>2 · MB · `glyphwing` · L47 · 15<br>3 · W · `ironquill` · L46 · 10<br>4 · SB · `glyphwing` · L48 · 20<br>5 · LB · `wargraven` · L49 · 30 |
| `area-43` | Taleweaver Haven | 3 | 0 · W · `runebeak` · L47 · 10<br>1 · W · `ironquill` · L47 · 10<br>2 · MB · `glyphwing` · L48 · 15<br>3 · W · `glyphwing` · L47 · 10<br>4 · SB · `ironquill` · L49 · 20<br>5 · LB · `wargraven` · L50 · 30<br>6 · RB · `oracrow` · L50 · 50 |

#### group-11 · Celtic-inspired fantasy — interim roster, see §6 (own family art-blocked)

| Area | Name | stage | Slots (slot · kind · creatureId · L · first-win XP) |
|---|---|---|---|
| `area-44` | Moss Trail | 0 | 0 · W · `plumeserp` · L48 · 10<br>1 · W · `plumeling` · L48 · 10<br>2 · MB · `plumeserp` · L49 · 15<br>3 · W · `plumeling` · L48 · 10<br>4 · SB · `jadecoil` · L50 · 20<br>5 · LB · `galeplume` · L50 · 30 |
| `area-45` | Briar Grove | 1 | 0 · W · `plumeserp` · L49 · 10<br>1 · W · `plumeling` · L49 · 10<br>2 · MB · `galeplume` · L50 · 15<br>3 · W · `plumeserp` · L49 · 10<br>4 · SB · `jadecoil` · L50 · 20<br>5 · LB · `galeplume` · L50 · 30 |
| `area-46` | Elder Woodland | 2 | 0 · W · `plumeserp` · L50 · 10<br>1 · W · `jadecoil` · L50 · 10<br>2 · MB · `galeplume` · L50 · 15<br>3 · W · `jadecoil` · L50 · 10<br>4 · SB · `galeplume` · L50 · 20<br>5 · LB · `verdantcrest` · L50 · 30 |
| `area-47` | Bloom Sanctuary | 3 | 0 · W · `plumeserp` · L50 · 10<br>1 · W · `jadecoil` · L50 · 10<br>2 · MB · `galeplume` · L50 · 15<br>3 · W · `galeplume` · L50 · 10<br>4 · SB · `jadecoil` · L50 · 20<br>5 · LB · `verdantcrest` · L50 · 30<br>6 · RB · `skyweaver` · L50 · 50 |

## 5. Unlock rules

Proposed rules. They are separate from travel so the existing visited-area behaviour stays compatible.

1. **Inside an area, slots unlock in order.** `<area>/slot-n` can be challenged after a first win of `<area>/slot-(n-1)`. Slot 0 is open whenever the area is battle-unlocked. `area-00/slot-0` is always open, as it is today.
2. **Area clear.** An area is *cleared* after the first win of its `location_boss`. Its id joins `BattleProgress.defeated` exactly as it does now, with no new state.
3. **Next area in a group** (stage 1–3): battle-unlocked when the previous area is cleared. This mirrors ZPet's `bossDefeated(area-1)`. ZPet's 5 000-step requirement is **not** proposed, because ZBattle has no step source of its own and walking credit belongs to ZPet (D-EXPEDITION). See Q-D.
4. **Next group** (stage-0 area of group g+1): battle-unlocked after the first win of group g's `region_boss`. ZPet keeps every stage-0 area open, but this curve is linear across all 48 areas, so starting at `group-7` would be over-tuned. An option is to keep ZPet's open stage-0 rule and show a "recommended level" instead (Q-C).
5. **Region completed.** A group counts as completed once its `region_boss` id is in `defeated`. `completedRegions = count of groups g where "area-(4g+3)/slot-6" ∈ defeated`, from 0 to 12. Phase C (Q10) reads this count for mystical-boss unlocks at 3/6/9/12. **No mystical boss is designed here.**
6. **Travel stays as it is.** `TravelRules.isUnlocked` (stage 0 open, or previous area visited) and `PlayerSettings.visitedAreas` are unchanged. Travelling to an area is never revoked. The campaign adds a separate *battle* gate. A visited but battle-locked area shows its slots with the requirement ("Defeat Crownstorm in Olympian Foothills"), in the way preview cards look today.
7. **Migration (legacy cleared progress).**
   - `defeated`, `wins` and an active battle on `area-00/slot-0` keep their meaning, so no save rewrite is needed.
   - `BattleProgressCodec` rejects an active battle whose encounter id is unknown. The new table must therefore contain `area-00/slot-0`, which it does.
   - A player who already beat `area-00/slot-0` sees slot 1 as unlocked and is not paid again.
   - `visitedAreas` is kept, so nobody loses travel access.
   - Nothing is retroactively re-awarded, and the 60 XP already earned is not recalculated.
   - Bump `rulesRevision` only when the formula for existing content changes. This proposal changes no existing value.
8. **Replays** keep paying 0 XP until D-REPLAY-REWARDS is decided.

## 6. Art-blocked families (groups 9–11)

The regions in groups 9–11 belong to families with **no artwork** (Rebirth bird, Web spider, Forest deer). This proposal does **not** use any of their 18 forms. These groups use an **interim family from the same `family % 3` class**, so the advantage maths (`Skills.advantage`) gives the same result it would give for the region's own family:

| Group | Areas | Region family (art-blocked) | Interim family in JSON | Why |
|---|---|---|---|---|
| `group-9` | `area-36`…`area-39` | 9 Rebirth bird | 6 Sun scarab (`dawnscarab`, `solcarapace`, …) | Same %3 class (0); dawn/rebirth theme |
| `group-10` | `area-40`…`area-43` | 10 Web spider | 1 Rune raven (`runebeak`, `oracrow`, …) | Same %3 class (1); lore/knowledge theme |
| `group-11` | `area-44`…`area-47` | 11 Forest deer | 5 Feathered serpent (`plumeserp`, `skyweaver`, …) | Same %3 class (2); jade-garden/verdant theme |

Once ChatGPT has created and Zeus97x has approved the art, the proposed swap replaces each interim creature with the same form index of the region's own family. For example, group 9 slot 0 `dawnscarab` (form 1) becomes `cinderwing`, and the region boss `solcarapace` (form 3) becomes `pyresovereign`. Levels, XP and stats do not change, so the swap is a pure data edit.

Alternative: keep groups 9–11 locked, show "coming soon" as ZPet does (`familyIllustrated`) and leave the campaign at 36 areas until the art exists. Q-B asks which.

## 7. Difficulty curve

### 7.1 Opponent formula (proposed)

Let `T = areaIndex` (0–47). This extends the current stage-based `forOpponent(stage, boss)` into one ramp across all 48 areas:

```
power = 5 + T/3       + kindPower
guard = 4 + T/4       + kindGuard
speed = 5 + T/5       + kindSpeed
maxHp = 30 + (7·T)/2  + kindHp                 (integer division throughout)
opponentLevel = min(50, 2 + T + T/16 + kindLevel)
```

| Kind | power | guard | speed | hp | level |
|---|---|---|---|---|---|
| wild | +0 | +0 | +0 | +0 | +0 |
| mini_boss | +1 | +0 | +0 | +8 | +1 |
| stage_boss | +1 | +1 | +0 | +12 | +2 |
| location_boss | +2 | +1 | +0 | +18 | +3 (same as the current boss +3) |
| region_boss | +3 | +2 | +0 | +25 | +4 |

Why this shape:
- At T = 0 a wild opponent is exactly today's 30 HP / 5 / 4 / 5 at level 2, so `area-00/slot-0` does not change.
- The divisors /3, /4, /5 are the player's growth per level in `CreatureStats.forCreature`. One area is worth about one player level, so the gap stays roughly constant.
- ZPet's own ramp (+2 power and +10 HP per stage) assumes ZPet's step-funded training. ZBattle has none, and ZPet's ramp would be **unwinnable** for an untrained Baby starter by stage 3. The first draft of this proposal reused it with group offsets, and a Baby starter then won 0% of location bosses.
- The current `boss` flag (+4/+3/+0/+30) is close to the `location_boss` bonus. The bonuses here are smaller because a boss now comes after five earlier fights in the same area.

### 7.2 First-win XP per kind

| Kind | Draft (solo-tuned) | **In the JSON now** (ECONOMY Option B) | Note |
|---|---|---|---|
| `area-00/slot-0` (legacy) | 60 | **60** | Unchanged. Must stay. |
| wild | 10 | 30 | |
| mini_boss | 15 | 60 | Plus 1 Rare ticket on first clear (Q9) |
| stage_boss | 20 | 100 | Plus 1 Epic ticket on first clear (Q9) |
| location_boss | 30 | 150 | |
| region_boss | 50 | 250 | Plus 1 Legendary ticket on first clear (Q9) |

- **Draft values:** 95 XP per area (plus 50 at a group end), 5 210 XP over the campaign.
- **Option B:** 22 230 XP over the campaign. Split across a party of 3 (D-PARTY-XP proposal), that is about 1.5 levels per area per member, and members reach the level cap around `group-7`.

**Why not ZPet's 60/200 everywhere?** With 300 encounters the ZPet values add up to about 33 000 XP. Players would hit the level-50 cap around `area-07`, and the remaining 40 areas would have no player growth at all. The current 60/200 stays only on the shipped encounter. If Zeus97x wants ZPet-scale rewards everywhere, the level curve (`XP_PER_LEVEL = 100`, cap 50) has to change instead (Q-F).

### 7.3 Expected player level by group (first wins only)

| Group | Areas | XP available | Exp. player L at group start | at group end |
|---|---|---|---|---|
| group-0 | area-00..area-03 | 480 | 1 | 5 |
| group-1 | area-04..area-07 | 430 | 5 | 10 |
| group-2 | area-08..area-11 | 430 | 10 | 14 |
| group-3 | area-12..area-15 | 430 | 14 | 18 |
| group-4 | area-16..area-19 | 430 | 18 | 23 |
| group-5 | area-20..area-23 | 430 | 23 | 27 |
| group-6 | area-24..area-27 | 430 | 27 | 31 |
| group-7 | area-28..area-31 | 430 | 31 | 35 |
| group-8 | area-32..area-35 | 430 | 35 | 40 |
| group-9 | area-36..area-39 | 430 | 40 | 44 |
| group-10 | area-40..area-43 | 430 | 44 | 48 |
| group-11 | area-44..area-47 | 430 | 48 | 50 |

### 7.4 Sample encounters

This table is an estimate. It uses attack-only turns: turns for the player to win, and turns for the opponent to knock the player out. It ignores skills, Burn and Weaken, and the heavy strike every third turn. The player is at the expected level before that encounter.

| Area | Slot / kind | Creature | Opp L | Opp HP/Pow/Grd/Spd | Exp. player L (before) | Baby: turns to win / to lose | A-final: win / lose |
|---|---|---|---|---|---|---|---|
| area-00 | 0 wild | voltmaw | 2 | 30/5/4/5 | 1 | 5 / 8 | 2 / 15 |
| area-00 | 2 mini_boss | voltmaw | 3 | 38/6/4/5 | 1 | 6 / 7 | 3 / 12 |
| area-00 | 4 stage_boss | galestride | 4 | 42/6/5/5 | 1 | 6 / 7 | 3 / 12 |
| area-00 | 5 location_boss | crownstorm | 5 | 48/7/5/5 | 2 | 7 / 7 | 4 / 11 |
| area-01 | 0 wild | voltmaw | 3 | 33/5/4/5 | 2 | 5 / 8 | 3 / 16 |
| area-01 | 5 location_boss | crownstorm | 6 | 51/7/5/5 | 3 | 8 / 7 | 4 / 11 |
| area-03 | 0 wild | voltmaw | 5 | 40/6/4/5 | 4 | 5 / 8 | 3 / 14 |
| area-03 | 5 location_boss | tempestral | 8 | 58/8/5/5 | 5 | 8 / 7 | 4 / 11 |
| area-03 | 6 region_boss | astrapex | 9 | 65/9/6/5 | 5 | 10 / 6 | 5 / 9 |
| area-04 | 0 wild | runebeak | 6 | 44/6/5/5 | 5 | 6 / 9 | 3 / 15 |
| area-04 | 5 location_boss | glyphwing | 9 | 62/8/6/5 | 6 | 9 / 7 | 5 / 11 |
| area-07 | 0 wild | runebeak | 9 | 54/7/5/6 | 8 | 6 / 9 | 4 / 14 |
| area-07 | 5 location_boss | wargraven | 12 | 72/9/6/6 | 9 | 9 / 8 | 5 / 12 |
| area-07 | 6 region_boss | oracrow | 13 | 79/10/7/6 | 9 | 10 / 8 | 5 / 11 |
| area-11 | 0 wild | kilnback | 13 | 68/8/6/7 | 12 | 8 / 10 | 4 / 16 |
| area-11 | 5 location_boss | pyrelisk | 16 | 86/10/7/7 | 13 | 9 / 9 | 5 / 13 |
| area-11 | 6 region_boss | vulcarion | 17 | 93/11/8/7 | 13 | 11 / 8 | 6 / 11 |
| area-15 | 0 wild | sandward | 17 | 82/10/7/8 | 17 | 8 / 11 | 5 / 16 |
| area-15 | 5 location_boss | duskjudge | 20 | 100/12/8/8 | 17 | 10 / 9 | 6 / 13 |
| area-15 | 6 region_boss | tombwarden | 21 | 107/13/9/8 | 18 | 11 / 9 | 6 / 12 |
| area-19 | 0 wild | emberveil | 22 | 96/11/8/8 | 21 | 9 / 11 | 6 / 16 |
| area-19 | 5 location_boss | dawnflare | 25 | 114/13/9/8 | 22 | 10 / 10 | 6 / 13 |
| area-19 | 6 region_boss | veilnine | 26 | 121/14/10/8 | 22 | 11 / 9 | 7 / 12 |
| area-23 | 0 wild | plumeserp | 26 | 110/12/9/9 | 25 | 9 / 12 | 6 / 17 |
| area-23 | 5 location_boss | verdantcrest | 29 | 128/14/10/9 | 26 | 11 / 11 | 7 / 14 |
| area-23 | 6 region_boss | skyweaver | 30 | 135/15/11/9 | 26 | 12 / 10 | 7 / 13 |
| area-27 | 0 wild | dawnscarab | 30 | 124/14/10/10 | 30 | 10 / 12 | 6 / 16 |
| area-27 | 5 location_boss | aurorabeetle | 33 | 142/16/11/10 | 30 | 11 / 10 | 7 / 13 |
| area-27 | 6 region_boss | solcarapace | 34 | 149/17/12/10 | 31 | 12 / 10 | 8 / 12 |
| area-31 | 0 wild | tidecanter | 34 | 138/15/11/11 | 34 | 10 / 13 | 6 / 17 |
| area-31 | 5 location_boss | crestcharger | 37 | 156/17/12/11 | 35 | 12 / 11 | 8 / 14 |
| area-31 | 6 region_boss | abysscourser | 38 | 163/18/13/11 | 35 | 12 / 11 | 8 / 13 |
| area-35 | 0 wild | crescenthop | 39 | 152/16/12/12 | 38 | 11 / 13 | 7 / 17 |
| area-35 | 5 location_boss | moondancer | 42 | 170/18/13/12 | 39 | 12 / 11 | 8 / 14 |
| area-35 | 6 region_boss | lunarwarden | 43 | 177/19/14/12 | 39 | 13 / 11 | 9 / 13 |
| area-39 | 0 wild | dawnscarab | 43 | 166/18/13/12 | 43 | 10 / 13 | 7 / 17 |
| area-39 | 5 location_boss | aurorabeetle | 46 | 184/20/14/12 | 43 | 12 / 12 | 8 / 14 |
| area-39 | 6 region_boss | solcarapace | 47 | 191/21/15/12 | 44 | 12 / 11 | 8 / 14 |
| area-43 | 0 wild | runebeak | 47 | 180/19/14/13 | 47 | 11 / 13 | 8 / 16 |
| area-43 | 5 location_boss | wargraven | 50 | 198/21/15/13 | 48 | 12 / 12 | 8 / 14 |
| area-43 | 6 region_boss | oracrow | 50 | 205/22/16/13 | 48 | 13 / 11 | 9 / 14 |
| area-47 | 0 wild | plumeserp | 50 | 194/20/15/14 | 50 | 11 / 14 | 8 / 17 |
| area-47 | 2 mini_boss | galeplume | 50 | 202/21/15/14 | 50 | 12 / 13 | 8 / 16 |
| area-47 | 4 stage_boss | jadecoil | 50 | 206/21/16/14 | 50 | 13 / 13 | 9 / 16 |
| area-47 | 5 location_boss | verdantcrest | 50 | 212/22/16/14 | 50 | 13 / 12 | 9 / 15 |
| area-47 | 6 region_boss | skyweaver | 50 | 219/23/17/14 | 50 | 13 / 12 | 9 / 14 |

### 7.5 Simulated win rates (current engine, draft solo XP)

The drafting simulation mirrors `BattleEngine.act` and the B1 `AutoFight` policy (skill when ready, otherwise attack). It includes:
- Burn and Weaken
- the +5 heavy strike every third turn
- speed order
- the 50-turn limit.

Each cell is the share of fights won by a single companion at the expected level for that point. "Baby" uses the three approved starters (`sparklit`, `inkling`, `cindlet`, families 0–2). The other rows use the same families at later forms. The Baby rows are the realistic baseline today, because evolution (B3) is not approved.

| Group | Player form | wild | mini_boss | stage_boss | location_boss | region_boss |
|---|---|---|---|---|---|---|
| group-0 | Baby | 100% | 100% | 100% | 92% | 0% |
| group-0 | Young | 100% | 100% | 100% | 100% | 100% |
| group-0 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-0 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-1 | Baby | 100% | 100% | 100% | 50% | 0% |
| group-1 | Young | 100% | 100% | 100% | 100% | 100% |
| group-1 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-1 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-2 | Baby | 100% | 100% | 100% | 83% | 0% |
| group-2 | Young | 100% | 100% | 100% | 100% | 100% |
| group-2 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-2 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-3 | Baby | 100% | 100% | 100% | 58% | 0% |
| group-3 | Young | 100% | 100% | 100% | 100% | 100% |
| group-3 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-3 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-4 | Baby | 100% | 100% | 100% | 100% | 0% |
| group-4 | Young | 100% | 100% | 100% | 100% | 100% |
| group-4 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-4 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-5 | Baby | 100% | 100% | 75% | 42% | 100% |
| group-5 | Young | 100% | 100% | 100% | 100% | 100% |
| group-5 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-5 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-6 | Baby | 100% | 100% | 100% | 67% | 67% |
| group-6 | Young | 100% | 100% | 100% | 100% | 100% |
| group-6 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-6 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-7 | Baby | 100% | 100% | 100% | 92% | 33% |
| group-7 | Young | 100% | 100% | 100% | 100% | 100% |
| group-7 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-7 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-8 | Baby | 100% | 100% | 100% | 83% | 0% |
| group-8 | Young | 100% | 100% | 100% | 100% | 100% |
| group-8 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-8 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-9 | Baby | 100% | 100% | 100% | 83% | 100% |
| group-9 | Young | 100% | 100% | 100% | 100% | 100% |
| group-9 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-9 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-10 | Baby | 100% | 100% | 100% | 100% | 33% |
| group-10 | Young | 100% | 100% | 100% | 100% | 100% |
| group-10 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-10 | B-final | 100% | 100% | 100% | 100% | 100% |
| group-11 | Baby | 100% | 100% | 100% | 58% | 0% |
| group-11 | Young | 100% | 100% | 100% | 100% | 100% |
| group-11 | A-final | 100% | 100% | 100% | 100% | 100% |
| group-11 | B-final | 100% | 100% | 100% | 100% | 100% |

What the simulation shows:
- A Baby starter clears every wild, mini boss and stage boss tier. It fails some location bosses (42–100% depending on the group, against 0% under the ZPet ramp). Region bosses are a wall for a lone Baby (0–100%, usually low).
- Any evolved form clears everything. Once D-EVOLUTION and D-PARTY are decided, the boss bonuses should be re-tuned upward, because the curve is currently easy for evolved companions. That re-tune changes only the `STAT_BONUS` table in `tools/campaign_proposal.py`.
- `area-00/slot-0` is won by all three starters at level 1, as it is today.

### 7.6 Reconciliation with the economy proposal (Claude)

Claude re-ran the simulation with the values now in the JSON: Option B XP, and a party of 3 splitting it, so each member's level is `1 + (xp/3)/100`. Command: `python3 tools/campaign_proposal.py --simulate --party 3`.

| Group | Baby: wild / mini / stage / location / region |
|---|---|
| group-0 | 100 / 100 / 100 / 92 / 67 % |
| group-1 … group-10 | 100 / 100 / 100 / 100 / 100 % |
| group-11 | 100 / 100 / 100 / 58 / 0 % (both sides at the level-50 cap) |

**Finding:** the two drafts don't fit together.
- With Option B and a trio, members out-level the opponent curve from `group-1` onwards, so almost nothing is hard.
- With the draft XP and a trio, members would sit about 30 levels below the curve by the end.

The pair can be made coherent in one of three ways, and this is Zeus97x's call (Q-F):
1. **Keep Option B and make the opponent ramp steeper.** For example `T = areaIndex × 1.5` in `opponent_stats`; it still needs approval.
2. **Keep the solo-tuned draft XP and give each participant the full amount** (`PartyXpRule.EachFull`, an option under D-PARTY-XP).
3. **Use XP between the two** so that a trio gains about one level per area: roughly 300 XP per area in total.

The simulation approximates a party as its strongest single member. A real party (B2) has three HP pools, so it is even easier than the table shows.

## 8. Open questions for Zeus97x (D-CAMPAIGN)

- **Q-A. Kinds.** Is "stage boss" the boss of the numbered track inside an area (this proposal) or of `Area.stage`? Keep five kinds, or merge stage and location boss?
- **Q-B. Groups 9–11.** Use the same-%3 interim families now and swap them when art lands (this proposal), or lock the three groups as "coming soon" (36-area campaign)?
- **Q-C. Group gating.** Should the next group open only after the previous region boss (this proposal), or should every stage-0 area stay open as in ZPet, with a recommended level shown?
- **Q-D. Steps.** Should area unlocks also need ZPet route steps (ZPet: 5 000), or only boss victories (this proposal)? Steps would need the ZPet bridge and an answer to D-OFFLINE-TRUST.
- **Q-E. Size.** Is 6–7 encounters per area (300 total) right, or should it be lean (5 per area, 252) or rich (8 per area)?
- **Q-F. XP and curve (updated after reconciliation).** Choose one of the three ways in section 7.6 to make the party-of-3 XP and the opponent curve coherent. ZPet's 60/200 everywhere is still possible, but it needs a different `XP_PER_LEVEL` or level cap.
- **Q-G. Difficulty target.** Should a lone Baby starter be expected to beat region bosses, or are region bosses meant to require evolution, a party (B2) or replays? This decides whether the region-boss bonus stays or drops.
- **Q-H. Form choice.** Is reusing one family's six forms per area acceptable (the ZPet rule), or should some wild slots draw from neighbouring families (still 0–8 only)?
- **Q-I. ZPet "easier boss" rule.** ZPet offers an easier boss after 3 losses (`WorldState.begin(easy)`). Should ZBattle adopt it for location and region bosses?
- **Q-J. Phase C hook.** Confirm that "completed region" means the region boss's first win is recorded in `defeated`. Q10 counts it for 3/6/9/12; mystical bosses are not designed here.
