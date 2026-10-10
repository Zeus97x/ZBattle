# Element system proposal (Q18)

> **PROPOSAL — inactive and configurable; needs Zeus97x approval (D-ELEMENT)**
> Co-owner: ChatGPT (design and art direction). Until approval, the existing **family % 3 advantage stays fully operational and unchanged**, and `elementId` stays `null` in every contract record. Every value in this document is proposed and configurable.

Written 2026-10-10 (America/Toronto) by Claude. Sources were read without changes: ZBattle main `9d09592`, ZPet main `1adcedb`.

---

## 1. Current state (evidence)

| Area | What exists | Evidence |
|---|---|---|
| Element data | **Neither app has any.** ZPet has no element, type or weather field. ZBattle has none either. | `grep -rniE "element\|weather"` over the ZPet sources finds only SQL `jsonb_array_elements`. ZBattle `*.kt` has no match. `DECISIONS.md` D-ELEMENT. |
| ZBattle advantage | `Skills.advantage`: **+3** if `(atk%3+1)%3 == def%3`, **−2** in the reverse case, otherwise 0. It applies to **Skill only**: `max(3, power + 9 − guard/2 + advantage)`. | `core/src/main/kotlin/com/zeus97x/zbattle/core/battle/Stats.kt` (`Skills.advantage`), `BattleEngine.kt` (`act`) |
| ZPet advantage (same rule) | Adventure Skill gets +3/−2 by the same formula. | `zpet/app/src/main/java/com/zeus97x/zpet/AdventureState.java:87-88` |
| ZPet ranked (same triangle) | Skill-stance damage is 22 when strong, otherwise 18. There is no weakness penalty. | `zpet/.../RankedRules.java:10-11` |
| Skill effect by class | `family % 3 == 1` gives Weaken, others give Burn. | `Stats.kt` `Skills.forFamily`; `AdventureState.java` `move` (`family%3==1`) |
| Families | 12 families (0–11). Only 0–8 have art; 9–11 must never get substitute art. | `core/src/main/kotlin/com/zeus97x/zbattle/core/Creatures.kt` (`FAMILIES_WITH_ARTWORK = 9`) |
| Species variants | Four rarities per family reuse the family art. | `zpet/.../SpeciesCatalog.java` (header comment), `BLUEPRINT.md:235` |
| Contract | `elementId`: `string\|null`, pattern `^[a-z][a-z0-9-]{1,31}$`, "PENDING D-ELEMENT … null until approved". It is required, as a nullable field, in `CompanionSnapshot` and `LineageSnapshot`. All fixtures use `null`. | `schemas/common.schema.json` `$defs.elementId`; `companion-snapshot.schema.json`; `lineage-snapshot.schema.json`; `fixtures/records/*` |
| Downstream users | Phase F weather/element effects; G2 "elemental flare" cosmetics ("actual element metadata drives selection"). | `CLAUDE-009` Phase A outcome; `CLAUDE-010` G2 |

Current triangle classes, by `family % 3`:

| Class | Families |
|---|---|
| 0 | 0 Storm wolf, 3 Guardian jackal, 6 Sun scarab, 9 Rebirth bird |
| 1 | 1 Rune raven, 4 Spirit fox, 7 Sea horse, 10 Web spider |
| 2 | 2 Fire dragon, 5 Feathered serpent, 8 Moon rabbit, 11 Forest deer |

Class 0 beats 1, 1 beats 2, 2 beats 0.

## 2. Goals
1. Give G2 cosmetics and F2 weather a real element label without rebalancing combat.
2. **Require no new art.** Elements are text plus existing theme colour tokens. Approved flare assets are an optional later layer with a text fallback.
3. Keep both apps' combat (Adventure and Ranked) consistent. Any change to the matchup must be mirrored in ZPet, so do not change it in the first version.
4. Derive elements from the family, so every form and rarity variant inherits them automatically and no per-creature data or art is needed.

## 3. Options considered

| Option | Description | Art needed | Balance risk | Verdict |
|---|---|---|---|---|
| A. Status quo | Keep the unnamed `family % 3` rule; no elements | none | none | Blocks G2 and weather |
| **B. Named affinity + family element (recommended)** | Name the 3 existing classes ("affinity") and add one flavor element per *family* (6 elements, 2 families each). The matchup is still computed from the affinity, i.e. identical to today. | **none** (text and colour token) | **none** | Recommended |
| C. Full element matrix | A 6×6 type chart replaces `family % 3` | none for data, but players expect icons | High; ZPet Adventure and Ranked would diverge | Not recommended now |
| D. Per-form or per-rarity elements | Branch A/B or rarity variants get different elements | **Yes**, they would need visually distinct art | High | Rejected (art rule) |

## 4. Recommended design (Option B), all values proposed and configurable

### 4.1 Affinity (names for the existing triangle; behaviour unchanged)
| `family % 3` | Proposed affinity id | Display | Beats | Skill effect today |
|---|---|---|---|---|
| 0 | `might` | Might | `mystic` | Burn |
| 1 | `mystic` | Mystic | `wild` | Weaken |
| 2 | `wild` | Wild | `might` | Burn |

The values stay exactly as they are: **+3 strong / −2 weak / 0 neutral, on Skill only**. Affinity is *derived* and never stored. It is not a contract field.

### 4.2 Elements (one per family; 6 elements; each spans two families)
| Proposed `elementId` | Display | Families (index · affinity) | Lore basis (`Creatures.kt` text) |
|---|---|---|---|
| `storm` | Storm | 0 Storm wolf · might; 5 Feathered serpent · wild | thunder fur; "weave sky currents" |
| `fire` | Fire | 2 Fire dragon · wild; 9 Rebirth bird · might | ember pearl; "ash gives way to flame" |
| `light` | Light | 6 Sun scarab · might; 4 Spirit fox · mystic | solar armour; lantern flames, Dawnflare |
| `earth` | Earth | 3 Guardian jackal · might; 11 Forest deer · wild | desert crossings; living antlers, woodland |
| `water` | Water | 7 Sea horse · mystic; 8 Moon rabbit · wild | deep water; moon and tides |
| `arcane` | Arcane | 1 Rune raven · mystic; 10 Web spider · mystic | lost runes; story webs |

Design notes:
- Element and affinity are deliberately independent: five of the six elements mix affinities (only `arcane` is single-affinity). Elements therefore add identity without duplicating the triangle.
- Families 9–11 receive element *metadata* only. They stay unplayable or use honest placeholders until approved art exists, so no substitute art is created.
- All four rarity variants and all six forms share the family's element. No rarity or branch changes it.

### 4.3 What elements do (phased, each phase off by default)
| Phase | Flag (default) | Effect |
|---|---|---|
| E0 Metadata | `elements.metadata=false` | `elementId` is filled in contract records and shown as a text chip next to the affinity. **No combat effect.** |
| E1 Weather link | `elements.weather=false` (needs D-ADV-COMBAT) | A matching weather gives the matching element **+2 Skill damage** (see ADVANCED-COMBAT-PROPOSAL.md). No other modifiers. |
| E2 Cosmetic flares (G2) | `elements.flares=false` | Uses ChatGPT-approved flare assets keyed `fx/element-<id>` when they exist. Without them, the text chip is shown. |
| — | not proposed | Element-vs-element damage multipliers (Option C). Revisit only together with ZPet Ranked. |

### 4.4 Presentation without new art
| Element | Proposed colour token (existing palette only; ChatGPT to confirm) | Text label |
|---|---|---|
| storm | existing accent/secondary | "Storm" |
| fire | existing error/warm token | "Fire" |
| light | existing tertiary/gold token | "Light" |
| earth | existing neutral-variant token | "Earth" |
| water | existing primary/cool token | "Water" |
| arcane | existing outline/violet if present, otherwise neutral | "Arcane" |
No palette changes are made without ChatGPT approval (CLAUDE-009 "no palette alterations"). If no suitable token exists, the chip is a plain outline.

## 5. How it plugs in

| Concern | Owner | Where |
|---|---|---|
| Element catalogue (family → elementId) | Shared contract bundle, approved by Zeus97x and ChatGPT | New file `ai/integration/catalogue/elements-r1.json` (proposed), mirrored byte-identically in ZPet and covered by `CONTRACT-BUNDLE.sha256` |
| `CompanionSnapshot.elementId` | Producer = the companion's `originApp` | Set from the catalogue by `SpeciesCatalog.family(speciesId)`. It stays **`null` until approval**. |
| Validation | Authority (R9 catalogue) | Proposed new check: a non-null `elementId` must equal the catalogue value for the species family, otherwise `REJECT_CATALOGUE` |
| `LineageSnapshot.elementId` | ZPet | Selects the G2 flare; `null` means no flare |
| ZBattle code | ZBattle | Add `element` and `affinity` to `CreatureFamily` (`Creatures.kt`) as *data only*. `Skills.advantage` stays exactly as written and may be reworded to use `affinity`, with tests proving identical outputs. |
| ZPet code | ZPet owner | Optional display only; `AdventureState`/`RankedRules` are untouched |

## 6. Migration and compatibility
- **Before approval:** no code change. Family advantage stays operational. `elementId = null` everywhere, and the current fixtures stay valid.
- **After E0 approval:** producers start sending element strings. Consumers on older builds accept them, because the schema already allows non-null strings matching the pattern. A consumer ignores unknown element ids (display falls back to text) rather than rejecting the companion.
- Old saves need no migration because the element is derived from the family at read time.
- `rulesRevision` (`zbattle-rules-N`) is bumped only when E1 changes damage. E0 and E2 do not change rules.
- If a future revision changes an element mapping, the catalogue revision (`elements-r2`) is used. Companion records carry the element of their snapshot revision.

## 7. Test plan
| # | Case | Expected |
|---|---|---|
| T1 | All 12×12 family pairs: `Skills.advantage` before and after the affinity refactor | Identical (+3/−2/0) |
| T2 | Snapshot of `BattleEngine.act` outputs for a fixed script, flags off | Byte-identical log/state to current main |
| T3 | Every family maps to exactly one element; each element has exactly two families | Pass |
| T4 | Rarity variants `f:0..3` and forms 0–5 resolve to the same element | Pass |
| T5 | Contract: `elementId=null` fixtures still valid; a valid mismatching element → `REJECT_CATALOGUE` (after R9 extension) | Pass |
| T6 | Families 9–11: element shown, no artwork loaded, placeholder rules unchanged | Pass |
| T7 | UI: chip readable at large text and in reduced-effects mode; no image required | Manual/device (unavailable now) |
| T8 | ZPet parity: ZPet Adventure skill damage is unchanged | ZPet owner check |

## 8. Open questions
1. Approve Option B (named affinity + 6 family elements), or keep Option A until later?
2. Affinity names Might/Mystic/Wild: approve or rename (ChatGPT)?
3. Element assignments: any family you want moved? Spirit fox could be `fire` instead of `light`, and Moon rabbit could be `arcane` instead of `water`.
4. Should ZPet display elements too, or should elements stay ZBattle-only flavour?
5. Is E1 (+2 Skill in matching weather) acceptable, or should weather be cosmetic only?
6. Is a full type chart (Option C) ever wanted? If so, it must be decided jointly with ZPet Ranked.
