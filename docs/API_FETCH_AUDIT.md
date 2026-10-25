# API fetch policy and inventory

## Mode and mutual discovery data

Modes and `!mutuals` share `PartyDiscoveryCache` through `SafariLookup`. This cache has **no time expiry** for unchanged current party members. It is in memory, scoped to the local account, Alpha/main scoreboard realm and selected API provider. It survives ordinary server transfers and new Safari runs in the same game process.

- Initial membership loads a discovery baseline for each member.
- Adding a member loads only that member. Removing a member discards their cached entry and immediately excludes their missing discoveries from mode calculations. Remaining members retain their baselines. A removed member who rejoins gets a fresh baseline.
- Changing modes or starting another run reuses the cache. Neither causes another profile fetch for loaded members.
- A server-confirmed Sparkling catch inside Safari updates the discoveries locally for party members present in that Safari instance. Members outside the run receive no invented credit. Detection alerts and player-written chat never grant discoveries.
- Local catch additions overlay pending API results, so delayed responses cannot undo them. The pre-catch snapshot determines history uniqueness when available; an unavailable baseline leaves the unique flag unconfirmed without another HTTP request.
- Requests for the same member coalesce into one in-flight load. Locks are released during HTTP, so a catch update on the client thread does not wait for network I/O.
- Successful members remain cached if a different member's lookup fails. Mode recovery retries missing/failed data after 60 seconds. One-shot commands still stop on failure.
- Unknown membership cannot be treated as a confirmed solo roster. Old membership results cannot replace a different selected party/provider's state.

Mode discovery loading is active inside Safari in every mode, or outside Safari with Sparkling mode selected. An accepted unchanged-party snapshot is not polled periodically. `!mutuals` may initialize missing baselines outside Safari, but repeated commands reuse them indefinitely. The intersection and missing-member lists are calculated from the selected members' discovery sets; the modes do not need profile cards, ticket balances or unrelated stats.

## Every HTTP trigger

| Trigger | Timing / cache |
| --- | --- |
| Mode discovery requirements | Initial/new-member baselines only, plus one-minute failed-load recovery while active. Shared retained party cache above. |
| `!mutual` / `!mutuals` | Shared retained party cache; only missing/new members fetch. Same sender/command has a ten-second request cooldown; no automatic command retry. |
| Personal stats / achievements | Initial SkyBlock load, then **30 minutes** between normal attempts. Saving a run does not refresh them. Ordinary server transfers retain the timer; account/provider changes reset it. Failed reads retain the existing 60-second retry policy. |
| `!missing <IGN>` | One-shot; same sender/command suppressed for ten seconds. Uses the ordinary shared HTTP profile cache, not the retained party cache. Separate uncached Mojang IGN lookup. |
| `!ticket` / `!tickets` | Same one-shot behavior as missing. Reads current ticket data through the ordinary profile path, with separate Mojang IGN lookup. |
| `/sparkling`, `/sparkling <IGN>`, comparison lists | One profile lookup per distinct requested player per invocation, with uncached Mojang IGN resolution. No command cooldown or automatic retry. Uses the ordinary shared HTTP cache. |
| `/sparkling party` | One profile lookup per party UUID per invocation, using the ordinary shared HTTP cache. Labels reuse the existing asynchronous party-name resolver and its one-minute name cache, including members outside the tab list. A failed name lookup displays an explicit unavailable-name label instead of a raw UUID. |
| Sparkling catch / history uniqueness | **No API fetch.** Uses and updates the retained party discovery snapshot as described above. Imported history also does not fetch. |
| Bazaar prices | One request at history initialization, then lazily as valuation needs it; **at most once every three hours**, including after a failed attempt. Failures retain existing prices. One in flight; 20-second request timeout. Public endpoint, no shared/personal key. |

`!m`, `!since`, PB commands, command help, starting-item messages, completion checks and supply reminders do not independently fetch SkyBlock data. `/rates` reads the verified roster and retained party discoveries only; it never requests roster or profile data. `/profitablecritters` reads already-cached Bazaar quotes without triggering even a normal refresh; it never adds HTTP requests. `getHoneyRefills()` exists but has no production callers; hive waypoints do not request API refill data.

## Transport and service boundaries

A configured personal Hypixel key takes precedence. Without one, the shared Shiny Hunter Worker is used if enabled. Without either provider, profile HTTP requests do not start.

`CloudApi.profiles` retains successful parsed HTTP 200 profile responses per normalized UUID for **60 seconds from request start**. This shorter transport cache remains in place for explicit profile/ticket commands and personal stats. HTTP/transport failures are not cached as successes. A structurally valid JSON response can be cached even if a later Safari reader rejects its contents. Missing Safari discovery data is still accepted as zero discoveries by the party baseline loader; malformed responses remain errors.

An uncached shared-provider lookup typically needs **one Worker request per player**; a personal-key lookup typically needs **two Hypixel requests** (`profiles`, then `profile`). The personal-key transport has no general response cache. The retained party discovery cache avoids either route for loaded members. Profile HTTP requests use ten-second timeouts and have no transport-level automatic retry.

Caches are per mod process, with no cross-user coordination. Party-command responder staggering suppresses duplicate replies, but each eligible mod client can start its own lookup. There is no client-wide rate budget or failure backoff shared across all consumers. The Worker source comment mentions a one-minute server cache and per-address limits, but deployed limits/configuration are unavailable in this repository and were not verified.

## Other lookups, unchanged

| Service | Current timing / cache |
| --- | --- |
| Mojang IGN-to-UUID | Named commands still resolve names per invocation without a cache at `HypixelApi.getPlayerUuid`. Guard/downtime remembers successful sender identities and retries failed pending resolutions after ten seconds. Catch-history verification no longer performs name or profile HTTP requests. |
| Mojang session profiles for party names | Autocomplete and `/sparkling party` reuse loaded names first, then asynchronous verified UUID lookups. Results, including unresolved results, are cached for 60 seconds. Waiting for an unknown in-game roster has a five-second timeout. Guard/downtime's separate resolver retains its ten-second failed-name retry. |
| Hypixel Mod API roster | In-game packets, not shared-key HTTP. Requested on server hello, membership changes and unknown-roster demand, plus every 60 seconds by active mode loading. In-flight requests coalesce; five-second timeout. Known roster information is reused. |

## Source and verification

Sources under `src/main/kotlin/net/johnceo/sparklingmutuals/`: `api/PartyDiscoveryCache.kt`, `api/HypixelApi.kt`, `api/CloudApi.kt`, `commands/SafariLookup.kt`, `safari/SafariSparklingMode.kt`, `commands/PartyCommands.kt`, `commands/SparklingLookup.kt`, `commands/PartyNameSuggestions.kt`, `history/OwnStats.kt`, `history/SparklingLog.kt`, `history/BazaarPrices.kt`, `history/RunHistory.kt`, and `party/PartyManager.kt`.

Focused automated coverage exercises retained members, joining/leaving members, local catch credits, delayed API results, partial failures, provider invalidation and the three-hour Bazaar boundary. Live roster and server-message timing remain Minecraft checks.

## Last updated

2026-10-25 — Renamed the local rate command to `/rates`; no new API requests. `/profitablecritters` now ranks ten results by estimated coins per run.

2026-10-21 — Added `/sparklingrates` as a read-only local consumer of cached roster/discovery data, with no new API requests.

2026-10-21 — Added `/profitablecritters` as a read-only consumer of existing Bazaar quotes; no new fetch trigger.

2026-10-21 (America/New_York) — Implemented retained member discovery baselines, local catch updates, thirty-minute personal stats and three-hour Bazaar throttling; reused resolved IGNs for party profile cards.
