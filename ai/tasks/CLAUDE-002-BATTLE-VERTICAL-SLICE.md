# CLAUDE-002: Battle vertical slice, ZCubes planning and Android inset fixes

Updated: 2026-10-09 America/Toronto  
Repository: Zeus97x/ZBattle  
Recommended owner: Claude  
Status: IN PROGRESS — claimed by Claude 2026-10-09 ~21:00 America/Toronto; branch claude/zbattle-battle-vertical-slice (based on PR #3 head 5b15a62, which is main 2051a4b + this task's docs)  
Priority: High  
Base branch: main  
Suggested branch: claude/zbattle-battle-vertical-slice  
Do not merge automatically.

## Current app state

ZBattle currently has the app shell, screens, navigation and local saving. Everything that makes it a real game is still a labelled placeholder.

The merged foundation in PR #2 provides:
- Kotlin + Jetpack Compose Android app.
- Region/location/challenge/profile/shop/event screens.
- Local saving for settings, Pet Master, current location and visited areas.
- Existing ZPet creature art reused from this repository.
- Battle preview screen layout.

The Battle screen is not a game yet:
- HP bars are fixed preview UI.
- Attack/Skill do not apply damage.
- Switch/Retreat are layout controls only.
- Win/lose/results are not implemented.
- Challenges use preview opponents and placeholder boss/progress thresholds.

## User-reported test fixes from first device install

Fix these before or alongside the vertical slice if they still reproduce on main:

1. Android navigation bar overlap
   - Battle action controls are sitting under the Android system navigation bar on the user's phone.
   - Apply correct window inset/safe-area padding to bottom controls.
   - Attack, Skill, Switch and Retreat must be fully visible and tappable.
   - Apply the same bottom inset treatment to sticky bottom navigation/actions where relevant.

2. Bottom center lightning button shape
   - The center lightning action in bottom navigation should render as a clean full circle.
   - It should not be clipped, flattened or hidden by the nav bar or screen edge.
   - Keep the current pink lightning style.

## Feature name decision

The catching tool/item is called **ZCubes**.

Add ZCubes to docs/planning as a future catching-system feature:
- Needs a custom visual design from ChatGPT.
- Needs multiple tiers.
- Suggested placeholder tiers: Basic ZCube, Great ZCube, Ultra ZCube, Mythic/Legendary ZCube.
- Do not implement catching logic unless it is required for the approved battle vertical slice.
- If catching is needed in the vertical slice, use a minimal placeholder and clearly mark art/tier balancing as future work.

## Explicit deferral

Double battles are deferred. Do not include double battles in this task. Do not build two-versus-two rules, double battle UI, or double battle progression yet.

## Main objective

Build the first real gameplay vertical slice:

One area, one opponent, a real turn-based fight, and a saved result.

Focus on core gameplay dependencies in this order:
1. Creature stats and levels.
2. Battle engine.
3. Saving battle progress.

Everything else should remain documented or stubbed unless it is needed to make the vertical slice work.

## Baseline instructions

Use ZPet as a baseline where useful:
- Check ZPet's catalogue/area data if accessible in this repo or linked source.
- Reuse existing creature names, families, stages, rarity/catch concepts and region/location structure when available.
- Do not copy unfinished or incompatible systems blindly.
- ZBattle progress must remain separate from ZPet.
- Do not write back to ZPet.
- Do not depend on ZPet being installed unless a future import feature explicitly supports it.

If ZPet already has battle-like stats or encounter rules, document what was reused. If not, create a simple provisional ZBattle stat model that can be expanded later.

## Scope to implement now

### 1. Creature stats and levels

Create a simple battle stat model for the starter/opponent creatures used in the slice.

Recommended first version:
- Level.
- Max HP.
- Attack.
- Defense or guard value.
- Speed or turn priority.
- One basic skill per creature or per style.
- XP value awarded by opponent.

Rules should be simple and readable. Prefer data-driven definitions that can grow into all creatures later.

Acceptance criteria:
- At least the starter and one opponent have real stats.
- HP derives from stats, not fixed UI values.
- Stats are deterministic and saved/loaded correctly where needed.
- The data model leaves room for evolution stages and families.

### 2. Battle engine

Implement a real turn-based battle loop for the first area/opponent.

Minimum behavior:
- Start battle from the challenge/opponent card.
- Load player party and opponent roster.
- Show real HP values/bars.
- Attack applies damage.
- Skill applies damage or an effect.
- Turn order is deterministic and understandable.
- Opponent takes turns.
- Creature fainting is handled.
- Player win/lose is handled.
- Retreat exits the battle without granting victory rewards.
- Switch can remain limited if the current party only has one creature, but the UI should explain that clearly.
- Results screen or results state shows outcome and rewards/progress.

Recommended formula:
- Start simple, for example: damage = max(1, attack + movePower - defense).
- Add small type/stage modifiers only if the data already supports them cleanly.
- Avoid overbuilding elemental systems until the user approves battle rules.

Acceptance criteria:
- A user can complete one real fight.
- Attack/Skill affect HP.
- The fight can end.
- Results reflect win/loss/retreat.
- No duplicate reward claim from re-opening the result.

### 3. Save battle progress

Implement a proper first save format for battle progress.

Minimum saved data:
- Owned creatures.
- Creature level and XP for the starter.
- Completed opponent/result for the first area.
- Area progress for the vertical slice.
- Enough state to recover safely after the app is closed mid-battle.

Recommended approach:
- Version the save schema.
- Keep migration simple.
- Avoid storing secrets or device-specific paths.
- Make duplicate rewards impossible.

Acceptance criteria:
- Closing/reopening the app preserves starter, XP/level and completed first opponent.
- A completed battle does not grant rewards twice.
- Mid-battle recovery is safe: resume, reset to pre-battle, or mark the battle abandoned. Pick one behavior and document it in code/docs.

## Keep documented for later, not implemented unless necessary

### Core gameplay backlog
- Real rosters for all 48 areas and region bosses.
- Region boss requirements.
- Step counts and boss-win requirements.
- Unlock rules for locations and regions.
- More creature acquisition through encounters/catching/ZCubes.
- ZPet catalogue catch chances and per-area rarity rolls.
- XP/coin economy beyond the vertical slice.

### Economy and rewards
- Coins are currently fixed at 0.
- Shop has placeholder disabled items.
- Equipment and consumables need real definitions.
- Rewards must never be grantable twice.

### Pet Master systems
- Pet Master skills and equipment, with all styles having equal access.
- AFK tasks: gathering, crafting and companion expeditions.
- Capped offline progress.
- Pet Master level progression.

### ZPet connection
- One-way import from ZPet to ZBattle.
- Stable pet IDs so re-importing does not reset progress.
- Legacy Bonus with small capped imported-pet bonus.
- Transfer method: file export/import or share intent.

### Extras
- Events tab definitions and timing.
- Achievements and medallions.
- Music and sound; the setting is saved but no audio exists.
- Missing creature art: Ashpeep, Threadbit and Budfawn lines.
- 48 colour-variant species requiring ZPet BranchPalette data.
- Permanent signing key and release build only on Zeus97x's instruction using GitHub Secrets and real-device checks.

## Files to inspect first

Start with:
- `AGENTS.md`
- `DEVELOPMENT_LOG.md`
- `ai/README.md`
- `ai/UI_LAYOUT_SPEC.md`
- `ai/tasks/CLAUDE-001-UI-FOUNDATION.md`
- Current app source under `app/`, `core/`, `ui/`, and `preview/`
- ZPet asset/catalogue files under `ZBattle-ZPet-Assets/`

If ZPet source or relevant battle/catalogue files are available elsewhere in the repository, inspect them before inventing data.

## Build and test requirements

Run the existing Android checks before marking ready:
- Unit tests, if present.
- Lint, if configured.
- Debug assemble/build workflow.
- Manual device check when possible for nav bar insets and battle controls.

Document:
- Commands or workflow runs used.
- Any failing checks.
- What was tested on device.
- Known limitations.

## Completion instructions

When done:
1. Open a dedicated PR from `claude/zbattle-battle-vertical-slice` to `main`.
2. Do not merge automatically.
3. Update `DEVELOPMENT_LOG.md`.
4. Update `ai/README.md` and task status.
5. Include a clear completion report:
   - Gameplay implemented.
   - Save behavior.
   - Battle rules chosen.
   - Device/UI verification.
   - Remaining placeholders.
6. Note whether ChatGPT should design ZCube visuals next.

## Completion report (Claude, 2026-10-09)

### Gameplay implemented
- **Stats and levels** (`core/.../battle/Stats.kt`):
  - Form base stats come from ZPet `Progression.stats()`.
  - Level is 1 + XP/100, capped at 50 (ZPet).
  - Max HP is 45 + 3×level + guard (ZPet).
  - Provisional per-level growth: +1 power per 3 levels, +1 guard per 4, +1 speed per 5. ZPet grows stats through step-funded training instead.
  - Opponent stats follow ZPet's area-stage formula. Each family has one signature skill (ZPet names) with Burn or Weaken.
- **One real opponent** (`Encounters.kt`): Wild Voltmaw, Lv 2, slot 0 of Olympian Foothills.
  - It is chosen by ZPet's encounter rule: the young form of the area's region family.
  - It awards 60 XP on the first win only (ZPet amount).
  - Every other opponent card stays a labelled preview.
- **Battle engine** (`BattleEngine.kt`): deterministic and pure (every move returns new state); adapted from ZPet `AdventureState.Battle`.
  - Attack and Skill rules:

    | Action | Damage | Extra |
    |---|---|---|
    | Attack | max(2, power+5−guard/2) | — |
    | Skill | max(3, power+9−guard/2 ± family advantage) | 3-turn cooldown; Burn or Weaken for 3 turns |

  - Turns and endings:
    - The opponent lands a heavy strike (+5) every third turn, and the screen shows that intent in advance.
    - The faster creature acts first.
    - A battle ends when either side faints. Reaching 50 turns counts as a defeat.
  - Retreat ends the battle with no rewards.
  - Switch explains that only one companion exists.
  - Guard and Potion from ZPet are not included. They are not in the approved action grid, and potions need the economy task.
- **Results screen:** Victory, Defeat or Retreat, with XP gained, level change and an XP bar.

### Save behaviour
- **Format** (`BattleProgress.kt`, `BattleProgressCodec.kt`):
  - Versioned binary schema v1, stored as Base64 in SharedPreferences `zbattle.settings.v1` → `battleProgress`.
  - Holds owned creatures (uid, species, XP), encounters already beaten, win counts, the active battle and the last result.
- **Unreadable data** is moved to `battleProgress.unreadable` and never deleted. The player keeps going with fresh progress, and their starter is re-granted.
- **Mid-battle recovery is RESUME.** The battle is saved after every turn. Reopening the app goes straight back into the battle.
- **No double rewards:**
  - Rewards are applied in the same state change that ends a battle, and only if that battle is still the active one.
  - Reopening or acknowledging a result cannot pay again.
  - Only the first victory against an encounter pays XP. Rematches are practice until step-funded encounters or ZCubes exist.
- **Starter:** existing CLAUDE-001 saves are migrated by granting the chosen starter once.

### UI fixes from the device test
- **Battle controls:** they now pad for the system navigation bar (`WindowInsets.navigationBars`). The bottom bar is hidden in battle, so nothing else did that.
- **Home button:** the centre lightning action is drawn above the bar's `Surface`, which had been clipping its top. It is now a full circle with a surface-coloured ring.
- **Large text:** at 1.3× font the action buttons stack the icon above the label.

### Not done / deferred
- Double battles. The screen now says "deferred".
- Catching. ZCubes are planned only; see `ai/ZCUBES_PLAN.md`.
- Opponent rosters for all 48 areas and the region bosses.
- Steps and unlock rules.
- Economy and coins.
- Switching between multiple creatures.
- Guard and Potion actions.
- Evolution in ZBattle.

### Verification
- See `DEVELOPMENT_LOG.md` (CLAUDE-002 entry) and the PR's Actions run.
- No device test by Claude: there is no device access from this environment.

### Next for ChatGPT
Design the ZCube tiers (art keys in `ai/ZCUBES_PLAN.md`). Battle scenery for Olympian Foothills (`location/area-00/battle`) would also make the slice look finished.
