# Sparkling modes and Safari helpers update

- `!commands` now returns only party commands; local commands remain in settings help.

- Settings now open with `/sm`; use `/sm gui` for the HUD editor and `/sm debug` for diagnostics.

- Consolidated ESP settings into one tab with independent “Only in current biome” toggles for floor drops, Forest, Cavern, Icy and Haunted. Modes manage targets; all color customizations remain available.
- Sparkling mode uses your own missing discoveries when solo and refreshes the combined list when party membership changes.
- Party lookups start immediately. Ready replies follow descending UUID order at 500 ms intervals and check for matching chat results again before sending.
- Added `/sparkle` and a Modes settings tab for Unique, Full Clear and Sparkling runs.
- Sparkling mode checks every party member’s missing sparkling discoveries, including your own, and highlights the combined list for the whole run.
- Added a separate “Critter ESP for profitable shards” setting for Sparkling mode.
- Party lookups run in the background with shared caching. API errors stay local; incomplete lookups never mark discoveries as complete for the party.
- Forest floor drops hide when everyone has all three sparkling birds.
- Cavern floor drops hide when everyone has sparkling Gemzie, when Gemzie has been caught, or when all gems have been found.
- Haunted floor drops stay visible while anyone still needs sparkling Gimmiegold. Otherwise they hide after four Soothing Incense or a Doomspiral capture, including incense found at the start of the run.
- Icy floor drops remain hidden in Unique mode and hide after two Icebreakers in Full Clear and Sparkling modes.
- Full Clear returns biome PBs and all-species sparkling lookups. Unique returns unique-biome and Doomspiral/Wumpa PBs with timesave lookups. Sparkling returns Doomspiral/Wumpa PBs only, also with timesave lookups.
- Auto Clicker now requires physically holding Mouse 0 on a Rockmite mound, stopping immediately on release or target changes.
- Added a Full candle hitbox setting in Safari Helpers, making Haunted candles easier to click while holding Soothing Incense.
- Party help lists actual commands available in your mode. Local command help, settings descriptions and chat feedback are shorter and clearer.
- Warp reminder settings now describe their purpose: warping party leechers in before entry closes.
- Updated README. Builds are available from GitHub Actions.
