# Fix Forest bee nest highlighting and prevent premature Honeybug completion

## Problem

In the current version, Forest bee nests are not highlighting. Earlier versions highlighted them only when the player was close. Honeybug completion has also been marked too early after three captures when no further nests were highlighted, even though additional nests could be outside the currently loaded area.

Forest has 13 potential nest/hive locations. A run contains 3–6 Honeybugs associated with actual bee nests. Three is the minimum; six is the possible maximum. Do not require six in every run, and do not assume three is the total simply because no additional nests or bees are currently visible.

## Implementation task

Investigate and implement the fix in this repository. Read applicable repository instructions and the Safari mode documentation if present. Preserve unrelated work. Deliver code, regression tests, and an explanation of the verified cause and resulting behavior.

### 1. Restore reliable nest detection and highlights

- Check every potential Forest site whose block data is available to the client, across all elevations. Detect valid nests without requiring the player to stand close to each nest when its data is already loaded.
- Verify the 13 coordinates against available evidence. Investigate block classification, biome/run gating, mode/party helper visibility, render callbacks, and rendering filters. Do not assume the rendering pipeline or coordinates are the cause without evidence.
- Highlight actual unpunched bee nests using the existing outline/waypoint style when the active mode requires them. Ordinary beehives are alternative site classifications and should not be highlighted as nests to punch unless investigation establishes different game behavior.
- Keep evidence collection independent of HUD/ESP visibility. Removing a highlight after an interaction must not imply the resulting Honeybug has been captured.

### 2. Prove survey coverage before declaring nests handled

- Prefer surveying all 13 sites immediately when their actual block data is available. Client code must not assume it can request or force the multiplayer server to send unavailable chunks.
- Track unresolved sites explicitly. An unloaded or unverified location is unknown, not an empty site, and never evidence that no additional nest exists.
- If all sites cannot be inspected on entry, add useful survey/render coverage guidance so the player knows which areas still need to be loaded. Mark coverage only after the relevant sites have actually been inspected; entering a radius alone is insufficient if their data is unavailable. Account for all elevations and varying client/server view distances.
- Distinguish known nests remaining from sites still unverified in the helper UI. A display of zero known nests must not suggest the survey is complete while unknown sites remain.
- Retain validated site classifications and nest interactions when chunks unload or the player revisits Forest within the same run. Deduplicate interactions and clear survey state for a new run. Preserve a punched nest's evidence if the server removes its block afterward.
- Local nest completion requires all 13 sites reliably classified, at least three actual nests found, and every actual nest handled. A fully surveyed three-nest run can finish; a six-nest run requires all six. Do not invent a fixed radius or classify arbitrary air/invalid blocks as verified absence without establishing that this is safe.

### 3. Keep completion consistent with the active mode

- Full Clear requires complete nest evidence and capture evidence for all Honeybugs required by the verified run, including any released but not yet observed/captured. Three captures plus an empty local scan must not complete a run with unresolved sites or additional bees outstanding. Audit whether the current observed-UUID check can miss a bee released outside entity tracking range.
- Preserve Unique's one-capture requirement and its additional nest/discovery checks when the party still needs the Honeybug sparkling. Preserve Sparkling's nest-check behavior rather than adding a Full Clear capture quota to it.
- Audit progress, helper visibility, Forest clear status, automatic `fd` announcements, and applicable personal-best checks so local completion does not use absence of highlights as proof. Preserve documented party/manual overrides and shared versus personal capture semantics; do not silently redefine them.
- Finding new unresolved/remaining evidence must invalidate any reversible local completion state. Prevent premature announcements and PB records at their source.

## Repository starting points

Paths below are relative to the repository root; verify them on the branch being implemented.

- `src/main/kotlin/net/johnceo/sparklingmutuals/safari/BeeNests.kt`: tick scan, interactions, render path, and helper gating.
- `src/main/kotlin/net/johnceo/sparklingmutuals/safari/BeeNestSurvey.kt`: fixed coordinates, classification, punches, and `allChecked`.
- In the same directory: `SafariHelperRules.kt`, `SafariRun.kt`, `SparklingChecks.kt`, `SafariBiomeClearTracking.kt`, `SafariPanels.kt`, `SafariCompletion.kt`, `SafariTracking.kt`, and `SafariFullClear.kt`.
- `ForestCompletionZones.kt` and `IcyCompletionZones.kt` (the latter also defines `SafariCompletionZones`): existing completion-zone patterns, if useful for coverage guidance.
- Existing tests under `src/test/kotlin/net/johnceo/sparklingmutuals/`, especially `BeeNestSurveyTest.kt`, `BeeNestCompletionTest.kt`, `ForestCompletionTest.kt`, `SafariClearConsistencyTest.kt`, and mode/party completion tests.

Historical context: commit `0b43b3a` introduced the fixed-site survey. Its parent scanned only within 24 blocks horizontally and 12 blocks vertically of the player, explaining the older proximity restriction. The current fixed-site survey already requires all 13 classifications and has a six-nest test. Diagnose what is failing in runtime detection/rendering and completion integration; do not simply recreate those existing checks. These observations are based on checkout `de6011d`, not a confirmed runtime root cause.

## Acceptance criteria and validation

1. Loaded, unpunched nests highlight across Forest elevations without the old close-proximity scan restriction; handled nests stop highlighting.
2. Three handled/captured bees with any site unverified remain incomplete, even when no nest highlights or nearby bees are visible.
3. A fully verified three-nest run completes once its active-mode requirements are met; four-, five-, and six-nest runs require their actual totals in Full Clear.
4. A newly loaded fourth–sixth nest appears and remains required. A released uncaptured bee blocks Full Clear even if it was not previously recorded by the entity scanner.
5. Visiting every survey area without receiving the relevant block data does not complete coverage. The UI communicates unresolved sites; loading and inspecting them resolves coverage.
6. Chunk unloading/reloading, Forest exit/re-entry, duplicate left/right clicks, and disappearing punched blocks preserve correct same-run evidence. A new run starts cleanly.
7. HUD/ESP toggles do not alter tracking or completion. Unique, Sparkling, and party/manual behavior retain their documented semantics.
8. No local Forest done announcement or applicable Full Clear PB is produced from a partial survey or insufficient capture evidence.

Add focused regression tests for missing coverage, actual totals 3–6, and the runtime integration gap you identify. Run `./gradlew.bat test build` on Windows (or the equivalent wrapper command on another OS), with the repository's required Java version. Perform in-game validation of highlights and coverage at differing view distances if a Minecraft test session is available. Report any unavailable in-game validation explicitly; pure survey tests alone cannot establish that rendering works.

Summarize the cause, changed files, test results, and any remaining uncertainty about server block/entity availability. Update the Safari behavior documentation if present and changed by the implementation.

## Last updated

2026-10-15 - Added document revision tracking.
