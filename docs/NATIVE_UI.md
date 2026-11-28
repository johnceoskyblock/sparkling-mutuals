# Native UI and catch celebrations

## Settings

`/sm` opens the settings workspace. The sidebar contains HUDs, Safari, Party, Highlights in Standard or ESP in Full, History, Misc, and Reset. Search across settings, descriptions, and breadcrumb paths. Navigation and expanded groups are remembered during the game session.

Right-click a HUD in `/sm gui` to open its settings. HUD previews, dragging, resizing, and snapping use the live layout dimensions. Positions carry across compatible GUI scales and editions. [Configuration and HUD defaults](CONFIG_DEFAULTS.md)

**Share Config** next to **Done** copies settings and HUD layouts as a Mutuals share code. Use **Paste code** in another instance to review the number of compatible settings, then **Apply settings**. Previous settings are backed up locally. API keys, linked accounts, Warp Lists, and Safari records are excluded.

## HUD symbols

Biome, bird, food, incense, and gem icons use compact pixel sprites. Progress icons reflect existing completion state. The display itself creates no completion evidence.

## Player Warp List

Both Full editions have the **Party → Sparkle Warping** settings. The Warp List stores per-player critter selections and priorities. CSV imports add players and update priorities for existing entries. [Sparkle Warping and Warp List](WARP_ALERTS.md)

## Confirmed catch celebration

The **Unique sparkling catch alert** uses server-confirmed sparkling catch evidence and the local collection state. Enable **Celebrate duplicate sparklings** to include duplicates. These controls are separate from the alert for spotting a sparkling critter. Sound previews can play with settings open. Use **Preview catch celebration** to see the visual.

**Build a Sparkling celebration** opens the editor for particle effects, appearance, music, and duration. Preview a draft, save named presets, or share the draft as a celebration recipe. **Save & use** selects **Custom** in the existing style dropdown. The built-in styles and catch trigger settings remain available.

## Update status

Each edition has a **What’s New** button that reads the bundled catalog and displays entries for the current edition. Acknowledgement is stored independently per edition. Full also offers an update-status screen with a verified **Download Update** action and adjustable **Auto Update**. Installation happens after Minecraft closes. [Editions](../README.md#installation)

The settings workspace, icons, and catch celebrations draw on SafariUtils. [MIT attribution](../licenses/SafariUtils-MIT.txt)

## Last updated

2026-11-23 (America/New_York) — Updated settings, edition differences, the Warp List, and release highlights.
