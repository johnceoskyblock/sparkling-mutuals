# Native UI and catch celebrations

The settings workspace is adapted from Serkosea's SafariUtils v3.3.0, commit `2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8` (MIT), with Sparkling Mutuals branding. Attribution ships in `licenses/SafariUtils-MIT.txt`. Existing configuration files, setting keys, modes, tracking and automation remain the owners of feature behavior.

## Settings

The v4.0.2 candidate restores **SkyBlock → Helpers → Hunting Box total value** as an independent saved switch for one cumulative sell-offer estimate above the inventory. General item/chest valuation stays shelved. See [Hunting Box total](HUNTING_BOX_VALUE.md).

The Hunting Box total and top-ten guidance list use the Safari Progress HUD's existing panel style, including its saved border/background/transparency. The total displays one named coin amount; the shard list omits auxiliary ownership/session/profile footer text.

`/sm` opens the native workspace. HUDs, Safari, Party, ESP, History and Advanced areas with tabs, full-catalog search, grouped cards, boolean switches, dropdowns, sliders, text editors and HSV/opacity/chroma color controls replace MoulConfig. Changes use the existing apply/save actions. Search includes every nested setting across areas; Enter opens a single matching action. Text and color dialogs require Save; Escape cancels the dialog. Search includes nested group context. Navigation and expanded groups are remembered while running. Existing HUD drag/resize behavior and saved placements are retained.

The default Rainbow theme matches SafariUtils. The Theme button exposes SafariUtils' 34 built-in workspace palettes. This selection affects settings screens, not gameplay requirements or existing per-HUD color choices. Color strings keep their existing speed/alpha/RGB representation, including animated hue offsets.

## HUD symbols

Compact SafariUtils pixel sprites identify biomes, birds, food, incense and gems. Check/cross marks use existing completion results; rendering creates no completion evidence. Symbols are drawn as geometry and do not depend on special Unicode characters or resource-pack fonts. HUD widths account for the extra symbols.

## Player Warp List

Sparkle Warping > Warp List uses player cards and per-biome critter flags adapted from SafariUtils Ticket Trading. Add an IGN, select the player, then toggle which eligible sparkling species should trigger an Auto-party invitation. All/None selects or clears all 26 supported early-detection species for that player. A player with no selected species remains saved for later editing.

Import CSV replaces all player cards and flags using supported critter headers and name rows 2-11; later rows are ignored. Existing per-critter lists load into the new player view without changing their invitation order. No profile lookup is needed to edit or import. The Auto-party toggle is the existing setting; the 19-second cutoff, returning-leecher batch, leader/capacity checks and warp deadline are unchanged. Leech whitelists remain separate.

## Confirmed catch celebration

The Unique Sparkling catch alert under Safari helpers > Detection and alerts responds only to a server-confirmed sparkling catch while in Safari that is confirmed new to the local account, including credited party catches. Duplicates and unknown discovery status do not trigger it. It is independent of history recording, so it also works on Alpha. Its toggle, preset selector and preview are separate from the existing sparkling-found gold screen.

SafariUtils' 41 named presets provide the original bounded visual effect recipes, rainbow frame/title, species rarity color, durations and synthesized arrangements. Geometry is sampled at 40 FPS and submitted in one bounded batch with owned frame data. Only this alert's sounds stop when it expires or disconnects. Existing found alerts, party announcements, source/capture evidence, progress and imports are unaffected.

## Update status

The lower-left button checks public GitHub release metadata for `johnceoskyblock/sparkling-mutuals`, off the game thread when settings open and every five minutes while they remain open. Opening the update status button forces a fresh check; failures retry after one minute. Only published stable releases with a matching Minecraft JAR are considered. Unknown/network failures show Couldn't check, never Up to Date. Opening the button shows release notes and a link to GitHub releases. No automatic download or installation occurs and no player data is sent.

## Validation

Config/settings and HUD regression tests cover persistence and existing completion presentation. Focused native tests cover catalog/control coverage, color compatibility and release-version comparison. Verify search, nested groups, controls, secondary screens, HUD placement at different GUI scales, and actual/preview catch celebrations in Minecraft.

Validation on 2026-11-01: 40 focused settings/HUD/Warp List tests passed on 26.1.2. Kotlin and Java compiled on 26.1.2, 26.2 and 26.3. The 26.1.2 installable JAR passed packaging verification without MoulConfig, and distribution-verifier regressions passed. An isolated 26.1.2 client loaded the published SafariUtils reference JAR (SHA-256 checked) for direct comparison. Native settings, nested search/action activation, catch preview, HUD editor icons, empty Warp List and a player card were inspected in-game. No Hypixel session or automatic invitations were used for this validation. Final live catch/audio behavior and party invitations remain in-game checks.

## Last updated

2026-11-01 — Initial native workspace, HUD symbols and SafariUtils catch-celebration port.

## Command and control cleanup

Modes switch through `/unique`, `/fc` and `/sparkle`; their dropdowns contain visual preferences only. `/captures` opens counts; its redundant button is removed. `/hotspot` controls hotspot announcements without config toggles. Party outline color and name scale are under Safari helpers → Party visibility. Color controls open their own picker; the global selected hex and Apply selected color buttons are removed. Detection and catch alerts have separate explicit names. Capsule projections and the Rockmite auto-clicker descriptions are clarified.

## Last updated

2026-11-01 — Native UI cleanup, food/podium icons and All Coins HUD.

2026-11-01 — Completed progress biomes use biome icons; missing critters have no X; helper footers have resource icons. Catch celebrations are unique discoveries only.

2026-11-01 — HUDs → HUDs replaces the Progress tab and includes an independent Safari contest timer visibility toggle. Contest tracking and ticket logic continue when the timer is hidden.

2026-11-02 — Advanced → SkyBlock has Helpers and Endgame subtabs. Hunting Box value is in Helpers; Phantomleaf Solver moves to Endgame with its existing saved setting.

2026-11-02 — Detection and alerts → Sparkling catch count defaults on. Confirmed new discoveries show NEW; other confirmed catches show the species catch number from saved/imported history, including credited party catches. Messages go to party chat while in a Safari party. No catch-time API request is added; Alpha does not update the counter.

General item and chest valuation are shelved. Hunting Box valuation is documented separately in [Hunting Box value](HUNTING_BOX_VALUE.md).

2026-11-02 — Removed redundant setting descriptions and shortened remaining guidance. The standalone Sparkle Warping explanation is unchanged. General command help now lists slash commands only.

2026-11-02 — SafariUtils-style Biome Objectives is now a separate, independently movable HUD. Missing/Unchecked retains only species and its completion summary. Objective rows use lighter biome colors and icons, with compact Forest food details.

2026-11-02 — Safari → Banner now owns all banner appearance and event controls. Tracking appearance moved to HUDs → Appearance; critter/waypoint colors remain under ESP; sparkling detection appearance sits with its detection settings. See [banners and HUD themes](BANNERS.md).

2026-11-02 — Settings cards measure and wrap their complete labels, descriptions and values. Controls move below text on narrow screens; long lists use a wider multiline editor (Ctrl+Enter saves). Search/theme controls and Warp List toolbars share consistent spacing. Right-clicking any HUD in `/sm gui` opens its config toggle, expands the containing groups, scrolls to it and briefly highlights it. Closing settings returns to the HUD editor. Banner alerts and the biome captures HUD have direct visibility toggles; existing saved preferences are retained.

2026-11-02 — The update status button uses a bordered card with wrapped status text. Release details use an opaque centered panel, formatted headings/bullets, readable spacing and a draggable scrollbar; Markdown markers are omitted from displayed notes.

2026-11-02 — Fixed Boolean-backed action button sizing; shortened action labels. Welcome dividers match in rendered width. Each HUD’s toggle, appearance and placement controls are together; right-click navigation expands that group. Progress has optional Profit/Hotspot lines; Contest has optional Bracket/Score rows. Sound preview buttons play immediately and banner previews close settings. Helpers → Sparkling detection and alerts uses its explicit name. Item/chest/Hunting Box valuation is shelved outside runtime sources. See BANNERS.md for current alert and appearance ownership.

2026-11-02 — All native screens use Minecraft mouse/key constants so HUD dragging, right-click settings, history controls, Warp List and keyboard shortcuts match across 26.1.2, 26.2 and SDL-based 26.3. Update status refreshes during an open settings session and on demand; failed refreshes never retain an Up to Date label.

Release validation, 2026-11-02: all 1,139 tests passed on each of 26.1.2, 26.2 and 26.3, plus 21 build-helper tests. Native offline clients exercised HUD click/drag/release, scroll resize, right-click settings, settings click/Escape and layout persistence on each target. The 26.3 probe used SDL button codes 1/3 and Return/Escape 40/41; older targets used 0/1 and 257/256. Public update selection and timed refresh/failure behavior have automated regressions. Windows screenshot capture was unavailable; this is not a full visual, live Hypixel or shader-pack parity certification.

2026-11-02 — Fresh-install settings and normalized HUD placements now mirror the current Skyblock instance, with hotspot/warping/leech/starting-item announcements off, a blank Warp List and the saved leech whitelist retained. Existing configs are not overwritten. Starting-item reminder titles also respect the announcement toggle.

2026-11-02 — Completed Missing/Unchecked and Biome Objectives HUDs remain visible with a green completion line. Missing uses 'All caught and checked!' in Unique/Full Clear and 'All checked!' in Sparkle; objectives use 'All objectives complete!'.

2026-11-02 — Icy Biome Objectives lists only Capture Wumpa in Unique/Full Clear, and in Sparkle only when Wumpa is needed. After capture, the completed-objectives line remains.

2026-11-02 — Hotspots announce immediately on Hypixel's message. Assignment waits two seconds after the third distinct reporter (all members for smaller parties), with a five-second fallback. Manual biome names/close typos are accepted. Only the leader sends one assignment instruction per member without an SM/SU announcement.

2026-11-02 — /sm gui adds SafariUtils-style snapping: centers within five GUI pixels and screen edges within three, with center alignment guides while dragging. Snap defaults on and its editor button saves the preference. Arrow keys nudge the last hovered HUD one pixel without snapping. Normalized positions round to the nearest pixel so nudges and snapped alignment survive saving. Dragging, scroll resize and right-click settings remain available.

2026-11-02 — Leader-owned hotspot coordination adds pre-run Hotspot wins to the leader announcement and publishes one Biomes map. Followers validate the current leader and full roster, replace local counts, and follow the final map even after missed reports or a local fallback. Duplicate messages do not double-credit wins. Individual instructions remain leader-only for non-mod reporters. No additional API requests.

2026-11-02 — Locked secret achievements hide progress. Biome Objectives includes a concise remaining-critter instruction per mode. Forest omits zero food/bird counters, removes bird rows only after mode completion, and reconciles feeder slot/chat consumption with consistent cursor-safe inventory and transaction buffering.
