# Safari banners and HUD themes

## Controls

Safari → Banner contains the shared banner toggle, appearance, position and per-alert controls. Each alert has enable, playback, text, color, duration, sound, volume, pitch and preview controls. Banner previews close settings for display. Sound-only previews play from the settings screen. The HUD editor uses the banner’s rendered dimensions.

## Active alerts

- Run and party alerts include full party joined, biome complete, and run complete in all editions.
- **Full and Full No Auto Update** include **Warping now**. **Standard** includes **Party member kicked**, triggered by a verified teammate’s kick report in party chat during Safari. Use `<PLAYER>` for that teammate’s name.
- Encounters: all bird food collected, birdfeeder empty, all gems ready, all coins collected, four incense ready.
- Contest: start, five/three/one minutes remaining, end, selected ticket earned.

The Forest dropdown is merged into Encounters. Encounter alerts require a readiness transition witnessed while remaining in the relevant biome. A ready condition first observed elsewhere or inherited on entry does not replay. The retired hotspot/floor-drop/species/stage banners never play, including when an old config has them enabled. Saved legacy properties remain harmless for compatibility.

`/hotspot` retains its separate full-screen biome assignment alert and party announcement. Sparkling detection and the Unique Sparkling catch alert also remain separate.

## Appearance and navigation

HUDs → HUDs groups each tracking HUD’s Enabled control, appearance and placement button. Progress additionally has Profit line and Hotspot line toggles. Profit uses existing Bazaar cache prices and this run’s personal shards, feathers and observed Safari Essence balance gains; the reward summary remains authoritative. Rendering does not fetch prices. Contest additionally has Bracket row and Score row toggles; hidden rows collapse the panel height.

Nearby sparklings and detection appearance are next to their toggles in Safari → Helpers → Sparkling detection and alerts. Doomspiral timer appearance is next to its timer settings. Critter colors are in **Highlights → Customization** in Standard and **ESP → Customization** in Full. Right-click a HUD in `/sm gui` to open its setting group. The global Biome HUD borders option overrides per-HUD border colors.

The SafariUtils frame, gradient, timer bars, sounds and melodies remain attributed under MIT. Banner audio manages only its own sound instances. Banners do not change captures, requirements or party-report authority.

## Last updated

2026-11-23 (America/New_York) — Documented edition-specific party banners and settings paths.

2026-11-02 — Reduced active banners, witnessed encounter transitions, reliable previews, grouped HUD controls and exact editor bounds.
