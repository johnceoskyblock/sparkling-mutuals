# Safari API lookups and recovery

## Discovery data

Sparkling-mode progress and discovery-dependent helpers use the selected party's shared Sparkling discoveries. Each member must have a successful lookup before that party state is ready. A complete party containing a new player is valid data: an absent/null `safari` object or absent/null `discovered_sparkling_critters` list on an otherwise valid selected profile member means **zero Sparkling discoveries**, so every species still needs checking for that player. `/sparkling` uses the same parsing and shows zero captured Sparklings when there are no discoveries and no supplied total.

Party discovery lookups also treat the specific `SkyBlock profile or Safari data unavailable` result (`DATA_UNAVAILABLE`) as zero discoveries, including absent selected profiles/members. This lets the party state become ready with every species needed for that member. Each mod user independently sends their own qualifying unused starting food/coin quantities; another mod user's report does not suppress that client's report. The successful empty discovery result uses the normal provider-scoped cache.

HTTP errors, invalid credentials, rate limits and malformed discovery objects/lists remain failures and retry normally. Malformed payloads use a separate failure kind so they cannot accidentally trigger the no-data fallback. One-shot profile cards and personal-stat reads retain their absent-profile error behavior; the fallback above applies to shared discovery lookup requirements. Unrelated absent stats, such as Hunting XP or essence, retain their existing unknown presentation.

## Retry behavior

An active background party-discovery lookup retries **60 seconds after a failure**, without switching modes. Failed API-backed party-command requests use the same one-minute retry window while their party/mode/run context remains valid. Personal Safari stats also retry after a minute on failure, retaining their normal ten-minute refresh after successful reads. Requests run off the client thread. Missing credentials with the cloud disabled still require configuring a usable provider.

A repeated failure schedules the next attempt one minute later. Changing the provider/key can wake a failed request immediately. Existing per-provider successful discovery caching lasts 60 seconds; failed calls are not cached as successes. A successful complete lookup ends the failed-lookup retry cycle. Repeated identical discovery warnings remain suppressed.

The Sparkling-mode worker releases its busy flag before rejecting a result for temporarily unavailable/different party membership. Rejected or incomplete results schedule recovery; they cannot leave progress permanently displaying `?` because a finished worker remains marked busy. Generation checks protect newer lookups from older callbacks. Unknown current party membership still cannot be treated as solo or as successful data.

One-shot `/sparkling` card requests keep their existing explicit-command behavior; this retry policy describes maintained background state and still-valid pending party commands.

## Validation

Focused automated checks cover the exact 60-second boundary and repeated failures, provider changes, empty discovery fields feeding a ready party state, malformed responses remaining failures, and existing mode/data behavior. Live API failure recovery and party-roster timing remain Minecraft checks.

Test in Sparkling mode without toggling modes: let a request fail, restore API access, and wait for the next one-minute retry. Test a party member with no Sparkling history or unavailable profile data: progress should leave `?` after all members load, all species should need Sparkling checks for that member, and every mod user holding starting food/coins should send their own combined report. Malformed payloads and transport errors should remain failures. Confirm a new party or provider cannot accept an earlier lookup's result.

## Last updated

2026-10-19 - Extended zero-discovery party requirements to unavailable profile/member data and verified independent starting-drop reports from multiple mod users.

2026-10-19 - Added one-minute failed-request recovery, valid empty-discovery profiles, and finished-worker cleanup during party changes.
