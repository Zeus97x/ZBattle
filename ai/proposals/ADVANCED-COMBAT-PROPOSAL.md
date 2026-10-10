# Advanced combat proposal (Q20)

> **PROPOSAL — inactive and configurable; needs Zeus97x approval (D-ADV-COMBAT)**
> Covers Duo activation, Last Stand, weather and rivalries (CLAUDE-009 F1–F3). Every value is proposed and configurable. Each mechanic has its own flag, and all flags default to **off**. With the flags off, battles must stay byte-identical to the current engine. Weather effects also depend on D-ELEMENT (see ELEMENTS-PROPOSAL.md).

Written 2026-10-10 (America/Toronto) by Claude. Sources were read without changes: ZBattle main `9d09592`, ZPet main `1adcedb`.

---

## 1. Current state (evidence)

| Item | State | Evidence |
|---|---|---|
| Actions | Attack: `max(2, power+5−guard/2)`. Skill: `max(3, power+9−guard/2+advantage)`, 3-turn cooldown, applies Burn (3 dmg × 3 turns) or Weaken (−3 enemy dmg × 3 turns). Retreat. | `core/src/main/kotlin/com/zeus97x/zbattle/core/battle/BattleEngine.kt`, `Stats.kt` |
| Enemy | `max(2, power+4−guard/2)`; **+5 every 3rd turn** (telegraphed "Heavy strike incoming") | `BattleEngine.kt` (`turn % 3 == 0`, `enemyIntent`) |
| Advantage | family%3 +3/−2 on Skill only | `Stats.kt` `Skills.advantage` |
| Order and limit | The faster side acts first; ties go to the player. **A 50-turn limit counts as a defeat.** | `BattleEngine.kt` |
| Party | One creature (`playerUid`). Party/switching is B2 work, now decided by Q4: **party of 3, one active; switching consumes the turn; the enemy acts normally; replacing a fainted creature is free.** | `BattleEngine.kt`; owner decision Q4 |
| RNG | None in battle. The engine is fully deterministic. | `BattleEngine.kt` |
| Duo, Last Stand, weather, rivalry | **None implemented** in ZBattle. **None exist** in ZPet: weather and personality (Brave/Curious/Loyal/Mischievous/Lazy/Lucky Idiot) return no source matches. | `grep -rniE "weather\|duo\|last ?stand\|personalit\|lucky"` over both repos' sources; `DECISIONS.md` D-ADV-COMBAT |
| ZPet boss-loss tracking | `losses[area]++` on each boss defeat. After **3 defeats**, an "easier version" is offered: enemy power ×2/3 (min 3), HP ×2/3. | `zpet/.../WorldState.java:22,76,84-85`; `zpet/.../AdventureState.java` `Battle.ease()` |
| ZPet "rivals" | A *social* feature: a mutually accepted rival bond between players (Phase 10; `RivalWidget.java`). It is **not** an enemy-encounter rivalry. | `zpet/BLUEPRINT.md:7,76,108` |
| Constraints | No hidden stat inflation; deterministic, recoverable state; normal battles unchanged when disabled; no invented opponents or art; double battles deferred until F1 is activated. Origin bonus ≤ 20%, applied once (Q15). Legacy prestige cosmetic only (Q21). | `ai/tasks/CLAUDE-009-ADVANCED-COMBAT.md`; owner decisions |

## 2. Goals
1. Add tactical depth that suits a party of 3 with one active creature, without turning battles into double battles.
2. Every effect is deterministic, saved in `BattleState`, visible in the UI and log, and individually switchable.
3. Effects are flat, situational and labelled. Nothing multiplies stats or stacks with the 20% origin bonus.
4. No new opponents, art or dialogue text are invented. Content slots are keys that need approved text.

## 3. Party-of-3 rules these mechanics respect
| Q4 rule | How each mechanic honours it |
|---|---|
| One active creature | Duo Strike brings in a benched partner for **one hit only**. The partner never becomes active and takes no enemy damage. |
| Switching consumes the turn | Duo Strike is its own action, not a switch. The active creature stays the same. |
| The enemy acts normally | After a Duo Strike the enemy acts as usual, including the 3rd-turn heavy strike. |
| Replacing a fainted creature is free | Unchanged. Last Stand triggers only when **no** replacement exists. |

## 4. Recommended design (all values proposed and configurable)

### 4.1 Duo Strike (F1), flag `combat.duo` = off
| Setting | Proposed value |
|---|---|
| Activation | The **Duo gauge** (team-level, 0–100) fills by **+25** for each player Attack or Skill that deals damage and **+10** when the player's creature takes a heavy strike. The gauge persists across switches. |
| Requirement | Gauge at 100, at least one **benched, non-fainted** party member, and the battle is not practice-locked |
| Use | Action **Duo Strike** (pick a partner). It uses the player's turn and the enemy acts normally. |
| Damage | `max(4, activePower + floor(partnerPower / 2) + 7 − enemyGuard/2 + advantage(active))` |
| Effects | No Burn/Weaken; the active creature's Skill cooldown ticks −1 as for Attack |
| Limit | **Once per battle** |
| Attribution | The partner joins `participantCompanionIds` and gets an XP share (participation rules: EXPEDITION-PROPOSAL §3.4) |
| Auto-fight | Never uses Duo Strike (configurable) |
| Future (F4) | ZPet friendship or Secret Handshake data could add a cosmetic intro only, never extra damage, until approved |

*Sample:* a Young (6 power) active with a Young partner (6) against a stage-0 wild (guard 4) deals `6+3+7−2 = 14` (neutral advantage), against 13 for a neutral Skill and 9 for an Attack. Duo Strike adds no Burn or Weaken, so its total value stays close to a Skill.

### 4.2 Last Stand (F2), flag `combat.lastStand` = off
| Setting | Proposed value |
|---|---|
| Trigger | A hit would reduce the active creature to 0 HP **and** it is the **last non-fainted party member** **and** its HP before the hit was > 1 |
| Chance | **100%** (deterministic; the engine has no RNG) |
| Effect | Survives at **1 HP**. For the next **2** player actions, its Attack/Skill/Duo damage gets **+3** (flat, shown as "Last Stand +3"). |
| Limit | **Once per battle** (per party, not per creature) |
| Exclusions | It does not prevent the 50-turn-limit defeat and does not trigger on retreat |
| Stacking | Adds to weather and advantage only as flat terms. It is never a multiplier. |

### 4.3 Battlefield weather (F2), flag `combat.weather` = off; effects need D-ELEMENT (E1)
| Weather id | Matching element | Display |
|---|---|---|
| `clear` | — | Clear |
| `thunderstorm` | `storm` | Thunderstorm |
| `heatwave` | `fire` | Heatwave |
| `sunlit` | `light` | Bright sun |
| `sandstorm` | `earth` | Sandstorm |
| `rain` | `water` | Rain |
| `mist` | `arcane` | Arcane mist |

| Setting | Proposed value |
|---|---|
| Roll | At battle start: `H(battleId, "weather") mod 100`, **saved in BattleState** (never recomputed) |
| Weights | `clear` 50%. The weather matching the **area family's element** 30%. Each of the other 5 weathers 4%. |
| Duration | The whole battle (static). No mid-battle changes in the first version. |
| Effect (with elements approved) | A Skill from a creature whose element matches gets **+2** damage. An enemy whose element matches gets **+1** on its heavy strike only. |
| Effect (elements not approved) | **Cosmetic label only, with no damage change** |
| Exclusions | Bosses: configurable (proposal: included). Mystical: per approved content. |
| Real-world weather | Not used (no location permission) |
| Presentation | Text chip in the battle header. Weather overlays only with ChatGPT-approved assets. Reduced-effects mode keeps text only. |

### 4.4 Rivalries and revenge (F3), flag `combat.rivalry` = off
| Setting | Proposed value |
|---|---|
| Scope | Boss encounters (`mini-boss`, `stage-boss`, `location-boss`, `region-boss`). Wild, campaign, practice and mystical are excluded. |
| Becomes a rival | After **2 defeats** (`outcome=defeat`, including the turn limit; retreats do not count) to the same `encounterId` |
| Rival effect on the enemy | **None.** No stat change and no hidden inflation. It is presentation only: a badge plus dialogue slots. |
| Revenge victory | The first victory while it is a rival gives a **one-time bonus of +50% of that encounter's first-win XP** (for example, a 200 XP boss gives +100). The encounter is then marked `avenged`. |
| Easier version (ZPet parity) | After **3** defeats, an "easier version" can be offered with enemy power ×2/3 (min 3) and HP ×2/3, as in ZPet `ease()`. **Choosing it forfeits the revenge bonus.** |
| Persistence | `BattleProgress.rivals[encounterId] = {defeats, state: none\|rival\|avenged, rulesRevision}` |
| Dialogue | Keys `rival/<encounterId>/{intro,taunt,lose,win}`. **No text is shipped until it is approved**; an empty key shows nothing. |
| Naming | The UI says "Rival boss", to avoid confusion with ZPet's social *rival bond* |

### 4.5 Stacking and order of resolution (per player turn)
1. The player picks an action (Attack / Skill / Duo Strike / Switch / Throw ZCube if catching is approved / Retreat).
2. The order is the existing speed rule. The enemy strike is checked for Last Stand when it would KO.
3. Player damage = base formula + advantage + weather (+2 Skill) + Last Stand (+3), all flat. Minimum floors as today.
4. Burn tick, then the enemy strike (if the player is faster). Weaken applies as today.
5. Gauge update, then outcome check, then the turn limit.

| Combination | Allowed? |
|---|---|
| Last Stand + Duo Strike | Yes: +3 applies to the Duo hit |
| Weather + Duo Strike | No (Duo is not a Skill) |
| Origin bonus (≤20%) + any of the above | Yes. The origin bonus is applied once to stats at start (Q15). Nothing here touches stats. |
| Legacy prestige | Cosmetic only (Q21). No interaction. |

## 5. How it plugs in
| Concern | Owner | Where |
|---|---|---|
| Rules | ZBattle | `BattleEngine.act`: new `BattleAction.DuoStrike(partnerUid)`. Last Stand check in the enemy-strike step. Weather term in Skill damage. |
| Saved state | ZBattle | `BattleState` additions: `duoGauge`, `duoUsed`, `lastStandUsed`, `lastStandTurnsLeft`, `weatherId`, `rivalState`, plus B2's `party[3]`/`activeIndex`. Saved-battle format version +1. |
| Rules revision | ZBattle | `rulesRevision` = `zbattle-rules-N+1` per enabled mechanic set. It is carried in `BattleCompleted.rulesRevision`. |
| Participation | ZBattle → ZPet | `BattleCompleted.participantCompanionIds` includes Duo partners. No schema change; `maxItems: 6` ≥ 3. |
| Element input | Shared catalogue | Family → element (ELEMENTS-PROPOSAL §4.2); `elementId` stays null in the contract until approved |
| Personality/friendship (F4) | ZPet producer | Needs a new ZPet record type in a future contract revision; not part of this proposal |
| Content | ChatGPT / Zeus97x | Dialogue strings and any weather or Duo visuals |

## 6. Migration and compatibility
- All flags are off by default. A golden test verifies that the engine output with the flags off equals current main.
- Saved battles created before an upgrade keep their own `rulesRevision`. They resume under their old rules: new fields default to disabled and `weatherId = clear`.
- `BattleProgress.rivals` defaults to empty. Defeats before the feature is enabled are **not** counted retroactively.
- Double battles (two active creatures) stay deferred. Duo Strike does not need them.
- No contract schema change is needed. `elementId` stays `null` until D-ELEMENT is approved, and weather is cosmetic until then.

## 7. Test plan
| # | Case | Expected |
|---|---|---|
| T1 | All flags off; scripted 50-turn battle | Byte-identical log and state to current main |
| T2 | Duo gauge: 4 damaging actions → ready; switch keeps the gauge | Pass |
| T3 | Duo with no benched non-fainted partner | Disabled |
| T4 | Duo Strike on a heavy-strike turn | Enemy +5 strike resolves normally |
| T5 | Duo partner listed in participants; never-active bench member not listed | Pass |
| T6 | Last Stand while a non-fainted bench member exists | Not triggered; free replacement offered instead |
| T7 | Last Stand trigger, 1 HP, +3 for 2 actions, once per battle | Pass |
| T8 | Last Stand vs the 50-turn limit | Defeat still applies |
| T9 | Weather saved at start; kill and resume | Same weather |
| T10 | Weather distribution over 10⁵ battleIds | 50/30/4×5 ±1% |
| T11 | Weather with elements disabled | Label only, damage unchanged |
| T12 | Rivalry: 2 defeats → rival; retreats ignored; revenge bonus once; easier version forfeits the bonus | Pass |
| T13 | Resume a pre-upgrade saved battle | Old rules, no crash |
| T14 | UI: small screen, large text, reduced effects, labels for all modifiers | Device check (not available now) |
| T15 | Two simultaneous flat bonuses never exceed the documented sum | Pass |

## 8. Open questions
1. Duo: once per battle at a 100 gauge (+25 per damaging action), or a cooldown that repeats? Does the partner earn full XP or a share?
2. Should Duo Strike be allowed in auto-fight?
3. Last Stand: deterministic 100%, or a configurable chance (which needs seeded RNG in battle)? Only for the last party member (proposal), or once per creature?
4. Weather: static per battle (proposal), or rotating every N turns? Should weather affect bosses?
5. Weather damage values (+2 Skill, +1 enemy heavy strike): accept, or make weather cosmetic only?
6. Rivalry thresholds: 2 defeats for a rival and 3 for the easier version. Accept the +50% revenge XP? This interacts with D-REPLAY-REWARDS.
7. Who writes the rival dialogue lines (ChatGPT or Zeus97x)?
8. Should a ZCube-fled wild creature be able to return as a "roaming rival"? Not proposed; this needs approved content.
