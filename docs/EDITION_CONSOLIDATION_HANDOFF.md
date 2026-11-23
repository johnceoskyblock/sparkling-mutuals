# Sparkling Mutuals edition consolidation

Consolidate Sparkling Mutuals into `johnceoskyblock/sparkling-mutuals`. Use the legit fork’s implementation and tests as the starting point for Standard. Keep Unique, Full Clear, and Sparkle mode logic, backend sync, HUDs, and history in shared code so future features and fixes only need one PR. [Legit fork implementation](https://github.com/1tsethan/sparkling-mutuals/commit/b297adf)

## Build three editions

The local consolidation retains three Stonecutter targets with edition source overlays and the manual release rehearsal. PR #138's Full downloader and restart installer use the explicit Full filename, with updater classes and controls excluded from both other editions. Automatic downloads default to off. Windows CI covers restart installation with the running JAR locked. Standard publication on Modrinth and retirement of the legit fork remain pending Modrinth review and migration verification.

- **Standard** is the default build and the only edition published to Modrinth. Keep the fork’s line-of-sight ESP and sparkling detection. Compile out gameplay automation, through-terrain ESP, and hidden sparkling alerts, including their hooks, commands, mixins, and settings. Use Modrinth’s normal update flow for Standard.
- **Full** ships on GitHub with the original features and configs. The update screen offers **Auto Update** and **Download Update**. A staged Full download displays **Ready to install on restart**.
- **Full No Auto Update** ships on GitHub with the same gameplay features as Full. Build it without automatic update checks or the in-game downloader and installer.

## Implement the Full updater

Use Phantom Addons’ flow as the reference. Check GitHub Releases, select the asset for the same edition and exact Minecraft version, download it off the game thread, and stage the install for restart. Make automatic installation opt-in so existing installs keep their current check-only behavior until the player enables it. Show download progress and a clear retry state if it fails. [Phantom Addons update checker](https://github.com/PhantomOrigin/Phantom-Addons/blob/main/src/main/java/com/phantomaddons/UpdateChecker.java) · [Installer](https://github.com/PhantomOrigin/Phantom-Addons/blob/main/src/main/java/com/phantomaddons/UpdateInstaller.java)

Verify the download’s SHA-256 against the verified release manifest and check the JAR’s mod ID, edition, Minecraft target, and version before replacing anything. Keep the installed JAR usable if the download, verification, or swap fails. Replace only the current Sparkling Mutuals JAR and keep the last working copy outside the active `mods` folder for rollback. Test restart installation on Windows with the running JAR locked.

## Keep one build and release workflow

Keep the original GitHub settings, `master` ruleset, required `build` check, Java 25, and Stonecutter setup. Build all nine JARs from one tagged commit across Minecraft **26.1.2, 26.2, and 26.3**. Publish all nine edition and Minecraft JARs to GitHub. Publish the three verified Standard JARs to Modrinth only after review. Make the Modrinth upload reject any artifact that fails Standard verification. Confirm with Modrinth how to link the shared source repo before listing Standard. [Modrinth Content Rules](https://modrinth.com/legal/rules/#3-cheats-and-hacks)

Set `release_artifact_layout=editions` in `gradle.properties` when the tag workflow can publish the following GitHub assets for a single release version.

- Standard uses `sparkling-mutuals-<version>+mc<minecraft>.jar`.
- Full uses `sparkling-mutuals-<version>-full+mc<minecraft>.jar`.
- Full No Auto Update uses `sparkling-mutuals-<version>-full-no-auto-update+mc<minecraft>.jar`.
- Publish `build-manifest.json` from the verified build for all nine GitHub JARs. Reuse the existing schema with `version` and `files`, where each JAR lists `name`, `minecraft`, `size` in bytes, and `sha256`.

Use `config/sparkling-mutuals` for all editions. Import missing saves from the old Standard folder once and keep existing shared files when both folders have a copy. Standard preserves Full-only values during saves and resets. Test upgrades from both existing editions, including an interrupted download and a mismatched JAR. Once Standard is live and the consolidated release is verified, archive the legit fork with a migration notice.
