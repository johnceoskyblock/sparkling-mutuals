# Sparkling Mutuals

Use `johnceoskyblock/sparkling-mutuals` as the home for all editions. Use the [edition consolidation handoff](docs/EDITION_CONSOLIDATION_HANDOFF.md) for the migration and the [shipping guide](docs/SHIPPING.md) for commits and releases.

## Edition boundaries

- Keep tracking, modes, party sync, HUDs, and history in shared code. Apply shared changes once. Place edition-specific code and resources in separate source sets.
- **Standard** is the default build and the only Modrinth edition. Reuse the legit fork's line-of-sight behavior and tests. Exclude gameplay automation, through-terrain highlights of dynamic targets, and hidden sparkling detection and alerts from the JAR, including hooks, mixins, commands, and settings. Capsule trajectory geometry and fixed Snoozle wall waypoints may remain visible through terrain. Apply visibility rules to features that consume shared sync data and to critter contacts in the projection. Check uncertain behavior against [Hypixel's Allowed Modifications guide](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications).
- **Full** keeps the original features and settings. Use the `-full` filename suffix and offer opt-in downloads for the same edition and Minecraft version.
- **Full No Auto Update** has Full gameplay features and excludes updater code and settings from its JAR.
- All editions share `config/sparkling-mutuals`. Standard keeps Full-only values on disk when saving or resetting settings and leaves those values inactive. Add new Full-only properties to Standard's preservation rules.

## Development and shipping

- Preserve unrelated local work. When asked to ship, fetch and rebase on this repository's `origin/master`. Use one PR for the change and pass the required build check and repository rules before merging to protected `master`.
- Keep Java 25 and the Stonecutter targets for Minecraft 26.1.2, 26.2, and 26.3. After consolidation, verify all nine edition and Minecraft combinations from one tagged commit. Publish Standard, Full, and Full No Auto Update on GitHub. Enable Standard on Modrinth after review of the shared source link and distribution. [Modrinth Content Rules](https://modrinth.com/legal/rules/#3-cheats-and-hacks)
- The consolidated workflow builds all three editions. GitHub publishes all nine edition and Minecraft JARs and their manifest. Run the release workflow manually to rehearse verification before tagging. Keep the remote legit fork available until migration and Standard publication on Modrinth are verified.
- Shared code lives in `src/main` and `src/test`. Standard replacements and tests live in `src/standard` and `src/standardTest`. Changes to code that has an edition-specific replacement must be checked in each version of that file.
