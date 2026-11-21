## Capture tracking, ESP, and nameplates

- Improved capture-to-model matching for delayed server messages, repeated throws, slightly off-target throws, hidden chat, and models that disappear before confirmation. Captured critters retire from ESP without hiding unrelated nearby critters.
- Improved pity tracking across escapes and body/name-tag replacements, including Hideon critters and other capsule species. Ambiguous nearby critters no longer inherit each other's attempts, and capture-animation markers are matched to the relevant target.
- **Show catch pity now defaults off.** Existing saved preferences are preserved.
- Fixed Rockmite mound progress when hit messages are hidden or the player switches targets.
- Improved retained Shyworm head positioning and visibility while underground, including when glow ESP is enabled without Box ESP.

## Completion and personal bests

- Fixed Full Clear handling for species with known capture quotas, so leftover models do not keep verified quota completion pending. Eligible biome-clear reports can finish after leaving the biome.
- Corrected **Lake Cleared** timing: Unique and Sparkling modes require one personal catch of Tepid, Nozzlenose, and Mantis Shrimp; Full Clear waits for their full observed populations. Loot shares alone do not trigger the alert.
- Added **Local PB messages** controls for Doomspiral and Wumpa solo, duo, observed-solo, and observed-duo catches, plus Full Clear, Unique Run, and Party Unique Run biome records. These default on; hiding messages does not stop PB recording.
- PB feedback now shows the relevant best time in `(PB: …)` form and identifies new records.

## Hotspot party-run history

- Added a completed party-run count to the Hotspot HUD, backed by saved run history and the current party's UUIDs.
- Tracks whether Hotspot assigning was enabled, whether the roster was present together, and whether the reward summary was received. Roster refreshes, late summaries, and teammates leaving before the summary do not lose or double-count the recorded run.
- Party changes reset continuity; older or unconfirmed history is not guessed into the count.

## Recovery and presentation

- Fixed automatic reconnect handling for network/login failures and disconnects during server configuration. Retry backoff survives failed attempts; cancellation, intentional quits, and recognized non-retryable denials stop further retries.
- Refreshed the Doomspiral Lottery reveal with revised reel timing, a clear **SPARKLING / REGULAR** marquee, and animated critter-name styling.
- Added project credits and attribution links to the README.

Builds target **Minecraft 26.1.2, 26.2, and 26.3**. Choose the JAR matching your Minecraft version.
