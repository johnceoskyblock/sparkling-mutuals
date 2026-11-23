# Clear water

In **Full** and **Full No Auto Update**, **Safari → Helpers → Visual cleanup → Clear water** applies the saved water opacity settings inside Safari.

**Water opacity by biome** has a slider for Forest, Cavern, Icy, and Haunted, from 0% invisible to 100% solid. A legacy single opacity supplies the fallback when a biome value is missing. Water fog and the underwater overlay are cleared while the feature is active. The same water color rule handles vanilla and Sodium fluid meshes. Changing the toggle or active opacity rebuilds chunk meshes once.

This is an independent water-only implementation; the supplied ClearWaterLava JAR is not embedded. That standalone mod independently changes lava/water fog and should be removed when testing the integrated toggle. Shaders or renderers that replace these hooks still need in-game validation.

Tests cover water-only alpha preservation, disabled/outside-Safari behavior, config persistence and vanilla hook targets. The installed Sodium 0.9.2 renderer's color conversion call was inspected. Live verification: toggle from shore and underwater, confirm the lake bottom is visible within loaded render distance, then check that lava remains normal without the standalone mod installed.

## Last updated

2026-10-28 - Release verification added version-specific overlay and chunk-rebuild APIs for 26.1.2, 26.2 and 26.3, with per-target bytecode hook checks. Live renderer/shader checks remain pending.

2026-10-28 - Changed default opacity to 60%, preserving saved preferences.


2026-10-28 - Added the Water opacity slider beside Clear water.


2026-10-28 - Increased water surface opacity to 20% so water remains visibly distinct from air.

2026-10-28 - Added Clear water under Safari helpers > Visual cleanup.
