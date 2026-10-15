# Automatic contest downtime request

**Miria contest → Auto !dt contest** controls an automatic party-chat request. When enabled, the client sends `/pc !dt contest` once as a new Miria contest window starts. This is the literal downtime request, using the same contest schedule as the existing tracker.

- The setting defaults on and is saved in `sparkling-mutuals-contest.properties` as `autoContestDowntime`. Explicit saved off values persist. It is independent of the contest HUD and warning switches.
- Require an online player, SkyBlock detection, and known party membership with more than one member. Do not send to public chat or when solo/party membership is unknown.
- Observe a transition into a different absolute contest window, including across dates. A first observation establishes a baseline. Only a transition observed in the new window's first ten seconds sends the request; do not catch up missed/offline contests.
- Send once per observed transition. Repeated polls, profile/scope changes, joining a party or enabling the switch later in the same window do not send another request. Disabled, non-SkyBlock and solo transitions are skipped.

## Minecraft checks

1. Enable **Auto !dt contest**, stay online in SkyBlock with a party, and cross a contest start. Expect exactly one `!dt contest` in party chat. Continue polling and switch profiles: no duplicate for that window.
2. Turn it off before another start: no message. Reenable mid-contest: no stale message. Restart and verify the switch persists.
3. Repeat solo and outside SkyBlock: no message. Join a party or log in partway through a contest: no catch-up request. Check that disabling only contest HUD/tracking does not disable an enabled automatic request.

## Last updated

2026-10-15 - Added the toggleable automatic !dt contest party request with transition deduplication and no late catch-up.
