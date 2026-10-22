# Configuration defaults and storage

## Purpose

Describe settings actually loaded and saved by the reviewed implementation. Defaults apply to fresh/missing settings; mode presets and migrations can also change derived settings. This page owns configuration behavior, while each feature guide owns its gameplay effect.

## Controls and defaults

Open `/sm` for settings. Independent feature switches shown in the current GUI default on except these:

| Setting | Saved key | Default |
| --- | --- | --- |
| Warp reminder | `warpAlertsEnabled` | Off |
| Coins at insta-sell | `runCoinsInstaSell` | Off (sell-offer valuation) |
| Achievements to party | `achievementsToParty` | Off |
| XP / Essence / Shard floor drops | `esp.rewardDrops` | Off |

Achievements themselves and achievement sound default on. A button's backing field is not an enable switch. Mode selectors are mutually exclusive: fresh configuration selects **Unique** (`fullClearMode=false`, `sparklingMode=false`); they are not an all-on feature group.

Capture projections and automatic ticket selection default on. **Preferred ticket** defaults to **First Class**. Explicit saved feature switches and preferences are preserved; these defaults apply only to missing or invalid settings.

### GUI organization

Related controls use collapsible dropdowns, in this order:

- **Safari helpers:** Safari entry; Capture tools; Biome interactions; Party visibility; Visual cleanup; Detection and alerts. Ticket preference sits below automatic selection; capture width and terrain visibility sit below capture projections.
- **ESP:** Floor drops (reward toggle, biome restriction, individual text sizes and colors); Other text sizes (general labels and party names); Critter biome limits (Forest, Cavern, Icy, Haunted).
- **Customization:** shared selected hex color; Tracking HUDs; Alert HUDs; Highlights and waypoints; Critter colors ordered Forest, Cavern, Icy, Haunted. Each HUD retains its border, colors, and transparency controls.

Moving controls does not rename saved keys. Collapsed groups save their current choices just like expanded groups.

| Numeric/text control | Default | Implemented load validation |
| --- | --- | --- |
| Warp delay | 25 seconds | Parsed integer clamps to 1–86400; missing/unparseable → 25. |
| Capsule path width | 3 | Finite values clamp to 1–5; missing/unparseable/non-finite → 3. |
| Show guide through terrain | On | Strict saved boolean; invalid/missing → on. |
| Flying capsule distance | 2 blocks | Finite values clamp to 0.5–6; missing/unparseable/non-finite → 2. |
| ESP text scale | 2 | Finite values clamp to 0.5–4; missing/unparseable/non-finite → 2; GUI step 0.25. |
| Progress HUD location | Safari and entrance | `showWhere=1`; accepts 0–2. |
| Sparkling alert sound/volume | `minecraft:block.amethyst_block.chime`, 100 | Valid sound identifier; volume 0–100. |
| Run-completion title/sound/volume | `Run Complete`, `minecraft:block.bell.use`, 100 | Nonblank title ≤120 characters; valid identifier; volume 0–100. |
| Contest warnings | 5, 3, 1 minutes | Only these three thresholds are used. |

General boolean loading uses strict `true`/`false` with a field-specific fallback. Contest's automatic downtime switch does likewise; older contest tracking/warning/title boolean readers use `toBoolean()` (an invalid value becomes false). Do not assume every malformed property has the same fallback.

The **Misc Endgame Features** tab includes **Phantomleaf Solver**, default on and saved as `phantomleafSolver`. It is active only in Garden during a Phantomleaf search. See [Phantomleaf Solver](PHANTOMLEAF_SOLVER.md).

## Implemented behavior

### Mode presets and saved choices

Ordinary saved GUI choices are loaded, but presets are explicit exceptions. Mode selection sets timesave filtering/PB category and panel presentation: Full Clear enables the capture panel/mound statistics and disables unique-only counts; Unique/Sparkling do the opposite. Full Clear/Sparkling also enable source counters. These panel flags are mode-owned, not extra independently switchable current-GUI defaults.

Startup and mode changes apply an ESP preset enabling all groups, species and Rockmite mounds, even when older saved `enabled=false` properties exist. Per-biome visibility restrictions and colors remain separate. Startup `syncMode` does not itself reapply every panel preset. See [audit D5](IMPLEMENTATION_AUDIT.md#d5) for the former overbroad saved-choice claim.

### Feature bindings

**Safari helpers → Gemzie gem auto-select** uses the independent saved key `gemzieGemAutoSelect`. It defaults on for missing/malformed values, preserves an explicit off choice, and is not changed by mode presets. It selects a whole hotbar gem for one fresh top-face Gemzie podium interaction; see [Gemzie](critters/gemzie.md#whole-gem-selection-helper). It does not move inventory/offhand items or add completion evidence.

- [Capture projections](CAPTURE_SHOT_RECORDING.md#experimental-capsule-path) has one enable switch, independent of recording and capsule hiding. Missing/invalid saved enable values are on. The obsolete `allowUncalibratedCapsuleEstimates` and `hud.capsule_status.*` keys are ignored and omitted on save; no projection status HUD is offered.
- [Warp reminder](WARP_ALERTS.md) controls both the alert and automatic `/p warp`; changing its switch/delay cancels pending work.
- [Automatic contest downtime](CONTEST_DOWNTIME.md) defaults on independently of HUD/tracking. [Guard Safari entry](SAFARI_ENTRY_GUARD.md) and **!dt downtime** retain independent saved switches and stage-specific behavior.
- History's **History** button precedes **Import History from Logs**. There is no startup-import scan/toggle; old auto-import keys are ignored. See [history, import and achievements](HISTORY_IMPORT.md).
- **Missing / Unchecked panel** shares `missingPanel` and its HUD layout across modes. Sparkling labels it **Unchecked** for every biome; Unique and Full Clear use Missing.
- Automatic unused starting-food/coin reminders have no independent toggle; see [tracking](critters/TRACKING.md).

### ESP appearance

Other ESP text scale applies to critter timers, nest labels and unknown sparkling labels. Each floor-drop type and party names have independent scales; teammate names also retain distance scaling. Scale 1 restores original label size.

**ESP → Floor-drop colors** offers Lime Gem, Purple Gem, Orange Gem, Wriggleworm, Yogi Berry, Bag of Seeds, Soothing Incense, Shining Coin and Icebreaker. Each changes outline, fill and label. Per-type `esp.floor.<type>.color` values fall back to the saved shared color (fresh green). **Customization → All floor-drop colors** deliberately changes every type; merely applying an unchanged shared value preserves individual choices. Generic drops remain unhighlighted and colors do not change eligibility.

## Lifecycle and persistence

| File under `config/` | Owner/data |
| --- | --- |
| `sparkling-mutuals.properties` | General settings, API preference/key, ESP appearance and HUD layouts. |
| `sparkling-mutuals-contest.properties` | Contest HUD, warnings and automatic downtime. |
| `sparkling-mutuals-pbs.properties` | Boss solo/duo, biome Full Clear/Unique, party Unique, observed-member records and selected biome category. |

General/PB writes use temporary replacement and atomic moves where supported. Contest saves use their own properties writer. Settings controls apply booleans/presets and save text fields through the settings screen lifecycle; independent feature state such as running timers or recording is not restored by setting persistence.

### Personal best storage and migration

Startup normalizes legacy `pb.*` keys from the general file and the earlier `Sparkling Mutual's PBs.properties` filename into the dedicated PB file. Older unrestricted boss keys retain Duo meaning. Valid duplicate times keep the faster result; dedicated-file metadata takes precedence. The destination is written before legacy PB keys or the earlier filename are removed. Failed writes preserve legacy sources for retry; unrelated general properties survive migration. Repeated startup does not reset PBs.

## Edge cases

- A missing default does not authorize overwriting an ordinary explicit saved choice; the documented presets are exceptions.
- Mutually exclusive mode flags, derived panel flags, GUI buttons and independent feature switches have different meanings.
- Appearance clamps do not calibrate capture physics or alter mode completion.
- Do not treat a successful settings save as live gameplay validation.

## Source and validation references

- Load/save/defaults and initialization: [ConfigManager](../src/main/kotlin/net/johnceo/sparklingmutuals/config/ConfigManager.kt), [ContestConfig](../src/main/kotlin/net/johnceo/sparklingmutuals/config/ContestConfig.kt), [SparklingMutuals](../src/main/kotlin/net/johnceo/sparklingmutuals/SparklingMutuals.kt).
- GUI/apply/lifecycle: [SafariSettings](../src/main/kotlin/net/johnceo/sparklingmutuals/config/SafariSettings.kt), [SafariConfigScreen](../src/main/kotlin/net/johnceo/sparklingmutuals/config/SafariConfigScreen.kt); presets: [SafariFullClear](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariFullClear.kt); migration: [PersonalBestConfig](../src/main/kotlin/net/johnceo/sparklingmutuals/config/PersonalBestConfig.kt).
- Appearance: [SafariEspConfig](../src/main/kotlin/net/johnceo/sparklingmutuals/config/SafariEspConfig.kt), [SafariEspSettings](../src/main/kotlin/net/johnceo/sparklingmutuals/config/SafariEspSettings.kt); HUD: [SafariHud](../src/main/kotlin/net/johnceo/sparklingmutuals/hud/HudLayout.kt).
- Existing secondary coverage: [ConfigTest](../src/test/kotlin/net/johnceo/sparklingmutuals/config/ConfigTest.kt), [SafariSettingsTest](../src/test/kotlin/net/johnceo/sparklingmutuals/config/SafariSettingsTest.kt), [SafariCustomizationTest](../src/test/kotlin/net/johnceo/sparklingmutuals/config/SafariCustomizationTest.kt), [SafariFeatureDefaultsTest](../src/test/kotlin/net/johnceo/sparklingmutuals/config/SafariFeatureDefaultsTest.kt). No tests or live settings session ran in this audit. Reopen/restart, migration failure recovery and preset transitions require their appropriate runtime checks.

## Last updated

2026-10-22 — Grouped Safari helpers, ESP and Customization into related dropdowns. Capture projections and ticket auto-selection now default on; the four explicit opt-outs remain off, First Class remains the default preference, and saved choices are preserved.

2026-10-19 - Added Misc Endgame Features and the persisted Phantomleaf Solver switch (default on).

2026-10-19 (America/New_York) — Integrated the Gemzie whole-gem selection helper from `c4908de` onto master `a1f98be`. Reviewed helper input, item identity, settings and mixin registration; live podium placement remains unverified.


2026-10-19 (America/New_York) — Audited loaded defaults, saved choices, presets, appearance and PB migration against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`. See [documentation conventions](DOCUMENTATION_GUIDE.md).
