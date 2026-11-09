# Cloudflare backup for Shared Safari sync

Status: proposed design for review; no implementation or deployment approved by this document.

## Intent and agreed scope

Keep Shared Safari sync available to updated Mutuals clients when Serkosea's service is unavailable. Serkosea remains the primary so mixed Mutuals/SafariUtils parties continue sharing normally. The user explicitly accepts Mutuals-only sharing during an outage. Target scale is tens of installed users and usually five to ten simultaneous users, spread across time zones. Avoid operating a personal device and aim to remain on Cloudflare's Free plan.

The backup is an independent service, not a proxy to the primary. It must continue authenticating clients, admitting rooms, relaying observations and coordinating assignments without contacting Serkosea. It cannot redirect unmodified SafariUtils or old Mutuals clients.

## Existing system and source baseline

Reviewed Mutuals master: `9dc0fabc28edaabca24e730ec54e9563823434e6` (v4.2.1). Preserve the separate completed `feature/config-navigation` work at `1d6bdcd`. This design has its own isolated checkout based on the released master.

Owners: `docs/PARTY_SYNC.md` and `src/main/kotlin/net/johnceo/sparklingmutuals/sync/`. `SafariPartySync` adapts visit-local state, preserves provenance and publishes coalesced observations. `PartySyncTransport` owns asynchronous HTTP authentication, bounded socket queues, serialized sends, ping/liveness detection, reconnect backoff and callback generations. `PartySyncState` validates and merges progress, locks assignments and rejects unexpected backend run IDs. `PartyRoomDiscoveries` gates auto entry on an atomic snapshot from the currently validated socket.

The current primary uses UUID-claim session authentication, not Minecraft account authorization. It groups rooms by canonical Safari server ID and exact confirmed roster. It supports aggregate versions 2 and 3, `A/N/U/S/H/B` envelopes, atomic room Sparkling snapshots, biome reports and authoritative hotspot allocation. Primary socket leases can expire within ten minutes; renewal is expected and must not immediately trigger failover.

Upstream integration reference reviewed at SafariUtils `09b5b48bd673847032b5df53284fdd0514dd74e3`, particularly `docs/integrations/PARTY_SYNC.md`, `PARTY_SYNC_REFERENCE.md` and `PARTY_SYNC_UPDATE_2026-11-03.md`. Its private backend source and signing secrets are not in Mutuals and are not prerequisites for the backup.

## Approaches and recommendation

1. **Independent Cloudflare standby with automatic client failover — recommended.** Keep primary traffic unchanged during normal operation. Use a small shared routing record to coordinate updated Mutuals clients, and open backup data sockets after a sustained failure. Once switched, keep that provider for the current visit. This satisfies the agreed primary/backup behavior and avoids continuously duplicating gameplay observations.
2. **Continuously mirror all traffic to both services.** Faster recovery could be possible, but two assignment authorities and data copies create greater conflict, duplicate-message and usage risks. It is unnecessary for the stated small standby service.
3. **Proxy the existing service through Cloudflare.** This would still depend on Serkosea and would not meet the outage requirement.

Cloudflare was selected by the user. The standby mechanism, timing and implementation language below are engineering proposals. The code is a project spanning the mod and a separate service, not an endpoint-only edit.

## Architecture

- Keep all existing game trackers, mode requirements and completion accounting. Add provider selection around the existing transport and retain one gameplay socket per client.
- Add a TypeScript Worker and one SQLite-backed Durable Object coordinator for the initial five-to-ten-user deployment. The coordinator contains separately keyed logical party rooms and their routing records; one object does not mean cross-party sharing. This avoids multiplying active-object duration by the number of party rooms. Revisit physical partitioning after measured usage, without changing the room protocol.
- Host both independent UUID session issuance and `/v1/sync` on the backup origin. Tokens are signed by a secret stored only in Cloudflare and are scoped to that origin. The mod never receives deployment credentials or a signing secret. A primary token must never be sent to the backup, or vice versa.
- Use Cloudflare's WebSocket Hibernation API. Store the latest active-source snapshots and room-union data in connection attachments; persist routing epochs, assignment locks, acknowledgments and fairness in SQLite. Do not retain a permanent run-history database.
- No permanent secondary gameplay socket while primary is healthy. During an eligible Safari party, a bounded asynchronous routing check occurs at join and every 30 seconds with jitter. These requests send only the room identity, self UUID, local visit nonce and provider status, not inventory or encounter data. Solo and non-Safari sessions make none.
- Cloudflare routing availability is optional while primary works. A routing failure never blocks primary sharing. Backup selection is unavailable if both services cannot be reached; local gameplay continues.

## Provider selection and lifecycle

1. A new eligible visit tries Serkosea immediately. A routing check in parallel can discover an already-selected backup for that party visit. Do not delay primary startup waiting for Cloudflare.
2. Begin a sustained-unavailability timer when primary is not usable. Reset it only after a validated welcome and usable room state, not just a TCP connection or successful ping write. Propose a switch after 30 seconds continuously unavailable. Preserve the existing seven-second initial chat-allocation fallback; import finalized chat choices if the backup connects later.
3. Existing socket liveness detection remains bounded by the current 75-second no-inbound timeout; a silent black hole can therefore take longer than 30 seconds to detect and migrate. Do not advertise a 30-second maximum recovery time. Respect `Retry-After` and per-provider backoff. Refresh a rejected token first; invalid roster/lobby input must be corrected rather than triggering provider cycling.
4. Backup selection is a serialized, monotonic routing decision for the current room generation. Updated Mutuals clients check that shared decision even if their primary connection still works, so a partial outage does not leave them permanently divided. Convergence is bounded by the next successful routing check plus connection time; a brief partition is possible and is not an atomic cross-service transfer.
5. Close/invalidate the old gameplay socket before accepting the new provider's gameplay frames. Do not send the old server an explicit Safari `leave` merely for a transport/provider migration; its lease will expire. Explicit game-world departure still sends leave when possible.
6. Keep the backup for the rest of the visit. A mid-visit backup outage reconnects to backup and preserves assignments; it does not automatically open a competing primary or chat allocator. At a positively established new visit, prefer primary again.
7. Routing records use a server-issued generation plus per-player random visit nonces, separate from socket generations. Reconnect retains the nonce. Explicit departure retires it; a new local visit changes it. Old callbacks, records and nonces cannot clear or reopen a newer generation. If a known player presents a new visit nonce while other old-generation participants remain active, retire only that player's previous registration and hold the new one pending. It neither resets the other participants nor receives their old gameplay data. New generation creation waits until all old registrations have positively departed or the entire old generation has expired. Until then the pending newcomer can continue ordinary primary/local behavior but has no backup sharing or auto-entry permission. A confirmed never-before-registered roster addition may join the incumbent generation; it cannot reset it. A fully abandoned generation expires after ten minutes with no renewed participant lease. Treat ambiguous retained generations the same way: no silent gameplay import. This intentionally retains the existing reused-lobby uncertainty instead of guessing that an old room is fresh.
8. Keep source identity, timestamps, provider and generation on received data. Mismatched rosters never exchange gameplay state. Canonicalize `mini...`/`m...` and lobby case consistently. A stale roster connection cannot reverse a confirmed expansion or membership revision.

## State transfer without fabricated discoveries

- Preserve local visit evidence, outbound sequence, objective facts, caught-species negatives, finalized assignments and already-submitted chat instructions through a provider change.
- Clear transport readiness, current-inventory freshness and auto-entry permission during the switch. Incoming data from the replaced connection cannot revive them.
- Re-publish each client's own `A/N/U/S` snapshots and local biome reports after backup welcome. Keep the sequence increasing across providers and reconnects. This is a new live snapshot, not a catch-up-only upload.
- Import previously received irreversible visit facts through a separate validated failover-bootstrap extension. Preserve their original reporter/provenance; do not export them as the sender's new local `A` discoveries. A bootstrap may carry checked/resolved masks, monotonic terminal flags and caught-species negatives. It must not assert another player's current inventory or use prior-provider positive Sparkling sightings alone to authorize auto entry.
- Inventory becomes current only after that reporter contributes a validated snapshot to the backup. Disconnected SafariUtils players' last known holdings remain stale/unknown for live shared-supply decisions.
- Backup migration captures a reconciliation cohort from the currently leased updated Mutuals registrations at route selection. Every cohort member must finish its bounded bootstrap or positively retire its registration before the backup can authorize auto entry or finalize new assignments. Lease expiry alone is not evidence that its missing caught negatives or locks are safe to omit; an unreachable member leaves those automatic actions disabled for the visit while objective sharing continues.
- Bootstrap data uses a bounded staged transaction (multiple individually bounded frames if needed), completed before that client's positive Sparkling snapshot becomes eligible for automatic actions. Stage the room's positive sightings until the migration cohort reconciles, and merge caught negatives first. Welcome may advertise `room-sparklings-v1` immediately, but withhold the `sparklings` snapshot and positive Sparkling replay until the barrier is satisfied. This allows objective sharing and ordinary welcome timeouts without granting entry permission or requiring a second welcome. Once reconciled, send a complete atomic Sparkling room snapshot before individual Sparkling replay. A later admitted participant completes its own bootstrap before its positives are published. Auto entry still requires the exact current room, native visit, socket generation and a freshly received atomic backup snapshot. The barrier is necessary because an early found-only snapshot must not outrun a surviving cohort member's already-known catch.
- Bounded snapshots cannot reconstruct information that existed only on the failed service and was never received by a surviving client. No personal catches, rewards, PBs or history entries are synthesized.

## Assignments, fairness and duplicate instructions

- Implement the published hotspot contract, including confirmed attendance/ticket use, report timing, Java signed UUID-string hash tie-breaking, biome order, final locks, late arrivals, conflict freeze and leader-only notices. Do not substitute an independent allocation algorithm.
- Backup admission includes an explicit provider-migration bootstrap containing existing assignment locks and known already-submitted instruction targets. The backup imports these before accepting new allocation reports. Imported locks do not award new fairness wins.
- Add an explicit client authority-transition operation. Changing the validated provider may establish a new backend run/revision namespace while retaining every finalized lock and conflict. Ordinary unexpected run changes on the same provider remain rejected. This is necessary because the existing client correctly rejects arbitrary new backend run IDs mid-visit.
- Duplicate chat suppression must survive provider-specific notification ID changes: carry already-instructed player/biome targets alongside original run IDs. The verified current leader remains the only client allowed to submit requested chat instructions, with acknowledgment after submission.
- Record validated primary fairness snapshots in the updated client (the existing parser validates but does not retain those counters). Seed a new backup fairness ledger from the latest available matching primary run/revision, once, rather than adding copies reported by multiple clients. Retain separate primary and backup namespaces; do not feed backup-only wins back into the primary or retroactively award imported locks. Primary changes never received by a surviving client cannot be reconstructed. Treat missing counters as unknown bootstrap evidence until reconciliation, with zero as the published allocator's default where history genuinely is unavailable. Contradictory equal-revision bootstrap data freezes allocation. This can affect future unallocated tie priority but never changes displayed roles.
- During routing convergence, merge imported locks from all joining clients before finalizing new allocations. A new assignment requires the fixed migration cohort to reconcile as defined above; if a participant cannot reconcile, freeze new assignments rather than assume consent or a missing lock. Existing confirmed objectives continue sharing. Contradictory locks always freeze assignments and instructions.
- Unmodified SafariUtils/old Mutuals may continue their own outage handling. Their independent behavior cannot be controlled by this backup. Observe valid party-chat locks under the existing rules and freeze on contradictions; do not promise coordinated mixed-mod allocation during an outage.

## Protocol and admission

- Preserve existing version/length pairs: `A` v2 = 38 integers, v3 = 39 integers with held coins at index 38; retain exact bounds for `N/U/S/B`. Keep `catchUpOnly` retention/live-precedence semantics.
- Preserve current 4096-byte client message and 3072-character inner-payload bounds; enforce a maximum of 40 messages per ten seconds per socket, with the client staying at its existing 36-message budget. Reject malformed integer/string types, foreign senders, oversized frames, invalid rosters and expired tokens before publishing.
- Advertise `hotspots-v1`, `biome-reports-v1` and `room-sparklings-v1` only when their corresponding implementations and tests are present. Version routing and bootstrap separately; these extensions are used only on the owned backup.
- UUID claims identify service budgets; they are not proof of Minecraft account ownership. Retain that explicit trust model. Use bounded account/IP/global admission and connection leases. Do not put a shared backend password in a public mod or add Minecraft account authentication.
- No raw bearer tokens, complete payloads, headers or network addresses in diagnostics. Display primary/backup/retrying state through existing debug/status surfaces, with a short user-facing notice when a real provider switch occurs.

## Free-plan operation and delivery

Current published Durable Object Free allowances: 100,000 request units/day, 13,000 GB-seconds/day, 100,000 SQLite rows written/day, five million rows read/day and 5 GB stored. Workers have a separate request allowance. WebSocket request metering uses the published incoming-message weighting; include handshakes, routing checks, auth, alarms and storage operations in estimates. Exceeding a Free limit can interrupt the backup. Free availability is an aim, not an uptime guarantee.

At ten players for twelve eligible hours, a 30-second routing interval implies about 14,400 client routing requests/day before auth and retries. Gameplay snapshots are sent to backup only during outages. Use change-only writes, bounded attachments and retention, no empty-room heartbeat/alarm loops and no per-tick persistent history. Measure all resource categories against the current documented limits before public activation. Both services using Cloudflare means a provider-wide Cloudflare incident may affect both.

Package a standalone backend directory, deployment configuration, local protocol fixtures and operating instructions in the isolated checkout. A free `workers.dev` address is sufficient; a paid domain is not required. Use the user's own Cloudflare Free account. Deployment credentials remain outside the mod and version control. No deployment is considered successful until a real authenticated TLS WebSocket smoke test passes against the deployed origin.

Prepare and verify all local code before account/deployment handoff. Do not push, merge, create releases, contact others or alter the game instance without the user's explicit authorization. Never read `.devAuth`. A production endpoint or rollout does not exist yet.

## Required validation and acceptance

1. Healthy primary: existing wire behavior and mixed-mod support remain intact; unavailable Cloudflare routing does not disrupt it; no backup gameplay traffic is mirrored.
2. Primary transport/auth failure, prolonged outage and short lease-renewal interruption: only sustained failure activates backup; Retry-After is respected; no blocking work reaches the game thread.
3. Partial outage: updated clients converge on the same route; different-room/roster clients remain isolated; unreconciled clients block new assignment decisions, not objective updates.
4. Mid-visit migration: preserve sequences, local/received provenance, checked objectives, catches-as-negatives, locks and chat suppression. Stale holdings are not current inventory. Arbitrary same-provider run changes still fail validation.
5. Atomic Sparkling catch-up and auto entry: caught-before-found never triggers a false entry or alert; migration revokes entry permission until current backup snapshot validation; replaced sockets cannot authorize actions.
6. Backend: use Cloudflare's local Worker/Durable Object runtime to test real HTTP/WebSocket upgrade, auth, room isolation, hibernation recovery, replay, rate bounds, allocation timing, stable locks and leader acknowledgments. Unit tests alone do not prove those runtime properties.
7. Explicit departure, long disconnection, reused Safari server ID and confirmed roster expansion: reject stale generations and never silently import old visit data. Ambiguous recovery fails closed.
8. Both services unavailable and recovery: local gameplay stays usable; retain adopted roles without a competing allocator; next clean visit prefers primary. A backend-specific kill switch can disable the new route while leaving primary operational.
9. Run the mod's relevant regression suites and required multi-target builds after implementation. Live Hypixel/mixed-mod testing is a separate, explicitly authorized stage; no game-instance changes are implied.

## Engineering choices and remaining dependencies

Proposed choices for review: TypeScript Worker, one initial coordinator object, 30-second failure window, 30-second healthy-primary route checks, backup selected for the rest of a visit, no full live mirror, and fail-closed allocation when route reconciliation is incomplete. Exact source file decomposition and test-tool versions belong in the implementation plan after design approval.

The user still needs a Cloudflare Free account available for deployment. No account access or signing secret has been requested or read. Account setup, a deployed origin and live validation remain delivery dependencies, not completed work.

References: [Mutuals sync guide](https://github.com/johnceoskyblock/sparkling-mutuals/blob/9dc0fabc28edaabca24e730ec54e9563823434e6/docs/PARTY_SYNC.md), [upstream integration contract](https://github.com/Serkosea/safariutils/blob/09b5b48bd673847032b5df53284fdd0514dd74e3/docs/integrations/PARTY_SYNC.md), [upstream wire reference](https://github.com/Serkosea/safariutils/blob/09b5b48bd673847032b5df53284fdd0514dd74e3/docs/integrations/PARTY_SYNC_REFERENCE.md), [Cloudflare WebSocket hibernation](https://developers.cloudflare.com/durable-objects/best-practices/websockets/), [Cloudflare pricing](https://developers.cloudflare.com/durable-objects/platform/pricing/).
