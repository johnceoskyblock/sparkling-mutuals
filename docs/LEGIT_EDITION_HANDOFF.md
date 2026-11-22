# Sparkling Mutuals legit edition

Fork `johnceoskyblock/sparkling-mutuals` for the legit edition. Use the existing codebase as the starting point and make targeted edits. Keep the same mod structure, HUDs, commands, and Safari behavior wherever they’re unaffected by the changes below.

## Changes for the legit edition

- **ESP requires line of sight.** Keep boxes, glow, nameplates, and other markers for targets the player can currently see. Render them with normal terrain occlusion and respect normal render distance. Hide markers when the target is behind terrain, unloaded, or out of sight. Apply this to critters, teammates, floor drops, and structures, including any predicted or last-seen markers.
- **Sparkling detection alerts require line of sight.** Keep the alert when the local client can see and identify the sparkling critter. Particle hints, cached sightings, and WebSocket reports can still update internal state, but they can’t trigger a discovery alert or reveal the critter’s location before the player sees it.
- **Remove automatic gameplay and party actions.** Strip auto-party, auto-warp, reparty automation, auto-entry, auto-ticket selection, auto-clicking, automatic gem selection, automatic quest acceptance, leech auto-accept, and automatic reconnect or SkyBlock join. Audit the remaining features for automated movement, aiming, clicks, hotbar changes, or party-management commands. Remove the associated hooks, commands, and settings so an old config or incoming sync message can’t reactivate them.

**Keep Unique, Full Clear, and Sparkle mode logic 1:1 with Sparkling Mutuals.** Preserve mode gates, completion requirements, objective tracking, and backend sync, including shared critter progress, biome completion, and hotspot assignments. Apply the line-of-sight restrictions at the point where the mod displays a target or fires a discovery alert. Shared state should still update normally across biomes.

## Mirror the original GitHub settings

- Keep the fork public with `master` as the default branch. Enable Issues, Projects, and Wiki. Leave Discussions off.
- Enable merge commits, squash merges, rebase merges, auto-merge, and automatic deletion of merged branches. Leave branch-update suggestions and web commit sign-off requirements off. Match the original merge and squash commit-message defaults.
- Recreate the active **Master Ruleset** for the default branch. Block deletion and force pushes. Require a PR, resolved review conversations, and an up-to-date passing GitHub Actions `build` check. Allow all three merge methods, require zero approvals, and use an empty bypass list.
- Enable GitHub Actions and allow all actions and reusable workflows. Keep default workflow permissions read-only, leave Actions PR creation and approval off, and require approval for first-time contributors’ fork workflows. The original repo has no deployment environments.
- Compare the fork’s Settings and Rulesets against the original after setup. Configure any fork-specific credentials, webhooks, and secrets separately.

## Keep the build and release workflow

Preserve Java 25, the Gradle wrapper, Stonecutter, and the **26.1.2, 26.2, and 26.3** build matrix. Keep the aggregate `build` check and tag-based release workflow that verifies all three JARs before publishing. Update the hardcoded repository references in `.github/workflows/`, `buildSrc`, `scripts/Ship.ps1`, the mod metadata, and the updater so the fork publishes to its own repo. Give it a separate config namespace, preserve the license and credits, and verify the PR checks and all three JARs before the first release.
