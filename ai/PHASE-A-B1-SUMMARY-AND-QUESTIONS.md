# ZBattle: Phase A + B1 summary and open questions

Updated 2026-10-10 (America/Toronto) by Claude.

> **Answered:** Zeus97x answered Q2–Q21 in decision batch 1 (2026-10-10). The answers are recorded in [integration/DECISIONS.md](integration/DECISIONS.md). This file is kept as history. This file covers what was done in Phase A (CLAUDE-004) and Phase B step B1 (CLAUDE-005), and lists every question that needs your input.

To answer, write under each question's **Your answer:** line. You can also just reply in chat with the numbers, e.g. "Q4: party of 3, switching costs a turn".

---

## Part 1 — What was done

### Pull requests
| PR | What | Branch | Status |
|---|---|---|---|
| [#10](https://github.com/Zeus97x/ZBattle/pull/10) | Phase A: audit, shared contract v0.2, decision register | `claude/zbattle-phase-a-contract` | Open, waiting for your review. Includes PR #8. |
| [#11](https://github.com/Zeus97x/ZBattle/pull/11) | Phase B1: auto-fight | `claude/zbattle-b1-auto-fight` (stacked on #10) | Open, waiting for your review. Merge #10 first. |

Nothing was merged or released. ZPet's repo, the backend and the artwork were not touched.

### Phase A — A1: Evidence audit (`ai/integration/AUDIT-A1.md`)
I read both apps' real code (ZBattle main `d2f938d`, ZPet main `1adcedb`) and wrote down what exists and what's missing. Eight gaps against the original contract v0.1:

| # | Gap | What I found |
|---|---|---|
| G1 | Creature identity | ZPet ids are `pet-1`, `pet-2`… per save. ZBattle uses number ids. Neither is unique across apps or devices. |
| G2 | Species | ZPet species = family + rarity. ZBattle stores only form names. |
| G3 | Evolution form | ZPet has forms 0–5 with branch A/B. ZBattle has no evolution yet. |
| G4 | Bond % | **ZPet has no bond percentage**, only a "First bond" quest. The origin bonus formula depends on it. |
| G5 | Elements | **Neither app has element data.** Both use a simple 3-way family advantage. |
| G6 | Origin bonus rounding | The v0.1 formula didn't say how to round, or what 0% bond gives. |
| G7 | Cheating / verification | Battles and step counts are reported by the phone, not checked by a server. |
| G8 | Accounts | ZPet uses Supabase login. ZBattle has no accounts or network code. |

I also corrected an earlier note: ZPet's branch colours only tint Branch-B forms, and only on screen.

### Phase A — A2: Shared contract v0.2 (`ai/integration/CONTRACT-v0.2.md`)
The "contract" is the agreed data format both apps will use to share creatures, battle results and rewards. v0.2 turns the v0.1 text into files a computer can check:

- **9 JSON schemas** (`ai/integration/schemas/`), one for each kind of record:
  - companion snapshot
  - battle completed
  - reward earned
  - reward redeemed
  - expedition snapshot
  - boss state
  - lineage snapshot
  - common types
  - the envelope that wraps every record
- **31 example records and 12 test scenarios** (`ai/integration/fixtures/`). They cover:
  - reward retry (no double pay)
  - out-of-date updates
  - re-importing a creature
  - another account's data
  - the wrong app sending a record
  - duplicate species
  - bond correction
  - evolution can't go backwards
  - catalogue mismatches
  - participation credit
  - unknown versions
  - redeem only once
- **Rules R1–R12** with error codes. These are the rules for when a record is accepted or rejected.
- **New ids:** each creature gets a UUID once. The old id is kept as `legacyLocalId`.
- **Your real save:** I added a ZBattle save as a test file (Cindlet after one Voltmaw win) and a mapping that shows how it would move to the new ids. A test checks both.
- **Checker:** `tools/validate_contract.py` checks all of the above, and `tools/check_doc_links.py` checks the doc links. Both now run in CI.

### Phase A — A3: Decisions and ZPet handoff
- `ai/integration/DECISIONS.md` has 20 open decisions. Each has evidence, my proposal, an owner and what it blocks. These are the questions in Part 2.
- `ai/integration/CONTRACT-BUNDLE.sha256` holds the bundle fingerprint `afc3a8de1367e1ffa0d684463ed81ddb020ccc18cbbc51f36146e6202a83e2c4`. ZPet's copy must produce the exact same value.
- `ai/integration/ZPET-HANDOFF-A3.md` lists what the ZPet side must do.
- Tasks CLAUDE-005 to CLAUDE-010 now say which decision each step is waiting on.

### Phase B — B1: Auto-fight (PR #11)
- **Button:** an "Auto battle / Stop auto" button on the battle screen.
- **How it picks moves:** Skill when ready, otherwise Attack.
- **Same rules:** it uses the same battle rules and rewards as playing by hand. Nothing about XP or saves changed.
- **Limits:**
  - It never uses items and never retreats.
  - It only runs while the app is open.
  - It stops when you tap a move, press Back or Retreat, leave the screen, leave the app, or the battle ends.
  - It pauses while a dialog is open.
  - It isn't saved, so after a restart it starts off.
- **No double moves:** a move can't play twice, and nothing plays after the battle ends.
- **Speed:** 0.9 s between moves, or 0.4 s with battle animations off.

### Checks run
| Check | Result |
|---|---|
| `python3 tools/validate_contract.py` | OK: 31 records, 12 scenarios, hash matches |
| Each invalid example fails for the right reason | Confirmed |
| `python3 tools/check_doc_links.py` | OK |
| `./gradlew -p preview test` (includes 6 new auto-fight tests and an app-flow test) | All passed |
| Screenshots of the auto-fight screen at 360dp, 412dp and large text | Checked by eye, OK |
| Android build + lint | **Not run here** (Google's download server is blocked in this session). CI runs them on the PRs. |
| Real-phone test | Not available |

### Not done / blocked
- The ZPet copy of the contract doesn't exist yet. I'm not allowed to write to ZPet's repo.
- There is no ZPet save example for the `pet-N` → new id move (ZPet-owned).
- B2–B5 and everything after are waiting on the questions below.

---

## Part 2 — Questions that need you

Each question shows the ids from `DECISIONS.md`, what it blocks, and my proposal. "Blocks" uses the phase letters below. **Bold** questions block the next work.

| Phase | Meaning |
|---|---|
| B | Core progression: auto, party, evolution, campaign, repeat battles |
| C | Bosses, tickets, rewards, shop |
| D | Accounts, backend and save migration |
| E | ZPet expeditions |
| F | Advanced combat |
| G | Legacy / endgame |

### Needed now (to merge and finish Phase B)

**Q1. Merge order.** Review and merge #10, then #11. Merging #10 also makes old PR #8 redundant. PRs #1, #3 and #4 also look outdated. Close them?
**Your answer:**

**Q2. Auto-fight scope (D-AUTO-FIGHT, blocks the merge of #11).** I built: only while the app is open, no items, stops when interrupted. Keep that, or change it? For example, should auto use potions? Should it keep going in the background?
**Your answer:**

**Q3. ZPet contract copy (D-CONTRACT-ACCEPT, blocks the Phase A gate).** Someone working on ZPet must copy `ai/integration/schemas/` and `fixtures/` unchanged and confirm hash `afc3a8de…`. Steps are in `ai/integration/ZPET-HANDOFF-A3.md`. Who does this, and do you accept contract v0.2 as the baseline?
**Your answer:**

**Q4. Party size and switching (D-PARTY, blocks B2).**
- How many creatures can you bring into a battle (1–6)?
- Does switching use up your turn? Does the enemy get a free hit?

Proposal: none, this is your call.
**Your answer:**

**Q5. Evolution (D-EVOLUTION, blocks B3).**
- Can creatures evolve inside ZBattle? Or does only ZPet decide forms, with ZBattle showing them?
- If ZPet ever has to lower a form (a correction), how should that work?

Proposal: ZBattle does not evolve creatures imported from ZPet. Native ZBattle creatures are your call. Corrections only through a special ZPet "correction" record later.
**Your answer:**

**Q6. Campaign content (D-CAMPAIGN, blocks B4).** For each area:
- How many stages?
- Which creatures appear?
- Where are the mini, stage, location and region bosses?
- What unlocks the next area, and how fast does difficulty rise?

Today there is only 1 real battle (Wild Voltmaw). ZPet's rule is "area family, boss every 10th". I won't invent rosters, so this needs a list from you or ChatGPT.
**Your answer:**

**Q7. Replay rewards (D-REPLAY-REWARDS, blocks B5 and C2).** Should beating an opponent again give anything (XP, coins, tickets), or stay at 0 like now? Today every battle also starts at full HP. Should HP carry over between repeat battles?
**Your answer:**

### Needed for Phase C (bosses and rewards)

**Q8. Rarity mapping (D-RARITY, blocks C2, C3 and ZCubes).** ZPet uses Common / Heroic / Mythic / Celestial. The roadmap uses Common / Rare / Epic / Legendary. Map them 1:1 in that order?
Proposal: yes, 1:1, keeping ZPet's ids.
**Your answer:**

**Q9. Economy (D-ECONOMY, blocks C1 and C2).**
- How many coins does each battle or boss give?
- What are the shop prices?
- How many tickets does each boss give, and can boss tickets be earned again on replay?

Coins are currently always 0.
**Your answer:**

**Q10. Mystical boss weekly limit (D-WEEK-WINDOW, blocks C4).** Is "15 wins per 7 days" a fixed weekly reset, or a rolling last-7-days count?
Proposal: a fixed 168-hour window per boss, starting from the first win.
**Your answer:**

**Q11. Mystical exclusive rewards (D-EXCLUSIVE-POOL, blocks C5).** Which exclusive ZPets and items can mystical bosses drop? Until this is defined, those bosses stay unavailable. Ordinary rewards won't be swapped in.
**Your answer:**

### Needed for Phase D (accounts, backend, save migration)

**Q12. Backend (D-BACKEND, blocks D3–D5).** Should ZBattle use ZPet's existing Supabase project for accounts and shared records, or a new one? How should a guest player link to an account?
Proposal: a read-only audit first, then a written plan for your approval. Nothing is deployed without your OK.
**Your answer:**

**Q13. Offline / unverified results (D-OFFLINE-TRUST, blocks D5 and the C timers).** What can a battle or step count that no server has checked unlock?
Proposal: show it as "pending". Only server-confirmed results transfer between apps.
**Your answer:**

**Q14. Species for ZBattle-only creatures (D-NATIVE-SPECIES, blocks D1).** Creatures that start in ZBattle (like your starter) need a ZPet-style species id.
Proposal: use the family's Common rarity (`family:0`).
**Your answer:**

**Q15. Origin bonus (D-ORIGIN-ROUNDING, blocks D2).**
- Should a ZPet creature with **0% bond** still get the +10% stat bonus?
- Rounding: OK to round down per stat?

Proposal: whole-number maths, rounded down. ZPet has no bond % yet, so for now there's no bonus at all.
**Your answer:**

### Later phases (no rush)

**Q16. Expedition participation (D-PARTICIPATION, blocks E1).** Do losses, retreats or practice battles count toward ZPet expedition credit?
Proposal: wins only.
**Your answer:**

**Q17. Expedition settings (D-EXPEDITION, blocks E).** How long expeditions last, how they can be sped up, the 50% cap and walking credit. These are ZPet-owned settings.
**Your answer:**

**Q18. Elements (D-ELEMENT, blocks F and G2).** Add a real element system, or keep the current 3-way family advantage?
Proposal: keep the current advantage until elements are designed (you and ChatGPT).
**Your answer:**

**Q19. ZCubes catching (D-ZCUBES).** Catch odds, cube tiers, prices and how cubes are earned. ZPet uses 80/65/50/35% by rarity plus a pity counter. My proposals are in `ai/ZCUBES_PLAN.md`.
**Your answer:**

**Q20. Advanced combat (D-ADV-COMBAT, blocks F).** Values for Duo activation, Last Stand, weather and rivalries.
**Your answer:**

**Q21. Legacy / lineage / emblems (D-LEGACY, blocks G).** Who qualifies, and how lineage and emblems work. ZPet-owned, with ChatGPT for art.
**Your answer:**

---

## What happens after you answer
- **Q1–Q3:** #10 and #11 can merge, and the Phase A gate closes once ZPet confirms the hash.
- **Q4:** I start B2 (party and switching).
- **Q5:** B3 (evolution).
- **Q6:** B4 (campaign).
- **Q7:** B5 (repeat battles).
- **Q8–Q11:** Phase C (bosses and rewards) can start once Phase B is done.

Artwork for anything new stays with ChatGPT.
