# License and attribution inventory

Last updated: 2026-11-23 (America/New_York). Reviewed the existing source notices and aligned project metadata and packaged licenses.

## Project distribution

Sparkling Mutuals is distributed under **LGPL-2.1-only**, including modifications to the SkyHanni-derived code already identified as LGPL in source headers and the notices below. The complete license is in the repository's [LICENSE](../LICENSE). Fabric metadata uses the same SPDX identifier for every edition. This project is not advertised as wholly public domain.

The earlier CC0 text is preserved in [SparklingMutuals-Original-CC0-1.0.txt](SparklingMutuals-Original-CC0-1.0.txt). Previously granted CC0 rights for original contributions remain available; this change does not revoke those grants or put third-party work into the public domain. File-level and upstream notices remain applicable. Compatible permissive components retain their attribution and original license terms.

## Adapted components

| Source | Included license / notices | Identified use |
| --- | --- | --- |
| SkyHanni | [LGPL-2.1](SkyHanni-LGPL-2.1.txt), [skills](SkyHanni-Skills-NOTICE.txt), [Phantomleaf](SkyHanni-Phantomleaf-NOTICE.txt), [Ubik timer](SkyHanni-Ubiks-Cube-NOTICE.txt), [shelved values](SkyHanni-Value-NOTICE.txt) | Skill overflow and XP feedback, Phantomleaf Solver, Ubik cooldown and Safari area data; historical item-value adaptations remain under tools/shelved. |
| SafariUtils | [MIT](SafariUtils-MIT.txt), [party-sync provenance](SafariUtils-PartySync-NOTICE.txt) | Settings/HUD presentation, Safari tracking helpers, and shared party-sync interoperability. |
| CritterMod | [MIT](CritterMod-MIT.txt) | Critter roster, quotas, progress presentation and Safari tracking. |
| ShinyHunter | [MIT](ShinyHunter-MIT.txt) | Profile/capture lookups, quest prompt handling and presentation helpers. |
| Nebulune | [BSD-3-Clause](Nebulune-BSD-3-Clause.txt) | Critter model identification and rendering offsets. |
| SkyOcean | [SkyOcean License v1](SkyOcean-LICENSE.md) | Code and visual-flow inspiration for Doomspiral Lottery. Its code license is MIT; its non-code assets are reserved and are not relicensed here. |
| NotEnoughUpdates-REPO | [MIT](NotEnoughUpdates-REPO-MIT.txt) | Constants retained with shelved item-value tooling. |

Upstream links, authors and revision details are retained in the notices and README credits. Preserve these notices when copying or modifying the corresponding components. Do not infer permission for unrelated assets from a project's code license.

## Project assets and dependencies

The maintainer confirmed on 2026-11-23 that the mod icon, Doomspiral Lottery image, and Max Win image/audio were created for Sparkling Mutuals. They are project assets, not a redistribution of SkyOcean's reserved artwork or audio. This records the maintainer's provenance statement; it does not claim endorsement by another project.

Nested dependencies retain their own metadata and licenses. HM-API's notice is included by the build as licenses/HM-API-LICENSE. Fabric, Minecraft and other external runtime dependencies keep their own terms; the project's license does not relicense them.

## Source and redistribution

Both installable and sources JARs include LICENSE_sparkling-mutuals and the licenses directory. Corresponding source, resources and build scripts are available from the same public repository and release tag as the binaries: https://github.com/johnceoskyblock/sparkling-mutuals. Keep release tags and source access available when distributing binaries. GitHub's source archives identify the matching tagged checkout.

For the Modrinth listing select LGPL-2.1-only and retain the third-party attribution link. Standard is the only edition proposed there; submission and approval remain pending. A license declaration does not replace the separate edition-content and distribution review.
