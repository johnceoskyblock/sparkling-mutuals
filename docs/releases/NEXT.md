## Editions and updates

- Added **Standard**, **Full**, and **Full No Auto Update** builds from the same codebase for Minecraft 26.1.2, 26.2, and 26.3.
- **Standard** includes shared Safari tracking, party sync, HUDs, and history with visible critter outlines, generic floor-drop highlights, and local sparkling alerts. Gameplay automation and hidden-target highlights are excluded.
- **Full** keeps the original gameplay features and adds a **Download Update** button and opt-in **Auto Update** toggle. Downloads match your edition and Minecraft version, are verified before installation, and install after Minecraft closes. Automatic downloads default off; failed installations retain a verified rollback copy and support retry.
- **Full No Auto Update** keeps Full's gameplay features without in-game update checks, downloads, or the installer.
- GitHub releases provide all three editions: Standard (unsuffixed), Full (`-full`), and Full No Auto Update (`-full-no-auto-update`). Modrinth publication is pending. Install only one edition and match its `+mc` suffix to your Minecraft version.

## Shared settings and Standard helpers

- All editions now share settings, HUD layouts, and Safari history within the same Minecraft instance. Missing saves are imported from the old Standard folder without overwriting existing shared saves. Standard preserves Full-only settings while leaving them inactive.
- Standard includes capsule trajectory previews, known Snoozle wall waypoints, and the Doomspiral lottery. Critter contacts in capsule previews and lottery results respect local visibility; trajectory geometry and fixed Snoozle wall waypoints can remain visible through terrain.
- Added Standard keybinds for **Invite Next Sparkling Player**, **Prepare Party Warp**, and **Accept Hideyho Quest**. Each press performs one action: invite an eligible player in priority order, prepare the warp command, or select the current quest prompt. Configure invite priorities under **Party → Interactions**.

## Party commands and customization

- Added `!sparkling [IGN]` for total, unique, and duplicate sparkling counts, and `!rates` for each responding player's session catches and runs per sparkling.
- Added a **Reset** tab to restore default settings and HUD layouts or disable configurable features without clearing saved Safari records.
- Added per-row progress HUD controls, including a timer-only display, and a master switch for custom critter names.
- Reorganized settings navigation and Standard's **Highlight Visible** controls to show the options available in that edition.

## Fixes

- Improved Warp List rank colors using party-roster and tab-list evidence.
- Corrected installed-release highlights so the What's New screen reflects the current upgrade and includes features from skipped releases.
- Updated release detection and download verification for the new Full filenames, and strengthened shipping checks for all nine GitHub JARs and their manifest.
