## What’s New in 5.0.2

### All editions

- **Unique biome completion** reports and banners use the required captures and the party-needed checks enabled under **Safari → Modes → Sparkling Checks Options**. Use `!sparkle <critter> on/off` to change an optional check for everyone using the mod, or `!sparkle after clear on/off` to show remaining checks in Biome Objectives once the biome clears.
- **Party mode commands** let you switch the party to `!unique`, `!fc`, or `!sparkle`. `!fullclear` and `!full clear` also work. `!commands` and `/sm help` group related commands, and lookups accept common singular and plural forms such as `!sparkling` and `!sparklings`.
- **Emerald Dye streaks** are available through `!since dye`, which reports each responding player's saved runs since their last recorded drop. Import older logs to recover the last drop date for your selected history account.
- **Hotspot assigning** now shows teammates' actual hotspots when SafariUtils reports them through the backend. Turning it on adds a clickable **[Preferences]** link in local chat. You can also open preferences with `/hotspot preferences`.
- **Party Ready** plays in Torrhus Canyon once you earn your contest ticket and each teammate confirms theirs in party chat. Find it under **Safari → Banner → Contest alerts**.
- **Share Config**, next to **Done**, copies and imports settings and HUD layouts using clipboard codes. Importing backs up your settings and keeps account details and Safari records intact.
- **Build a Sparkling celebration** lets you mix effects, text, and music, then preview, save, or share your design. Your existing catch celebration styles and toggles are still available.
- **Doomspiral Gambling** starts a shorter animation on “Something stirs” for nearby players who didn't place the last candle. Both animations finish at the same point in the ritual, and the Sparkling Found sound waits until the animation ends.
- **Recatch waypoint** marks a critter during a confirmed capsule attempt and briefly after an escape. It's under **Safari → Helpers → Capture tools**, alongside **Left-click to leave Gazer beds**.
- **Rockmite mound glow** outlines the mound model. Critter boxes now render only when their box setting is enabled, and Woodchucker highlights cover the western Forest canopy above Cavern.
- **Hide Icy frost overlay** removes the freezing vignette in the Icy Biome. Find it under **Visual cleanup**.
- The **Sparkling Critterdex** grays out the entire row when your count falls below the selected dex tier's requirement.

### Standard edition

- **Doomspiral Gambling** keeps the reels spinning while the result is pending. A visible Sparkling Doomspiral gets the jackpot celebration and follow-up alert.
- **Rockmite mound glow** and **Recatch waypoint** respect line of sight. After a Unique biome clear, **Show remaining Sparkling checks after biome completion** lists the remaining checks in Biome Objectives.

### Full editions

- Choose glow outlines and boxes directly for each critter under **ESP → General**. Those settings also control linked quest drops and structures. In Unique mode, bird food and Shining Coins stay eligible for highlights while their linked styles and floor-drop visibility are enabled.
- The Rockmite switch under **ESP → Always On Toggle** also keeps intact mounds highlighted after mode completion requirements are met.
- **Show remaining Sparkling checks after biome completion** brings back party-needed objectives and highlights for unchecked nests, mounds, and walls after a Unique clear. The Missing panel reads **All captured** while those checks are available, and the Progress HUD keeps the biome marked complete.

## Downloads

GitHub releases provide all three builds for Minecraft **26.1.2**, **26.2**, and **26.3**. The release workflow also publishes the verified **Standard** builds to Modrinth, one version per Minecraft target. Install one JAR with the matching `+mc` suffix.

| Edition | Filename suffix before `+mc` | Updates |
| --- | --- | --- |
| **Standard** | None | Manual downloads |
| **Full** | `-full` | Verified in-game downloads with a configurable Auto Update toggle |
| **Full No Auto Update** | `-full-no-auto-update` | Manual downloads |
