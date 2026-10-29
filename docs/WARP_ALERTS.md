# Sparkle Warping

## Purpose

An enabled warp reminder shows a local **Warping Now** alert and sends `/p warp` once after a Safari Hunting Hotspot. Sending the command does not establish that the server accepted or completed a warp.

## Controls and defaults

| Control | Default | Effect |
| --- | --- | --- |
| `/warping` or **Sparkle Warping** switch | Off | Enables both the alert and automatic command. |
| `/warpingdelay <seconds>` or settings delay | 23 seconds | Accepts 1–86400 seconds; changing it cancels the pending timer. |

Explicit saved choices use the general [configuration file](CONFIG_DEFAULTS.md). Enabling the switch waits for the next Hotspot; it does not revive a cancelled timer.

## Implemented behavior

The non-overlay Fabric GAME-message listener accepts an exact Hunting Hotspot system line for Forest, Cavern, Icy or Haunted, after the shared trusted-message filtering. Player-chat quotes cannot schedule it; exact text belongs to [Messages](critters/MESSAGES.md).

Scheduling ensures a run exists and binds the timer to that run object, client connection and client world. The latest accepted Hotspot cancels and replaces any pending timer. At expiry, the client must still have a player, the feature enabled, Safari detection active and the same unended run, connection and world. A generation token makes the expiry action single-use. The alert and command share this decision; there is no separate leadership check. Normal server party permissions apply.

## Lifecycle and persistence

The switch and delay persist; timers do not. Toggling the switch, changing the delay or disconnecting cancels the timer and clears the Warp alert. A changed world, connection, ended run or replacement run fails the expiry check, including a new run in the same world. No new timer is inherited from an earlier run.

## Edge cases

- A nonleader may send the command; success depends on the server.
- Overlay text and quoted Hotspots do not start or replace timers.
- A stale scheduled callback cannot consume a newer timer.
- A visible alert proves the local expiry action, not successful party transport.

## Source and validation references

- Registration/reset: [SparklingMutuals](../src/main/kotlin/net/johnceo/sparklingmutuals/SparklingMutuals.kt), `registerFeatures` and `resetSession`.
- Trigger, context checks and output: [AlertManager](../src/main/kotlin/net/johnceo/sparklingmutuals/alerts/AlertManager.kt); single-use generation: [WarpAlertTimer](../src/main/kotlin/net/johnceo/sparklingmutuals/alerts/WarpAlertTimer.kt); commands: [AlertCommand](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/AlertCommand.kt).
- Existing secondary coverage: [AlertDefaultsTest](../src/test/kotlin/net/johnceo/sparklingmutuals/config/AlertDefaultsTest.kt), [SafariAlertGhostTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/integration/SafariAlertGhostTest.kt). No test or Minecraft session was run for this documentation audit. Live checks should cover expiry/replacement, every cancellation route, both settings controls, quoted triggers and server warp permissions.

## Auto-party and Warp List

`/autoparty` and the Auto-party toggle (off by default) enable invitations from the saved Warp List. Its editor contains the 26 supported critters, with ordered space-separated IGNs under each. Invalid names are omitted; repeated names are invited at most once in a warp window.

A known positive Sparkling detection queues that critter's names independently of alert and party-announcer switches. Unknown particle detections do not invite. Invitations require an active `/warping` timer in the same Safari run, world and connection. Existing parties require the local player to be leader; a confirmed solo player can create a party. Unknown rosters wait for the existing party lookup.

One `/p <IGN>` is sent every two seconds. Players already known to be in the party are skipped, and the next name is tried whether or not the preceding invitation was accepted. Repeated detections do not restart a list. Other detected critters append their names in detection order. The scheduled warp deadline, a sent warp command, a server warp notice, cancellation, leaving Safari or disconnect stops invitations. Invitations already sent remain subject to Hypixel's normal expiry.

**Import CSV** in the Warp List editor replaces all saved entries immediately. Row 1 supplies case-insensitive critter headers; only rows 2-11 supply names, in order. Blank or invalid IGN cells and unsupported columns are ignored, and duplicates within a critter are removed. Missing or empty critter columns clear their old entries. CSV quoting and UTF-8 BOMs are supported. Cancelling, an unreadable file, malformed rows 1-11, duplicate critter headers or no supported headers leaves the current list unchanged. Content after row 11 is never parsed.

No new profile or ShinyHunter API requests are introduced. Warp List entries and the toggle persist in the main properties file; pending invitations do not. Existing saved warp delays are preserved; new installations and Reset use 23 seconds.

## Last updated

2026-10-29 - Fresh installations seed the maintainer-supplied JohnFounder Warp List (115 ordered name assignments). Existing configuration files keep their saved or empty lists; Auto-party remains off by default.

2026-10-29 — The Sparkle Warping introduction uses standalone full-width wrapped text at normal font size, with no option box or automatic shrinking.

2026-10-29 (America/New_York) — Added Warp List CSV replacement import using the header and ten name rows. Renamed controls to Sparkle Warping, `/warping` and `/warpingdelay`, changed the fresh default to 23 seconds, and added bounded Warp List auto-party invitations.
