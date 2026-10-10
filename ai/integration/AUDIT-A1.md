# CLAUDE-004 / A1 — Evidence audit (implemented vs missing)

Written by Claude on 2026-10-10 (America/Toronto). This is a read-only audit: no gameplay, save or backend changes were made.

| Source | Commit |
|---|---|
| ZBattle `main` | `d2f938db9b00d729a5e517480b192af185e68aed` |
| PR #8 branch `ai/chatgpt/cross-app-roadmap` | `a304c72` |
| ZPet `main` | `1adcedb4583339545e3da8209025492916795bc7` (2026-10-09 20:29 −04:00), cloned read-only |

No ZPet file was modified.

## 1. ZBattle — what exists today

| Area | Status | Evidence |
|---|---|---|
| Compose shell, nine layouts, Pet Master setup | Implemented (CLAUDE-001, PR #2) | `ui/src/main/kotlin/com/zeus97x/zbattle/ui/*`, `core/.../PetMaster.kt` |
| Catalogues | Implemented, verbatim ZPet copy | `core/.../Creatures.kt` (12 families × 6 forms; families 0–8 have art), `core/.../Regions.kt` (12 groups / 48 areas, numeric ids `group-N` / `area-NN`) |
| Manual battle engine | Implemented (CLAUDE-002, PR #5) | `core/.../battle/BattleEngine.kt`: Attack/Skill, 3-turn cooldown, Burn/Weaken, speed order, 50-turn limit |
| Stats / levels | Implemented, provisional | `core/.../battle/Stats.kt`: ZPet form stats, level = 1+xp/100, HP 45+3L+guard, per-level growth |
| Encounters | **One** real encounter | `core/.../battle/Encounters.kt` (`area-00/slot-0`, Wild Voltmaw); all other cards are `PreviewContent` |
| Saved battle progress | Implemented, schema v1 | `core/.../battle/BattleProgress.kt`, `BattleProgressCodec.kt`; SharedPreferences key `battleProgress` (`app/.../PrefsSettingsStore.kt`) |
| Owned-creature identity | `OwnedCreature.uid: Long`, local counter `nextUid` | Not globally unique. Stores `creatureId` = **form name id** (e.g. `sparklit`), **not** a ZPet species id (`family:rarity`) |
| Once-only settlement | Implemented locally | `BattleProgress.settle` checks `active.battleId`; first win only pays XP; rematch = 0 XP |
| Scenery art | Implemented (CLAUDE-003, PR #6) | `app/src/main/assets/art/{region,location}/**.webp` |
| Auto-fight, party/switch, evolution, campaign, bosses | **Missing** | Phase B (CLAUDE-005) |
| Inventory, coins, tickets, gacha, mystical bosses | **Missing** (coins shown as 0) | Phase C (CLAUDE-006) |
| Origin/bond bonus, import, inbox/outbox, accounts, network | **Missing**. ZBattle has no network permission and no backend client. | Phase D (CLAUDE-007) |
| Expedition events, duo/weather/rivalry, legacy/Hall | **Missing** | Phases E–G |
| ZCubes catching | Planned only | `ai/ZCUBES_PLAN.md` |

## 2. ZPet — what exists today (read-only)

| Area | Finding | Evidence |
|---|---|---|
| Companion identity | `WorldState.Pet.id = "pet-" + nextPet`, a per-save counter.<br>Not a UUID. The `normal` and `test` profiles keep separate counters, and a cloud restore replaces the whole world, so the same `pet-1` can mean different companions across saves and devices. | `WorldState.java` `add()`, `encode()`; `ProgressStore.java` `world()`, `restoreCloud()` |
| Species | `family:rarity` strings, e.g. `0:0` = Sparklit/Common.<br>Rarities: Common, Heroic, Mythic, Celestial.<br>Species names: `SpeciesCatalog.NAMES`. | `SpeciesCatalog.java` |
| Forms / evolution | Stored per pet in `Progression`:<br>- `form` 0–5<br>- `branch` 0 = none, 1 = power/A, 2 = speed/B<br>- evolution quests<br>- level = 1+xp/100, capped at 50<br>- XP capped at 4900 | `Progression.java` |
| Nickname | Per pet, 1–24 characters; an empty value falls back to the species name | `WorldState.rename()` |
| Bond | **No bond percentage exists.** Only the "First bond" quest flag `Progression.intro`. | `Progression.quest()` |
| Element | **No element metadata.** Advantage is `(family%3+1)%3`, the same as ZBattle today. | `AdventureState.Battle.move()` |
| Branch palette | Display-only tint for **Branch-B forms**, not for species variants (correction to `ai/ZCUBES_PLAN.md`) | `BranchPalette.java`, `MainActivity` line ~308 |
| Steps | Local SQLite ledger with boot/raw counter acceptance. The server accepts client-reported dated snapshots (`steps_put`) and **flags** values that look suspicious; it does not verify them. | `StepAccounting.java`, `StepStore.java`, `backend/schema.sql` |
| Capture | `WorldState.reveal()` / `capture()`: rarity roll per area stage, catch chance by rarity, pity after 2 failures; one attempt per 2,500 steps | `WorldState.java`, `SpeciesCatalog.java` |
| Local adventure battles | Client-only, settled once (`cleared+1 == number`), saved in `AdventureState`. Not server-verified. | `AdventureState.java` |
| Account | Supabase Auth user UUID (`zpet_profiles.user_id`). Sessions are encrypted with an Android Keystore key. | `CloudClient.java`, `backend/schema.sql` |
| Cloud save | `zpet_saves(user_id, slot∈{normal,test}, revision, payload)`. The whole `WorldState` blob, with optimistic concurrency (`save_put` with a stale revision returns `conflict`). | `backend/schema.sql` `save_put` / `save_get` |
| Server API | Edge function `zpet-api` → service-only RPC `zpet_dispatch(actor, operation, body)`; the actor comes from verified Auth. Operations: profile, save_get/put, steps_put, friends, rivals, invites, battle_*, coop_*, rank_*, competitions, league, dispute, moderation_review, dashboard, improvement. | `backend/index.ts`, `backend/*.sql` |
| Idempotency precedent | Ranked matches use `request_id uuid` with `unique(a, request_id)` | `backend/expansion.sql` |
| Portable backup | `ZPET-SAVE-1` + SHA-256 checksum. The checksum detects corruption only; it is not an authority. | `PortableSave.java` |

## 3. Contract v0.1 vs evidence — gaps to resolve

| # | Contract v0.1 says | Evidence | Consequence |
|---|---|---|---|
| G1 | `companionId` immutable individual UUID | ZPet ids are `pet-N` counters; ZBattle ids are `Long` counters | Both apps need a one-time UUID assignment migration. The ZPet side is owned by the ZPet project. The schema keeps `legacyLocalId` for traceability (see `schemas/`). |
| G2 | `speciesId` = ZPet canonical `family:rarity` | ZBattle stores form-name ids (`sparklit`) with no rarity | ZBattle native companions must record `speciesId`. Today every ZBattle-owned creature is a starter (rarity 0), so the mapping is exact (`sparklit` → `0:0`). |
| G3 | `formId/evolutionBranch` | ZPet: `form` 0–5 + `branch` 0/1/2; no string form ids | Contract v0.2 uses `formIndex` 0–5 + `branch` `none`/`A`/`B`, both catalogue-backed. Form names stay display-only (`Pyre Sovereign` contains a space). |
| G4 | `bondPercent` / `bondRevision` from ZPet | Does not exist | The bond producer is ZPet Phase D work. Until then imported snapshots carry `bondPercent: null` and ZBattle applies **no** bond bonus. |
| G5 | `elementId` from approved metadata | No element data in either app | `elementId` stays nullable and its value set is pending. ZBattle keeps the family-modulo advantage until elements are approved (decision D-ELEMENT). |
| G6 | Origin multiplier formula | — | Integer basis-point maths in v0.2: `1000 + floor(b/25)*250`, capped at 2000 bp. The 10% base applies even at 0% bond; confirm (D-ORIGIN-ROUNDING). |
| G7 | Battle events verified | No server verification of local battles or steps exists | Every client event is **claimed**, not verified, until Phase D authority exists. The schema has `verification: "unverified-client" \| "server-settled"`. |
| G8 | accountId | Supabase Auth UUID in ZPet; ZBattle has no accounts | ZBattle account linking, and whether it reuses the ZPet Supabase project, is a Phase D decision (D-BACKEND). No credentials are shared between APKs. |

## 4. Active PRs and owners

Listed from git refs (`git ls-remote`). The GitHub API session was unavailable during the audit, so open/closed state comes from the last known reports.

| Repo | PR | Branch | Owner | State / note |
|---|---|---|---|---|
| ZBattle | #1 | `ai/codex/zbattle-foundation` | Codex | Superseded by CLAUDE-001 (#2); user may close |
| ZBattle | #3 | `ai/chatgpt/claude-battle-vertical-slice` | ChatGPT | Content merged via #5; user may close |
| ZBattle | #4 | `ai/chatgpt/artwork-handoff` | ChatGPT | Content merged via #6; user may close |
| ZBattle | #8 | `ai/chatgpt/cross-app-roadmap` | ChatGPT | Open; this Phase A branch is built on it |
| ZPet | #1 | `signing/permanent-key-2026-10-09` | ZPet owner | Not touched |
| ZPet | #2 | `ai/codex/ai-workspace` | Codex / ZPet | Not touched |
| ZPet | #3 | `ai/codex/ai-003-artwork-archive` | Codex / ZPet | Not touched |

## 5. Corrections to earlier ZBattle docs
- `ai/ZCUBES_PLAN.md`: `BranchPalette` is a Branch-B form tint, not the species-variant colouring. Corrected in this sub-PR.
