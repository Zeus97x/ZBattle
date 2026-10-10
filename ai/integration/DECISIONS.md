# Decision register — ZBattle × ZPet

- Created: 2026-10-10 (America/Toronto) in Phase A (CLAUDE-004 / A3).
- Updated: 2026-10-10 with **Zeus97x's decision batch 1**, which answers Q2–Q21 from [PHASE-A-B1-SUMMARY-AND-QUESTIONS.md](../PHASE-A-B1-SUMMARY-AND-QUESTIONS.md). The decisions are recorded here before any code that uses them.

Claude does not invent values. Where a decision asks for a proposal, the proposal is a separate document. Its values stay **inactive** (configured but switched off) until Zeus97x approves them.

| Status | Meaning |
|---|---|
| **Decided** | Approved by Zeus97x; may be implemented. |
| **Decided — proposal requested** | Direction approved; values must come back for approval. |
| **Decided — gated** | Approved, but blocked on content, art or cross-app delivery. |
| **Open** | Not decided. |

Owners:
- **Zeus97x:** product owner.
- **ZPet Claude:** the Claude session working in the ZPet repository.
- **ChatGPT:** design and artwork.
- **Claude:** this ZBattle session.

## Decided (batch 1, 2026-10-10)

| ID | Q | Decision (Zeus97x) | Implementation consequence | Blocks / used by |
|---|---|---|---|---|
| D-AUTO-FIGHT | Q2 | Foreground only, no automatic items, stops on interruption. Keep the Stop control. | B1 as built (PR #11) stands. | B1 ✔ |
| D-CONTRACT-ACCEPT | Q3 | ZPet Claude owns adoption. It reviews v0.2 against ZPet's real saves, copies schemas/fixtures unchanged, verifies the full bundle hash and supplies a real migration fixture. Incompatibilities are reported **before** acceptance. Neither copy is silently altered. | Claude (ZBattle) updates the handoff and does not write to ZPet. A contract change made here gets a new hash and is announced in the handoff. | Phase A gate |
| D-PARTY | Q4 | Party of **3**, one active creature. Switching **consumes the player's turn**: the enemy takes its normal turn, with no extra free hit. Replacing a **fainted** creature does **not** consume a turn. | B2 | B2 |
| D-EVOLUTION | Q5 | Both games support evolution. Identity, rarity, branch, nickname and progress are preserved. ZBattle owns combat XP; ZPet owns walking/care progress. Shared evolution needs **validated unlock events** and never downgrades on an ordinary stale snapshot. Founder Stage 4 unlocks through **either** game's challenge, not both. Until cross-app delivery exists, imported forms are preserved and nothing pretends to synchronize. | B3 builds the identity model, the monotonic form guard and the unlock-event ledger. Thresholds stay inactive (see D-EVOLUTION-THRESHOLDS). | B3, D |
| D-CAMPAIGN | Q6 | Prepare a campaign **proposal** from existing catalogue creatures and exact region/location ids: stage counts, bosses, unlocks, difficulty. Do not invent creatures or finalize content. | B4: proposal document plus an inactive, validated config. Runtime stays on approved encounters only. | B4 |
| D-REPLAY-REWARDS | Q7 | Repeat victories earn **reduced** XP/coins and **no** repeat first-clear tickets. Each separate encounter starts at full HP; reopening an active battle keeps its saved HP. Replay quantities need approval. Tracking and duplicate-safe settlement come first. | B5: replay tracking, settlement ledger and repeat sessions. Replay quantities are configured but inactive (pay 0) until approved. | B5, C2 |
| D-RARITY | Q8 | ZPet Common/Heroic/Mythic/Celestial = ZBattle Common/Rare/Epic/Legendary. The canonical ids stay as they are (`family:rarity`, rarity 0–3). | Display mapping only; no id changes. Contract §1 G2 notes it. | C2, C3, ZCubes |
| D-ECONOMY | Q9 | Prepare an economy **proposal**: battle rewards, replay reductions, shop prices, consumables. Keep the one-ticket first-clear boss rewards (mini-boss → 1 Rare, stage boss → 1 Epic, region boss → 1 Legendary). Other quantities need approval. | Proposal document; ticket rule recorded. | C1, C2 |
| D-WEEK-WINDOW | Q10 | Each mystical boss has its own fixed **168-hour cycle starting at its first victory**, capped at **15 wins**, plus a **4-hour cooldown** after each victory. The first victory after a cycle expires anchors the next cycle. Unused wins don't carry over. | C4 | C4 |
| D-EXCLUSIVE-POOL | Q11 | Exclusive designs are with ChatGPT. Exclusive rewards stay gated until catalogue ids, eligibility rules and approved runtime art are supplied. Founder availability never becomes a mystical drop automatically. | C5 stays unavailable. | C5 |
| D-BACKEND | Q12 | Audit the existing backend **read-only** and recommend how both apps use accounts and shared records. No deployment and no new backend resources. | Audit document. | D3–D5 |
| D-OFFLINE-TRUST | Q13 | Offline play keeps local progress. Cross-app rewards and progression stay **pending** until the approved delivery authority validates them. A server receiving a phone report is **not** verification by itself; the actual validation must be documented. | Contract `verification` semantics; backend audit lists the real validation. | D5, C timers |
| D-NATIVE-SPECIES | Q14 | Native starters use the existing **Common** family species (`family:0`), once the catalogue mappings are validated. Species stays separate from the instance UUID and the evolution form. | B3 identity model; already in the migration fixture. | D1 |
| D-ORIGIN-ROUNDING | Q15 | A **verified** ZPet-origin companion gets +10% even at 0% bond. Bonus % = `10 + 2.5 × floor(bondPercent / 25)`, capped at 20. It applies once to unmodified derived stats, the final stat is rounded down, and it never compounds on re-import. **Unknown bond = 0%** (the bonus is kept, not removed). | Contract §1 G4/G6 amended (v0.2 draft, new bundle hash). | D2 |
| D-PARTICIPATION | Q16 | Expedition battle acceleration needs a qualifying **victory** and actual **participation** by that companion. Losses, retreats and rewardless practice are excluded. Battle events are deduplicated. | Contract R12 confirmed. B2 keeps a participant ledger. | E1 |
| D-EXPEDITION | Q17 | Prepare a recommended design as a separate decision document; values stay configurable and inactive. | Proposal document. | E |
| D-ELEMENT | Q18 | Prepare a recommended design as a separate decision document. The existing family advantage stays operational until elements are approved. | Proposal document; `elementId` stays null. | F, G2 |
| D-ZCUBES | Q19 | Prepare a recommended catching design as a separate decision document; inactive until approved. | Proposal document. | catching task |
| D-ADV-COMBAT | Q20 | Prepare a recommended advanced-combat design as a separate decision document; inactive until approved. | Proposal document. | F |
| D-LEGACY | Q21 | Legacy requires **two fully bonded companions** and a **chosen successor**. Lineage and generation history are preserved. Prestige is cosmetic, with no combat bonus beyond the 20% bond cap. Detailed successor rules and artwork are pending. | Recorded. Lineage schema already forbids combat fields. | G |

## Still open (raised while implementing batch 1)

| ID | Question | Proposal | Owner | Blocks |
|---|---|---|---|---|
| D-PARTY-XP | How is first-win/replay XP shared among up to 3 participants? | Split evenly among participants (round down; remainder to the creature that landed the final hit). Total XP doesn't grow with party size. Implemented as a config switch. | Zeus97x | B2 (single-creature behaviour unchanged) |
| D-SWITCH-COOLDOWN | Does each creature keep its own Skill cooldown across switches? | Per-creature cooldown that only counts down on turns that creature acts. | Zeus97x | B2 |
| D-EVOLUTION-THRESHOLDS | Combat-XP thresholds and challenges per form, and Founder definitions | None; inactive until supplied | Zeus97x + ChatGPT + ZPet Claude | B3 activation |
| D-REPEAT-SESSION | Maximum battles per repeat session | 10 (a UI bound, not a reward value) | Zeus97x | B5 |

Proposal documents live in [../proposals/](../proposals/README.md).
