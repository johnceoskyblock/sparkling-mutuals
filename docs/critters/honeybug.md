# Honeybug

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Forest. **Rarity:** Uncommon. Use the actual bee-producing nest count from the initial boat-exit survey. Keep that count and each classified site for the whole run; unloaded or later empty blocks cannot erase it. Beehives are not bee-producing nests. A new run resets the survey.

## Captures and source evidence

Let **N** be the verified initial actual nest count, **C** shared Honeybug captures, and **U** currently rendered uncaptured bees. Personal captures add one personal and shared catch; loot shares add one shared catch. Keep catches received outside Forest. Historical bee UUIDs are not additional captures.

All nests are accounted for once every actual nest was personally punched or **C + U >= N**. In particular, **C >= N** accounts for the entire known nest population without needing to render those caught bees again. A punch or sighting does not itself add a catch. Once classified, retain the initial nests/sites after disappearance and unloading. Newly identified real nests can increase N; an incomplete initial survey cannot invent a verified total.

## Mode behavior

| Mode | Progress | Species row |
| --- | --- | --- |
| Full Clear | Capture N Honeybugs, using shared capture messages from any biome. No local empty scan, personal punch history or party report is needed once N is known. Known extra population and actual pending Sparklings remain blockers. | Clears when the actual capture requirement passes. A report alone cannot supply captures. |
| Unique, Sparkling Honeybug already owned by everyone | One shared Honeybug catch. | Disappears after one shared catch. |
| Unique, someone needs Sparkling Honeybug | One shared catch **plus** local nest checking or a trusted party Forest-done (`fd`) report. Outside Forest, trust the party report for Sparkling confirmation. | Still disappears after one shared catch, even while progress awaits checking/report confirmation. |
| Sparkling, someone needs Honeybug | Local nest checking or a trusted `fd`; no ordinary capture requirement. | Disappears when checking is complete. The panel is **Unchecked**. |

A trusted `fd` remains valid for Honeybug after entering Forest later, leaving again, or changing modes. Missing bees in render range, incomplete later scans and unpunched local history cannot disprove the party member's completed check. Reports supply checking trust only: Unique still needs a real shared catch, and Full Clear still needs the retained capture total. A detected uncaught Sparkling overrides report trust in every mode; retain that capture obligation through unloading. The shared confirmed-Sparkling capture override remains available in Unique/Sparkling.

## Highlights and helpers

In Full Clear, retain highlights on unpunched actual nests until shared Honeybug captures reach the retained verified nest count. Neither fd nor captures plus currently rendered bees can hide those remaining nests. Punching an individual nest immediately removes its waypoint; when every nest is punched there are no nest waypoints even if bees still need catching. Unique/Sparkling retain their existing local-check and trusted-fd behavior.

Keep nest work separate from the species row. In Unique, needed nest helpers may remain after the first catch until local sources are resolved or a trusted `fd` is received. When no party member needs Sparkling Honeybug, the first catch also hides its Unique helper. Sparkling shows nest helpers only for a needed discovery. Full Clear still tracks its actual captures after source work is resolved/reported.

Ordinary Honeybug ESP: Full Clear keeps uncaught bees highlighted, Unique stops after one shared catch, and Sparkling shows no ordinary bee ESP. Positive Sparkling detection/capture tracking is independent of those switches.

## Minecraft checks

- On first boat exit, confirm N is recorded from actual nests. Move to another biome, unload Forest and collect loot shares: N stays fixed, and C reaching N completes Full Clear Honeybug without returning for a render scan.
- In Unique with a needed discovery, capture one bee outside Forest: its missing row clears but progress waits for fd. Receive fd and verify progress; enter Forest with incomplete rendering and retain that checking trust. Without a needed discovery, one catch is sufficient.
- In Sparkling, inspect/punch all nests locally or receive fd: no ordinary catch is required. An unconfirmed check keeps Honeybug listed under Unchecked. Repeat Forest entry after fd and retain completion.
- Start a new run and confirm no prior nest/report history carries over. A positively detected uncaught Sparkling must still block all modes.

## Last updated

2026-10-17 - Full Clear retains unpunched nest highlights until the known nest population is captured; fd and rendered bees cannot hide them. Unique/Sparkling report trust is unchanged.

2026-10-17 - Retained the initial nest count for run-wide capture-based Full Clear, separated Unique Honeybug progress from its first-catch missing row, and retained party fd checking trust through Forest entry. Sparkling uses Unchecked across every biome.
