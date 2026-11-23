# Edition layout and migration

Sparkling Mutuals ships Standard, Full, and Full No Auto Update from one repository. Standard started from the former legit fork’s gameplay and visibility changes. [Standard source history](https://github.com/1tsethan/sparkling-mutuals/commit/b297adf)

## Source sets

- `src/main` and `src/test` contain shared tracking, modes, sync, HUDs, and history, including the default Full implementations.
- `src/standard` and `src/standardTest` contain Standard-specific behavior, exclusions, resources, and tests. Standard shows visible critters and local sparkling detections, with generic floor-drop highlights. Fixed Snoozle waypoints, known Torrhus Honeyhive markers, and the optional through-terrain capsule path remain available.
- `src/noUpdates` and `src/noUpdatesTest` cover Standard and Full No Auto Update. Full uses the verified downloader and restart installer, with automatic downloads off by default.

## Switching builds

Install one JAR that matches your Minecraft version. Older combined releases used a plain filename, so install the explicitly suffixed `-full` JAR manually when moving an old Full install to the consolidated builds. Shared settings and records live at `config/sparkling-mutuals`. Missing files from the former Standard folder are imported once, and preferences for other editions are preserved. [Installation and filenames](../README.md#installation) · [Configuration storage](CONFIG_DEFAULTS.md)

## Distribution

GitHub releases contain all three editions for each of the three Minecraft versions. Modrinth publication of the verified Standard JARs is pending review. The former fork remains a migration reference. [Build matrix](MULTI_VERSION_BUILDS.md) · [Shipping](SHIPPING.md) · [Modrinth draft](MODRINTH_DRAFT.md)
