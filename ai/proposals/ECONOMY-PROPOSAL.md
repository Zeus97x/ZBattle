# ZBattle — Economy proposal (draft)

> **PROPOSAL — inactive; values need Zeus97x approval (D-ECONOMY, D-REPLAY-REWARDS).**
> Nothing here is implemented or canon. Every number marked *(proposed)* is a draft for review. The only fixed values are the owner decisions listed in §1. Prepared 2026-10-10 (America/Toronto), read-only from ZBattle and the ZPet clone. No repository was modified.

> **Decision batch 2 (Zeus97x, 2026-10-10): approved with changes.** First-win XP is Wild 20 / Mini 40 / Stage 80 / Location 120 / Region 150 (not Option B); coins as proposed; replay 25% rounded down before sharing; no location-boss ticket; S1 split; §7 consumables, equipment, cosmetics and prices approved; Basic/Great/Ultra ZCubes only; no materials; starter kit 100 coins + 3 Potions; deferred items stay deferred. Q-E4 (daily replay coin cap) and Q-E13 (retroactive tickets) were not answered. See [DECISIONS.md](../integration/DECISIONS.md) batch 2.

> **Claude review note (2026-10-10).** This draft was written before [CAMPAIGN-PROPOSAL.md](CAMPAIGN-PROPOSAL.md) existed, so the simulation in §10 uses its own reference layout (8 wild per area). The campaign proposal uses 3 wild per area plus the same bosses. The campaign JSON now uses this document's **Option B** XP, and the combined effect for a party of 3 is analysed in CAMPAIGN-PROPOSAL §7.6 (open question Q-F). Replay quantities in §5 are **not active**: B5 ships replay tracking with a replay XP/coin table that pays 0 until these values are approved. Coins have no balance in the save until C1.

---

## 1. Owner decisions this proposal builds on (approved, not up for change here)

| Ref | Decision (Zeus97x) | Effect on economy |
|---|---|---|
| Q7 | Repeat victories earn **reduced** XP/coins. They never earn a first-clear ticket. Each separate encounter starts at **full HP**. Reopening an active battle keeps its saved HP. | Replay table in §4. Potions only matter inside one battle (§7). |
| Q9 | One ticket per first-clear boss: **mini boss → 1 Rare, stage boss → 1 Epic, region boss → 1 Legendary**, first clear only. All other quantities need approval. | §8. All other numbers in this file are proposals. |
| Q8 | Rarity mapping ZPet Common/Heroic/Mythic/Celestial = ZBattle Common/Rare/Epic/Legendary. | Ticket tiers and ZCube tiers use the ZBattle names. ZPet ids (`family:rarity`) are kept. |
| Q4 | Party size **3**. | XP-sharing options in §5 assume up to 3 participants. |
| Q16 | Expedition credit only for **qualifying victories with participation**. | The participation definition in §5 is reused for credit. |
| Auto | Auto-fight **never uses items**. | Item consumption always needs a manual tap (§7). |

## 2. Current baseline (evidence)

**ZBattle** (read-only, `/home/user/ZBattle`):
- `core/.../battle/Encounters.kt`: `firstWinXp = if (boss) 200 else 60`. There is one playable encounter (area 0, slot 0).
- `core/.../battle/BattleProgress.kt` `settle()`: XP is paid only when `firstVictory`, and replays pay 0. There are no coins at all. `defeated` (first-clear ledger) and `wins` (victory count) already exist. Settlement is idempotent per `battleId`.
- `core/.../battle/Stats.kt`: level = `1 + xp/100`, capped at **50**, so 4,900 XP reaches the cap. Opponent stats depend only on the area's stage inside its region (0–3), not on region index. Opponent display level is 2–14.
- `core/.../battle/BattleEngine.kt`: actions are Attack/Skill only. Skill has a 3-turn cooldown and applies Burn (3 dmg × 3 turns) or Weaken (−3 enemy damage × 3 turns). There is a 50-turn limit, and reaching it is a defeat. There is no item action yet; the comment says ZPet Guard/Potion are "not approved yet".
- `core/.../PreviewContent.kt`: shop categories **Equipment**, **Consumables** and **Cosmetics**, each with 4 placeholder items `demo-<category>-1..4`. All of it is marked non-canon.
- `ai/ZCUBES_PLAN.md`: 4 ZCube tiers (Basic/Great/Ultra/Mythic), proposed +0/+10/+20/+35 catch bonus. Prices are open (D-ZCUBES).
- `ai/tasks/CLAUDE-006-BOSS-REWARDS.md`: C1 inventory/ledger, C2 boss rewards, C3 ticket roll tables (Rare 70/30; Epic 50/35/15; Legendary 40/30/21/9).

**ZPet** (read-only, `/home/user/zpet`). ZPet has **no coin currency and no shop**:
- `app/src/main/java/com/zeus97x/zpet/AdventureState.java`: a victory pays **60 XP / 200 boss**, **+1 potion** and **+1 shard / +5 boss**. Players start with 3 potions, capped at 9,999. A boss is every 10th encounter on a route (`number%10==0`). Each encounter is banked per 500 walked steps (`travel/500`).
- Same file, `Battle.move`: **Potion heals 25 HP**, uses the turn (0 damage dealt) and is unavailable in practice or at full HP. **Guard** divides incoming damage by 3.
- `MainActivity.java:534`: "Potions heal 25 HP in battle. Myth shards are saved for future crafting." Line 589: "Used potions stay spent." Line 564: practice gives no rewards and uses no items.
- `Progression.java`: same level formula (1 + xp/100, cap 50). Quest XP rewards are 200/700/1000. Training energy: 250 steps = 1 energy, at most 24 per day. The training cap is 30.
- `WorldState.java`: a capture attempt costs 2,500 banked steps. Pity guarantees a catch after 2 fails. `SpeciesCatalog.java:40` catch chance is 80/65/50/35.
- `RegionCatalog.java`: 48 areas in 12 groups of 4. `walkingCost(target) = stage × 1500` steps.

**Coherence rule this proposal follows:** ZPet earns progress through walking (steps gate both encounters and catches). ZBattle has no step gate, so its per-battle XP must be **lower than ZPet's** to avoid trivial levelling, and its coins are new to ZBattle only. Nothing is transferred to ZPet. ZPet potions and shards stay ZPet-owned.

## 3. Principles

1. **Sources vs sinks.** Sources are first-clear battle rewards (main), replay rewards (small, capped) and boss tickets (first clear only). Sinks are consumables (recurring), equipment (one-time per creature/tier), ZCubes (recurring, if approved) and cosmetics (open-ended surplus sink with no power).
2. **No real money.** There is no premium currency, no purchase of coins, tickets, ZCubes or items, and no ads-for-rewards. Every price is in coins earned in play.
3. **No pay-to-win assumptions.** Nothing is designed around a paid shortcut. Power bought with coins is small and capped (one equipment slot, +1/+2 stats). Tickets and the Mythic ZCube can't be bought.
4. **First clear is the main source; replay is maintenance.** Replay pays 25% (floored) and has a daily coin cap, so farming exists but never beats progressing.
5. **Same rewards for manual and auto** (CLAUDE-005 acceptance). Auto just can't spend items.
6. **Integer, deterministic math.** All reductions use integer floor (`value * pct / 100`). Rewards are paid in the same state change as settlement (the existing `settle()` guard), once per `battleId`.
7. **Defeat, retreat and turn limit pay nothing.** Consumed items stay spent (ZPet parity).

## 4. Currencies and inventory

| Currency / item | Type | Source | Sink | Cap *(proposed)* |
|---|---|---|---|---|
| **Coins** | soft currency, ZBattle-only | battle victories | shop | wallet 999,999 |
| **Rare ticket** (= ZPet Heroic) | gacha ticket | mini boss first clear | C3 roll (1 creature) | none (finite supply) |
| **Epic ticket** (= ZPet Mythic) | gacha ticket | stage boss first clear | C3 roll | none |
| **Legendary ticket** (= ZPet Celestial) | gacha ticket | region boss first clear | C3 roll | none |
| Consumables | inventory stack | shop | battle use (manual only) | 99 per item |
| Equipment | owned item | shop | equipped (1 slot per creature) | — |
| **Materials** | — | **none in v1** *(proposed)* | — | — |

Materials: ZPet's Myth shards are ZPet-owned and stay in ZPet. I propose **no ZBattle material in v1** until crafting is designed. C1's ledger should still support a `material` kind so it can be added without a save migration.

## 5. Battle rewards by encounter kind *(proposed)*

Encounter kinds follow CLAUDE-005 B4: wild, mini boss, stage boss, location boss, region boss. Counts and rosters are D-CAMPAIGN and are not set here.

### 5.1 Recommended values ("Option B, campaign-paced")

Replay = **25% of first clear, rounded down**, for both XP and coins.

| Kind | First clear XP | First clear coins | First clear ticket | Replay XP (25%↓) | Replay coins (25%↓) | Replay ticket |
|---|---:|---:|---|---:|---:|---|
| Wild | 30 | 20 | — | 7 | 5 | never |
| Mini boss | 60 | 50 | **1 Rare** (fixed, Q9) | 15 | 12 | never |
| Stage boss | 100 | 80 | **1 Epic** (fixed, Q9) | 25 | 20 | never |
| Location boss | 150 | 120 | — *(no ticket specified; see Q-E3)* | 37 | 30 | never |
| Region boss | 250 | 250 | **1 Legendary** (fixed, Q9) | 62 | 62 | never |
| Defeat / retreat / turn limit | 0 | 0 | — | 0 | 0 | — |

### 5.2 Alternative ("Option A, ZPet parity")

This keeps today's 60/200 values (`Encounters.kt`, `AdventureState.java`) and extends them.

| Kind | First XP | Replay XP (25%↓) | First coins | Replay coins (25%↓) |
|---|---:|---:|---:|---:|
| Wild | 60 | 15 | 20 | 5 |
| Mini boss | 120 | 30 | 50 | 12 |
| Stage boss | 200 | 50 | 80 | 20 |
| Location boss | 300 | 75 | 120 | 30 |
| Region boss | 500 | 125 | 250 | 62 |

**Why I recommend B:** ZPet gates each 60 XP behind 500 walked steps. ZBattle has no step gate. With Option A and the reference layout in §10, the first-clear campaign gives 58,800 XP, which is enough to cap **12** creatures at level 50. A fixed party of 3 sharing XP would cap by about area 12. Option B halves that (≈29,400 first-clear XP), and a fixed trio caps around area 23 of 48.

### 5.3 Replay and session caps *(proposed)*

- **Daily replay coin cap: 300 coins per local calendar day.** Once it is reached, replays still pay replay XP but 0 coins. First clears are never capped. The day key is the device local date. Trust and clock-rollback handling depend on D-OFFLINE-TRUST, and this is not claimed to be tamper-proof.
- **No daily XP cap.** The level cap of 50 and the 25% rate are enough.
- **Repeat session (B5): at most 10 battles per started session.** The session stops on defeat, on interruption, and when the daily coin cap is reached (shown as "coin cap reached, continue for XP only?").
- **No replay of a boss within the same session** is *not* proposed. Bosses replay like any encounter, at 25%, with no ticket.

## 6. Party XP sharing (up to 3 participants) — **decision needed (D-PARTY-XP, new)**

**Participant** means a party member who took at least one action (Attack, Skill or Item) during the battle. The same definition is used for Q16 expedition credit. Benched members who never entered get nothing.

| Option | Rule | Example: 100 XP, 3 participants | Pros / cons |
|---|---|---|---|
| **S1 Split (recommended)** | `floor(XP / n)` each. The remainder goes to the participant on the field at victory. | 34 / 33 / 33 | Total XP is constant whatever the party size, so the economy is predictable. Slightly punishes switching. |
| S2 Full to all | Each participant gets the full XP. | 100 / 100 / 100 | Simple, but it triples the XP inflow. Only viable with values well below Option B. |
| S3 Lead full, others half | The on-field-at-victory member gets 100%, others `floor(50%)`. | 100 / 50 / 50 | Rewards the finisher. Total varies 100–200. |
| S4 Finisher only | Only the on-field member gets XP. | 100 / 0 / 0 | ZBattle's current single-fighter behaviour. Discourages switching. |

The simulation in §10 uses **S1**. All options apply the same 25% replay reduction **before** splitting. For example, a wild replay pays 7 XP, split 3/2/2.

## 7. Shop *(proposed; every item is preview until approved)*

Only the three categories that exist in `PreviewContent.ShopCategory` are used. The proposed items replace `demo-<category>-1..4` one to one, keeping four per category. Labels are placeholders, and art comes from ChatGPT later. All items are **preview** and not purchasable until D-ECONOMY is approved and C1 exists.

### 7.1 Consumables (fit the current engine)

General rules *(proposed)*:
- Item use is a **new manual action** that takes the player's turn. It deals 0 damage, the opponent still strikes, and the Skill cooldown ticks down by 1 as on Attack (ZPet Potion parity).
- **Auto-fight never uses items.** The Item button is disabled while auto runs. The player must stop auto first (tap the Attack button, which reads "Stop" while auto runs).
- At most **1 item per turn** and **5 items per battle**.
- Items are consumed when used and stay spent on defeat or retreat. They are not refunded.
- HP resets to full at each new encounter (Q7), so healing items are only for within one fight.
- Engine and save changes need a `rulesRevision` bump (contract v0.2).

| Preview id | Placeholder name | Effect (engine terms) | Restriction | Price (coins) |
|---|---|---|---|---:|
| `demo-consumables-1` | Potion | Heal **25 HP** (ZPet parity), up to max HP | Not at full HP | **30** |
| `demo-consumables-2` | Super Potion | Heal **60 HP**, up to max HP | Not at full HP | **80** |
| `demo-consumables-3` | Ember Vial | Apply **Burn** to the opponent: 3 dmg at the end of each of the next 3 turns (same as Skill Burn, refreshes rather than stacks) | — | **50** |
| `demo-consumables-4` | Sapping Dust | Apply **Weaken** to the opponent: it hits for 3 less (min 1) for 3 turns (same as Skill Weaken, refreshes) | — | **50** |

Deferred consumables, proposed for when their systems exist:
- Focus Tonic resets the Skill cooldown to 0 (price 60).
- Revive Seed brings back a fainted party member at 50% max HP. It needs B2 switching (price 150).

### 7.2 Equipment

Equipment uses one slot per creature *(proposed)*. It gives flat stat bonuses and is applied in `CreatureStats.forCreature`. Bonuses are small compared with the level growth (+16 power at level 50). An item can be moved between owned creatures freely when no battle is active.

| Preview id | Placeholder name | Bonus | Tier I price | Tier II bonus / price |
|---|---|---|---:|---|
| `demo-equipment-1` | Fang Charm | +1 Power | **200** | +2 Power / **600** |
| `demo-equipment-2` | Shell Charm | +1 Guard | **200** | +2 Guard / **600** |
| `demo-equipment-3` | Feather Charm | +1 Speed | **200** | +2 Speed / **600** |
| `demo-equipment-4` | Heart Charm | +10 Max HP | **250** | +20 Max HP / **700** |

Tier II replaces tier I and has no trade-in value *(proposed)*. Selling is not proposed for v1.

### 7.3 Cosmetics (pure sink, no stats)

| Preview id | Kind | Price |
|---|---|---:|
| `demo-cosmetics-1` | Trainer frame | **300** |
| `demo-cosmetics-2` | Party banner | **500** |
| `demo-cosmetics-3` | Battle backdrop tint | **800** |
| `demo-cosmetics-4` | Trainer title | **1,000** |

### 7.4 ZCubes (category placement and prices need D-ZCUBES)

ZCubes are not in `PreviewContent`. If they are approved, they would sit under Consumables.

| Tier | Price | Note |
|---|---:|---|
| Basic | 25 | |
| Great | 75 | |
| Ultra | 200 | |
| Mythic | **not sold** | drop/ticket only, to be decided |

## 8. Ticket rules (fixed by Q9, implementation proposals marked)

1. **Mini boss → 1 Rare ticket, stage boss → 1 Epic ticket, region boss → 1 Legendary ticket.** Each is paid only on the **first clear** of that encounter id.
2. **Never on replay**, defeat, retreat or turn limit. Practice (if it is added) never pays tickets.
3. Location boss, wild and mystical encounters pay no ticket under Q9. Mystical rewards are their own roll (CLAUDE-006 C5).
4. *(proposed)* The ticket is granted in the same atomic settlement as XP/coins and is keyed by `encounterId` in the existing `defeated` first-clear set. A reload or a second settlement can't regrant it (the existing `active.battleId` guard).
5. *(proposed)* Each consumed ticket persists exactly one roll outcome (C3 tables, injected RNG). Retries re-show the stored outcome and never reroll.
6. *(proposed)* Manual and auto first clears earn identical tickets.
7. *(proposed)* Migration: encounters already in `defeated` before C2 ships **do not** retroactively grant tickets. Only one real encounter exists today, and it is wild, so the impact is nil.

## 9. Integration notes (ZPet coherence)

- Coins, ZBattle consumables and equipment never write to ZPet. ZPet potions and shards are not imported.
- Q16: an expedition credit event fires only for a qualifying victory, for each participant (as defined in §6). Replays qualify only if D-EXPEDITION says so (open question Q-E6).
- The ZPet values reused here are Potion = 25 HP, level formula 1 + xp/100 capped at 50, and the boss every 10th encounter, which inspired the reference layout of 10 encounters before the location boss.

## 10. Progression simulation *(proposed values, Option B, S1 split)*

**Reference layout (assumption only, D-CAMPAIGN not approved):** each of the 48 areas has 8 wild, 1 mini boss, 1 stage boss and 1 location boss. Each 4-area region group ends with 1 region boss.

**Expected play:** clear everything once, plus 5 wild replays per area. The party is a fixed trio sharing XP (S1).

**Expected spend:**
- Consumables: 2 Potions and 1 Ember Vial per area (110 coins).
- Equipment: 600 coins at the end of each group in groups 1–4 (three tier I pieces for a growing roster), 1,800 in groups 5–8 (three tier II), and nothing in groups 9–12.

| After areas | Total XP earned | XP per member | Member level | Coins earned | Coins spent | Net coins | Rare / Epic / Legendary tickets |
|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 585 | 195 | 2 | 435 | 110 | 325 | 1 / 1 / 0 |
| 4 (1 region) | 2,590 | 863 | 9 | 1,990 | 1,040 | 950 | 4 / 4 / 1 |
| 8 | 5,180 | 1,726 | 18 | 3,980 | 2,080 | 1,900 | 8 / 8 / 2 |
| 12 | 7,770 | 2,590 | 26 | 5,970 | 3,120 | 2,850 | 12 / 12 / 3 |
| 24 | 15,540 | 5,180 | 50 (cap) | 11,940 | 8,640 | 3,300 | 24 / 24 / 6 |
| 36 | 23,310 | 7,770 | 50 (cap) | 17,910 | 13,560 | 4,350 | 36 / 36 / 9 |
| 48 (full) | 31,080 | 10,360 | 50 (cap) | 23,880 | 14,880 | 9,000 | 48 / 48 / 12 |

Per area: 585 XP = 8×30 + 60 + 100 + 150 + 5×7. Coins: 435 = 8×20 + 50 + 80 + 120 + 5×5. Each region boss adds 250 XP and 250 coins.

How to read the table:
- **Coherence.** Net coins stay positive, so players can always afford Potions. Equipment costs about one region's income at tier I and about one region per piece at tier II. The surplus at the end (≈9,000) is absorbed by cosmetics (2,600 for all four) and by ZCubes if they are approved.
- **Evolution gates.** ZPet's level 3/10/20 gates are reached around areas 1–2, 5 and 9 for a fixed trio, which is a reasonable pace. Rotating in caught creatures slows each one down.
- **Replay value.** Replays are about 6% of per-area XP and 6% of per-area coins under expected play. The daily cap of 300 coins limits farming to about 60 wild replays per day for coins.
- **Option A comparison.** With the same layout, A gives 1,100 XP per area and 58,800 in total, and a fixed trio caps by about area 12.

## 11. Risks

1. **Difficulty does not scale with region.** Opponent stats depend only on stage 0–3 (`Stats.kt`), so the levels in §10 make later regions trivial. D-CAMPAIGN needs a region multiplier, or XP must be lowered further.
2. **Level cap reached mid-campaign** (area ~23 for a fixed trio). Overflow XP is wasted unless rotation or catching spreads it (see Q-E5).
3. **Ticket volume.** The reference layout yields 48 Rare, 48 Epic and 12 Legendary tickets, which is 108 creatures. That could be more creatures than the C3 pools and the collection UI can support. It depends directly on the D-CAMPAIGN boss counts.
4. **Client-side trust.** The daily caps and the ledger are local. Clock rollback or save edits can bypass them until D authority (D-OFFLINE-TRUST).
5. **Item power.** Ember Vial gives Burn to Weaken families, and stacking items with Skill could trivialise bosses. The 5-per-battle cap mitigates this, but boss tuning should be tested with and without items.
6. **Auto + replay farming.** Even at 25%, unattended loops are possible. Foreground-only auto, the 10-battle session cap and the daily coin cap mitigate this.
7. **Save migration.** Adding coins, inventory, tickets and an item action needs a `BattleProgressCodec` schema bump and a `rulesRevision` bump.

## 12. Open questions (for Zeus97x)

| ID | Question | Proposal |
|---|---|---|
| Q-E1 | Option B (campaign-paced) or Option A (ZPet parity) XP? | **B** |
| Q-E2 | Replay rate: 25% floor for both XP and coins? | Yes, 25% for both |
| Q-E3 | Does the location boss get a ticket? Q9 lists none. | No ticket. It gets XP and coins only. |
| Q-E4 | Daily replay coin cap of 300, and a session cap of 10? | Yes |
| Q-E5 | Party XP sharing S1–S4 (new decision D-PARTY-XP)? | **S1 split** |
| Q-E6 | Do replays qualify for Q16 expedition credit? | Only first clears and capped replays. ZPet owner to confirm. |
| Q-E7 | Item action takes the turn, max 5 per battle? | Yes |
| Q-E8 | Consumable list and prices in §7.1 | As listed |
| Q-E9 | Equipment: one slot, tiers I/II, prices in §7.2? | As listed |
| Q-E10 | Should ZCubes be sold, and is Mythic shop-excluded? | Basic/Great/Ultra sold. Mythic not sold. |
| Q-E11 | Materials in v1? | None. Reserve the ledger kind. |
| Q-E12 | Starting coins / starter kit for new saves | 100 coins + 3 Potions (ZPet starts with 3 potions) |
| Q-E13 | Do retroactive tickets go to encounters cleared before C2? | No |

---
*Sources read:*
- ZBattle: `ai/CROSS_APP_ROADMAP.md`, `ai/ZCUBES_PLAN.md`, `ai/integration/DECISIONS.md`, `ai/tasks/CLAUDE-005-CORE-PROGRESSION.md`, `ai/tasks/CLAUDE-006-BOSS-REWARDS.md`, `core/src/main/kotlin/com/zeus97x/zbattle/core/{PreviewContent.kt,Regions.kt}`, `core/.../battle/{Encounters,Stats,BattleProgress,BattleEngine}.kt`
- ZPet: `/home/user/zpet/app/src/main/java/com/zeus97x/zpet/{AdventureState,Progression,WorldState,SpeciesCatalog,RegionCatalog,MainActivity}.java`

No campaign proposal file was present at the time of writing.
