# Shared Safari sync

Shared Safari sync is always active for eligible Safari parties; there is no configuration toggle. Mutuals connects to Serkosea's existing service without an extra mod, API key, or Minecraft account authorization. Solo sessions do not open a coordination socket. `/hotspot` still controls whether an assignment alert is shown.

## Shared state

- Personal quest-item discoveries and held feed, gems, incense, and Shining Coins use aggregate version 3 (`A`). Held counts come from the player inventory and menu cursor. Discoveries use starting grants and confirmed pickups, separate from floor-drop ESP predictions. The Forest objectives show food counts and unused starting food by player when their starting-item announcement and WebSocket inventory are both available. Cavern shows only gems still missing from the podium. Haunted omits per-player holdings. Turning off starting-item announcements excludes unused starting feed and coins from the shared counts. Gems and incense are always shared. Version 2 snapshots leave held coins unknown.
- Feeder consumption and remaining observed birds use the aggregate's usage fields; incoming `U` messages are also supported. Consumption generations advance monotonically, and equal-generation bird counts cannot increase through a delayed snapshot.
- Canonical checked nests (`N`), resolved walls/mounds, placed gems, candles and encounter flags merge within the visit. Local and received source evidence remain separate, so received observations are not exported as new local discoveries.
- Known mounds remain in the objective count after unloading. A local attack followed by break chat or a sustained disappearance within reliable Cavern entity range can resolve a known mound and publish its canonical slot.
- Known sparkling discoveries/catches use the species masks (`S`). Received discoveries can reach auto-party, but received catch masks never retire a separately observed local entity or update permanent catch history.
- Local biome clears use `B` independently of the party-chat toggle. Received backend clears are authoritative across biomes. Manual chat clears are trusted until positive contradictory evidence is observed. Reports do not create captures, rewards, PBs or run-history entries. Reports received before ticket use are retained for the current visit.

Public encounter chat is processed immediately by the existing trackers. Its aggregate-only changes upload as `catchUpOnly`; inventory, feeder or private/world observations in the same batch take live-delivery precedence. Reconnect publishes a live snapshot. Received terminal encounter flags update objectives, not capture totals.

## Hotspot authority and fallback

The backend owns allocation and fairness when `hotspots-v1` and `biome-reports-v1` are advertised. Clients send confirmed presence and actual reports; party membership alone does not establish attendance. The backend's validated final assignment is displayed directly. Only the verified current party leader sends requested non-display player instructions, and acknowledges them after submitting game chat.

Wait up to seven seconds for initial backend authority. If unavailable, retain the existing leader-owned chat allocation. Finalized chat choices are imported as locks on backend recovery. No displayed assignment may move. A backend-owned visit retains its results and reconnects through outages instead of starting a competing chat allocation. Conflicts freeze new automatic assignments/instructions.

With /hotspot enabled, clients still announce their actual hotspot in party chat. Backend coordination continues to own assignment and suppresses the chat fallback allocator. With auto biome reports enabled, local clears queue a party message independently of server self-echoes. The client waits briefly for competing reports before sending. Unique Icy shares completion without a chat code. Normal sparkling announcements and user-requested party commands retain their existing behavior.

## Sparkling auto entry

Enable **Sparkling auto entry** in **Party → Party warping**. It defaults off. After a verified leader warp, an uncaught Sparkling in the current shared room can trigger the approach to the Safari Manager and selection of the lowest available Safari ticket. The entry attempt is bounded to the current arrival. Manual input, an unavailable room, or loss of entry readiness cancels the attempt.

## Connection and privacy

Service: `https://api-dev.serkosea.dev`; socket: `wss://api-dev.serkosea.dev/v1/sync`. Authentication sends only the local player's compact UUID to `/v1/auth/uuid/session`. Short-lived tokens are reused across reconnects. Minecraft session credentials, passwords and private keys are never used. The service receives public roster UUIDs, the Safari server ID, and the bounded objective data above—not a complete inventory dump.

The complete confirmed 1–4 UUID roster and Hypixel Mod API server name identify the room. Uncertain roster changes pause hotspot publication. Objective rooms retain the visit roster through temporary disbanding; confirmed physically present additions can expand it. An expansion preserves sequence numbers and local evidence. Leaving/changing Safari clears visit observations and invalidates old socket callbacks. Explicit departure sends `leave` before socket close; transport failures do not send leave.

Network work is asynchronous, queues are bounded, outgoing messages are serialized and capped below the server's 40/10-second limit, and reconnects back off from 2 to 30 seconds (or longer Retry-After). Tokens and raw network errors/headers/payloads are not logged. `/sm debug` includes room readiness, authority, conflict, roster and sequence status. No additional Hypixel collection API requests are made.

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

2026-11-03 — Implemented the supplied SafariUtils party-sync v2, hotspot and biome-report contracts. Kept existing mode/capture requirements and chat fallback.
