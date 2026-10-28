# Safari API lookups and recovery

## Discovery data

Sparkling-mode progress and discovery-dependent helpers use the selected party's shared Sparkling discoveries. Each member must have a successful lookup before that party state is ready. A complete party containing a new player is valid data: an absent/null `safari` object or absent/null `discovered_sparkling_critters` list on an otherwise valid selected profile member means **zero Sparkling discoveries**, so every species still needs checking for that player. `/sparkling` uses the same parsing and shows zero captured Sparklings when there are no discoveries and no supplied total.

Party discovery lookups also treat the specific `SkyBlock profile or Safari data unavailable` result (`DATA_UNAVAILABLE`) as zero discoveries, including absent selected profiles/members. This lets the party state become ready with every species needed for that member. Each mod user independently sends their own qualifying unused starting food/coin quantities; another mod user's report does not suppress that client's report. The successful empty discovery result uses the normal provider-scoped cache.

HTTP errors, invalid credentials, rate limits and malformed discovery objects/lists remain failures. Maintained background features retry those failures; one-shot commands stop after reporting the failure. Malformed payloads use a separate failure kind so they cannot accidentally trigger the no-data fallback. One-shot profile cards and personal-stat reads retain their absent-profile error behavior; the fallback above applies to shared discovery lookup requirements. Unrelated absent stats, such as Hunting XP or essence, retain their existing unknown presentation.

Per-member discovery sets are retained alongside their intersection. Completion-time supply reminders can therefore name exactly which current party members need each related Sparkling, including zero-discovery members. Party/provider changes invalidate both views together; incomplete refreshes cannot replace a complete accepted snapshot.

## Retained party data

Timesave `!missing` lists each missing bird by name; `!mutual`/`!mutuals` lists each bird shared by every party member. Bluebird, Parakeet and Macaw are no longer collapsed into `All Birds`, so partial discoveries remain visible. Bird-food spawning and completion rules still treat the birds as a shared source group. Older `All Birds` replies remain recognized for mixed-version parties.

Mode requirements and `!mutuals` use the same retained discovery baselines. Roster additions fetch only new members; removals drop that member from the calculation immediately and retain everyone else. A new run or mode switch reuses loaded data. Server-confirmed catches update participating members locally and cannot be overwritten by a delayed API result. Discovery caches are scoped to account, provider and Alpha/main realm and survive ordinary server transfers. Process restart, provider/account/realm changes, or leaving and rejoining the party require appropriate fresh baselines.

Live history uniqueness now uses the pre-catch cached discovery set without another API call. If the baseline is unavailable, the flag stays unconfirmed. Personal stats refresh normally every thirty minutes, and Bazaar attempts are throttled to three hours, including failures. `/sparkling party` uses the existing UUID/name resolver instead of UUID fragments. Explicit `!missing`, ticket and profile-card fetch behavior is unchanged.

## Retry behavior

An active background party-discovery lookup retries **60 seconds after a failure**, without switching modes. User-invoked party commands such as `!missing` attempt their lookup once per request. Any failure, including an invalid IGN, is shown locally and that request ends; another attempt requires a new command. `/sparkling <IGN>` and multi-player/profile-card requests likewise remain one-shot. Personal Safari stats also retry after a minute on failure, retaining their normal thirty-minute refresh after successful reads and no after-run refresh. Requests run off the client thread. Missing credentials with the cloud disabled still require configuring a usable provider.

A repeated failure schedules the next attempt one minute later. Changing the provider/key can wake a failed request immediately. Successful party discovery baselines persist for unchanged members with no time expiry; failed calls are not cached as successes. A successful complete lookup ends the failed-lookup retry cycle. Repeated identical discovery warnings remain suppressed.

The Sparkling-mode worker releases its busy flag before rejecting a result for temporarily unavailable/different party membership. Rejected or incomplete results schedule recovery; they cannot leave progress permanently displaying `?` because a finished worker remains marked busy. Generation checks protect newer lookups from older callbacks. Unknown current party membership still cannot be treated as solo or as successful data.

One-shot `/sparkling` card requests keep their existing explicit-command behavior; automatic one-minute retries apply only to maintained background feature state, not past user commands.

## Validation

### Party-name autocomplete

`/sparkling` suggests current-party IGNs and the `party` keyword. For comparison lists, complete only the current name token and omit names already entered. Name arguments in `!missing`, `!ticket`/`!tickets`, `!pb doom` and `!pb wumpa` also suggest current-party names. Supported chat prefixes are plain `!` input, `/pc`, `/pchat`, `/p chat` and `/party chat`. Boss PB suggestions retain the `duo` option. Commands that take a biome, downtime reason or other non-name argument keep their normal suggestions.

Suggestions wait for the verified Mod API party roster, then use loaded player profiles, the existing party-name cache, or an asynchronous Mojang session-profile lookup for each unresolved roster UUID. Those UUID/name results are cached for a minute. This works independently of entry-guard/downtime settings and includes party members on other servers. Typing/Tab never starts a SkyBlock discovery request. Unknown roster/name information is not replaced by unrelated lobby players or raw UUIDs. Suggestions match prefixes case-insensitively and retain the IGN's display casing. Vanilla handles the dropdown and Tab selection; unrelated chat and commands keep their existing completion behavior.

Automated checks cover prefix matching, comparison-name replacement ranges, already-entered names, supported party-chat wrappers, irrelevant input and actual Minecraft suggestion-hook call sites. Live checks: type `/sparkling Jo`, `/sparkling Alice Bo`, `!missing Jo` while in party chat and `/pc !tickets Jo`, then press Tab. Confirm only the current name changes and unrelated commands still complete normally. Live behavior with other mods remains unverified.

Focused automated checks cover the exact 60-second boundary and repeated failures, provider changes, empty discovery fields feeding a ready party state, malformed responses remaining failures, and existing mode/data behavior. Live API failure recovery and party-roster timing remain Minecraft checks.

Test in Sparkling mode without toggling modes: let a request fail, restore API access, and wait for the next one-minute retry. Test a party member with no Sparkling history or unavailable profile data: progress should leave `?` after all members load, all species should need Sparkling checks for that member, and every mod user holding starting food/coins should send their own combined report. Malformed payloads and transport errors should remain failures. Confirm a new party or provider cannot accept an earlier lookup's result.

## Fetch inventory

See [API fetch audit](API_FETCH_AUDIT.md) for every current HTTP trigger, client cache, cooldown and retry scope. The inventory describes the current reduced-fetch policy.

## Last updated

2026-10-27 — `!missing`, `!ticket` and `!tickets` accept an omitted IGN and resolve it to the verified party-chat sender, never the responding mod user. Explicit-name queries remain supported. Omitted and explicit self queries share the same request fingerprint and existing cooldowns; lookup/cache behavior is unchanged.

2026-10-22 — Listed individual birds in timesave discovery replies, including partial missing/shared bird sets.

2026-10-21 — Retained party discovery baselines and local catch credits; only new members fetch. Personal stats now refresh every thirty minutes, Bazaar every three hours, and party profile cards resolve IGNs.

2026-10-21 — Documented all API fetch triggers and cache timings without changing API behavior.

2026-10-20 — Fixed party-name completion when the roster or name cache was not already populated; unresolved verified UUIDs now resolve asynchronously.

2026-10-20 - Added roster-only player-name autocomplete for Sparkling and player-taking party commands without extra profile fetches.

2026-10-20 - Retained per-member discoveries for named unused-supply party reminders.

2026-10-19 - Removed automatic retries for one-shot party commands; nonexistent IGNs and other failures end the request, while background mode/feature retries remain.

2026-10-19 - Extended zero-discovery party requirements to unavailable profile/member data and verified independent starting-drop reports from multiple mod users.

2026-10-19 - Added one-minute failed-request recovery, valid empty-discovery profiles, and finished-worker cleanup during party changes.
