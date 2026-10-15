# Config GUI defaults

Implemented defaults for fresh or missing settings. Enable all independently switchable GUI features by default **except**:

| Setting | Default |
| --- | --- |
| Coins at Insta Sell (`runCoinsInstaSell`) | Off |
| Achievements to Party (`achievementsToParty`, including any equivalent achievement-to-party toggles) | Off |

Keep achievements themselves enabled; the exception is their party announcement. These are defaults for fresh/missing settings, not permission to overwrite existing saved user choices on every launch. Automatic unused starting-food/coin title alerts and party reminders have no config toggle, as documented in [tracking rules](critters/TRACKING.md#unused-starting-foodcoin-reminders).

Mode selectors remain mutually exclusive; “all on” applies to independent feature toggles, not simultaneous Unique/Full Clear/Sparkling modes. Retain established numeric/text selections and explicit mode-preset behavior unless separately changed. Field initialization, config-load fallbacks and GUI defaults agree. Explicitly saved GUI choices remain in effect.

## ESP label size

The ESP tab offers **ESP text scale**, from **0.5 to 4** in **0.25** steps. The new default is **2** (twice the original label size); **1** restores the original size. It controls floor-drop names, critter timers, nest labels and teammate names. Teammate names retain their existing distance scaling. Labels stay centered and retain their colors and visibility settings. Changes apply through the GUI and persist across restarts as `esp.textScale`; invalid/non-finite values use the default, and finite values are clamped to the allowed range.

## Last updated

2026-10-15 - Added ESP text scale in the ESP tab: default 2, range 0.5–4, saved across restarts.
