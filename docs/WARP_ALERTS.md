# Warp reminder and automatic party warp

## Purpose

An enabled warp reminder shows a local **Warp** alert and sends `/p warp` once after a Safari Hunting Hotspot. Sending the command does not establish that the server accepted or completed a warp.

## Controls and defaults

| Control | Default | Effect |
| --- | --- | --- |
| `/alert` or **Warp reminders** switch | Off | Enables both the alert and automatic command. |
| `/alertdelay <seconds>` or settings delay | 25 seconds | Accepts 1–86400 seconds; changing it cancels the pending timer. |

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

## Last updated

2026-10-19 (America/New_York) — Audited trigger registration, timer context, cancellation and command output against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`. See [documentation conventions](DOCUMENTATION_GUIDE.md) and [audit limitations](IMPLEMENTATION_AUDIT.md).
