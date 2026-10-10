# Haunted mode completion rules

- Areita, Bloodbat, Solsnatcher, Litterbug, Duplico and Hideonwall use (-3, -63): Full Clear absence within 30 blocks with capture minimums; Sparkling distinct UUID minimums within 40 blocks.
- Gazer needs four distinct UUIDs; Hideyho and Doomspiral need one, without zones. Full Clear additionally requires their captures. Sparkling Doomspiral also requires capture.
- Gimmiegold UUID/capture minimums scale with picked-up Shining Coins (at least three). Existing center, cleared floor drops, spent coins and empty coin inventory requirements remain.
- Haunted Sparkling ESP expires after ten seconds per UUID, including profitable Hideonwall. Doomspiral stays until capture.
- Forest specifications remain pending. Live in-game zone/entity-loading verification remains needed.

# Icy mode completion zones

- Icy Full Clear confirms absence only within the supplied 30-block species zones, retaining capture minimums. Sparkling needs 40-block zones plus distinct UUID spawn minimums.
- Troodon uses three distinct UUIDs without a location zone, plus three captures in Full Clear.
- Icy Sparkling ESP expires after ten seconds per UUID. If the party still needs Wumpa checked, prerequisite ESP instead stays until one capture, including Mantis Shrimp with profitability enabled.
- Cavern rules remain. Forest and Haunted zone specifications can follow separately; live in-game entity-loading validation remains needed.

# Cavern mode completion zones

- Cavernfish, Flitter, Shyworm, Driftling, Chuckwalla and Gemzie confirm Full Clear absence only inside their specified species zones, while keeping capture minimums.
- Sparkling checks require each species' zone and distinct UUID spawn minimum; Scrappy requires three distinct UUIDs without a zone.
- Full Clear Scrappy additionally needs three observed UUIDs and three captures. Rockmite mounds/captures and Snoozle walls/absence retain their requirements.
- Sparkling keeps Driftling ESP, hides Rockmite silverfish and Shyworm path/timer helpers, and limits other Cavern critter ESP to ten seconds per UUID (including profitable Chuckwalla).
- Other biome completion rules remain; their zone specifications can be added separately. Live in-game zone and entity-loading validation remains needed.

# Shyworm return-to-start follow-up

- Rest messages retain their eight-second countdown when the model resets to its start, including lingering labels and non-Y1 hiding positions.
- The first observed travel side stays warned during the countdown and “moving soon...” phase; movement resumes next-side warnings.
- Red 1×7 outlines use the nearest known emergence ground level (Y40, Y43 or Y60), with a tiny offset to avoid flickering.
- Initial travel direction requires observed movement; live in-game reset and alignment testing remains needed.

# Chat polish and Shyworm follow-ups

- All local mod feedback uses one `[SM]` prefix per line, including command help and contest warnings.
- Light-gold accents across settings, HUD titles and default panel styling; custom colors and semantic colors are preserved.
- Rest messages queue for the nearest stationary underground Shyworm, including models hidden before the message.
- Red 1×7 warning always covers the next clockwise side after direction is observed, with ground height independent of head bobbing.

# Shyworm corner warning and countdown

- Warns the next clockwise 1×7 path before corner emergence once movement establishes its direction.
- Rest messages start an eight-second green/yellow/red countdown, then white “moving soon...”.
- Moving ESP follows head height; only models at the hidden sentinel height use the saved surface projection. Mode and capture eligibility still apply.

## Full Clear progress follow-up

- Full Clear progress now uses the captures panel's minimum, nearby-entity and structure completion rules.
- Party `fd`, `cd`, `id` and `hd` messages complete unvisited biome progress. Visiting that biome restores local evidence checks.
- Manual progress never adds captures, Sparkling checks or personal bests. Unique and Sparkling progress retain their rules.

# Shyworm underground helpers

- Loaded Shyworms retain ESP while underground, projected to their last observed surface height. Captured, removed and unloaded models remain excluded.
- A red 1×7 ground outline anticipates the next clockwise side at corners. Rest messages start a per-worm eight-second colored countdown; brief corner pauses do not start it.
- Normal mode filters and Sparkling per-UUID expiry gate the box, outline and timer together. Ambiguous messages do not assign a timer to several worms; helper state clears on unloading or run reset.

# Icy floor drops and moving-critter ESP

- One Icebreaker is enough to stop automatic Full Clear Icy floor ESP; consumption and biome changes retain that progress for the run.
- Unique and Full Clear restore loaded moving-critter ESP when name tags disappear at distance or movement pauses, including Driftling. Capture retirement, entity validity and mode target filters still apply.
- Other Sparkling lifecycle checks, per-UUID windows, capture observations and PB requirements retain their existing rules. Issue #18 clarified clockwise movement and long-rest chat boundaries.

# Run mode fixes

- Full Clear missing rows remain until the capture minimum is met and no matching critters are nearby.
- Full Clear Cavern floor drops stay highlighted until all three gem types are held together, regardless of party sparkling discoveries.
- Unique mode highlights every uncaught species, even when everyone already has its sparkling discovery. Existing profitable-shard and shiny-check exceptions still apply after captures.

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
- Icy floor drops remain hidden in Unique and Sparkling modes and hide after one Icebreaker in Full Clear.
- Full Clear returns biome PBs and all-species sparkling lookups. Unique returns unique-biome and Doomspiral/Wumpa PBs with timesave lookups. Sparkling returns Doomspiral/Wumpa PBs only, also with timesave lookups.
- Auto Clicker now requires physically holding Mouse 0 on a Rockmite mound, stopping immediately on release or target changes.
- Added a Full candle hitbox setting in Safari Helpers, making Haunted candles easier to click while holding Soothing Incense.
- Party help lists actual commands available in your mode. Local command help, settings descriptions and chat feedback are shorter and clearer.
- Warp reminder settings now describe their purpose: warping party leechers in before entry closes.
- Updated README. Builds are available from GitHub Actions.

Issue #19 follow-ups: Sparkling Icy progress uses captures of needed species when Wumpa is needed; a Wumpa capture completes the row. Exact global chamber-opening, darkness-fading and cave-collapse messages check Gemzie, Doomspiral and Wumpa respectively. The saved chat toggle is now labeled “Hide useless chats in safari” and also hides the four mound progress lines. Other mode and personal-best rules are unchanged.

Mode switches keep the current run’s captures, completion evidence and Sparkling checks. Party fd/cd/id/hd messages are retained for Sparkling and unvisited Full Clear progress regardless of the selected mode; they do not create catches or personal bests. A new run clears that evidence.

Birdfeeder announcements now confirm one food used each, including the two-Macaw announcement. Bird completion waits for all collected food to be fed; Full Clear requires every announced Bluebird and Parakeet plus one Macaw, even if extra Macaws unload. Captures, progress and missing rows use these checks. Unique and Sparkling keep their existing capture/discovery rules; personal PBs still require personal catches.

Full candle hitbox now applies to red candles in Haunted while holding Soothing Incense, without pedestal or candle-count restrictions. White and other decorative candles retain their normal selection shapes; collision and server block state are unchanged.
