# Warp alert and automatic party warp

When **`/alert` is enabled**, the real Safari Hunting Hotspot message starts the configured delay. At expiry, show the existing Warp alert and send **`/p warp` once** to bring the party into the run. The default remains **25 seconds**, adjustable with `/alertdelay <seconds>` or **Warp reminders** settings, from 1 to 86400 seconds.

The existing `/alert` switch controls both the visible alert and automatic warp. Settings text and command feedback describe both actions. No extra enable step is required.

## Timer lifecycle

- Accept an exact trusted system Hotspot line for Forest, Cavern, Icy or Haunted. Player-chat quotes and overlay messages never start the timer.
- The latest Hotspot replaces an earlier pending timer. A timer's expiry can act only once; stale callbacks cannot trigger a newer timer's warp.
- At expiry, require the feature still enabled, a player in the same active Safari run, and the same client connection and world/instance as scheduling. A later run in the same world cannot inherit the timer. Leaving the run, changing instances or disconnecting must not warp into a later location.
- Disabling `/alert`, changing the delay or disconnecting cancels the pending timer and clears its alert. Enabling it later does not resurrect that timer; wait for the next real Hotspot.
- Send the standard command through the current Minecraft connection. The server processes its usual party-command permissions; the mod does not claim a successful warp merely because the command was sent.

## Minecraft checks

1. Enable `/alert`, use a short `/alertdelay`, and enter a real run as party leader. At expiry expect the Warp alert and one `/p warp`, with party members brought into that run.
2. Disable the feature before expiry: no alert or warp. Repeat after changing the delay, leaving the Safari, switching instances and disconnecting/reconnecting; none may fire an old timer.
3. Trigger a replacement Hotspot/new run before an older timer expires: only the newest valid timer may act. Quoting a Hotspot in party chat must not schedule or replace a timer.
4. Restart to verify the enabled choice and configured delay persist. Test both command and settings controls.

## Last updated

2026-10-15 - Added automatic /p warp with trusted triggers, cancellation and binding to the exact active run, including same-world run replacement.
