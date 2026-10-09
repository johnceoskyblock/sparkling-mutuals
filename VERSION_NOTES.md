# Sparkling ESP and Wumpa prerequisites

- While the party needs Wumpa checked, Sparkling mode keeps uncaught Icy prerequisites in the missing panel and ESP until each is captured.
- Needed critter UUIDs get independent ten-second ESP windows, remembered across unloading for the run. Newly found UUIDs get their own window; profitable-shard ESP stays visible when enabled.
- ESP expiry does not remove entities from capture, checklist or PB evidence. Unchecked Rockmite mounds remain highlighted until checked.
- Verified the existing hidden Litterbug countdown also covers issue #16.

# Party biome completion fix

- Fixed party `fd`, `cd`, `id` and `hd` messages being filtered out before reaching the Sparkling checklist. They now mark the matching biome checked during an active Sparkling run.
- Player chat remains excluded from capture, entry and PB tracking. Unique and Full Clear behavior is unchanged.

# Litterbug emergence timer

- Hidden Litterbug ESP boxes now show an estimated 8.0-second countdown that changes from green to yellow to red, followed by white "Moving soon..".
- Each entity keeps its own timer. Emergence clears it; the next wall entry starts a new countdown. Timers reset with the Safari run and follow normal ESP visibility.

# Sparkling checklist update

- Sparkling missing panels and progress now track party-needed checks instead of unique captures.
- Biome-center checks cover ordinary critters. Special critters require nests, mounds, walls, bird feeding, coin spending or distinct UUID sightings, remembered for the run.
- Party messages `fd`, `cd`, `id` and `hd` mark the matching biome checked in Sparkling mode; no automatic progress messages are sent.
- Remaining nests, mounds and walls disappear after their Sparkling check is complete. Unique and Full Clear helper rules remain intact.
- Verified the previously implemented party-cleared biome display: earlier party catches and no personal catches use minimum counts and rendered critters, without relaxing personal PB requirements.

# Sparkling modes and Safari helpers update

- Modes now manage remaining structures and their highlights; removed redundant Remaining, Bee Nest Highlight and Snooper Wall Highlight controls.
- Full Clear missing lists follow currently rendered species, restoring a row if critters reappear. Capture completion and quota labels are unchanged.
- Unique Honeybug, Rockmite and Snoozle shiny checks remain visible after a unique catch while anyone in the party still needs their sparkling; unavailable party discovery data keeps checks enabled. Sparkling mode hides both Rockmite forms and Snooper walls when everyone has their discoveries.
- Gem, bird food, incense and warp alerts are smaller independent HUDs. Move and resize them in `/sm gui`, with separate appearance controls.
- Icy floor drops stay hidden in Sparkling mode.

- Hidden Litterbug ESP now appears at the known mansion floor or ledge height, preferring that entity's last observed emergence height. Unknown or ambiguous spawns stay at their real position until observed. The box follows the real entity again when it emerges; capture and PB tracking are unchanged.

- Party-cleared biome capture displays now use minimum catches and absence of rendered critters after your first visit, including Honeybug, Rockmite, Snoozle and birds. Previously collected Forest nests stop highlighting. Personal PB requirements stay unchanged.
- Restored the centered sparkling title, critter name, location and tinted screen flash. Added customizable sparkling sound, volume and sound preview in Safari Helpers → Alert.

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
- Icy floor drops remain hidden in Unique and Sparkling modes and hide after two Icebreakers in Full Clear.
- Full Clear returns biome PBs and all-species sparkling lookups. Unique returns unique-biome and Doomspiral/Wumpa PBs with timesave lookups. Sparkling returns Doomspiral/Wumpa PBs only, also with timesave lookups.
- Auto Clicker now requires physically holding Mouse 0 on a Rockmite mound, stopping immediately on release or target changes.
- Added a Full candle hitbox setting in Safari Helpers, making Haunted candles easier to click while holding Soothing Incense.
- Party help lists actual commands available in your mode. Local command help, settings descriptions and chat feedback are shorter and clearer.
- Warp reminder settings now describe their purpose: warping party leechers in before entry closes.
- Updated README. Builds are available from GitHub Actions.
