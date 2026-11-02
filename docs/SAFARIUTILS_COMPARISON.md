# SafariUtils comparison and adaptation notes

Reviewed on 2026-10-31 against this repository and SafariUtils commit [`2e572ab`](https://github.com/Serkosea/safariutils/commit/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8), released as v3.3.0. This is a source and documentation comparison. It does not include a live Minecraft side-by-side visual test, so claims about code structure and stated behavior are firmer than subjective appearance claims.

## Executive summary

The two mods overlap in their central job: client-side Critter Safari run tracking, progress and missing-catch displays, Sparkling support, party coordination, encounter/objective markers, Miria's Contest support, and quality-of-life automation. SafariUtils is not just a small add-on beside SparklingMutuals; it has independently built many of the same player-facing systems.

The strongest adaptation case is presentation. SafariUtils has a purpose-built settings application with search, themed cards, responsive controls and remembered navigation, plus a dedicated HUD layout editor with live content, placeholders and alignment snapping. SparklingMutuals already has a sizeable settings model, themes and HUD editor, but its settings are presented through MoulConfig categories and accordions. The useful lesson is to improve information hierarchy, navigation, and preview/edit interactions; wholesale copying its screen would be a large rewrite and would duplicate the existing settings model.

For ESP, SafariUtils' clearest visual differentiator is model glow for a small set of critters and Rockmite mounds on Minecraft 26.2. That may look cleaner than a field of boxes. However, SparklingMutuals already has more Safari-specific identification and capture-aware rendering logic, plus capture projections and floor-drop classification. SafariUtils' marker pipeline is worth studying for a unified visual style and consistent eligibility, but its model glow is an optional presentation idea, not evidence that its detection logic is broadly superior.

## Where the mods are almost identical

| Area | Shared purpose | Practical comparison |
| --- | --- | --- |
| Run/progress HUDs | Show catches or remaining work during Safari | Both track runs and offer movable HUDs. SafariUtils emphasizes a compact progress, missing-critters, party-objectives and contest set. SparklingMutuals has progress, missing, catches, sparklings and biome/resource panels. |
| Run history and statistics | Retain run and collection results | Both record history and expose statistics/profit. SafariUtils has detailed run views, per-player breakdowns and Personal Bests. SparklingMutuals has history/log import and repair, achievements, PBs, cached rates and ticket-specific shard/essence profitability. |
| Sparkling support | Identify, announce or organize Sparkling catches | Both support Sparkling mode, configurable alerts/effects, saved collection totals, and party-shared discovery information. The commands and the party data paths differ. |
| Safari markers and ESP | Help locate critters or useful Safari targets | Both draw per-critter markers and objective/resource hints with settings for visibility and color. The rendering style and species/target rules differ substantially. |
| Party support | Coordinate party readiness, progress and ticket entry | Both provide party-oriented status, ticket safeguards/reminders, objective progress and chat helpers. SafariUtils adds network party sync and ticket-trading automation; SparklingMutuals has a broader command and warp workflow. |
| Contest and alerts | Track Miria's Contest and alert on time/status | Both have contest timers/HUD and configurable alerts, with different contest workflows and warning details. |
| Common Safari QOL | Reduce repetitive input and noisy UI | Both include Hideyho acceptance, bird-feeder-related alerts, configurable starting-item messages, selectable HUDs/settings, and chat/visual controls. |

SafariUtils describes these public-facing features in its [README](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/README.md). SparklingMutuals' current feature entry points are in the [project README](../README.md) and [documentation index](README.md).

## What SafariUtils appears to do better or differently

### Settings experience

SafariUtils uses a custom settings screen rather than a generic config-category view. Its screen is arranged as a navigable workspace with areas/tabs, inline search, grouped cards, accordions, shared controls, theme pickers and settings editors. Search can jump to a setting and its parent context; tab scroll and expanded groups are retained. Its shared `SettingsUiKit` centralizes drawing/interaction primitives, and `SettingsMotion` animates small menu state changes without moving card geometry. See [`SafariSettingsScreen.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/SafariSettingsScreen.java), [`SettingsUiKit.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/SettingsUiKit.java), and its [settings UI design notes](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/docs/development/SETTINGS_UI.md).

SparklingMutuals has many of the same raw controls, custom theme and appearance support, and grouped MoulConfig categories in [`SafariSettings.kt`](../src/main/kotlin/net/johnceo/sparklingmutuals/config/SafariSettings.kt) and [`SafariConfigScreen.kt`](../src/main/kotlin/net/johnceo/sparklingmutuals/config/SafariConfigScreen.kt). The gap is mainly the information architecture and presentation layer: MoulConfig renders the settings model, whereas SafariUtils owns its full workspace layout and controls.

### HUD layout editor

SafariUtils' [`HudEditorScreen`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/HudEditorScreen.java) displays the actual HUD contents when available, useful placeholders when not in a run, and supports drag-to-move, scroll-to-resize, center/edge snapping, reset and a snap toggle. SparklingMutuals already exposes `/sm gui` for dragging and resizing, but the documented workflow is simpler. A clearer editor, previews outside Safari, and optional snap guides are low-risk polish candidates.

### ESP appearance

SafariUtils centralizes marker collection in a registry and renders marker styles through one waypoint renderer. It also offers Minecraft model outlines for Hideonwall, Hideonfloor, Duplico, Bloodbat and Rockmite mound models on 26.2 through Render Chest; its README says 26.1.2 keeps standard markers. See [`Markers.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/Markers.java), [`WaypointRenderer.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/WaypointRenderer.java), [`SafariGlows.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/SafariGlows.java), and its [release notes](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/RELEASE_NOTES.md).

That glow can make a target read as the actual critter instead of adding a bright rectangular outline around it. It is a plausible source of the “cleaner ESP” feedback. The scope is limited: it only supports selected model types/species and the newer target version, and glow ownership depends on matching the live rendered model to a unique critter sighting. It is not a replacement for labels, floor/objective markers or through-wall boxes in every case.

SparklingMutuals' renderer in [`SafariEsp.kt`](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/esp/SafariEsp.kt), [`SafariEspRules.kt`](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/esp/SafariEspRules.kt) and [`WorldRenderCompat.kt`](../src/main/kotlin/net/johnceo/sparklingmutuals/compat/WorldRenderCompat.kt) is more specialized: it identifies entities through species textures/types, filters stale static models, retains capture context and model-label relationships, adjusts body bounds, handles moving targets and projects Shyworm/Litterbug details. SparklingMutuals also has capsule capture projection guides and per-kind floor-drop labeling. These mechanics are established strengths to preserve while experimenting with style.

### Additional SafariUtils-only or more prominent features

- **Ticket Trading:** draggable invite queues, guest rules per player, Sparkling filters and timed party automation. SparklingMutuals has Sparkle Warping, Auto-party Warp Lists, Duo/leech handling and ticket selection, but not this same ticket-exchange interface/workflow. See [`TicketTradingScreen.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/TicketTradingScreen.java) and [`TicketTrading.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/TicketTrading.java).
- **Automatic party data sync:** SafariUtils advertises shared objectives and Sparkling discoveries, with known non-empty inventory rows. SparklingMutuals already has party discovery/API lookups and coordination, but does not appear to use the same automatic party-sync protocol. This may be valuable for group readiness, though it introduces protocol, consent, stale-data and trust questions; treat it as a separate design discussion.
- **Dedicated goal marks:** SafariUtils surfaces confirmed Gemzie podium, Icy Wumpa, Haunted candle and Forest Birdfeeder progress with selectable biome-clear marks in the Party Objective HUD. SparklingMutuals tracks many biome clears, hotspots, nests, snoopers, gems, birds and drops, but the views and evidence policy are different.
- **Per-player guest controls and player-facing release/update UI:** These are additional surfaces rather than core Safari tracking improvements.
- **Explicit Safe Mode and Extra builds:** SafariUtils separates ordinary-use features from some additional-information markers, with Extra flagged by its author as potentially unsafe. SparklingMutuals does not ship a comparable edition split. This difference matters for floor-item information and should not be treated as a feature gap without first deciding the mod's policy.

## SparklingMutuals' comparative strengths

These are reasons to adapt specific interaction patterns rather than port whole subsystems:

- **Safari modes and helper depth:** Unique, Full Clear and Sparkling modes have distinct behavior and ESP gates. SafariUtils' documented feature set centers on run tracking, Sparkling Mode and general objectives, without equivalent emphasis on the three-mode workflow.
- **Capture guidance and capture-aware state:** Capsule path/projection guide, contact geometry, capture lifecycle, and Sparkling encounter ownership go beyond a conventional waypoint marker.
- **Deep species-specific detection:** Separate rules and projections account for models, labels, visibility, stale mobs, capture transitions and hidden-body species. These rules are a major part of the existing ESP value.
- **Party workflow:** Commands for mutual/missing discoveries, tickets, biome remaining work, readiness/downtime, PB queries, streaks and Sparkle Warping / Auto-party are broader than SafariUtils' documented helpers.
- **History and local analysis:** History import/repair, achievements, biome and mob PBs, cached Sparkling rates, profitable-critters rankings and party streak commands provide a stronger local analytics/command surface.
- **Version breadth:** This checkout targets 26.1.2, 26.2 and 26.3; SafariUtils v3.3.0 documents 26.1.2 and 26.2. Any rendering feature should be checked against SparklingMutuals' own version compatibility layer rather than imported with a dependency assumption.

## Recommended adaptations, in priority order

| Priority | Adaptation | Why it fits | Suggested scope |
| --- | --- | --- | --- |
| 1 | Settings navigation and search polish | Many controls already exist; discoverability is the visible gap. | Prototype a left-side area/tab navigator, searchable settings with context, remembered scroll/expanded state, and consistent card/control spacing. Keep `SafariSettings` and persisted config as the source of truth. Evaluate an incremental custom shell around existing categories before replacing MoulConfig. |
| 2 | HUD editor preview and snap guides | Improves the existing `/sm gui` without changing run logic. | Add placeholders outside a Safari run, center/edge snap guides and a visible snap toggle/reset. Keep the existing drag and resize behavior. |
| 3 | ESP style choice | Directly addresses “cleaner ESP” while preserving detection. | Add an optional model-outline style only where model identity is unique and the version/render backend supports it. Keep current boxes/labels available for unsupported critters and versions. Test entity overlap, transparency, resource packs, Iris/shaders, depth behavior and performance in-game. |
| 4 | Marker visual hierarchy | Reduces clutter across critters, resources and objectives. | Review line weight, fill/occlusion, label scale/distance, duplicate labels and distance/importance fading; offer a few coherent presets rather than dozens more independent toggles. Reuse existing visibility/evidence gates. |
| 5 | Ticket exchange design | New social workflow if user demand exists. | First define the desired trade/queue rules, invite confirmation, party-leader constraints, cancellation and anti-spam behavior; then decide whether it belongs alongside Warp Lists or as a separate tool. |
| 6 | Automatic party sync | Could make shared objective progress more immediate. | Specify what is sent, when it expires, how users opt in, how disagreement is shown, and how messages are bounded before considering a protocol. Existing chat/API functionality may already cover the use case. |

## Things not to copy blindly

- Do not replace SparklingMutuals' species identification, capture ownership or visibility logic with SafariUtils' marker scan. Similar outputs do not imply equivalent capture semantics.
- Do not make model glow the only ESP path. Several critters/resources lack supported render models, version parity differs, and existing boxes/labels communicate different information.
- Do not adopt SafariUtils' floor inference or Extra behavior solely because it is implemented. Decide the desired evidence/safety policy first; SparklingMutuals already has its own per-drop prediction and visibility gates.
- Do not replace settings storage or settings ownership to copy the custom screen. The value is the workspace, shared visual primitives and navigation behavior; preserve the current config migration and saved settings.
- Do not call one mod categorically “cleaner” based on source alone. The two UIs need a live comparison at matching GUI scale, theme and screen resolution, and the ESP styles need side-by-side gameplay review.

## Best next workshop decisions

1. Agree on what “cleaner UI” means for our users: easier settings discovery, fewer sections, better visual theme, a full redesign, or better in-game HUD placement.
2. Choose whether ESP cleanup means less visual clutter, clearer labels, model outlines, better filtering by current goal, or all of these as selectable presets.
3. Get comparable screenshots or gameplay captures of both mods at the same resolution/GUI scale and in the same biome. Source review cannot settle taste or shader compatibility.
4. Pick a small first slice—settings search/navigation or ESP style presets are the most direct candidates—and retain existing tracking behavior.

## Source set

- SafariUtils [README](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/README.md), [settings UI notes](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/docs/development/SETTINGS_UI.md), [renderer](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/WaypointRenderer.java), and [release notes](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/RELEASE_NOTES.md).
- SparklingMutuals [README](../README.md), settings [`SafariSettings.kt`](../src/main/kotlin/net/johnceo/sparklingmutuals/config/SafariSettings.kt) and [`SafariConfigScreen.kt`](../src/main/kotlin/net/johnceo/sparklingmutuals/config/SafariConfigScreen.kt), and ESP [`SafariEsp.kt`](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/esp/SafariEsp.kt), [`SafariEspRules.kt`](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/esp/SafariEspRules.kt), [`WorldRenderCompat.kt`](../src/main/kotlin/net/johnceo/sparklingmutuals/compat/WorldRenderCompat.kt).
