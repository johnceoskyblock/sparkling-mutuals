# CJ’s Revenge Safari entry core

## Purpose

This local experiment implements coordinate-independent sighting, fresh-report and admission decisions. The **admission controller remains unregistered and does not automatically enter Safari**. The separate [26.1.2 dry-run integration](CJS_ENTRY_DRY_RUN.md) now connects detection, party-protocol exchange, an persisted setting and diagnostics. The separately opted-in .4 approach test connects bounded ordinary walking; NPC interaction, menu clicks and reach extension remain disconnected. Existing Safari entry and ticket selection behave as before. See the [live entry guard](SAFARI_ENTRY_GUARD.md).

The user named the experiment **CJ’s Revenge** and requested separation from normal instances. Its current version is `4.0.2-cjs-revenge.4`, targeting 26.1.2 only; packaged files retain the `sparkling-mutuals-` prefix required by the existing verifier. The Fabric display name is **CJ’s Revenge** and the mod ID remains `sparkling-mutuals`. Any test install requires a separate Modrinth profile/game directory; it must not replace the normal instance or run alongside another JAR with the same mod ID. A separate CJ test profile has .3 installed under a later direct request; .4 installation requires a new request.

## Implemented pure contracts

- Immutable local account/connection/world/arrival/party-continuity contexts. A server name is a claim to compare, not a unique run identifier. Genuine new-arrival evidence must come from a later validated adapter.
- Exact single-line `SMSE1` query/NONE/LIVE grammar, at most1024characters, bounded tokens and canonical species. Current `SPARKLING <species>!` party text supplies only a hint. Catch/lookup/public/quoted text is not a named sighting. Dry-run sender attribution uses a server Party envelope plus a unique current roster/profile mapping; GAME carries no signed sender UUID.
- One query per arrival with no retry, a12second context preparation bound,3second reply deadline and5second effective observation age including measured round-trip. No cross-client wall-clock comparison. Rollback and overflow fail closed.
- Initially unknown context may settle within that bound. Losing previously established party/destination verification cancels the old authorization; an equal roster later cannot restore its old query. Ordinary successful refreshes and temporary guard holds do not replenish the budget.
- A leader response proposal requires a current member, matching realm/server and fresh local unresolved observations. Enable/run/destination evidence is checked again when dequeuing. At most three peer responses are retained; proposals are paced1500ms apart and queue time consumes freshness/deadline budgets.
- One current local observation per species, stable event token per identity and bounded capture retirement. Up to128captured identities are retained per context; overflow disables positive reports for that context instead of forgetting old captures. A genuinely different post-capture identity may qualify.
- An admission controller proposes one Manager interaction and one ticket action per independently established arrival. It requires explicit current destination, party, guard, Manager identity/reach and exact-menu evidence. Default frames authorize no action.
- Ticket proposals are reservations. A future sender must call `consumeTicketPermit` with the complete current frame immediately before input; it checks context, guard, container and selected slot and consumes permission once. Merely calling `allowsAutomaticTicket` is insufficient.
- Cancellation after a Manager proposal latches suppression for automatic entry menus for that arrival. Menu close/reopen, toggle, roster refresh and re-arm cannot permit a second automatic spend. Already consumed input remains uncertain until explicit personal admission evidence; cancellation never claims rollback. A validated genuinely new arrival can reset the latches; a generic world replacement cannot.
- Capture inputs carry current context and monotonic receipt time. Known captured event tokens remain retired for the arrival, older local snapshots fail the capture-time fence, and pending pre-capture replies are cancelled. A genuinely newer independently identified local event can still qualify. Controller retirement is bounded at128events; overflow cancels instead of forgetting evidence.

`EntryEffect` values are proposed effects for tests and a future admission adapter. No current caller executes them. The dry run composes the requester/responder directly and has a distinct Party/Notice-only output type. The existing `SafariTicketSelect` has not been connected to the admission ownership gate.

Stateful core objects are confined to one client thread. A future adapter must serialize callbacks and supply elapsed monotonic milliseconds plus a current immutable frame; these objects are not a thread-safe background service.

A future dispatcher must retain each effect's emitting context/attempt and reject queued work from a previous arrival before asking for a permit. It must not substitute a new arrival's current frame for an old queued action. Manager proposal-to-send revalidation and real ticket-helper integration are also still future work; this core does not claim to verify an absent dispatcher.

## Files and validation

Production: `safari/automation/entry/EntryContext.kt`, `SparklingEntryProtocol.kt`, `EntryReportExchange.kt`, `SafariEntryController.kt`. Tests under the corresponding `src/test` package cover parsing, identity/context scopes, freshness, capture retirement, cancellation, late menus, slot replacement, one-use permission and arrival reset boundaries. The [implementation plan](superpowers/plans/2026-11-04-safari-entry-core.md) links the approved local design.

Automated results belong in the local execution receipts. The external51-case scenario matrix also includes native and live cases; those cases have not been executed simply because pure tests pass. No live Minecraft interaction or Hypixel admission was performed.

## Remaining integration evidence

Reported spawn `(-50,68,24)` and Manager `(-52,68,21)` are about3.61blocks apart. Cached native clients expose a default3block entity interaction attribute; the26.1.2predicate measures current eye-to-hitbox distance. The actual Manager body, server-supplied attribute, orientation and accepted interaction remain unverified. Do not infer reach from centre coordinates alone.

Location packet/world ordering, same-destination association, personal admission sequence and present-day server timing remain unverified. Dry-run transport retains candidate claims only; all gameplay admission adapters remain unimplemented. The existing published Hypixel rules also prohibit automated gameplay actions; implementing this core does not establish permission to deploy unattended entry or promise a Sparkling claim.

## Last updated

2026-11-04 — Connected the approved 26.1.2 detection/transport dry run; the admission controller remains inactive. No installation or GitHub publication.
