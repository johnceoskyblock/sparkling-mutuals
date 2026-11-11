# Shared Safari sync

Shared Safari sync is always active for eligible Safari parties; there is no configuration toggle. Mutuals connects to Serkosea's existing service without an extra mod, API key, or Minecraft account authorization. Solo sessions do not open a coordination socket. `/hotspot` still controls whether an assignment alert is shown.

## Shared state

- Personal quest-item discoveries and held feed, gems, incense, and Shining Coins use aggregate version 3 (`A`). Held counts come from the player inventory and menu cursor. Discoveries use starting grants and confirmed pickups, separate from floor-drop ESP predictions. The Forest objectives show food counts and unused starting food by player when their starting-item announcement and WebSocket inventory are both available. Cavern shows only gems still missing from the podium. Haunted omits per-player holdings. Turning off starting-item announcements excludes unused starting feed and coins from coordinated discovery totals. Field `A[38]` always reports actual held Shining Coins, including starting coins, clamped to 0–64 regardless of announcement settings. Gems and incense are always shared. Version 2 snapshots leave held coins unknown.
- Feeder consumption and remaining observed birds use the aggregate's usage fields; incoming `U` messages are also supported. Consumption generations advance monotonically, and equal-generation bird counts cannot increase through a delayed snapshot.
- Canonical checked nests (`N`), resolved walls/mounds, placed gems, candles and encounter flags merge within the visit. Local and received source evidence remain separate, so received observations are not exported as new local discoveries.
- Known mounds remain in the objective count after unloading. A local attack followed by break chat or a sustained disappearance within reliable Cavern entity range can resolve a known mound and publish its canonical slot.
- Known sparkling discoveries/catches use the species masks (`S`). Received discoveries can reach auto-party, but received catch masks never retire a separately observed local entity or update permanent catch history.
- Local biome clears use `B` independently of the party-chat toggle. Received backend clears are authoritative across biomes. Manual chat clears are trusted until positive contradictory evidence is observed. Reports do not create captures, rewards, PBs or run-history entries. Reports received before ticket use are retained for the current visit.

Per-critter completion on the Mutuals completion channel feeds the mode HUDs. Unique combines capture proof with shared checks for needed Sparkling variants. Sparkle consumes shared render checks, and Full Clear consumes shared capture and render completion. Positive local evidence of unfinished sources or remaining captures can contradict a peer result. Shared proof stays separate from local report and PB evidence.

Teammate starting food and Shining Coins remain available as shared inventory information, but never block this client's mode completion or biome reporting. Local starting items still follow this client's announcement preference. Backend biome reports, valid manual reports, and completed local or shared mode requirements update the Progress HUD's biome icon.

Public encounter chat is processed immediately by the existing trackers. Its aggregate-only changes upload as `catchUpOnly`; inventory, feeder or private/world observations in the same batch take live-delivery precedence. Reconnect publishes a live snapshot. Received terminal encounter flags update objectives, not capture totals.

## Hotspot authority and fallback

The backend owns allocation and fairness when `hotspots-v1` and `biome-reports-v1` are advertised. Clients send confirmed presence and actual reports; party membership alone does not establish attendance. The backend's validated final assignment is displayed directly. The verified current party leader also derives chat instructions from finalized assignments after a two-second grace period, without requiring backend notifications. Accepted peer sync messages and coordinated SM/SU hotspot reports identify known assignment clients; roster mapping alone does not. Known clients remain recognized through reconnects within the visit. Instructions are deduplicated per recipient and biome. Backend notification IDs are acknowledged only after actual chat submission.

Wait up to seven seconds for initial backend authority. If unavailable, retain the existing leader-owned chat allocation. Finalized chat choices are imported as locks on backend recovery. No displayed assignment may move. A backend-owned visit retains its results and reconnects through outages instead of starting a competing chat allocation. Conflicts freeze new automatic assignments/instructions.

With /hotspot enabled, clients still announce their actual hotspot in party chat. Backend coordination continues to own assignment and suppresses the chat fallback allocator. With auto biome reports enabled, local clears queue a party message independently of server self-echoes. The client waits briefly for competing reports before sending. Unique Icy shares completion without a chat code. Normal sparkling announcements and user-requested party commands retain their existing behavior.

## Sparkling auto entry

Enable **Sparkling auto entry** in **Party → Party warping**. It defaults off. After a verified leader warp, an uncaught Sparkling in the current shared room can trigger the approach to the Safari Manager and selection of the lowest available Safari ticket. The entry attempt is bounded to the current arrival. Manual input, an unavailable room, or loss of entry readiness cancels the attempt.

Auto entry continues while Minecraft is in the background. While the feature is enabled on Hypixel, Minecraft skips the automatic pause menu when its window loses focus.

## Connection and privacy

The primary socket requests protocol 2 with `X-SafariUtils-Sync-Protocol: 2`. A visit UUID and the accepted server run UUID survive reconnects. H hello advertises `run-reconciliation-v1` and `critter-completion-v1`, with `visitId` and `resumeRunId`; later H operations carry `visitId` and `runId`. Pending reconciliation retries every two seconds and preserves local tracking and finalized assignments.

Shared decisions wait for a matching `replay-complete` boundary after the entire replay has been applied on the game thread. Frames from another run, visit or replaced socket are ignored. Queue overflow reconnects rather than accepting partial replay. Retained aggregates with `inventoryFresh:false` preserve durable source facts without restoring held items or feeder occupancy.

With both capabilities negotiated, completion C uses the primary socket and includes `version`, `runId`, `visitId` and exactly 37 `rows`. Snapshots replace each reporter's previous evidence, even when counts decrease or flags clear. Expiry includes server age and client queue delay. Samples use existing local trackers at most every 500ms, with a ten-second heartbeat and coalesced pending changes. The independent legacy completion service retains its rows-only payload; only one completion channel is active.

Service: `https://api-dev.serkosea.dev`; socket: `wss://api-dev.serkosea.dev/v1/sync`. Authentication sends only the local player's compact UUID to `/v1/auth/uuid/session`. Short-lived tokens are reused across reconnects. Minecraft session credentials, passwords and private keys are never used. The service receives public roster UUIDs, the Safari server ID, and the bounded objective data above—not a complete inventory dump.

The complete confirmed 1–4 UUID roster and Hypixel Mod API server name identify the room. Uncertain roster changes pause hotspot publication. Objective rooms retain the visit roster through temporary disbanding; confirmed physically present additions can expand it. An expansion preserves sequence numbers and local evidence. Leaving/changing Safari clears visit observations and invalidates old socket callbacks. Explicit departure sends `leave` before socket close; transport failures do not send leave.

Network work is asynchronous, queues are bounded, outgoing messages are serialized and capped below the server's 40/10-second limit, and reconnects back off from 2 to 30 seconds (or longer Retry-After). Tokens and raw network errors/headers/payloads are not logged. `/sm debug` includes room readiness, authority, conflict, roster and sequence status. No additional Hypixel collection API requests are made.

## Independent backup (updated Mutuals clients)

The primary remains Serkosea. During an eligible Safari party the optional Cloudflare standby receives an asynchronous room-identity/provider check every 30 seconds with jitter, without mirroring existing A/N/U/S/B/H gameplay frames. The separate completion channel below is active independently. A continuously unusable primary can select the backup after 30 seconds; silent socket failure can take longer because the existing 75-second liveness detection still applies. Healthy clients in the same validated room follow the shared selection on their next successful check. Backup stays selected until the visit ends; a new visit prefers primary. Solo/non-Safari play sends no routing requests.

Migration preserves local observations, sequence numbers, irreversible received evidence, finalized roles and submitted instruction targets. Peer inventories are cleared until their reporters send fresh backup snapshots. Original reporters and room-snapshot provenance remain distinct from local discoveries. The fixed migration cohort must reconcile catches/locks or positively depart before automatic entry or new roles are enabled. An expired participant lease alone does not waive that barrier. A fresh atomic room snapshot from the exact current socket is still required for auto entry. Contradictions freeze new assignments and instructions; no captures, rewards or saved history are fabricated.

The client shows a brief notice on a real provider switch and reports primary/backup state in the existing developer status. There is no mid-visit competing allocator if backup also fails. Reused server IDs with ambiguous/new player visit nonces fail closed instead of importing old gameplay. Different rosters cannot exchange objectives. Unmodified SafariUtils/older Mutuals cannot follow this backup, and seamless mixed-mod outage behavior is not promised.

The separately hosted service, local runtime tests, deployment/kill-switch instructions and Free-plan usage worksheet are in `backend/cloudflare-sync/README.md`. UUID claims are not proof of Minecraft ownership. Tokens are origin-scoped; no service signing/deployment secret belongs in the mod. The hosted endpoint passed authenticated TLS/WebSocket and reconciliation/allocator smoke checks. Live Hypixel outage/gameplay testing remains separate and pending. The v4.2.2 release builds this integration for Minecraft 26.1.2, 26.2 and 26.3; the earlier private test artifact was 26.1.2 only. GitHub build/release checks also test the backend in Cloudflare's local runtime; those checks do not redeploy the hosted service.

## Testing with SafariUtils

Use protocol-compatible builds on the same Safari server and roster. Verify:

1. Duplicate hotspots produce identical assignments in both mods; only the leader instructs non-display players.
2. Gems, checked nests, resolved walls/mounds and feeder progress reach the other client without duplicate catches or alerts.
3. Late ticket use receives retained objective/biome reports; incomplete capture quotas remain incomplete.
4. Reconnect before and after allocation preserves assignments and does not duplicate instructions.
5. Backend failure uses chat fallback only before authority was adopted; recovery respects finalized locks.
6. Leaving and starting another Safari clears the previous visit, including when a server ID is reused.

Local validation includes a real localhost HTTP/WebSocket handshake and ordered leave/close test, malformed protocol bounds, welcome mapping, replay/deduplication, inventory freshness, source provenance, and assignment-lock conflicts. Live service admission and mixed-mod in-game behavior still require a joint test with Serkosea. A reused lobby ID can match a retained backend generation if earlier attendees never sent leave; unexpected generation changes are rejected rather than silently resetting shown assignments.

## Last updated

2026-11-10 — Added negotiated run reconciliation, ordered replay readiness, stale inventory handling and shared per-critter completion. Kept mode/capture requirements and legacy fallback.

## Per-critter mode progress (Mutuals extension)

The negotiated primary socket carries `critter-completion-v1` when run reconciliation is also available. Otherwise a separate `/v1/completion-sync` socket on our Cloudflare service reuses the validated exact roster, lobby, visit nonce and route generation. This fallback has no hotspot allocation, discovery masks, auto-entry authority or main failover bootstrap. Older clients continue their existing sync; they do not supply this extension.

The primary service advertises both capabilities in its WebSocket welcome; C activates after reconciliation and replay. No HTTP support probe is used. Unsupported servers retain the separate completion channel. Switching clears received completion evidence and sends fresh local proof. Shared C uses the main connection's sequence and validated sender mapping. Evidence lifetime includes time spent in the client's incoming queue.

Each client sends a replaceable snapshot of all 37 species in the existing `S` species order. Shared `C` envelopes contain increasing sequence, kind and a JSON-string payload `{version:1,runId,visitId,rows:[[flags,checks,captures],...]}`. The private legacy fallback omits runId and visitId. Flags are Unique completion=1, qualified Sparkling check=2, Full Clear capture/source/absence proof=4. Counts are integers 0–256. Boss checks still require the existing actual-catch predicate. Source waivers and qualified checks are observational client evidence, not server verification of Minecraft actions.

Changed snapshots are sampled every 500ms; unchanged evidence renews every ten seconds. Latest false flags replace earlier true flags. Evidence expires after 45 seconds, with server-supplied age deducted on reconnect replay. Exact visit/roster changes, disconnection or a new run clear received progress. Multiple players' counts use the maximum, never a sum of possibly overlapping UUID sightings or catches. Partial disjoint cohorts are deliberately not combined into a completed check.

Mode progress, missing lists, fixed counters, capture status icons and the `!m` response use this evidence. Live local source contradictions and remaining Full Clear critters still take priority. Local source/capture checklists, local-only biome announcements, personal counts, PB eligibility and saved history splits exclude the new evidence. It cannot create captures, rewards or permanent history and is never echoed as new local proof. Completed run alerts may use shared presentation progress.
