## What’s New in 5.0.0

### All editions

- **Import history from logs** recovers missing Main-server Safari runs and sparklings for your **History UUID**. Copy logs from your old Safari runs into the current Minecraft instance’s logs folder first. The importer can link midnight rollovers and preserves live-recorded runs when a log matches.
- **Stats** shows Doomspiral and Wumpa checks. Use `!doom` or `!dooms`, `!wumpa` or `!wumpas`, and `!gemzie` for recorded encounter counts in party chat.
- **Stats** separates recorded run time from `/playtimedetailed` and refreshes **API Captures** and biome totals when opened. Lifetime capture achievements can unlock during a run using the last API total and newer personal captures.
- Open **Critterdex** from **Sparklings** to see all 37 critters by biome, rarity, credited catches, and dex tier. **Profit** has a **New session** button, and `/sm history import` uses the same flow as **Import logs** in settings.
- Reopening `/playtimedetailed` refreshes the saved Safari playtime. Vincent’s **Dye Guide** syncs your Emerald Dye total from the **You’ve dropped** line when it is available.

#### Party sync

- Expand confirmed growing parties on a supporting provider's existing WebSocket, with replay gating and recovery.
- Mark confirmed visit departures and preserve same-visit state through temporary disconnects.
- Include initial food and coin grants in hotspot preference allocation and show recognized admission failure reasons in diagnostics.
- Contact the backup only after sustained primary failure and ignore delayed backup responses after the primary recovers.

### Full and Full No Auto Update

- Set per-critter choices separately under **Critter glow outlines** and **Critter boxes**.
- Optionally keep intact Rockmite mounds visible and continue highlighting remaining Sparkling checks after Unique biome completion. Both settings default to off.
- Glow-incompatible targets use a box fallback. Locally unpunched Forest nests retain their Full Clear markers after shared completion.

## Downloads

GitHub releases provide all three builds for Minecraft **26.1.2**, **26.2**, and **26.3**. Install one JAR with the matching `+mc` suffix.

| Edition | Filename suffix before `+mc` | Updates |
| --- | --- | --- |
| **Standard** | None | Manual downloads |
| **Full** | `-full` | Verified in-game downloads with a configurable Auto Update toggle |
| **Full No Auto Update** | `-full-no-auto-update` | Manual downloads |
