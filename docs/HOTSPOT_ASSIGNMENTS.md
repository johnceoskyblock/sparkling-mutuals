# Hotspot assigning

`/hotspot` toggles actual-hotspot announcements and assigned-biome alerts in every mode. There are no config toggles. The preference is saved; existing enabled per-mode preferences migrate to the command toggle.

Announce the actual hotspot immediately after the server Hotspot message. The leader includes the party's pre-run hotspot-win counts; other members use `[SM] Hotspot: <Biome>` or an equivalent supported report. Assignments do not change Fortune, mode requirements or the server hotspot.

## Fair assignments

Only reports from verified current party members participate. Supported mod prefixes, plain biome names, abbreviations and close typos are accepted; quoted chat, other channels and unknown biomes are rejected. Wait two seconds after the third distinct report, or after everyone reports in smaller parties. A fourth report does not restart that window. When too few report, use a five-second fallback from the first report. A sole claimant gets their hotspot; duplicate claims favor fewer pre-run hotspot wins, then a shared UUID hash. Displaced players receive unused biomes in Forest, Cavern, Icy, Haunted order.

The current leader publishes one final `[SM] Biomes:` map for the full roster. Updated followers use that map even after a missed report or a different local fallback. Sender, roster, uniqueness and freshness checks reject invalid coordination messages. Duplicate messages do not double-credit wins. The leader sends individual instructions only to members without a recognized mod announcement. Tallies survive runs with the same roster; a changed roster or disconnect resets them. Solo players keep their actual hotspot. No API requests are added. See [protocol and mixed-party compatibility](compatibility.md).

## Alert

The five-second centered alert names the assigned biome and shows the actual hotspot underneath. Borders are green for Forest, purple for Haunted, light blue for Icy and light orange for Cavern. Alerts respect hidden HUDs and only appear inside Safari.

## Last updated

2026-11-03 — Leader-owned snapshots and final maps, revised reporting windows and mixed-party instructions.

2026-11-01 — Command toggle, two-second report window and session-based fair hotspot allocation.

2026-11-01 — Accept other mods’ hotspot reports, including giannaaa’s CritterMod messages verified in JohnFounder logs; allow a bounded grace period for slower announcements.
