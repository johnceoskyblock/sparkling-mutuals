# Hotspot announcements and biome assignments

**Party Interactions → Hotspot announcer** contains separate Unique, Full clear and Sparkle toggles. Existing per-mode preferences are preserved; all three default off. When enabled for the active mode, announce the actual Hunting Hotspot in party chat and show a local assigned-biome alert. Solo players receive their actual hotspot as their assignment without sending party chat.

## Chat and assignment

The existing departure announcement remains `[SM] Hotspot: <Biome>`, sent at least 0.5 seconds after the Hotspot system message and before starting items. It always names the actual hotspot. Assignments never change Fortune, the hotspot, mode requirements, or send another party announcement.

Only exact hotspot announcements from verified current party members participate. The first reporter of each distinct hotspot keeps that biome. Remaining duplicate reporters are ranked by the signed Java string hash of their normalized lowercase, dashless UUID, with the UUID itself breaking hash ties. Unclaimed biomes, in Forest/Cavern/Icy/Haunted order, are assigned one per ranked duplicate. This is a stable pseudo-random player ranking shared by clients, rather than independent random choices. It can repeat for the same players and hotspots across runs.

For two Icy and two Forest reporters, the first Icy reporter keeps Icy and the first Forest reporter keeps Forest. The remaining two receive Cavern and Haunted according to their UUID rank. All four reporting players have different assignments.

The title waits for all current members' reports, or 3.5 seconds after local departure evidence. Nonreporting members cannot hold it indefinitely. A partial result can update when the remaining report arrives within the first 10 seconds. Once every member reports, or the 10-second window ends, the assignment stays fixed for the run; later roster changes do not reassign it. No profile/API/name-resolution requests are added; existing party UUIDs and loaded/cached IGNs are used. Older mod clients' existing `[SM] Hotspot:` messages remain valid inputs, though those clients do not display the new assignment alert.

## Alert

The five-second centered alert names the assigned biome and shows the actual hotspot underneath. Its screen border and light tint use green for Forest, purple for Haunted, light blue for Icy, and light orange for Cavern. Text scales to fit the window, fades during the last second, and respects hidden HUDs. New runs and disconnects reset all reports and assignment state. Alerts only appear inside Safari with the active mode's toggle enabled.

## Validation

Focused checks cover every four-player hotspot combination, first-reporter priority, unique allocations, shared UUID ranking, dashed UUID normalization, the two-Icy/two-Forest example, exact chat parsing, late/nonreporting members, outsider exclusion, frozen assignments and moved setting persistence. The actual alert rendering and cross-client chat delivery need live gameplay testing.

## Last updated

2026-10-30 — Moved the mode toggles to Party Interactions and added UUID-ranked biome assignments with colored local alerts; party announcements retain actual hotspots.
