# ZCubes catching proposal (Q19)

> **PROPOSAL — inactive and configurable; needs Zeus97x approval (D-ZCUBES)**
> Co-owner: ChatGPT (cube art and names). This document builds on `ai/ZCUBES_PLAN.md` (2026-10-09) and does not replace it. Every difference from that plan is listed in §4. Every value is proposed and configurable. Prices depend on D-ECONOMY.

Written 2026-10-10 (America/Toronto) by Claude. Sources were read without changes: ZBattle main `9d09592`, ZPet main `1adcedb`.

---

## 1. Current state (evidence)

### ZBattle
| Item | State | Evidence |
|---|---|---|
| Catching, inventory, purchases | **Not implemented** | `ai/ZCUBES_PLAN.md` ("planned, not implemented"); `ai/CROSS_APP_ROADMAP.md:9` |
| ZCube tiers and art keys | Basic/Great/Ultra/Mythic(-or-Legendary): `item/zcube-basic|great|ultra|mythic` | `ai/ZCUBES_PLAN.md` |
| Planned rules | Catch from a **won or weakened wild** encounter, no bosses. Tier bonus **+0/+10/+20/+35** points. Cap **95%**, Mythic cube **100%**. Consumed on use. One record per battle id. Result is an `OwnedCreature` in `BattleProgress`; nothing is written to ZPet. | `ai/ZCUBES_PLAN.md` "Proposed rules" |
| Wild opponents | Young form (`forms[1]`) of the area family; bosses use `forms[2]`. One playable encounter. | `core/.../battle/Encounters.kt` |
| Battle actions | Attack, Skill, Retreat; no items | `core/.../battle/BattleEngine.kt` |
| Coins | Fixed at 0; no shop | `DECISIONS.md` D-ECONOMY |

### ZPet (reference only; ZPet capture stays as is)
| Item | Value | Evidence |
|---|---|---|
| Catch chance by rarity | **80 / 65 / 50 / 35 %** (Common/Heroic/Mythic/Celestial) | `zpet/app/src/main/java/com/zeus97x/zpet/SpeciesCatalog.java` `catchChance` |
| Spawn rarity by area stage | stage 0: `roll<85` Common, else Heroic. Stage 1: `<65` C, `<92` H, else Mythic (65/27/8). Stages 2–3: `<50` C, `<82` H, `<97` M, else Celestial (50/32/15/3). | `SpeciesCatalog.java` `roll` |
| Pity | `success = pity>=2 \|\| catchRoll < catchChance`. Two failures guarantee the 3rd attempt. A success resets pity to 0. Pity is persisted (0..2). | `zpet/.../WorldState.java:103-104,151` |
| Attempt cost | One attempt per **2,500 accepted steps** (`attempts() = steps/2500 − captureSpent`) | `WorldState.java:90,97` |
| No reroll | `reveal()` persists the candidate **and** `catchRoll` before display | `WorldState.java:94-99`; `BLUEPRINT.md:233` |
| Skip | Consumes an attempt; does **not** count as a failure and does not reset pity | `WorldState.java` `skip()`; `BLUEPRINT.md:233` |
| Result | A distinct **level-1 first-form** individual; duplicates allowed; never discarded | `BLUEPRINT.md:214,233` |
| Status of values | "Provisional … beta tuning values, not finalized promises" | `BLUEPRINT.md:235` |

### Decisions already made
- Q8: Common/Heroic/Mythic/Celestial (ZPet) = Common/Rare/Epic/Legendary (ZBattle display). Species ids stay `family:rarity`.
- Q4: party of 3, one active. Switching consumes the turn and the enemy acts normally. Replacing a fainted creature is free.

## 2. Goals
1. Catching feels like ZPet: the same base odds, a pity guarantee and no reroll by reloading.
2. Cubes are the cost (ZBattle economy) instead of steps, so ZBattle does not double-spend ZPet's step economy.
3. The process is deterministic and recoverable: saved battles resume with the same roll, and a catch is settled once per battle.
4. No new creature art. Caught species variants reuse family art, as in ZPet.
5. ZPet is never written to. Caught creatures are ZBattle-native companions.

## 3. Recommended design (all values proposed and configurable)

### 3.1 Where catching is allowed
| Encounter kind | Catchable? |
|---|---|
| `wild` | **Yes** |
| `campaign` (non-boss) | No, at first (configurable) |
| `mini-boss`, `stage-boss`, `location-boss`, `region-boss`, `mystical` | **No**, as in the plan |
| `practice` / zero-reward rematch | No |
| Auto-fight | Never throws a cube (D-AUTO-FIGHT: no item use) |

### 3.2 Wild rarity
Each wild encounter rolls its rarity at battle start with ZPet's `roll` table. The roll is stored in `BattleState`, so it cannot be rerolled.

| Area stage | Common | Rare (Heroic) | Epic (Mythic) | Legendary (Celestial) |
|---|---|---|---|---|
| 0 | 85 | 15 | — | — |
| 1 | 65 | 27 | 8 | — |
| 2–3 | 50 | 32 | 15 | 3 |

The rarity changes only the species name and label (for example "Laurelclaw · Rare", using `SpeciesCatalog.NAMES`). Art stays the family form, and opponent stats are unchanged. Rarity "does not add automatic stat superiority" (`BLUEPRINT.md:216`).

### 3.3 Throw windows (both consume a cube)
| Window | Condition | Turn cost | Max throws |
|---|---|---|---|
| **In-battle "Throw ZCube"** | Enemy HP ≤ **50%** ("weakened") | **Uses the player's turn.** The enemy acts normally, the same rule as switching (Q4). | 3 per battle (shared with post-victory throws) |
| **Post-victory** | Battle won against a catchable wild | none (battle is over) | Up to the remaining throws, max **2** |
If the battle ends in defeat or retreat, there is no catch window.

### 3.4 Catch chance
`chance = min(cap(cube), base(rarity) + cubeBonus + hpBonus)`; then `pity` overrides it to 100.

| Rarity | Base (= ZPet) |
|---|---|
| Common | 80 |
| Rare (Heroic) | 65 |
| Epic (Mythic) | 50 |
| Legendary (Celestial) | 35 |

| Cube | Bonus (= plan) | Cap (= plan) |
|---|---|---|
| Basic | +0 | 95 |
| Great | +10 | 95 |
| Ultra | +20 | 95 |
| Mythic (tier-4 name TBD) | +35 | 100 |

| Enemy state | hpBonus (new) |
|---|---|
| HP 26–50% | +0 |
| HP 1–25% | +5 |
| Post-victory (fainted) | +10 |

Worst case: Legendary, Basic cube, HP 50% gives 35%. Best case: Legendary, Mythic cube, post-victory gives 80%. A Common with a Great cube after victory comes to 100%, which is capped at 95%.

### 3.5 Pity (account-wide, ZBattle-only, separate from ZPet's counter)
| Rarity | Failed throws before the next throw is guaranteed |
|---|---|
| Common, Rare | 2 (= ZPet) |
| Epic | 3 |
| Legendary | 4 |
- Pity counts failures for each rarity tier and persists across battles and app restarts.
- A success at a tier resets that tier's counter. The guarantee never changes the candidate's rarity (BLUEPRINT rule).
- Declining a post-victory window is like ZPet's skip: no cube is used and pity is unchanged.
- *Why tiered:* ZPet's attempts cost 2,500 steps each. With 3 throws per battle, a flat 2-failure pity would guarantee every Legendary for 3 Basic cubes.

### 3.6 Determinism and recovery
- `throwRoll[i] = H(battleId, accountSeed, i) mod 100`, with `i` = 0..2. It is computed when the throw is chosen and **persisted in BattleState before the result is shown** (mirrors ZPet `catchRoll`).
- The cube is consumed, the roll applied, pity updated and any creature created **in the same save transaction** as the settlement. The idempotency key is `(battleId, throwIndex)`.
- In-battle success: the battle ends as `Outcome.Victory` with `caught = true` in ZBattle-local settlement. Status timers (Burn) do not tick after a successful catch.

### 3.7 The caught creature
| Field | Proposed value |
|---|---|
| Identity | New `companionId` UUID, `originApp = zbattle`, `legacyLocalId = zb-uid-N` |
| `speciesId` | `family:rarity` as rolled. *This extends D-NATIVE-SPECIES (`family:0`), which then applies to starters only.* |
| Form | The form fought: **Young (formIndex 1)**. See open question 4; ZPet would give form 0. |
| Battle level | 1 (ZBattle XP; never touches ZPet XP) |
| Destination | Collection. Offered to the party only if a slot (of 3) is empty. Never discarded; duplicates allowed. |
| Families 9–11 | Not catchable: they have no encounters or art |

### 3.8 Acquiring cubes (depends on D-ECONOMY; placeholders)
| Source | Basic | Great | Ultra | Mythic |
|---|---|---|---|---|
| New-save grant (once) | 5 | — | — | — |
| First victory in each area | 1 | — | — | — |
| First stage/location-boss victory | — | 1 | — | — |
| First region-boss victory | — | — | 1 | — |
| Mystical boss | — | — | — | via exclusive pool only (D-EXCLUSIVE-POOL) |
| ZPet `RewardEarned` (`kind:"item"`, `itemKey:"zcube-basic"` etc.) | ZPet earning rules | | | |
| Shop price (coins) | 25 | 75 | 200 | not sold |
Prices are taken from the sibling ECONOMY-PROPOSAL.md §7.4, so the two documents agree. That document also sets the rule that there are no real-money purchases. Repeat-battle drops wait for D-REPLAY-REWARDS. Daily shop purchase limit: none for Basic, 5 for Great and 2 for Ultra (placeholders).

## 4. Differences from `ai/ZCUBES_PLAN.md`
| Plan says | This proposal | Why |
|---|---|---|
| "won or weakened wild encounter" | Kept, made explicit: weakened = HP ≤ 50% in battle; won = a post-victory window | Unambiguous rule |
| Tier bonus +0/+10/+20/+35, cap 95/100 | **Unchanged** | — |
| No HP factor | Adds +0/+5/+10 hpBonus | Rewards weakening without changing caps |
| Pity not mentioned | Tiered pity 2/2/3/4 | ZPet parity, adjusted for cube cost |
| "Every attempt recorded once per battle id" | Key is `(battleId, throwIndex)`, up to 3 throws | Several throws per encounter |
| Open: does a failure end the encounter? | No in battle (the turn is lost). Post-victory: the creature flees after the window. | Answers plan question 3 |
| Open: use `family:rarity`? | Yes, with ZPet spawn weights | Answers plan question 4; consistent with D-RARITY / Q8 |
| Tier-4 name open | Still open (ChatGPT, with the art) | — |

## 5. How it plugs in
| Concern | Owner | Where |
|---|---|---|
| Battle action `ThrowCube(tier)` | ZBattle | `BattleAction` in `BattleEngine.kt`, plus `BattleState` fields `wildRarity`, `throws[]`, `throwRolls[]` |
| Inventory and pity | ZBattle | `BattleProgress` (new versioned fields: `cubes[4]`, `pity[4]`) |
| Catalogue | Shared | `speciesId` pattern `^([0-9]\|1[01]):[0-3]$` (`common.schema.json`); names from ZPet `SpeciesCatalog.NAMES` |
| Cross-app | ZBattle → ZPet | Caught companions may appear as `CompanionSnapshot` with `originApp: zbattle`. No ZPet write. |
| Cube rewards from ZPet | ZPet earns, ZBattle redeems | `RewardEarned.reward.kind="item"`, `itemKey` `zcube-basic\|great\|ultra\|mythic` (pattern-valid) |
| Expedition | — | A catch ends the battle as a victory. Whether it qualifies for expedition credit is open question 6. |
| Art | ChatGPT | `item/zcube-*` keys from the plan. Until art is approved, a text button is shown. |

## 6. Migration and compatibility
- `BattleProgress` save version +1 adds `cubes` and `pity`, both defaulting to 0. Older saves load with an empty inventory.
- Saved battles from before this change have no `wildRarity`. On resume they are treated as Common and are not catchable, so they finish normally.
- `rulesRevision` is bumped (`zbattle-rules-N+1`) because a new action changes the turn flow.
- With the feature flag `catching.enabled=false` (default), there is no Throw button and no drops. Battles are byte-identical to today.
- ZPet capture (steps, pity 0..2) is unaffected. The two pity counters are separate.

## 7. Test plan
| # | Case | Expected |
|---|---|---|
| T1 | Chance table for all rarity × cube × hp combinations | Matches §3.4, caps respected |
| T2 | Kill the app after the throw is chosen and before the result is shown, then resume | Same roll, same result, one cube consumed |
| T3 | Retry the settlement with the same `(battleId, throwIndex)` | No duplicate creature or cube loss |
| T4 | Pity: 2 Common failures, then a throw | Guaranteed; the counter resets |
| T5 | Pity per tier is independent | A Legendary failure does not advance Common pity |
| T6 | Throw when enemy HP > 50% | Action disabled |
| T7 | Throw consumes the turn | The enemy acts, including the 3rd-turn heavy strike |
| T8 | Burn tick on a successful catch turn | No tick |
| T9 | Boss, mystical, practice, auto-fight | No throw available |
| T10 | Spawn table distribution (10⁵ seeded rolls) | Within ±1% of §3.2 |
| T11 | Flag off | Golden battle log is identical to current main |
| T12 | Caught creature: `speciesId`, form, level 1, collection, no ZPet write | Pass |

## 8. Open questions
1. Tier-4 name: Mythic or Legendary ZCube (ChatGPT, with the art)?
2. In-battle throws allowed, or post-victory only (simpler)?
3. Pity thresholds 2/2/3/4, or a flat 2 like ZPet?
4. Caught form: Young (as fought) or Baby (ZPet parity)? This depends on D-EVOLUTION, because ZBattle has no evolution.
5. Prices and drops (§3.8) under D-ECONOMY.
6. Does a successful catch count as a qualifying victory for expedition credit (proposal: yes, since the enemy was defeated or caught)?
7. Should D-NATIVE-SPECIES be amended so caught natives keep their rolled rarity (proposal: yes)?
