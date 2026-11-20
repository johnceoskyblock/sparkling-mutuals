## Nameplates and custom names

- Reworked critter nameplates with shared **None / Vanilla / Marker / Both** display modes and per-critter visibility toggles. Nameplates now default enabled in **Marker** mode; Litterbug's individual marker defaults off. Saved preferences for the shared controls take priority.
- Added optional distance labels, catch-attempt/pity counts, and Eagle pet rarity settings. Identified capture attempts stay attached to the correct critter, including supported body changes; a new run resets pity tracking.
- Added server-confirmed Rockmite mound hit progress, displayed as `(0/5)` through `(4/5)` until the mound breaks. Ambiguous or stale hits do not assign progress to a guessed mound.
- Nameplates can follow ESP colors or use fixed, rarity, or first-capture colors. First-capture and rarity colors default **off**; Sparkling markers retain animated highlighting. Hideyho labels describe its encounter phase instead of capture pity.
- Replaced the old per-critter nameplate templates with shared controls while preserving custom critter aliases. Obsolete template settings are retired when saved.
- Custom critter names now also appear in rendered Hypixel chat and Hunting Box tooltips, including plural names and formatted text. Original chat/log messages, item data, tracking, and outgoing messages keep canonical names.

## Settings and presentation

- Reorganized customization into separate **Critter Names, Nameplates, Colors, and Other ESP** views, with clearer labels, descriptions, and matching search routes.
- Put common controls before specialized helpers and HUD content before appearance and placement. Group headers expand their options; on/off switches remain inside each group.
- Kept existing saved settings and HUD editor links through the presentation changes.
- Updated HUD/icon drawing and reduced bundled image and sound file sizes.

## Connection recovery

- Added independent **Auto-reconnect to Hypixel** and **Auto-join SkyBlock** options, both **off by default**.
- Auto-reconnect retries unexpected disconnects with backoff and stops on recognized connection denials. Leaving the disconnect screen cancels recovery.
- Auto-join can return from Limbo through Prototype, enter SkyBlock from Prototype, and join SkyBlock after an automatic reconnect.

## Performance and maintenance

- Reduced repeated work in ESP, HUD/icon rendering, retained Sparkling checks, Honeyhive label matching, bird-food inventory reads, and capsule projection rendering.
- Added a PowerShell shipping command to automate commit, rebase, PR checks, merge, and verified three-target releases, with a preview mode and regression tests.

Minecraft targets remain **26.1.2, 26.2, and 26.3**. Pick the JAR matching your Minecraft version.
