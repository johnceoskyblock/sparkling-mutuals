# Safari entry guard

## Purpose

Cancel recognized Safari entry interactions before their normal packet path while party readiness or presence is unresolved. The guard only blocks interactions. An independently configurable ticket-selection helper can finish a manually initiated Manager interaction, through the same guard. [Contest and downtime](CONTEST_DOWNTIME.md) owns requests, readiness parsing and ticket release.

## Controls and defaults

| Stage | Default-on setting | Blockers |
| --- | --- | --- |
| Torrhus Canyon (`foraging_3`) | **Party commands → !dt downtime** | Outstanding readiness requests and pending readiness identity/overflow. |
| Safari instance (`safari`) | **Safari helpers → Guard Safari entry** | Current missing/loading party presence and a verified kick message's two-second pause. |
| Unknown server location | Either applicable setting | Conservative wait when the composed decision still has unresolved evidence. |
| Other known island | Not applicable | None. |

Each saved switch controls its own blockers. **Hold either physical Shift key** to bypass all blockers for that interaction, without clearing requests or marking anyone present. Releasing Shift restores the decision. In the entry GUI, Shift also converts a ticket control's quick-move into a normal selection click.

## Optional ticket auto-selection

**Safari helpers → Safari entry → Auto-select Safari ticket** is on by default. Directly below it, **Preferred ticket** offers **First Class** or **Basic** (fresh-install preference; existing choices are preserved). Only inside the actual Safari island (coordinates plus current Safari area evidence, or `safari` mode while sidebar data is absent), for a verified non-leader in a party, the helper detects the exact `Critter Safari Entry` menu and all four ticket controls. It chooses the preferred available tier: First Class → Premium → Economy → Basic, or Basic → Economy → Premium → First Class. Existing saved off choices are preserved.

Availability comes from menu lore counts or explicit usable/unavailable text, not guessed API balances or item stack counts. Unknown availability waits rather than spending another ticket. A short menu-loading delay precedes selection. While auto-selection is handling the menu, its contents/background are hidden and mouse clicks are ignored; the actual server menu stays open for the normal guarded selection. The menu becomes visible if entry is blocked, all tiers are unavailable, or loading/selection takes more than two seconds. Escape remains available. Every automated selection calls the normal guard and container-input path; readiness/presence blocks continue to hold selection unless Shift is physically held. The helper clicks at most once per opened menu. A new world/connection resets menu state. Selection and screen hiding share the Safari-island gate, with no chat trigger or 15-second arm timeout. A manual click after the menu becomes visible cancels automation for that menu. On Torrhus Canyon, other islands, or unknown location, the menu stays visible and accepts normal manual clicks through the existing entry guard. It does not initiate Manager interactions or spend tickets for party leaders/solo players.

## Implemented behavior

### Interception

One decision is called at the HEAD of `MultiPlayerGameMode.interact`, `attack`, `useItem`, `useItemOn` and `handleContainerInput`. It covers Manager use/attack, both-hand ticket use into air/block, and server entry-menu controls. Ticket recognition uses a held name containing `Critter Safari Ticket`; the menu requires exact title `Critter Safari Entry` (case-insensitive) and a server-owned name-tag slot. Player-inventory slots, close controls and unrelated inventories are not entry controls.

Manager recognition uses its exact name or an aligned player/armor-stand/interaction/text-display proxy near its nameplate. Recognized UUIDs and nameplate anchors are retained for the current world to handle transient nameplate loss; local/current-party players are excluded. Recognition does not cache permission: every interaction refreshes the current guard decision. The three-second notice cooldown never releases a blocked click.

### Identity and readiness

Non-overlay GAME events and CHAT events use the anchored party envelope. Sender names are checked against the current Mod API UUID roster before applying controls. Current connection player names are reused across transfers. Unknown senders resolve asynchronously through the existing Mojang name lookup, then recheck the current roster; failed lookups retry after ten seconds. Missing-member display names are independently resolved from roster UUIDs through the session service; name lookup never changes presence readiness.

The bounded inbox coalesces each sender's latest readiness state separately from kick reports. More than 32 distinct pending sender/control entries creates kind-specific overflow. Readiness overflow requires every current member to personally send an accepted ready/ticket message after recovery. Confirmed departures prune only that member; party replacement/disband clears old requests. Ordinary refreshes retain pending evidence. Each setting disables only its own inbox/overflow kind.

### Presence and kick recovery

Safari entry compares normalized current-party UUIDs, including self, with this connection's **listed player-info entries**, bound to the current world and Safari location. This does not depend on entity render distance. Unknown roster, missing local listed entry or incomplete loading blocks immediately; no permissive loading grace is implemented. A verified solo roster containing self is enough.

Any case-insensitive `kick` substring in a verified party **body** counts, including `kicked`, `I was kicked!` and `kickstart`; a username alone does not. It pauses entry for two seconds from receipt time, even when identity resolution completes later. A later report restarts the pause. After expiry, a fully present roster permits entry regardless of older kick/pending/overflow context. Missing members and unknown roster continue blocking without timeout. Run detection does not bypass Safari presence. Downtime applies separately at Torrhus.

### Notices and diagnostics

Refusals are local, at most once per three seconds, and append `Hold Shift to enter anyway.` A known missing list uses sorted player names, e.g. `[SM] Safari: 1 missing: JohnFounder.` Unknown names display `name loading`; no specific missing UUID displays `Waiting for the party to load`. `/sm debug` logs settings, stage/run state, roster readiness, missing UUIDs and pending identity/kick state; interception logs record route, recognition and decision.

## Lifecycle and persistence

Both switches persist in the [general configuration](CONFIG_DEFAULTS.md). Presence, kick pauses and Manager recognition are transient. World changes discard old-instance kick/recognition state. Same-party transfer retains downtime and pending readiness; confirmed party changes clear old-party requests. Disabling downtime drops its requests; disabling the presence guard clears its kick state. Disconnect resets the guard and transfers banked downtime rather than saving it to disk.

## Edge cases

- Listed-player evidence is client-visible evidence, not a server occupancy guarantee. Omitted or stale Hypixel tab entries can cause conservative waiting or misleading presence; use debug evidence and the deliberate Shift override.
- An old unresolved kick cannot hold a fully present roster forever after the receipt-time pause; an unresolved readiness request can still hold Torrhus entry.
- An unknown location does not unconditionally block every recognized control: it blocks only when enabled settings compose an unresolved decision.
- Quoted names, public/private/guild messages and another person's ready reply cannot clear someone else's request. The full readiness grammar is owned by [contest/downtime](CONTEST_DOWNTIME.md#requests-and-readiness).

## Source and validation references

- Decision, recognition and diagnostics: [SafariManagerGuard](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/automation/SafariManagerGuard.kt); presence: [SafariEntryPresence](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariEntryPresence.kt); inbox, pause and recognition state: [EntryGuardState](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/automation/EntryGuardState.kt).
- Events and identity: [SafariPartyInput](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/automation/SafariPartyInput.kt); actual boundary callbacks: [SafariManagerMixin](../src/main/kotlin/net/johnceo/sparklingmutuals/mixin/SafariManagerMixin.kt), registered in [mixins](../src/main/resources/sparkling-mutuals.mixins.json); startup/tick/reset: [SparklingMutuals](../src/main/kotlin/net/johnceo/sparklingmutuals/SparklingMutuals.kt).
- Existing secondary coverage: [EntryBoundaryTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/automation/EntryBoundaryTest.kt), [EntryGuardStateTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/automation/EntryGuardStateTest.kt), [EntryPartyInboxTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/automation/EntryPartyInboxTest.kt), [SafariManagerGuardTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/automation/SafariManagerGuardTest.kt). No local runtime suites, mixin startup or live packet capture ran for this documentation audit; CI results are separate. Live checks remain needed on all supported targets: repeated entry attempts with missing/late members; each intercepted route, hand and GUI input; Shift/release; unknown roster; kick-pause expiry/restart; transfers; independent holders; unrelated controls.

## Ticket selection trigger

Detect the exact entry menu directly inside a verified Safari instance. Neither a local entry notice nor Manager click is required. The log showed that entry notices name the party leader, so local-name matching could leave non-leaders unarmed. Menu detection also survives delayed roster, sidebar and Mod API data. Torrhus conversion menus remain visible and manual. Only known non-leaders can auto-select, and every selection still passes the entry guard.

## Last updated

2026-10-28 - Replaced entry-message arming with direct Safari ticket-menu detection.


2026-10-28 - Armed ticket selection from the local Safari entry message.


2026-10-28 — Ticket auto-selection and menu hiding now require current Safari area text, mapped Safari coordinates, Mod API Safari mode and a verified party non-leader. Revalidate for arming, clicks and hiding; stale mode packets cannot activate it on Torrhus. Conversion menus remain visible/manual there. Entry-guard rules are unchanged.

2026-10-26 — Restricted ticket automation and menu hiding to the actual Safari island; Torrhus ticket menus remain manual and visible.

2026-10-26 — Torrhus guard reasons now identify contest requests per player. A confirmed leader party warp starts the twenty-second release for other contest holds; see [contest/downtime](CONTEST_DOWNTIME.md).

2026-10-22 — Entry controls are grouped under Safari entry; automatic ticket selection now defaults on while preserving saved choices and the First Class preference.

2026-10-20 — Hide the ticket GUI during auto-selection, with a visible fallback for blockers/unavailable tickets/slow responses; no automatic menu closure.

2026-10-20 — Added optional non-leader ticket auto-selection after an allowed Manager click. Guard and physical Shift behavior still apply at the ticket packet boundary.

2026-10-20 - Contest downtime holds also release individually twenty seconds after the member's Torrhus FOLLOW travel notice; presence/kick guards and Shift override remain unchanged. See contest/downtime for timer ownership.

2026-10-19 (America/New_York) — Audited stage composition, tab presence, identity, interception and resets against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`. See [documentation conventions](DOCUMENTATION_GUIDE.md).
