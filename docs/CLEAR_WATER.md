# Clear water

**Safari helpers > Visual cleanup > Clear water** defaults on and is saved in the existing configuration. It only applies inside Safari.

Water fog and the underwater overlay are cleared. The adjacent Water opacity slider sets the water surface vertex alpha cap from 0% to 100%, defaulting to 60% so loaded terrain can be seen through the surface from shore. The same water-only color rule handles vanilla rendering and Sodium's optional fluid mesher. Lava, powder snow, ordinary distance fog and water physics are unchanged. Changing the toggle or active opacity rebuilds chunk meshes once. Opacity is saved in the main config; configs without a saved opacity use 60%; existing saved values are preserved.

This is an independent water-only implementation; the supplied ClearWaterLava JAR is not embedded. That standalone mod independently changes lava/water fog and should be removed when testing the integrated toggle. Shaders or renderers that replace these hooks still need in-game validation.

Tests cover water-only alpha preservation, disabled/outside-Safari behavior, config persistence and vanilla hook targets. The installed Sodium 0.9.2 renderer's color conversion call was inspected. Live verification: toggle from shore and underwater, confirm the lake bottom is visible within loaded render distance, then check that lava remains normal without the standalone mod installed.

## Last updated

2026-10-28 - Release verification added version-specific overlay and chunk-rebuild APIs for 26.1.2, 26.2 and 26.3, with per-target bytecode hook checks. Live renderer/shader checks remain pending.

2026-10-28 - Changed default opacity to 60%, preserving saved preferences.


2026-10-28 - Added the Water opacity slider beside Clear water.


2026-10-28 - Increased water surface opacity to 20% so water remains visibly distinct from air.

2026-10-28 - Added Clear water under Safari helpers > Visual cleanup.
