# Config GUI defaults

Implemented defaults for fresh or missing settings. Enable all independently switchable GUI features by default **except**:

| Setting | Default |
| --- | --- |
| Warp reminder (`warpAlertsEnabled`, `/alert`) | Off |
| Coins at Insta Sell (`runCoinsInstaSell`) | Off |
| Achievements to Party (`achievementsToParty`, including any equivalent achievement-to-party toggles) | Off |
| Capture projections (`experimentalCapsulePath`) | Off |

Keep achievements themselves enabled; the exception is their party announcement. Capture projections is separately opt-in under Safari helpers and does not start recording. Missing/invalid saved preview values default off; explicit choices persist. Capsule hiding and the visual preview work independently; either or both hiding options can stay on. See its [scope and controls](CAPTURE_SHOT_RECORDING.md#experimental-capsule-path). These are defaults for fresh/missing settings, not permission to overwrite existing saved user choices on every launch. Early starting-resource synchronization and later local-only unused starting-food/coin title alerts have no config toggle, as documented in [tracking rules](critters/TRACKING.md#unused-starting-foodcoin-reminders).

Mode selectors remain mutually exclusive; “all on” applies to independent feature toggles, not simultaneous Unique/Full Clear/Sparkling modes. Retain established numeric/text selections and explicit mode-preset behavior unless separately changed. Field initialization, config-load fallbacks and GUI defaults agree. Explicitly saved GUI choices remain in effect.

## Personal best storage and migration

Save personal bests separately in `config/sparkling-mutuals-pbs.properties`. This includes solo/duo boss times, biome Full Clear/Unique times, party Unique times, observed party-member records and the selected biome PB category. General settings and HUD layouts remain in `config/sparkling-mutuals.properties`.

Also migrate an existing `Sparkling Mutual's PBs.properties` file to the corrected filename, retaining faster records if both exist and removing the former file only after saving successfully.

On startup, automatically migrate legacy `pb.*` entries from the general config. Normalize older boss keys through the existing PB loader so unrestricted legacy times remain Duo records. If both files contain PBs, merge records and retain the faster valid time for each key; existing dedicated-file metadata takes precedence. Write the dedicated file before removing legacy PB entries, preserving unrelated general settings. Replace files through temporary files and atomic moves when supported. A failed destination write leaves the legacy file intact for retry. Repeated startups must not duplicate or reset migrated PBs.

## Missing and Unchecked panels

The existing `missingPanel` toggle and HUD layout are shared across modes. In Sparkling mode, every biome's checklist is named **Unchecked**, including the HUD editor label. Unique and Full Clear retain their Missing behavior. The config option names both uses; renaming does not reset saved positions or visibility preferences.

## Experimental capsule path

**Capture projections** is the only enable switch. Its existing `experimentalCapsulePath` key preserves deliberate saved on/off choices; missing/invalid values default off. The obsolete `allowUncalibratedCapsuleEstimates` key is ignored and omitted on the next save. Enabling the switch gives the full terrain ring/possible-critter diamond guide, including close launch, broader angles, Masterful, movement and water support. It never starts recording. Collision and target proxies remain unverified; strict recorder probes retain their eligibility rules.

Capsule path width saves independently as `capsulePathWidth`, defaults to **3**, and clamps finite values to **1–5**. Invalid/non-finite values use 3. **Show guide through terrain** (`showCapsuleGuideThroughTerrain`) defaults **on**, including for missing/invalid saved values; an explicit off choice persists. These appearance controls do not add another enable gate or change collision stops.

The projection status text and its HUD-editor entry are removed. Legacy `hud.capsule_status.*` properties are ignored and omitted on save; other HUD positions and settings remain unchanged.

## ESP label size

The ESP tab offers **ESP text scale**, from **0.5 to 4** in **0.25** steps. The new default is **2** (twice the original label size); **1** restores the original size. It controls floor-drop names, critter timers, nest labels and teammate names. Teammate names retain their existing distance scaling. Labels stay centered and retain their colors and visibility settings. Changes apply through the GUI and persist across restarts as `esp.textScale`; invalid/non-finite values use the default, and finite values are clamped to the allowed range.

## Floor-drop colors

The ESP tab has a **Floor-drop colors** accordion with separate color pickers for Lime Gem, Purple Gem, Orange Gem, Wriggleworm, Yogi Berry, Bag of Seeds, Soothing Incense, Shining Coin and Icebreaker. Each color controls that drop's outline, translucent fill and name label. The existing **Customization → All floor-drop colors** control changes every type together; applying unchanged shared settings does not overwrite individual choices.

All types initially inherit the old saved floor-drop color (green on a fresh configuration). Per-type choices persist as `esp.floor.<type>.color`; missing/invalid choices fall back to the shared saved color. Generic/unknown drops remain unhighlighted, and changing colors does not alter mode/biome/resource eligibility.

## Automatic contest downtime

**Miria contest → Auto !dt contest** defaults **on**, consistent with the independent-feature default policy, and explicit saved off choices persist. It sends the literal `!dt contest` to party chat once when a new contest window starts while online in SkyBlock with a known party and the confirmed Party Leader role. Members, moderators and unknown leadership do not send automatic requests. It works independently of the contest HUD/tracking toggle. Joining/loading settings, switching profiles, repeated scans and enabling it mid-contest must not send a duplicate or stale request. See [contest downtime](CONTEST_DOWNTIME.md).

## Warp alert action

The **Warp reminder** / `/alert` setting defaults **off** for fresh or missing settings. When enabled, it shows the timer alert **and sends `/p warp`** at expiry in the current Safari run. Explicit saved on/off choices, the default 25-second delay and 1–86400-second range remain unchanged. Disabling the feature or changing the delay cancels a pending timer. See [warp alerts](WARP_ALERTS.md).

## History actions

The History tab begins with the **History** button, followed immediately by **Import History from Logs**. Import is an explicit action that backfills both histories from existing logs. There is no **Import logs on launch** toggle or automatic startup scan: normal sessions already record history live. Older saved auto-import settings are ignored. See [history import](HISTORY_IMPORT.md) for supported files, estimates and merge rules.

## Safari entry protection

**Guard Safari entry** and **!dt downtime** retain their independent saved switches. Guard Safari entry uses Safari tab-list party presence, with a two-second pause after a party kick message; downtime controls Torrhus readiness holds until each requester says r or mentions ticket. Holding Shift overrides either for the current interaction without clearing the holds. See [entry guard](SAFARI_ENTRY_GUARD.md).

## Last updated

2026-10-19 (America/New_York) — Early absolute starting-resource reports at HOTSPOT+2s, explicit unknown coverage and local-only later reminders (issue68).

2026-10-18 (America/New_York) - Consolidated Capture projections into one saved switch and removed the status-text HUD.

2026-10-17 - Aligned entry guards with tab-first Safari presence, two-second kick pauses, ticket-message downtime release and Shift override.

2026-10-17 (America/New_York) - Separated visual capsule-guide availability from capsule hiding; preserved saved options and recorder calibration restrictions.

2026-10-17 - Corrected PB filename to sparkling-mutuals-pbs.properties; also migrate the briefly used display-name filename safely.

2026-10-17 - Moved personal bests to a dedicated file with automatic legacy migration and faster-time merging.

2026-10-17 - Named the Sparkling checklist Unchecked across every biome while retaining shared panel settings.

2026-10-17 - Default warp alerts off for fresh and missing settings while preserving saved choices.

2026-10-16 - Restricted automatic contest downtime requests to the confirmed party leader; retained existing defaults.
2026-10-16 - Clarified independent Safari entry settings and removed the sneak override.

2026-10-16 (America/New_York) - Added the explicit default-off experimental capsule path exception.

2026-10-17 - Documented guide-through-terrain defaults and the independent capsule-status HUD layout.
