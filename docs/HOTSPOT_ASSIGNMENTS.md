# Hotspot assigning

`/hotspot` toggles actual-hotspot announcements and assigned-biome alerts in every mode. There are no config toggles. The preference is saved; existing enabled per-mode preferences migrate to the command toggle.

Announce the actual hotspot immediately after the server Hotspot message using `[SM] Hotspot: <Biome>`. Party chat carries no win counts or full-roster biome map. Assignments do not change Fortune, mode requirements or the server hotspot.

## Fair assignments

Only reports from verified current party members participate. During the first thirty seconds of the local run, manual party messages can contain surrounding text or the copied server hotspot line. Forest, fprest, for, f; Cavern, cav, c; Icy, ice, i; Haunted, haunt, haunte, hvant, h are supported. Single-letter aliases must be the entire message. A message naming multiple biomes is ambiguous and rejected. Assignment instructions are excluded. Coordinated mod reports remain supported outside this manual window.

The local mixed-party fallback assigns immediately once every roster member has reported; otherwise it waits up to thirty seconds from the first report. A sole claimant gets their hotspot; duplicate claims favor fewer pre-run hotspot wins, then a shared UUID hash. Displaced players receive unused biomes in Forest, Cavern, Icy, Haunted order.

The WebSocket supplies assignments to connected clients. Only the current leader sends individual assignment instructions for backend-designated recipients, with sent-ID deduplication and acknowledgments. A manual biome message alone never creates a second instruction path for a WebSocket user. During chat fallback, the leader instructs uncoordinated reporters individually. Fairness tallies remain internal to assignment logic. Solo players keep their actual hotspot. No API requests are added. Incoming legacy wins and map messages remain readable for older-client compatibility.

## Alert

A locked WebSocket assignment is displayed after the local player's ticket admission, including when its actual hotspot is unknown. Unknown actual-hotspot text is omitted. Local allocation still requires the local actual hotspot. The alert's five-second lifetime begins when visible. It is shown at most once per run, including off/on toggles after display. The centered alert names the assigned biome and shows the actual hotspot underneath when known. Borders are green for Forest, purple for Haunted, light blue for Icy and light orange for Cavern. Alerts respect hidden HUDs and only appear inside Safari.

## Shared backend timing

The local backup allocator waits for all active roster members to report, with a thirty-second fallback from the first eligible report. Presence in the tab list alone does not prove ticket admission. A confirmed actual hotspot report proves admission but still needs attendance evidence. No allocation timer starts before an eligible report.

Serkosea's primary service needs the same timing contract to replace its previously agreed third-report/two-second and first-report/five-second deadlines. Until that is deployed, the primary remains responsible for its own early locks. The client preserves finalized locks rather than assigning conflicting biomes independently.

Unassigned reports retry every two seconds through the existing serialized, rate-limited queue. Own fallback reports come directly from the server hotspot observation, without depending on a party-chat echo or toggling the feature. A recycled lobby's retained prior-run state still requires server generation reconciliation.

## Last updated

2026-11-03 — Leader-owned snapshots and final maps, revised reporting windows and mixed-party instructions.

2026-11-01 — Command toggle, two-second report window and session-based fair hotspot allocation.

2026-11-01 — Accept other mods’ hotspot reports, including giannaaa’s CritterMod messages verified in JohnFounder logs; allow a bounded grace period for slower announcements.

## Hotspot HUD

HUDs → Hotspot GUI → Show all players defaults on. Turning it off displays only the local player's card, including in the HUD editor preview. This filters the display without changing shared assignments or fairness counts.

`/hotspot` controls both the assignment alert and the Hotspot HUD; there is no separate HUD enable switch. HUDs -> HUDs -> Hotspot GUI contains appearance and placement controls, also reached by right-click in `/sm gui`. The default is on the left, below the usual objective area; width is bounded by the Biome Objectives preview width. Players occupy a two-column, two-row grid, with separated cards for rank-colored names, actual hotspot (H), assigned biome (A), and gold win counts. Biomes use Forest green, Cavern orange, Icy blue and Haunted purple. Names reuse the Warp List's observed Hypixel rank colors without extra API requests; unknown ranks stay neutral and long names fit on one line by scaling down only when necessary. The repeated H/A legend is omitted. Unknown biomes show a neutral question mark rather than a guess. The backend remains the allocation/fairness authority; fallback rows use the local session roster and tally.
