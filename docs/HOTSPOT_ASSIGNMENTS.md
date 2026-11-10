# Hotspot assigning

`/hotspot` toggles actual-hotspot announcements and assigned-biome alerts in every mode. There are no config toggles. The preference is saved; existing enabled per-mode preferences migrate to the command toggle.

Announce the actual hotspot immediately after the server Hotspot message. The leader includes the party's pre-run hotspot-win counts; other members use `[SM] Hotspot: <Biome>` or an equivalent supported report. Assignments do not change Fortune, mode requirements or the server hotspot.

## Fair assignments

Only reports from verified current party members participate. Supported mod prefixes, plain biome names, abbreviations and close typos are accepted; quoted chat, other channels and unknown biomes are rejected. The local mixed-party fallback assigns immediately once every roster member has reported; otherwise it waits up to thirty seconds from the first report. Reports arriving after the lock cannot change assignments but can still trigger a missing manual instruction. A sole claimant gets their hotspot; duplicate claims favor fewer pre-run hotspot wins, then a shared UUID hash. Displaced players receive unused biomes in Forest, Cavern, Icy, Haunted order.

The current leader publishes one final `[SM] Biomes:` map for the full roster. Updated followers use that map even after a missed report or a different local fallback. Sender, roster, uniqueness and freshness checks reject invalid coordination messages. Duplicate messages do not double-credit wins. The leader sends individual instructions only after a verified manual reporter has supplied their hotspot. Publishing the summary map does not count as an individual instruction. Tallies survive runs with the same roster; a changed roster or disconnect resets them. Solo players keep their actual hotspot. No API requests are added. See [protocol and mixed-party compatibility](compatibility.md).

## Alert

A locked WebSocket assignment no longer waits for local hotspot/ticket information. The alert starts immediately rather than waiting behind the shared celebration/audio queue; its five-second lifetime begins when visible. It is shown at most once per run, including off/on toggles after display. The five-second centered alert names the assigned biome and shows the actual hotspot underneath. Borders are green for Forest, purple for Haunted, light blue for Icy and light orange for Cavern. Alerts respect hidden HUDs and only appear inside Safari.

## Last updated

2026-11-03 — Leader-owned snapshots and final maps, revised reporting windows and mixed-party instructions.

2026-11-01 — Command toggle, two-second report window and session-based fair hotspot allocation.

2026-11-01 — Accept other mods’ hotspot reports, including giannaaa’s CritterMod messages verified in JohnFounder logs; allow a bounded grace period for slower announcements.

## Hotspot HUD

`/hotspot` controls both the assignment alert and the Hotspot HUD; there is no separate HUD enable switch. HUDs -> HUDs -> Hotspot GUI contains appearance and placement controls, also reached by right-click in `/sm gui`. The default is on the left, below the usual objective area; width is bounded by the Biome Objectives preview width. Players occupy a two-column, two-row grid, with separated cards for rank-colored names, actual hotspot (H), assigned biome (A), and gold win counts. Biomes use Forest green, Cavern orange, Icy blue and Haunted purple. Names reuse the Warp List's observed Hypixel rank colors without extra API requests; unknown ranks stay neutral and long names wrap. Unknown biomes show a neutral question mark rather than a guess. The backend remains the allocation/fairness authority; fallback rows use the local session roster and tally.
