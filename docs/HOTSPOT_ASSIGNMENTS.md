# Hotspot announcements and biome assignments

`/hotspot` toggles actual-hotspot announcements and assigned-biome alerts in every mode. There are no config toggles. The preference is saved; existing enabled per-mode preferences migrate to the command toggle.

The departure announcement remains `[SM] Hotspot: <Biome>`, queued at least 0.5 seconds after the server Hotspot message and before starting items. It always names the actual hotspot. Assignments do not change Fortune, mode requirements or the server hotspot.

## Fair assignments

Only anchored hotspot announcements from verified current party members participate. Accept `[SM] Hotspot: <Biome>` and CritterMod’s `My hotspot: <Biome>`, plus equivalent plain or explicitly tagged hotspot announcements. Formatting and case are ignored; quoted chat, other channels and unknown biomes are rejected. Wait at least two seconds from the first received report. If members have not reported, wait for them up to five seconds before allocating from available reports. A biome with one claimant goes to that player. For duplicate claimants, the player with fewer previous actual-hotspot wins receives it. A shared UUID hash breaks ties. Assign remaining players unused biomes in a stable order. Chat order gives no priority.

Each allocation updates all reporting members' win tallies once. Tallies survive runs while the roster stays the same; a changed roster or disconnect starts a new session. The assignment stays fixed for that run. Solo players receive their actual hotspot. Missing members cannot extend the five-second grace period; allocations use the available reports and the local actual hotspot. Parties should all use Sparkling Mutuals for consistent reports and session tallies. No API requests are added.

## Alert

The five-second centered alert names the assigned biome and shows the actual hotspot underneath. Borders are green for Forest, purple for Haunted, light blue for Icy and light orange for Cavern. Alerts respect hidden HUDs and only appear inside Safari.

## Last updated

2026-11-01 — Command toggle, two-second report window and session-based fair hotspot allocation.

2026-11-01 — Accept other mods’ hotspot reports, including giannaaa’s CritterMod messages verified in JohnFounder logs; allow a bounded grace period for slower announcements.
