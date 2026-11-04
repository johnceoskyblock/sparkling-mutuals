# CJ's Revenge doom-check detection and dry run

## Approved scope

The human approved the arrival-triggered detection, fresh leader exchange, cancellation handling, off-by-default setting, dry-run diagnostics and regression tests described in this conversation ("Let's go for it"). The delayed doom-check warp is existing behavior: keep its configuration and timing unchanged. Work locally on `experiment/cjs-revenge`, build/test/deliver Minecraft **26.1.2 only**, and keep normal instances untouched. No GitHub writes, installation, Minecraft operation or Discord delivery is requested for this increment.

This document records the approved scope and the implementer's conservative choices. It does not authorize actual admission. Live NPC targeting/reach, ticket-menu recognition and personal admission verification still await a recording.

## Approaches

1. Connect the already tested fresh-report core to existing detectors and party/location events, with a dry-run-only output boundary. This is the approved approach: useful timing evidence without spending tickets.
2. Infer entry eligibility from an old announcement or party invite. Rejected: neither proves current presence at the destination.
3. Connect live clicks now using the supplied coordinates. Deferred: centre coordinates do not demonstrate reach or identify the server's menu/admission sequence.

## Runtime behavior

- Add `Auto Safari Entry (dry run)` under Safari > Safari entry, default off, persisted per instance. Its description explicitly states that it exchanges party reports and writes diagnostics but never clicks or spends tickets. Both leader and joining clients opt in. There is no live-action mode switch.
- Register the integration only for Minecraft 26.1.2. Do not change the normal warp, invitation, entry guard or ticket helper.
- Synchronize connection/world identity at every callback and tick. A location packet applies only to the world observed when received; changing worlds invalidates it. Missing/early packets wait rather than inheriting `SafariIsland`'s previous mode. A fresh location packet reporting mode `safari`, a bounded server name and Hypixel connection is a **candidate arrival**, not proof of server admission. Use monotonic elapsed time.
- Form immutable account/realm/connection/world/arrival/current-party context only when the current roster and leader are known. Successful equal-roster refreshes retain continuity. Loss of known party info, roster/leader/account/realm/server/world/connection changes invalidate outstanding work. Never resurrect an old exchange after a verification gap. World/location ordering remains an explicit dry-run limitation.
- A nonleader at the mapped Safari entrance sends at most one `SMSE1` party query per candidate arrival. The existing 12-second preparation and 3-second reply deadlines are anchored to arrival, not late roster readiness or toggling. No retries. Re-enabling after a spent/cancelled exchange cannot query again in that arrival.
- The leader refreshes sightings from existing positive named/skin scans, independent of alert/ESP display switches. Unknown particles and old party announcements do not create positive reports. Refresh only on a real scan, not every render/tick over a retained list. Capture callbacks retire sightings. Use existing 5-second freshness, event identities, 128 retirement bound and at-most-three paced peer responses.
- Receive protocol traffic only from non-overlay server GAME messages with an exact Party envelope. Standard player CHAT content cannot impersonate a server Party envelope. Resolve a sender against the current connection's unique profile name and current roster UUID; missing/ambiguous identity fails closed. Do not start async name lookups or retain unverified messages. This authenticates the server-provided party sender, not the honesty of a modified leader client.
- Before sending a proposal, recheck its originating context, enabled state, current location/roster and role. Use only bounded `pc <encoded protocol>` commands. No movement, NPC interaction, menu input, ticket-helper callbacks, or admission-controller permits exist at the dry-run output boundary.
- A fresh accepted local/leader sighting can produce a local "would check entry NPC/menu" diagnostic. It never claims reach, an accepted ticket, admission or a guaranteed claim. No fresh observation means no justification, not proof the whole Safari is empty. Report expiry/cancellation once, avoiding per-tick chat spam.
- Keep bounded status/timing diagnostics available through `/sm debug`; log arrival, query send, reply receipt, evidence readiness/expiry and cancellation relative to monotonic arrival time. Do not persist raw party chat or collect/upload recordings.

## Verification

Exercise the real pure session and location/context policy: late arrival with an old hint, fresh reply, captured/unloaded/expired evidence, duplicate queries, third queued peer under pacing, wrong leader/name/server, early location packet, repeated equal location/roster, roster invalidation, toggle off/on, world/account/realm change, missing player, deadline edges and disconnect. Verify no dry-run result can expose a Manager/Ticket action. Add config persistence/default coverage and an integrated session test using actual existing protocol/requester/responder/detector contracts.

Run only `:26.1.2:test` and `:26.1.2:build`, plus relevant helper checks. Use the established external strict JUnit temp-root harness and temporary compiler heap if needed. Package a distinctly versioned 26.1.2 JAR with accurate instructions. Automated tests do not establish live timing, packet ordering, reach or server acceptance. Get an independent whole-change review before delivery.

## Last updated

2026-11-04 — Recorded the directly approved dry-run increment and its no-admission boundary.
