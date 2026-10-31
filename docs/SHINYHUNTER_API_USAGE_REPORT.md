# Sparkling Mutuals — shared API usage report

Updated October 31, 2026, for the ShinyHunter shared API maintainer. This describes the current development implementation for Minecraft 26.1.2. Older installed builds may have higher refresh rates.

Hi! Here is how Sparkling Mutuals currently uses your shared API, including the reductions we have just implemented.

## Connection and request unit

- Endpoint: `GET https://shinyhunter-api.shinyhunter-api.workers.dev/profile/<uuid>`.
- User-Agent: `Sparkling Mutuals`.
- UUIDs are normalized to lowercase without hyphens.
- A normal uncached player lookup sends **one HTTP request to your Worker**, even though our code reads both the selected profile and its member data.
- A configured personal Hypixel key takes precedence: those profile requests go directly to Hypixel rather than your Worker. Shared access can also be disabled.
- HTTP request and connection timeouts are 10 seconds. There is no automatic retry inside the HTTP transport itself.

All counts below mean **requests from the mod to your Worker**, not requests your Worker makes to Hypixel. Your server-side cache and request coalescing determine upstream usage; we have not verified the deployed Worker configuration or limits.

## Client caches

Each running Minecraft/mod process has its own caches. Separate game instances, including ones on the same computer, do not share them.

1. **General profile cache:** successful selected profiles are reused for 60 minutes, with simultaneous reads sharing work. Observed catches update discovery lists and total lower bounds locally. This covers commands, stats and repartying.
2. **Transport response cache:** successful parsed HTTP 200 responses are cached per UUID for 60 seconds, measured from request start. All shared-profile consumers reuse this cache, and access is serialized inside the process. Cache expiry alone does not cause a request.
3. **Party discovery cache:** Safari requirements and `!mutuals` retain loaded discovery sets with **no periodic expiry** for unchanged party members. Simultaneous requests for the same member share an in-flight lookup. This cache survives new runs, mode switches and ordinary server transfers, but is not saved across process restarts.

Party discovery data is scoped to the local account, selected API provider and Alpha/main-server realm. Changing those scopes invalidates the applicable discovery cache. Ordinary leaving members are removed from the calculation and cache; if they rejoin, their baseline can reuse a profile fetched within the past hour. Managed Sparkle Warping/Leech disbands are an exception: the planned party is reserved in the session cache, so repeated re-invites of those members cause **zero additional profile requests** after their initial successful baselines. Expected leechers remain in the leader's mode calculation only while there is room for their planned return; this requires no additional profile endpoint. Members who remain do not need another lookup.

## What triggers shared API requests?

| Feature | When it fetches | Normal frequency |
| --- | --- | --- |
| Safari mode requirements | Initial discovery baseline for the current party, including self; then only new/rejoining members | One uncached lookup per newly needed member, not per run or mode switch |
| Personal Safari statistics / achievements | Initial eligible SkyBlock load, then periodic refresh | Checked every 30 minutes, but successful profiles are fetched at most hourly; no after-run refresh |
| `!mutual` / `!mutuals` | Missing party discovery baselines only | Loaded members stay cached; repeated commands need no Worker requests |
| `!missing <IGN>` | An accepted explicit party command | One lookup for the named player, subject to the 60-minute profile cache |
| `!ticket` / `!tickets` | An accepted explicit party command | Fresh-profile path, subject to the 60-second transport cache |
| `/sparkling` / `/sparkling <IGN>` | Explicit local command | One lookup for the requested player, subject to the profile cache |
| `/sparkling <IGNs...>` | Explicit comparison command | One lookup per distinct supplied player, subject to the profile cache |
| `/sparkling party` | Explicit local command | One lookup per party member, subject to the profile cache |

Mode discovery loading is active inside Safari in all modes, or outside Safari when Sparkling mode is selected. A confirmed solo roster needs only the local player's baseline. Unknown roster information does not trigger guesses about who to fetch.

Party commands suppress the same sender + command for 10 seconds on each client. This is **not a global command limit**: different senders or arguments have different cooldown tokens. Local `/sparkling` commands have no explicit command cooldown, although the lookup worker serializes them and successful profile responses remain cached.

## Catches and features that do not fetch

A server-confirmed Sparkling catch inside Safari updates the discovery cache locally for participating party members. It sends **no Worker request**. Local updates also survive an API response that was already in flight. Catch-history uniqueness uses the pre-catch cached baseline; if that baseline is unavailable, the unique flag stays unconfirmed rather than fetching again.

These also send no requests to your Worker:

- Ordinary captures, biome/run completion, mode switching with loaded data, and starting-item/supply reminders.
- Sparkling detection, alert messages and history imports.
- `!since`, `!since unique`, PB commands, `!m` and command help.
- `/rates`: reads existing party discoveries and roster only. `/profits`: reads existing Bazaar quotes only. Neither command starts a fetch.
- Honeyhive waypoints: an API refill-reader function exists but has no production callers.
- Party roster checks: these use Hypixel Mod API in-game packets, not your HTTP service.
- Player-name resolution: Mojang services, not your Worker. `/sparkling party` reuses the existing name resolver.
- Bazaar prices: the public Hypixel Bazaar endpoint directly, at most once every three hours per running process, including failed attempts.

## Failure and multi-client behavior

- Failed HTTP/transport responses are not cached as successful profiles.
- Active mode loading retries missing/failed discovery data 60 seconds after a failed pass. Successfully loaded members are retained, so they are not fetched again during that recovery.
- Personal-stat failures independently retry after 60 seconds while in SkyBlock.
- One-shot commands do not retry automatically; a new command can start another attempt.
- Missing profile/Safari discovery data is accepted as zero discoveries for party requirements and retained normally. Rate limits, transport errors and malformed data remain failures.
- There is currently **no process-wide request budget or shared failure backoff across features**. Roster/provider changes can reset the mode attempt cooldown.
- Multiple mod users may independently fetch for the same party command. Response staggering/deduplication reduces duplicate party-chat replies; it does not elect one client to make everyone's HTTP requests.

During a persistent outage, one mode-loading retry loop plus one personal-stat retry loop can therefore produce approximately **two failed requests per minute per active client**, assuming both features are active and no commands or roster/provider changes occur. This is an estimate of the two retry loops, not a hard rate limit.

## Estimated healthy usage

Let `P` be party size, including the local player, and `M` the number of running mod clients using shared access. Assume successful responses, no explicit lookup commands, no party changes and no process restarts.

| Scenario | Estimated Worker requests |
| --- | --- |
| One client initially loads a party of `P` | Approximately `P`; an initial personal-stat request can add one if it falls outside the self-profile cache window |
| Same party, subsequent Safari runs or mode switches | Zero additional mode/discovery requests |
| Stable session after initial loading | Approximately 1 request per hour per client for personal statistics |
| One new member joins | Up to 1 additional request per observing client; zero if that profile is already in its ordinary response cache |
| One member leaves | Zero; their data is removed locally |
| A party Sparkling is caught | Zero; discoveries are updated locally |
| A cold `/sparkling party` lookup | Up to `P` requests on the invoking client |
| A cold `!missing` or ticket command heard by `M` mod clients | Up to `M` Worker requests for that player |

**Example:** four mod users initially loading four players produce approximately **16 Worker requests** in total. The 30-minute personal-stat check reuses the hour-long cache. Thereafter, personal stats produce approximately **4 Worker requests per hour** across those clients. Explicit ticket queries can add requests through the short-cache path.

A retained party baseline need not refresh its full profile. A later explicit profile command can fetch again once that profile is at least an hour old; mode-only reads remain cached. Ticket queries intentionally use the shorter transport cache.

These are source-based estimates, not measured telemetry or upstream Hypixel request counts. Happy to compare them with your Worker logs and adjust the client if you see unexpected bursts.

## Source references

Current implementation: [CloudApi](../src/main/kotlin/net/johnceo/sparklingmutuals/api/CloudApi.kt), [PartyDiscoveryCache](../src/main/kotlin/net/johnceo/sparklingmutuals/api/PartyDiscoveryCache.kt), [SafariSparklingMode](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariSparklingMode.kt), [SafariLookup](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/SafariLookup.kt), [OwnStats](../src/main/kotlin/net/johnceo/sparklingmutuals/history/OwnStats.kt), and [full client fetch inventory](API_FETCH_AUDIT.md).

## Last updated

2026-10-31 — Documented session discovery reservations for managed disband/reparty cycles; repeated joins of the saved party do not trigger fresh baselines.

2026-10-25 — Renamed the local rate command to `/rates`; no new API requests. `/profits` now ranks ten results by estimated coins per run.

2026-10-21 — Included the two read-only local estimate commands; shared API traffic is unchanged.

2026-10-21 (America/New_York) — Maintainer-facing report of current local client behavior; no API calls, live load tests or code changes were made to prepare it.

Successful profile/discovery reads and IGN-to-UUID lookups are reused for 60 minutes, including after repartying. Simultaneous reads share one request. Failed lookups are not cached; background failures keep their one-minute retry. Observed catches update participating players’ discovery lists locally. Current mode discovery baselines remain cached while members are active or reserved for warping. Ticket and Honeyhive queries bypass the hour-long profile cache and retain the existing short transport-cache freshness. No periodic request is added by cache expiry. Account/provider/Alpha scope changes prevent cross-scope profile reuse.
