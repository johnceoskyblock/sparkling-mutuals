# Sparkle Warping

Sparkle Warping brings leechers into a Safari without using their tickets. Enable `/warping` and choose Solo or Duo in the Sparkle Warping tab. Leechers can stay in SkyBlock; the previous Prototype Lobby workflow is no longer required.

## Leader cycle

A cycle starts at the Hunting Hotspot message (leaving the boat), only for a confirmed solo player or party leader with fewer than four party members. Unknown party information waits for the existing party lookup. A normal run starting with four players does not start a timer or cleanup cycle, so the feature can remain enabled.

1. During the first 19 seconds, optional Auto-party invites saved Warp List players for positively identified visible Sparkling critters. Unknown particle detections do not invite.
2. From 19 seconds, pending Warp List candidates give way to the previous leechers. Only remaining spots are filled, with at most four players including the leader and Duo runner.
3. At the configured delay (25 seconds by default), show **Warping Now** and send `/p warp` once. Players invited during this window can bring the party to four without cancelling that warp. Sending the command does not prove server acceptance.
4. When the leader returns from this active Safari cycle to Torrhus Canyon or Springs, save the current leechers and prepare the next run. **Solo** sends `/p disband`. **Duo** keeps the selected runner and sends `/p kick <IGN>` for each other member. The leader must still hold leadership; incomplete roster/name information waits rather than guessing. No cleanup happens for a normal four-player run that never started a cycle.

Choose the Duo runner with `/duo <IGN>`; this also selects Duo mode. The same IGN can be edited in the tab. Duo cleanup requires that runner to be in the party, and the delayed warp also requires the runner to remain a member. Switch the dropdown to Solo to run alone.

Invitations are sent one at a time, two seconds apart. Joined/current members are skipped. The previous leecher list is session memory, replaced when the next disband/kick cycle has players to remove; an empty cleanup does not erase it. A disconnect clears it. Existing invites follow Hypixel's normal expiry.

## Leech

`/leech` or the Leech toggle enables automatic acceptance of **any** genuine Hypixel party invite. If already in a party, send `/p leave`, wait for leave/roster confirmation, then `/p accept <inviter>`. If solo, accept immediately once party state is known. A newer invite replaces the pending target; turning Leech off, disconnecting or a 15-second timeout cancels pending acceptance. Player-chat quotes cannot trigger it.

Leech works in Hypixel lobbies as well as SkyBlock and does not require `/warping` or Auto-party. While Leech is enabled, this client does not run the leader cycle. It defaults off.

## Controls and persistence

| Control | Default | Effect |
| --- | --- | --- |
| `/warping` / Auto warping | Off | Enable the leader cycle; safe to leave enabled in normal full-party runs. |
| Leader mode | Solo | Disband after an active cycle, or keep the Duo runner. |
| `/duo <IGN>` / Duo runner | Empty | Save the runner; the command also chooses Duo. |
| `/autoparty` / Auto-party | Off | Prioritize detected critters' Warp List players before 19 seconds. Returning leechers are invited even with this off. |
| `/leech` / Leech | Off | Accept party invites, leaving the current party first. |
| `/warpingdelay <seconds>` / Delay | 25 seconds | Range 1–86400. Existing saved custom delays, including 23 seconds, stay unchanged. Reset uses 25. |

A delay below 19 seconds ends before returning-leecher invitations can begin. The tab explains the cycle and keeps the normal full-width description text.

Timers bind to the Safari run, world and client connection. Repeated Hotspot triggers replace the previous timer. Disabling warping, changing its delay/mode/runner, enabling Leech, disconnecting, a stale run/world, or lost leadership stops pending actions. Invitations stop on a manual/confirmed party warp. Cleanup is only attempted on return to Torrhus after an eligible active cycle, never just on login or in other areas.

No new ShinyHunter/profile requests are introduced. Party membership uses the existing Hypixel Mod API roster; verified names use loaded profiles, the existing name cache or the existing asynchronous name resolver. Mode, runner, toggles, delay and Warp List persist in the main configuration. Run timers, cleanup queues, leecher lists and pending invites do not persist.

## Warp List

The editor retains the 26 supported critters and ordered IGN lists. Invalid names are omitted; repeated names are invited at most once per window. Disabling Auto-party only stops pre-19-second candidates, not returning leechers.

**Import CSV** in the Warp List editor replaces all saved entries immediately. Row 1 supplies case-insensitive critter headers; only rows 2-11 supply names, in order. Blank or invalid IGN cells and unsupported columns are ignored, and duplicates within a critter are removed. Missing or empty critter columns clear their old entries. CSV quoting and UTF-8 BOMs are supported. Cancelling, an unreadable file, malformed rows 1-11, duplicate critter headers or no supported headers leaves the current list unchanged. Content after row 11 is never parsed.

## Validation

Focused tests cover Solo/Duo cleanup selection, missing/invalid runners, leader/capacity gating, the 19-second phase boundary, spaced invitations and deadline expiry, exact multiline invitation parsing, leave-before-accept, cancellation/expiry, and settings persistence. Server-side party permissions, transport and invite acceptance need live Hypixel testing.

## Last updated

2026-10-30 — Solo/Duo leader cycles, previous-leecher fallback from 19 seconds, 25-second fresh/reset delay, `/duo` and `/leech`. Only parties smaller than four start cycles; filling the party through invites still permits the scheduled warp.

2026-10-29 - Fresh installations seed the maintainer-supplied JohnFounder Warp List (115 ordered name assignments). Existing configuration files keep their saved or empty lists; Auto-party remains off by default.

2026-10-29 — The Sparkle Warping introduction uses standalone full-width wrapped text at normal font size, with no option box or automatic shrinking.

2026-10-29 (America/New_York) — Added Warp List CSV replacement import using the header and ten name rows. Renamed controls to Sparkle Warping, `/warping` and `/warpingdelay`, changed the fresh default to 23 seconds, and added bounded Warp List auto-party invitations.
