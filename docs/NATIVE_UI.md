# Native UI and catch celebrations

## Settings

`/sm` opens the settings workspace. The sidebar contains HUDs, Safari, Party, Highlights in Standard or ESP in Full, History, Misc, and Reset. Search across settings, descriptions, and breadcrumb paths. Navigation and expanded groups are remembered during the game session.

Right-click a HUD in `/sm gui` to open its settings. HUD previews, dragging, resizing, and snapping use the live layout dimensions. Positions carry across compatible GUI scales and editions. [Configuration and HUD defaults](CONFIG_DEFAULTS.md)

## HUD symbols

Biome, bird, food, incense, and gem icons use compact pixel sprites. Progress icons reflect existing completion state. The display itself creates no completion evidence.

## Player Warp List

Both Full editions have the **Party → Sparkle Warping** settings. The Warp List stores per-player critter selections and priorities. CSV imports add players and update priorities for existing entries. [Sparkle Warping and Warp List](WARP_ALERTS.md)

## Confirmed catch celebration

The **Unique sparkling catch alert** uses server-confirmed sparkling catch evidence and the local collection state. Enable **Celebrate duplicate sparklings** to include duplicates. These controls are separate from the alert for spotting a sparkling critter. Sound previews can play with settings open. Use **Preview catch celebration** to see the visual.

## Update status

Each edition has a **What’s New** button that reads the bundled catalog and displays entries for the current edition. Acknowledgement is stored independently per edition. Full also offers an update-status screen with a verified **Download Update** action and adjustable **Auto Update**. Installation happens after Minecraft closes. [Editions](../README.md#installation)

The settings workspace, icons, and catch celebrations draw on SafariUtils. [MIT attribution](../licenses/SafariUtils-MIT.txt)

## Last updated

2026-11-23 (America/New_York) — Updated settings, edition differences, the Warp List, and release highlights.
