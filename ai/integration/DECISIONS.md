# Decision register — ZBattle × ZPet (Phase A, CLAUDE-004 / A3)

Updated 2026-10-10 (America/Toronto). Every unresolved gameplay or architecture choice has an owner and lists the phase it blocks. Claude does not invent values for any of these. A recommendation is only a proposal backed by evidence. Status is **Open** unless stated.

Owners:
- **Zeus97x:** product owner, final say.
- **ZPet owner:** the ZPet project's maintainer or assistant.
- **ChatGPT:** design and art direction.
- **Claude:** proposes from evidence.

| ID | Question | Evidence / options | Recommendation (proposal) | Owner | Blocks |
|---|---|---|---|---|---|
| D-RARITY | Map ZPet rarities to ticket tiers? | ZPet: Common/Heroic/Mythic/Celestial (`SpeciesCatalog`). Roadmap: Common/Rare/Epic/Legendary. | 1:1 by index, keeping ZPet ids (`family:rarity`) | Zeus97x + ZPet owner | C2, C3, ZCubes |
| D-NATIVE-SPECIES | Which `speciesId` does a ZBattle-native creature get? | ZBattle stores form names only; every ZBattle-owned creature today is a starter | `family:0` (Common), as in the migration fixture | Zeus97x + ZPet owner | D1 |
| D-ELEMENT | Element system | Neither app has element data; both use `family % 3` advantage | Keep the current advantage until element metadata is approved; `elementId` stays null | Zeus97x + ChatGPT | F, G2, (D2 if elements change stats) |
| D-ORIGIN-ROUNDING | Origin bonus rounding, and does 0% bond still give +10%? | v0.1 formula gives 1.10 at 0% bond | Integer basis points with floor per stat (v0.2 §1 G6); confirm the 0%-bond base | Zeus97x | D2 |
| D-EVOLUTION | Independent ZBattle evolution? Correction policy when a source form must go down? | ZPet owns imported forms (quests + level); ZBattle has no evolution | No independent evolution for imported companions; native companions TBD. Form corrections only by an explicit ZPet-issued correction flag in a future revision | Zeus97x + ZPet owner | B3, D |
| D-PARTICIPATION | Do losses, retreats or practice battles earn expedition credit? | Contract R12 currently counts non-practice victories only | Victories only, until approved | Zeus97x | E1 |
| D-BACKEND | Reuse ZPet's Supabase project for ZBattle accounts and events, or use a separate one? Guest → account linking? | ZPet: Supabase Auth plus service-only `zpet_dispatch`; ZBattle has no network code | Read-only audit first (D3), then a plan for approval. No deployment. | Zeus97x | D3–D5 |
| D-OFFLINE-TRUST | What may an offline/unverified result unlock? | Steps and local battles are client-reported (flagged, not verified) | Show unverified results as pending; only server-settled results transfer | Zeus97x | D5, C timers |
| D-WEEK-WINDOW | Mystical boss limit: anchored 168-hour window or trailing 7 days? | Roadmap notes the two readings conflict | Anchored 168h per boss (needs no timestamp ledger) | Zeus97x | C4 |
| D-CAMPAIGN | Stage counts, rosters per area, gates, difficulty curve | One real encounter today; ZPet rule: area family, boss every 10th | Needs approved content; Claude will not invent it | Zeus97x + ChatGPT | B4 |
| D-REPLAY-REWARDS | XP/coins/tickets for replays | Rematches currently pay 0 XP | Decide before auto/repeat battles | Zeus97x | B5, C2 |
| D-AUTO-FIGHT | Auto-fight scope (foreground only? item use?) | Roadmap proposes foreground only, no item use, stop on interruption | As proposed | Zeus97x | B1 |
| D-PARTY | Party size and switch turn cost | Contract allows up to 6 participants | Pending | Zeus97x | B2 |
| D-ECONOMY | Coins, prices, ticket repeatability and quantities | Coins fixed at 0; no shop pricing | Pending | Zeus97x | C1, C2 |
| D-EXCLUSIVE-POOL | Mystical exclusive ZPet/item pools | Empty; must not substitute ordinary rewards | Keep mystical encounters unavailable until defined | Zeus97x + ChatGPT | C5 |
| D-EXPEDITION | Durations, acceleration, 50% cap, walking credit | Illustrative only in roadmap | Pending; ZPet-owned configuration | ZPet owner + Zeus97x | E |
| D-ZCUBES | Catch odds, tiers, prices, acquisition | ZPet catch chances 80/65/50/35 by rarity, plus pity; proposals in `ai/ZCUBES_PLAN.md` | Pending | Zeus97x + ChatGPT | catching task |
| D-ADV-COMBAT | Duo activation, Last Stand, weather, rivalry values | Deferred; none implemented | Pending | Zeus97x | F |
| D-LEGACY | Legacy eligibility, lineage, emblems | ZPet-owned; no data exists | Pending | ZPet owner + ChatGPT | G |
| D-CONTRACT-ACCEPT | Accept contract v0.2 as the shared baseline | This PR; the ZPet copy must match the bundle hash | Review fixtures, then mirror them in ZPet | Zeus97x + ZPet owner | Phase A gate → B |
