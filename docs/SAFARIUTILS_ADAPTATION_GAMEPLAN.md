# SafariUtils adaptation gameplan

## Goal

Bring the SafariUtils interactions that felt better to play—especially the GUIs, event banners and sounds, Sparkling alerts, and the warping workflow—into SparklingMutuals. Keep SparklingMutuals' existing mode rules, capture evidence, critter identification, and floor-drop logic as the source of truth.

This is a workshop plan, not an implementation specification. The review compared SafariUtils commit [`2e572ab`](https://github.com/Serkosea/safariutils/commit/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8) with the SparklingMutuals checkout on 2026-10-31. The relevant SafariUtils files are [`BirdfeederWatch.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/BirdfeederWatch.java), [`ShiningCoinWatch.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/ShiningCoinWatch.java), [`EncounterAlerts.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/EncounterAlerts.java), [`SafariSettingsScreen.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/SafariSettingsScreen.java), and [`TicketTradingScreen.java`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/TicketTradingScreen.java).

## Bird food logic

### SafariUtils

- Counts floor-feed pickups and starting feed separately. It recognizes the three food types and tracks Birdfeeder spawn chat lines separately from food acquisition.
- Credits a starting-feed snapshot through `StartingItemsWatch`; it also watches the open Birdfeeder menu, its contents slot, cursor-held food and inventory changes.
- A feed stack disappearing while the Birdfeeder is open or shortly after clicking it is treated as a possible deposit, then settled briefly. Rejected clicks and cursor/inventory resync windows are handled to avoid claiming that food was accepted too early.
- A Birdfeeder message is accepted only if its anchored sentence matches and the named species is known. Each valid spawn line triggers its species alert. Two Macaws in a single line still represent one feed-use event.
- “All feed used” requires observed uses to cover known food and no food left held or in the feeder, with a second personal-deposit fallback for a single feeder user. The stronger Forest-wide assertion also requires at least nine observed feed-use events. A two-tick contents transition avoids mistaking a one-tick GUI refresh for an empty feeder.
- The user-facing layer can alert on individual bird spawns and an empty feeder, report the player's total feed to party chat, and report when their held feed has been deposited. Those are separate from proving that all feed uses produced birds. Feed reporting is gated until a proven Forest clear or nine floor-food pickups so partial observations do not announce a final state too early.

### SparklingMutuals

[`SafariBirdLedger`](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariBirdLedger.kt) does not treat Birdfeeder spawn chat as proof. It builds the required feeding count from the nine base Forest food opportunities, additional pickups, known starting food, party reports and a conservative early-visitor estimate. Current qualified scans count caught plus live birds; Macaw pairs count as one feed use. Known uncollected food or inventory blocks completion. If all food is resolved and a qualified population scan proves enough birds, that proof can survive unloading. Full Clear still requires actual catches under its own formula.

The existing **All Bird Food Collected** alert fires when the three food pickup counts reach three each, gated to the Forest and the setting. SparklingMutuals does not currently use SafariUtils-style Birdfeeder spawn lines for individual Bluebird/Parakeet/Macaw banners. That is a user-facing alert opportunity, not a reason to weaken the current completion ledger.

### Adaptation decision

Borrow the alert moments and better feedback: “bird attracted,” “feeder empty,” “food remaining,” and “all feed used.” Feed those alerts from existing ledger/menu evidence. A spawn message may produce a cosmetic notification, but must not by itself prove that all feeding opportunities or species checks are complete.

## Shining Coin logic

### SafariUtils

[`ShiningCoinWatch`](https://github.com/Serkosea/safariutils/blob/2e572ab0c8f6ada7a04ada2d5ccbf05fb94367c8/src/main/java/dev/serko/safariutils/client/ShiningCoinWatch.java) keeps two counts:

- **Acquired:** cumulative starting inventory plus floor-drop pickups, useful for historical/resource reporting.
- **Quota still accounted for:** held coins plus confirmed Gimmiegold spawn events. The inventory count must remain stable for 750 ms before it replaces the quota. A spawn message starts another settle window so the matching inventory update can arrive first.

This is a clear and useful remaining-use counter. Its source tracking is intentionally simple: it does not identify every unresolved floor-drop slot or independently prove a Gimmiegold completion condition.

### SparklingMutuals

SparklingMutuals separately records Shining Coin pickups, held inventory, confirmed Gimmiegold uses and live Haunted coin-source markers. [`HauntedCoinDrops`](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/HauntedCoinDrops.kt) counts unresolved markers, retaining unknown and partial groups as blockers while excluding a complete incense group. [`SparklingChecks`](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SparklingChecks.kt) tracks base coin demand plus known starting/extra coins and applies separate Unique, Sparkling and Full Clear requirements. The Missing HUD only shows known positive remaining work, and `/sm debug` exposes source/population state.

### Adaptation decision

Keep the richer SparklingMutuals coin/source ledger and mode rules. Consider SafariUtils' simple “coins remaining to use” presentation as a HUD/alert summary, derived from the existing state. Do not let a held-plus-spawned total overwrite unknown floor sources, extra-resource obligations or Full Clear catch requirements.

## Phased work

### Phase 1 — Turn the tested experience into concrete references

Record the specific screens, controls, alerts and warping steps that felt best in SafariUtils. Capture matching screenshots or short clips at the same resolution and GUI scale for both mods. Identify the parts users praise as layout, wording, animation, sound timing, or workflow so the first implementation targets the actual difference.

**Exit condition:** a short screen-by-screen reference and a ranked list of must-have interactions.

### Phase 2 — Settings and HUD experience

Build a visual prototype for a SafariMutuals settings workspace inspired by SafariUtils' navigation, search, grouped cards and persistent context. Preserve existing setting objects, saved property keys, defaults and migrations. Avoid reimplementing gameplay state inside UI controls.

Improve `/sm gui` with live HUD contents where possible, named placeholders outside a run, snap guides/toggle, and clear reset/done controls. Keep drag and scroll-resize behavior.

**Exit condition:** existing settings remain reachable and retain their values; GUI search and navigation are understandable; HUDs can be positioned before entering Safari.

### Phase 3 — Unified event-alert presentation

Use one presentation layer for event alerts while preserving each feature's current trigger and deduplication rules. Provide per-event playback choices (off, banner, sound, both), configurable wording, color, duration, sound and volume, plus preview/test controls. Let higher-priority alerts replace or queue lower-priority ones predictably.

Polish the existing Sparkling alert rather than replacing its detection queue: clear name/location, strong but brief visual hierarchy, customizable playback, duplicate suppression and cancellation when the sighting is identified as unknown or captured. Preserve current `SparklingMemory` rules and party-announcement pacing.

Add Birdfeeder spawn/empty/use notices backed by valid species messages and stable menu/inventory transitions. Reuse `SafariBirdLedger` for counts and completion; do not let the new banner path create completion evidence. Keep “my held food is gone,” “the feeder produced a bird,” and “all feeding opportunities are resolved” as distinct events.

**Exit condition:** notices are readable, configurable and non-spammy; the existing resource/capture ledgers make exactly the same completion decisions with alerts enabled or disabled.

### Phase 4 — Sparkle Warping workflow review

The checked SafariUtils source prominently contains ticket trading profiles/queues and a persistent Sparkling ticket reminder. SparklingMutuals' current `/start` flow instead owns a Solo/Duo leader cycle, returning-leecher invites, an optional 19-second Auto-party window, a delayed `/p warp`, return cleanup, whitelist Leech and contest/ticket safeguards. These are not identical mechanisms, so first map the exact SafariUtils behavior that felt better to you.

Then improve the user-facing workflow: make setup state visible, preview who will be kept/removed/invited, explain why a cycle is waiting, and make manual cancel/pause behavior easy to understand. Preserve current leader, roster, connection, contest and party-capacity checks. Treat a ticket-trading queue or guest profile UI as an optional extension if that is what you want, not as a replacement for the warp cycle.

**Exit condition:** a player can predict the next party action and why it will occur, while invalid or incomplete roster state never triggers destructive party commands.

### Phase 5 — Optional features after core polish

Evaluate automatic party objective/Sparkling sync and ticket trading separately. Decide consent, opt-in, expiry, conflict handling, chat/network budget, and compatibility with non-SafariUtils players before choosing a sync protocol. Consider selected model glow as a style option only after GUI/ESP screenshots and version/backend coverage are reviewed.

**Exit condition:** each feature has a distinct user need, bounded scope and a documented failure behavior.

## Implementation guardrails

- Keep modes and tracking independent from rendering. A display preference may hide an ESP marker but must not change a catch, source, completion or PB decision.
- Prefer additions at existing owners: `SafariBirdLedger`, `SafariFloorDrops`/`SparklingChecks`, `SparklingEncounters`, `SafariSettings`, `EncounterAlerts`-equivalent presentation, and the current HUD editor.
- Keep network party sync and automatic party actions opt-in and explainable.
- For an alert feature, define the trigger, debounce/deduplication key, reset boundary, and stale/missing evidence behavior before designing its screen.
- Compare real in-game rendering with resource packs, shaders, GUI scale, overlapping targets and both supported Minecraft backends before selecting a visual default.

## Proposed order

1. Visual references and user-facing requirements.
2. Settings workspace / navigation prototype.
3. HUD editor polish.
4. Shared alert presentation plus Sparkling and Birdfeeder UX.
5. Sparkle Warping workflow polish.
6. Optional sync, ticket trading and model glow decisions.

This order tackles the experiences you called out first and lets each be reviewed before feature work expands.
