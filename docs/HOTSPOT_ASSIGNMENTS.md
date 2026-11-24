# Hotspot assigning

`/hotspot` toggles actual-hotspot announcements and assigned-biome alerts in every mode. There are no config toggles. The preference is saved; existing enabled per-mode preferences migrate to the command toggle.

Announce the actual hotspot immediately after the server Hotspot message using `[SM] Hotspot: <Biome>`. Party chat carries no win counts or full-roster biome map. Assignments do not change Fortune, mode requirements or the server hotspot.

## Fair assignments

Only reports from verified current party members participate. During the first thirty seconds of the local run, manual party messages can contain surrounding text or the copied server hotspot line. Forest, fprest, for, f; Cavern, cav, c; Icy, ice, i; Haunted, haunt, haunte, hvant, h are supported. Single-letter aliases must be the entire message. A message naming multiple biomes is ambiguous and rejected. Assignment instructions are excluded. Coordinated mod reports remain supported outside this manual window.

**Party → Interactions → Biome Hotspot Preferences** opens a four-row editor. Use the arrows to move a biome up or down, then save. The default order is Icy, Forest, Haunted, Cavern.

A sole claimant gets their hotspot. With a supporting WebSocket server, duplicate claims favor fewer pre-run wins, then assignment clients over manual reporters, then the higher preference for that hotspot, then the higher UUID. Saved changes are used for the next unlocked assignment. [Preference sync contract](PARTY_SYNC_HOTSPOT_PREFERENCES.md)

The local mixed-party fallback assigns immediately once every roster member has reported, or thirty seconds after the first report. It uses wins, known mod reports, and UUIDs for ties because private preference orders are unavailable in party chat. Displaced players receive unused biomes in Forest, Cavern, Icy, Haunted order.

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

`/hotspot` controls assigning. **HUDs → HUDs → Hotspot GUI** has the display and appearance settings. Right-click the HUD in `/sm gui` to reach the same controls. Fresh installs place it in the bottom-right corner. The default shows party members with their actual hotspot (H), assignment (A), and win count. The display can show just your own card instead. Known Hypixel rank colors come from observed names and use no additional profile requests.

The run count includes the current run after Safari admission and consecutive completed Hotspot runs with the same roster. If teammates leave and rejoin between runs, the streak continues once the same party runs again. A recorded Safari with a different roster breaks the streak. The backend still determines assignments and wins.
