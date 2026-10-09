# ZBattle UI layout specification
Updated 2026-10-09 (America/Toronto). Status: implementation brief, not implemented app functionality.

## Authority and boundaries
User requested building the app foundation while ChatGPT finishes design/artwork. This supersedes the earlier blanket deferral in the root README. Existing creature artwork is reused; unfinished creature forms are deferred. ChatGPT handles ALL new image generation. Claude owns the implementation task described in tasks/CLAUDE-001-UI-FOUNDATION.md; do not generate or redesign artwork.
The screenshot-inspired mockups are visual direction, not approved gameplay data. Invented opponent names, prices, levels and totals appearing in mockups are placeholders. Do not turn them into canon.
ZPet remains independent. Preserve the planned one-way ZPet -> ZBattle player import boundary; no reverse writes. Import implementation is outside this UI task.
No new network/auth/backend integration, gameplay economy or release publication in this task.

## Visual references
Two generated boards in the design conversation: five core screens (Home, Collection, Travel, Challenges, Battle); four supporting screens (Shop sheet, Item Shop, Double Battle, Profile). Branding board shows lightning logo, app icon, splash.
Images are NOT committed with this brief. This written specification must be sufficient to proceed; use neutral placeholders for missing scenery. Request screenshot attachments from the user only if pixel comparison is needed. Do not claim pixel-perfect verification without references.
Choose the clean sans-serif typography from the FIRST/core board throughout; the supporting board's serif lettering is an inconsistency to correct.
Colours: background #141720; surface #242834; elevated surface #2D3140; accent #F64F8B; accent-dark #D63A73; primary text #F5F4F7; secondary #BBB7C2; border #414555; success #49B87C. Pink-to-rose header gradients optional. Preserve readable contrast; prefer dark text on pale chips or darken pink for small white-text controls.

## Layout tokens
Native Android portrait baseline: 412dp width; responsive down to 360dp.
Outer padding 16dp, gaps 12/16/24dp, card padding 16dp, card radius 20dp, sheet radius 28dp, compact chip radius 999dp.
System sans serif first; headings 24-28sp bold, section titles 20sp semibold, body 16sp, labels 12-14sp. Respect font scaling.
Controls >=48dp touch target; primary buttons 52-56dp high. No tiny buttons inherited from image boards.
Status/navigation bar insets must be handled. Reserve space for the bottom navigation and raised centre action; no controls cover list items. Header back arrow replaces floating Back to Home buttons.
Use scalable code/native icons for search, sort, filters, back, lock, trophy, shield, music, person and lightning. Buttons, gradients, progress bars and switches are code, not flattened screenshots.
Creature image: contain/fit with alpha preserved; scenery: crop/cover with readable scrim behind overlaid text.
Reusable components: AppHeader, StatChip, PrimaryButton, SectionHeading, CreatureCard, LocationCard, OpponentCard, ProgressRow, ShopItemCard, SettingsRow, EmptyState, ArtworkSlot.

## Navigation
Top-level: Collection | Shop | Home (raised lightning centre action) | Events | Profile. Home is initial destination.
Home -> Travel -> select region/location -> return Home with chosen area.
Home -> Challenges -> challenge confirmation -> Battle preview.
Home -> Double Battle preview.
Shop entry opens category bottom sheet; category -> Item Shop.
Collection -> creature detail sheet.
Profile -> achievements list and grouped local settings.
System Back dismisses sheets/dialogs before popping screens. Maintain selected tab and scroll position where feasible.
Events and unimplemented features show an honest empty/planned state, never fabricated active content.

## Screens
### 1 Home
Rounded-bottom pink header with ZBattle wordmark, player name, compact XP/coin stats.
Large horizontal location carousel, approximately 220-260dp tall, side peeks, region chip, Travel pill, location title, short two-line description over lower scrim and pagination dots.
My Party heading + count. Horizontal creature cards approximately 144dp wide with existing creature art, name, level placeholder/available data, type chip.
Primary Battle action leads to Challenges. Secondary Double Battle route. Keep encounter access near active area/party, not below all region lists.
### 2 Collection
Header, search, sort/filter controls, owned count/progress. Two-column grid, adaptive sizing at larger font scales.
Creature art above name, available type/status chips. Use actual existing assets/catalog mappings. Missing forms are deferred, not new generated art.
Search matches names; sort and filters genuinely affect displayed results. Detail sheet shows available catalog information; unknown stats are omitted.
### 3 Travel
Region selector supporting every ZPet catalogue group; initial viewport may focus on Greek/Norse/Chinese, not an exclusive hardcoded set.
Large map ArtworkSlot followed by four area rows for selected catalogue group. Selection highlight, active indicator, lock/progress state.
Pinned footer Travel Here with reserved list padding. Locked tap explains requirements; no silent navigation.
Location selection independent of creature family. Do not infer creature locking from matching mythology.
### 4 Challenges
Area subtitle, compact location header, opponent cards with portrait slot, party preview, completion badge and Challenge/Rematch.
Distinct Region Boss card. Steps and boss-win requirements are separate indicators where applicable.
For shell, use clearly marked preview data and no actual unlock/reward mutations. Thresholds and exact boss roster require later gameplay task.
### 5 Battle preview
Full-width scenery slot, opponent/player art, name/health rows, turn indicator, party thumbnails.
Bottom action grid Attack / Skill / Switch / Retreat. Render explicit preview status while battle engine is absent; do not fake persistent victories or rewards.
Retreat exits with confirmation. All visual health/state values are preview data.
### 6 Shop category sheet
Dimmed Home behind rounded sheet with handle/title and three rows: Equipment, Consumables, Cosmetics. Icons, subtitle, chevron.
Dismiss by back/scrim. Category names are provisional UI grouping, not commitment to unapproved economy items.
### 7 Item Shop
Header/back, coin stat, search, filter/category chips; compact stacked cards with item art slot, name, category, price and Buy.
No real purchases until approved economy exists. Preview items explicitly marked demo; action explains unavailable purchase. Preserve screen structure without hardcoding illustrative prices as production data.
### 8 Double Battle preview
Area header, concise two-per-side information card, challenger portrait slot and reward preview, 3x2 opposing lineup, party status, Challenge.
Use existing ZPet creature images for every creature thumbnail. Never create additional roster entries for mockup-only stone rams/stags etc.
### 9 Profile
Trainer card with editable local display name, avatar slot and available stats.
Region progress tabs/chips and four medallion slots; achievements route; grouped switches Music, Dark Mode, Battle Animations; account placeholder only if needed.
Local settings must persist; toggles must not promise effects without supporting functionality. Persist visual mode; label unsupported music/animation as planned until implemented. No cloud login required.

## Existing creature source
ZBattle-ZPet-Assets/manifest.json lists 54 monster PNGs and five Java reference files. Paths are relative to ZBattle-ZPet-Assets/. Read reference/MonsterCatalog.java, SpeciesCatalog.java and CreatureView.java before mapping entries; some variants reuse family artwork. Do not assume 72 forms means 72 images.
Preserve all supplied PNGs unchanged and their original designs. Inventory all existing images; expose created creatures where valid catalog mappings exist. Missing mappings must be recorded, not silently substituted or assigned invented names.
Reference Java files retain ZPet package/dependencies and are not standalone app code.
A proposed Android source stack is Kotlin + Jetpack Compose if there is still no app skeleton; inspect current main before scaffolding, and adapt if another implementation appears.

## Asset integration contract
Create a single art lookup keyed by stable IDs; layouts must not hardcode generated-image filenames.
Logical groups: branding/logo, branding/icon, branding/splash; region/{group}/map; location/{area}/hero; location/{area}/battle; boss/{id}; opponent/{id}; item/{id}; badge/{id}; avatar/{id}; event/{id}.
Missing new art uses a restrained themed gradient + native icon and correct text. All final new illustrations will come from ChatGPT. Swapping assets later must not require changing screen layout code.
Runtime may package files under app/src/main/assets/art/ (proposed, Claude may align with chosen stack). Generate no fake finished art, and do not extract assets from the UI boards.
Load creature files from the verified existing collection into runtime assets, retaining provenance.

## Exact region/location snapshot
Source: Zeus97x/ZPet app/src/main/java/com/zeus97x/zpet/RegionCatalog.java on main, retrieved 2026-10-09; blob cfb3b696838245a37c359622965ef9caabac41fa. Refresh/check changes before implementation. areaIndex 0..47; groupIndex=areaIndex/4; stage=areaIndex%4. Traditions are not unique IDs: Egyptian and Greek repeat. Preserve distinct group IDs.
| Group | Tradition | Ordered locations |
|---|---|---|
| 0 | Greek | Olympian Foothills; Thunderpeak; Underworld Gates; Elysian Horizon |
| 1 | Norse | Yggdrasil Roots; Rune Ruins; Frostbound Fjord; Aurora Citadel |
| 2 | Chinese | Jade Forest; Celestial Peaks; Dragon Palace; Heavenly Gate |
| 3 | Egyptian | Desert Crossing; Golden Necropolis; Veiled Dunes; Guardian Horizon |
| 4 | Japanese | Lantern Path; Mirror Grove; Foxfire Shrine; Dawn Sanctuary |
| 5 | Mesoamerican | Feathered Canopy; Wind Terrace; Jade Garden; Skywoven Summit |
| 6 | Egyptian | Dawn Sands; Solar Orchard; Halo Oasis; Sunrise Vault |
| 7 | Greek | Foaming Shore; Coral Passage; Abyssal Reef; Crest Horizon |
| 8 | Chinese moon folklore | Moonlit Meadow; Crescent Garden; Jade Moon Terrace; Lunar Sanctuary |
| 9 | Greek phoenix inspiration | Ash Nest; Cinder Ridge; Dawn Roost; Rebirth Summit |
| 10 | Akan storytelling inspiration | Story Grove; Riddle Crossing; Silk Canopy; Taleweaver Haven |
| 11 | Celtic-inspired fantasy | Moss Trail; Briar Grove; Elder Woodland; Bloom Sanctuary |

## Definition of done for initial shell
Buildable Android source, all nine layouts/routes accessible, consistent theme and safe insets, all existing creature assets inventoried/reused, all 48 exact areas represented without conflating duplicate traditions.
No generated artwork, invented final gameplay roster, secret material, reverse ZPet writes, auto-merge or public release.
Verify compile and navigation; inspect portrait layouts at 360dp and 412dp and large text if emulator/render available. Record unavailable checks honestly. Screenshots of implementation belong in the PR where possible.
User will review design after each artwork phase; Claude implementation can proceed with placeholders independently.
