# Safari banners and HUD themes

## Controls

Safari → Banner contains the shared banner toggle, appearance, position and per-alert controls. Each alert has enable, playback, text, color, duration, sound, volume, pitch and Preview controls. Preview closes settings and plays the selected banner/sound. The HUD editor uses the banner’s actual rendered dimensions.

## Active alerts

- Safari: full party joined, biome complete, run complete, warping now.
- Encounters: all bird food collected, birdfeeder empty, all gems ready, all coins collected, four incense ready.
- Contest: start, five/three/one minutes remaining, end, selected ticket earned.

The Forest dropdown is merged into Encounters. Encounter alerts require a readiness transition witnessed while remaining in the relevant biome. A ready condition first observed elsewhere or inherited on entry does not replay. The retired hotspot/floor-drop/species/stage banners never play, including when an old config has them enabled. Saved legacy properties remain harmless for compatibility.

`/hotspot` retains its separate full-screen biome assignment alert and party announcement. Sparkling detection and the Unique Sparkling catch alert also remain separate.

## Appearance and navigation

HUDs → HUDs groups each tracking HUD’s Enabled control, appearance and placement button. Progress additionally has Profit line and Hotspot line toggles. Profit uses existing Bazaar cache prices and this run’s personal shards, feathers and observed Safari Essence balance gains; the reward summary remains authoritative. Rendering does not fetch prices. Contest additionally has Bracket row and Score row toggles; hidden rows collapse the panel height.

Nearby sparklings and detection appearance are next to their toggles in Safari → Helpers → Sparkling detection and alerts. Doomspiral timer appearance is next to its timer settings. ESP colors remain under ESP. Right-clicking a HUD in `/sm gui` expands its setting group and scrolls to the toggle. The global Biome HUD borders option overrides per-HUD border colors.

The SafariUtils frame, gradient, timer bars, sounds and melodies remain attributed under MIT. Banner audio manages only its own sound instances. Banners do not change captures, requirements or party-report authority.

## Last updated

2026-11-02 — Reduced active banners, witnessed encounter transitions, reliable previews, grouped HUD controls and exact editor bounds.
