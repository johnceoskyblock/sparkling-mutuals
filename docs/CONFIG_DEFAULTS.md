# Configuration defaults and storage

## Purpose

Describe settings actually loaded and saved by the reviewed implementation. Fresh installations load a curated preferences profile. Existing files retain their saved values and use the original field fallbacks for missing keys; mode presets and migrations can also change derived settings. This page owns configuration behavior, while each feature guide owns its gameplay effect.

## Controls and defaults

On joining Hypixel (including Alpha), a short local welcome appears once the player/world is ready and two seconds have elapsed. It explains `/sm`, `/sm help`, Unique/Full Clear/Sparkle, HUD editing and history. It is shown once per connection, not on ordinary lobby/world transfers or respawns, and may appear again after disconnecting and reconnecting. Hypixel is recognized by its server hostname or explicit scoreboard address. The welcome sends no party messages, commands or API requests.

`/sm help` prints the existing mode-aware local and party command list in local chat. The same list remains available through General → Command help.

The welcome is one compact MOTD-style block with a gold heading/dividers and light body text, without repeated `[SM]` prefixes. Commands are gold, clickable, and show explanatory hover text. Ordinary local feedback retains its usual prefix.

Open `/sm` for settings. Fresh installs use [bundled main defaults](../src/main/resources/defaults/sparkling-mutuals.properties) and [contest defaults](../src/main/resources/defaults/sparkling-mutuals-contest.properties), copied from the author’s chosen settings. Most feature switches are on, with these notable preferences:

| Setting | Saved key | Default |
| --- | --- | --- |
| Sparkle Warping | `warpAlertsEnabled` | Off |
| Coins at insta-sell | `runCoinsInstaSell` | Off (sell-offer valuation) |
| Achievements to party | `achievementsToParty` | Off |
| XP / Essence / Shard floor drops | `esp.floor.rewards.enabled` | On, current biome only |
| HUD borders | `appearance.<panel>.borderEnabled` | Off |
| Five-minute contest warning | `contestWarnMinutes` | Off; warnings at 3 and 1 minutes |

Achievements themselves and achievement sound default on. A button's backing field is not an enable switch. Mode selectors are mutually exclusive: fresh configuration selects **Unique** (`fullClearMode=false`, `sparklingMode=false`); they are not an all-on feature group.

Capture projections and automatic ticket selection default on. **Preferred ticket** is **Basic** for fresh installs. Existing files preserve their preferences and retain First Class as the older missing/invalid-field fallback. The curated profile is applied only when the corresponding main or contest config file does not exist, including when a user explicitly deletes that file.

### Fresh-install profile and portable HUDs

Only explicit settings and appearance/layout properties are bundled. The main profile omits API keys and all `pb.*` properties. History, sparkling history, PB/stat files, achievements, account IDs, playtime, and honeyhive timers are never seeded from the author’s files. Main and contest files are checked independently; an existing file is loaded without merging the curated profile or rewriting it as part of applying defaults.

Fresh HUD layouts, including the contest HUD, use normalized positions and a 640×360 logical reference canvas (1920×1080 at GUI scale 3). Their effective text/panel scale follows the smaller viewport ratio, keeping physical layout consistent as GUI scale changes. Oversized adaptive panels shrink to fit and all positions clamp to the screen. The same calculation is used for rendering, previews and drag hitboxes; reference dimensions persist when saving. Very small windows can reduce text readability, and different aspect ratios retain normalized placement rather than identical pixel spacing.

Older HUD layouts without reference dimensions retain their fixed GUI-size behavior. Legacy contest pixel positions remain until the user edits them; opening the editor converts their current placement to normalized coordinates. **Reset positions** explicitly restores the bundled author layout, including adaptive sizing, without resetting feature settings.

### GUI organization

Related controls use collapsible dropdowns, in this order:

- **Safari helpers:** Safari entry; Capture tools; Biome interactions; Party visibility; Visual cleanup; Detection and alerts. Ticket preference sits below automatic selection; capture width and terrain visibility sit below capture projections.
- **ESP:** Floor drops (reward toggle, per-type outside-biome visibility, individual text sizes and colors); Other text sizes (general labels and party names); Critter biome limits (Forest, Cavern, Icy, Haunted).
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
| Contest warnings | 3, 1 minutes for fresh installs | Only 5, 3 and 1 are supported thresholds; older missing fields retain 5, 3, 1. |

General boolean loading uses strict `true`/`false` with a field-specific fallback. Contest's automatic downtime switch does likewise; older contest tracking/warning/title boolean readers use `toBoolean()` (an invalid value becomes false). Do not assume every malformed property has the same fallback.

The **Misc Endgame Features** tab includes **Phantomleaf Solver**, default on and saved as `phantomleafSolver`. It is active only in Garden during a Phantomleaf search. See [Phantomleaf Solver](PHANTOMLEAF_SOLVER.md).

## Implemented behavior

### Mode presets and saved choices

Ordinary saved GUI choices are loaded, but presets are explicit exceptions. Mode selection sets timesave filtering/PB category and panel presentation: Full Clear enables the capture panel/mound statistics and disables unique-only counts; Unique/Sparkling do the opposite. Full Clear/Sparkling also enable source counters. These panel flags are mode-owned, not extra independently switchable current-GUI defaults.

Startup and mode changes apply an ESP preset enabling all groups, species and Rockmite mounds, even when older saved `enabled=false` properties exist. Per-biome visibility restrictions and colors remain separate. Startup `syncMode` does not itself reapply every panel preset. See [audit D5](IMPLEMENTATION_AUDIT.md#d5) for the former overbroad saved-choice claim.

### Feature bindings

**Safari helpers → Gemzie gem auto-select** uses the independent saved key `gemzieGemAutoSelect`. It defaults on for missing/malformed values, preserves an explicit off choice, and is not changed by mode presets. It selects a whole hotbar gem for one fresh top-face Gemzie podium interaction; see [Gemzie](critters/gemzie.md#whole-gem-selection-helper). It does not move inventory/offhand items or add completion evidence.

- [Capture projections](CAPTURE_PROJECTIONS.md#controls) has one enable switch, independent of capsule hiding. Missing/invalid saved enable values are on. The obsolete `allowUncalibratedCapsuleEstimates` and `hud.capsule_status.*` keys are ignored and omitted on save; no projection status HUD is offered.
- [Sparkle Warping](WARP_ALERTS.md) controls both the alert and automatic `/p warp`; changing its switch/delay cancels pending work.
- [Automatic contest downtime](CONTEST_DOWNTIME.md) defaults on independently of HUD/tracking. [Guard Safari entry](SAFARI_ENTRY_GUARD.md) and **!dt downtime** retain independent saved switches and stage-specific behavior.
- History's **History** button precedes **Import History from Logs**. There is no startup-import scan/toggle; old auto-import keys are ignored. See [history, import and achievements](HISTORY_IMPORT.md).
- **Missing / Unchecked panel** shares `missingPanel` and its HUD layout across modes. Sparkling labels it **Unchecked** for every biome; Unique and Full Clear use Missing.
- Automatic unused starting-food/coin reminders have no independent toggle; see [tracking](critters/TRACKING.md).

### Dark interface theme

Settings, HUDs, alerts, the HUD editor, capture-count screen and API-key screen use near-black backgrounds (`#0D0D10`), charcoal controls (`#19191E`, hover `#292930`), light gray body text (`#E6E6EB`) and restrained gold accents (`#C9A75D`). HUD borders default to muted gold (`#605238`). Local chat uses brighter text (`#F1F1F5`) with a light gold `[SM]` prefix (`#E6C878`); explicitly styled feedback keeps its semantic colors. `/sm history` and its Stats tab retain their existing appearance.

Every HUD, including the run-completion alert, defaults to the dark background at 20% transparency. Exact saved old brown backgrounds and old gold borders migrate to the new defaults. The old mint run-completion background and its default 80% transparency migrate to dark at 20%. Other custom colors, chroma, opacity, border switches and layout remain saved as chosen. These migrations are written with the next ordinary config save.

### ESP label appearance

**ESP → Floor drops → Show outside current biome** provides independent switches for every named quest drop and the combined XP/Essence/Shard reward category. Quest switches default on (visible across biomes); the reward category defaults off (current biome only). These switches affect visibility, not whether mode/collection requirements select a drop. Reward ESP itself remains separately off by default.

Save each restriction as `esp.floor.<type>.onlyInBiome`, with the inverse of the displayed outside-biome switch. Existing per-type values are preserved on reload and mode switches. The old shared `esp.floor.onlyInBiome` is superseded, does not control these defaults, and is omitted from new saves. Critter biome restrictions, colors and label sizes remain independent.

Other ESP text scale applies to critter timers, nest labels and unknown sparkling labels. Each floor-drop type and party names have independent scales; teammate names also retain distance scaling. Scale 1 restores original label size.

**ESP → Floor-drop colors** offers Lime Gem, Purple Gem, Orange Gem, Wriggleworm, Yogi Berry, Bag of Seeds, Soothing Incense, Shining Coin and Icebreaker. Each changes outline, fill and label. Per-type `esp.floor.<type>.color` values fall back to the saved shared color (fresh green). **Customization → All floor-drop colors** deliberately changes every type; merely applying an unchanged shared value preserves individual choices. Generic drops remain unhighlighted and colors do not change eligibility.

## Lifecycle and persistence

| File under `config/` | Owner/data |
| --- | --- |
| `sparkling-mutuals.properties` | General settings, API preference/key, ESP appearance and HUD layouts. |
| `sparkling-mutuals-contest.properties` | Contest HUD, warnings and automatic downtime. |
| `sparkling-mutuals-pbs.properties` | Boss solo/duo, biome Full Clear/Unique, party Unique, observed-member records and selected biome category. |

General/PB writes use temporary replacement and atomic moves where supported. Contest saves use their own properties writer. Settings controls apply booleans/presets and save text fields through the settings screen lifecycle; independent feature state such as running timers is not restored by setting persistence.

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

## Mode visuals and automatic reports

Each mode dropdown has independent Hide critter ESP and Hide floor-drop ESP toggles, both off by default. They gate rendering only, including retained critter markers, and never disable scanning or requirements. Turning on either hide toggle disables that mode's profitable-critter ESP (Unique/Sparkle). Party Interactions > Auto biome reports defaults on and controls automatic fd/cd/id/hd messages without disabling progress tracking.

## Last updated

2026-10-31 — Warp List starts blank for every critter on fresh installs. Existing saved lists remain unchanged.

2026-10-31 — Leech defaults on; default whitelist: `JohnCEO _cj Rqach`. Applies when the corresponding settings are absent; explicit saved values remain unchanged.

2026-10-31 — Added an empty-by-default Leech invite whitelist; `/leech` only accepts approved IGNs. Warping pauses new run entry until the selected contest ticket is earned.

2026-10-30 — Hotspot announcer toggles now live under Party Interactions → Hotspot announcer. Existing per-mode keys and off defaults are preserved; each toggle controls actual-hotspot chat plus assigned-biome alerts.

2026-10-30 — Sparkle Warping uses a 25-second fresh/reset delay, Solo leader mode, empty Duo runner and Leech off. Existing saved delays stay unchanged.

2026-10-29 - Fresh installations seed the maintainer-supplied JohnFounder Warp List (115 ordered name assignments). Existing configuration files keep their saved or empty lists; Auto-party remains off by default.

2026-10-29 — Removed development shot recording controls and offline tooling; production projection settings remain supported.

2026-10-29 — Added the enabled-by-default Doomspiral throw HUD. Emergence delay defaults to 98 server ticks with a 4-tick throw lead; both are saved and adjustable.

2026-10-29 — Sparkle Warping defaults to a 23-second delay. Auto-party defaults off; Warp List names are saved in the main properties file. Existing saved delays remain unchanged.

2026-10-28 - Added mode-specific visual ESP hides, an auto biome report setting, and non-redundant Icy announcements.


2026-10-28 - Water opacity defaults to 60%. Announce starting quest items defaults off. Saved settings take precedence over these defaults.


2026-10-27 — Shortened config descriptions, removed repeated help for obvious controls, and renamed the profitable ESP label. The connect message names each mode without explaining it. Saved keys, defaults and behavior are unchanged.

2026-10-27 — Renamed Party commands to Party Interactions and added an independent Announce starting quest items toggle, enabled by default for existing and fresh installs unless explicitly disabled. ESP now owns all floor-drop color controls and the persisted party member highlight/name color (green by default); the duplicate bulk floor-drop color editor in Customization was removed without changing saved colors.

2026-10-26 — Bundled sanitized author preferences for fresh installations only. Added adaptive HUD reference sizing and normalized contest placements; preserved existing config files and legacy layout behavior.

2026-10-25 — Replaced the shared floor biome restriction with independent per-type visibility switches. Quest drops default to cross-biome visibility; XP/Essence/Shard defaults to current-biome only.

2026-10-25 — Restyled the welcome as a single MOTD block with clickable commands and no repeated prefixes; connection timing is unchanged.

2026-10-25 — Added a once-per-Hypixel-connection local welcome and `/sm help` for the command list.

2026-10-24 — Applied a near-black and charcoal theme with subtle gold accents and brighter local chat. Migrated old default HUD colors; preserved custom choices and the History/Stats GUI.

2026-10-22 — Grouped Safari helpers, ESP and Customization into related dropdowns. Capture projections and ticket auto-selection now default on; the four explicit opt-outs remain off, First Class remains the default preference, and saved choices are preserved.

2026-10-19 - Added Misc Endgame Features and the persisted Phantomleaf Solver switch (default on).

2026-10-19 (America/New_York) — Integrated the Gemzie whole-gem selection helper from `c4908de` onto master `a1f98be`. Reviewed helper input, item identity, settings and mixin registration; live podium placement remains unverified.


2026-10-19 (America/New_York) — Audited loaded defaults, saved choices, presets, appearance and PB migration against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`. See [documentation conventions](DOCUMENTATION_GUIDE.md).

Successful profile/discovery reads and IGN-to-UUID lookups are reused for 60 minutes, including after repartying. Simultaneous reads share one request. Failed lookups are not cached; background failures keep their one-minute retry. Observed catches update participating players’ discovery lists locally. Current mode discovery baselines remain cached while members are active or reserved for warping. Ticket and Honeyhive queries bypass the hour-long profile cache and retain the existing short transport-cache freshness. No periodic request is added by cache expiry. Account/provider/Alpha scope changes prevent cross-scope profile reuse.
