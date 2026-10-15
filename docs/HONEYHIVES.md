# Torrhus Canyon Honeyhives

Personal honeyhive waypoints show a green beacon and outline when the hive is available. Hide both while its personal refill deadline is in the future. A hive without a known cooldown is provisionally treated as available; an unobserved hive is not a confirmed loot or server readiness event.

## Evidence and state transitions

| Evidence | Effect on the matched hive |
| --- | --- |
| Current server hologram `Refill in: 23m 33s` | Empty until observation time plus the displayed countdown; hide the beacon/outline. |
| Current server hologram `Click to loot!` | Full; clear its cooldown and show its beacon/outline. |
| Own confirmed `HIVE!` loot response following a hive click | Empty; start the existing one-hour fallback refill timer. A click alone does not prove a loot. |
| `QUEEN BEE! The Honeyhive instantly refilled with loot!` following the confirmed loot | Immediately full again; clear that hive's cooldown and restore its beacon/outline. |
| Deadline reached | Available again; show its beacon/outline unless new authoritative evidence establishes another cooldown. |
| Vanilla bee-nest `honey_level` | No personal readiness change. The decorative block can disagree with the player's hologram. |
| Missing/unloaded hologram or chunk | Retain existing state; absence of a label does not prove full or empty. |

Queen Bee applies only to the hive associated with the preceding confirmed loot, not every hive or whichever hive is currently closest. Keep its confirmed full state against delayed empty holograms and decorative block changes until another confirmed loot of that hive clears the Queen Bee override. This full override survives reconnects. Loot updates have a two-second grace against stale server holograms; delayed Queen Bee messages can still follow a repeat click.

## Reading the server data

- Read both `Display.TextDisplay.text` and ordinary entities' custom names, including armor-stand holograms. Use only current, non-removed entities still registered in the current world.
- Strip Minecraft formatting, normalize non-breaking spaces and inspect each line separately. A multiline `Honeyhive` title plus `Refill in: ...` must expose its status line; the title itself is not a status. Support minute/second countdowns and case-insensitive status text.
- Match a status line to its nearest known hive, within 1.5 blocks horizontally of its center and between the hive's Y and Y + 4. Reject equal-distance ambiguous associations. Neighboring hives must not share a status line. If both full and countdown labels are loaded for one hive, prefer the countdown unless a confirmed Queen Bee override applies.
- Inspect loaded known nest positions only. Survey nearby loaded chunks to learn nest-on-fence/trapdoor structures; retain the excluded non-existent position `-730,132,184`.
- Refresh the beacon list once per second while enabled on Torrhus Canyon. Keep existing rendering range, contest-complete suppression and beam-renderer fallback behavior.

## Player/profile ownership

Cooldowns belong to the local player and SkyBlock profile. A missing profile join line must not disable current hologram observations: track them provisionally so visible empty/full state still updates. On profile discovery, promote only confirmed provisional loot/Queen Bee events into the known profile's saved state. Provisional hologram observations must not overwrite that profile's saved deadlines during promotion; the next live scan can verify the known profile normally.

Keep click ordering and the five-second response window. Unmatched or expired loot/Queen Bee messages and quoted party messages cannot choose an arbitrary nearby hive. Save confirmed per-profile deadlines and Queen Bee readiness using the existing version-2 properties format. Do not use the API's potentially delayed state to overwrite current server observations.

## Diagnostics and Minecraft checks

`/sm debug` includes honeyhive context, loaded block states, nearby saved deadlines/Queen Bee overrides and raw hologram carrier text/positions in `latest.log`. Raw labels are useful when the server changes its text or entity layout; block state is logged for comparison rather than used to determine personal readiness.

1. Stand by the empty hives shown in the screenshot. A label such as `Refill in: 23m 33s` should hide that hive's green beacon/outline within one scan; an adjacent full hive should remain highlighted.
2. Loot a full hive and confirm it becomes empty. Proc the exact Queen Bee message: that same hive returns to full immediately, other hives stay unchanged, and delayed old countdowns cannot undo it. Loot it again to start its new cooldown.
3. Wait for a refill or observe `Click to loot!`; confirm availability returns. Leave/reenter the area or switch servers and verify the correct player's/profile's saved state remains.
4. Test both text-display and armor-stand status carriers, including multiline/colored text. A generic Honeyhive title, an unloaded label or a vanilla honey level must not invent a personal loot.
5. If a beacon still disagrees with its hologram, run `/sm debug` next to that hive and inspect the raw label, coordinates and profile-known flag. Automated tests do not establish the actual live server entity layout.

## Last updated

2026-10-15 - Documented personal hive evidence, exact Queen Bee refill behavior, hologram association, provisional profile handling and diagnostics; implemented the corresponding observation fixes. Live Minecraft validation remains pending.
