# Phantomleaf Solver

## Settings and behavior

Open `/sm` → **Misc Endgame Features** → **Phantomleaf Solver**. The switch defaults on, matching upstream, and saves as `phantomleafSolver` in `sparkling-mutuals.properties`.

This Garden-only helper is adapted from [SkyHanni's PhantomleafSolver](https://github.com/SkyHanniStudios/SkyHanni/blob/b3543887dfb80d95ff9880b3f610167ac2c104f2/src/main/java/at/hannibal2/skyhanni/features/garden/greenhouse/PhantomleafSolver.kt). The upstream revision is pinned in the source and attribution notice. Garden eligibility uses the Hypixel location mode `garden`. It has no effect in Safari, Torrhus Canyon or other islands.

The crop message `Phantomleaf: Poof! Try and find me!` starts a search. Stand still horizontally for two qualifying bass-drum sounds. Raw pitch must be between 0.61 and 0.62. As upstream does, calculate horizontal distance as `30 * (1 - raw sound volume)` and search block centers within ±31 blocks on each horizontal axis, accepting distances within 0.001. Candidate blocks are at Y=74. The raw sound hook runs before player volume sliders and distance attenuation; it does not cancel or alter playback.

One candidate is green; several candidates are yellow. Candidates have through-terrain highlights and beacon beams. With several candidates or none, local chat asks the player to move a little. A new position requires two stationary qualifying sounds before recalculation. This preserves upstream calculation rather than inventing additional triangulation or a wider pitch range.

`Phantomleaf: You found me!` or `Phantomleaf: That's not me! Better luck next time!` clears the search. Disabling the feature, leaving Garden, changing worlds or disconnecting clears all state. There are no automatic clicks, crop actions or party messages.

## Validation

Focused tests cover unique/ambiguous/no-solution distances, negative coordinates, stationary samples, sound filtering, reset messages and saved config/GUI application. A bytecode regression check verifies the actual sound hook runs at vanilla's first volume read, after sound resolution. Reading pitch or volume at method entry is unsafe and caused the reported menu-music startup crash. Live Garden sound timing and rendering still require Minecraft testing.

In IntelliJ, start Phantomleaf, stand still for two notes, then move and stand still again. Check green/yellow highlights and beams, and confirm success/failure, switching islands and disabling the toggle remove them. Confirm Safari sounds cannot start a search.

## Attribution

Derived source remains LGPL-2.1; see [SkyHanni notice](../licenses/SkyHanni-Phantomleaf-NOTICE.txt) and the included [license](../licenses/SkyHanni-LGPL-2.1.txt). Original mod code keeps its own license.

## Last updated

2026-10-19 - Fixed the reported startup sound crash by moving the hook after sound resolution; added a regression check against actual Minecraft bytecode.

2026-10-19 - Ported the pinned SkyHanni Phantomleaf solver to a Garden-only helper and the Misc Endgame Features tab; live testing remains pending.
