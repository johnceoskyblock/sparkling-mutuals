# Automatic contest downtime request

**Miria contest → Auto !dt contest** controls an automatic party-chat request. When enabled, the client sends `/pc !dt contest` once as a new Miria contest window starts. This is the literal downtime request, using the same contest schedule as the existing tracker.

- The setting defaults on and is saved in `sparkling-mutuals-contest.properties` as `autoContestDowntime`. Explicit saved off values persist. It is independent of the contest HUD and warning switches.
- Require an online player, SkyBlock detection, and known party membership with more than one member. Do not send to public chat or when solo/party membership is unknown.
- Observe a transition into a different absolute contest window, including across dates. A first observation establishes a baseline. Only a transition observed in the new window's first ten seconds sends the request; do not catch up missed/offline contests.
- Send once per observed transition. Repeated polls, profile/scope changes, joining a party or enabling the switch later in the same window do not send another request. Disabled, non-SkyBlock and solo transitions are skipped.

## Ticket earned and entry release

A current Miria scoreboard tier of **Uncommon or higher** establishes the earned contest ticket. Retain the existing first-ten-seconds guard against a stale tier at a window rollover. Tier detection continues even with contest HUD/tracking, hive beacons or automatic contest requests disabled.

On the first confirmed earned tier, remove the local player's own `!dt contest` request immediately, whether it was waiting for the current run to end or already blocking Safari entry. With known party membership, send the exact party message **Ticket earned!** once per player and absolute contest window. Repeated scans, higher tiers and profile refreshes in the same window cannot send it again. A new contest window allows a new announcement.

On receiving a verified current party member's **Ticket earned!** message, remove **only that sender's outstanding downtime request, for any reason**. Standalone `r` does the same. Other requesters remain held. Local scoreboard detection still resolves only the local player's applicable contest request. Readiness holds the Torrhus entry stage; Safari waiting uses separate kick/presence protection. Neither ready message clears those protections, and Shift/sneak cannot bypass entry. Guild/private/public/quoted messages cannot release a party hold. See [entry guard](SAFARI_ENTRY_GUARD.md) for identity, transfer, setting and recovery rules.

The **Auto !dt contest** toggle controls the automatic start request, not whether an earned ticket resolves an existing manual contest request. It does not suppress the requested Ticket earned! announcement.

## Minecraft checks

1. Enable **Auto !dt contest**, stay online in SkyBlock with a party, and cross a contest start. Expect exactly one `!dt contest` in party chat. Continue polling and switch profiles: no duplicate for that window.
2. Turn it off before another start: no message. Reenable mid-contest: no stale message. Restart and verify the switch persists.
3. Repeat solo and outside SkyBlock: no message. Join a party or log in partway through a contest: no catch-up request. Check that disabling only contest HUD/tracking does not disable an enabled automatic request.

4. Request `!dt contest` and earn Uncommon: expect one **Ticket earned!** and immediate release of your own entry hold. Repeat with a banked in-run request, then with a hold already active outside Safari.
5. Have two party members waiting for contest tickets and a third waiting for another reason. One member earning a ticket removes only their own contest request. Confirm the final contest hold releases only after the other ticket is earned, while another requester remains held until their own `r` or `Ticket earned!`.
6. Keep the Uncommon row visible, advance to Rare, toggle HUD/tracking or refresh the profile: no duplicate announcement for the same player/window. At the next contest, earn the ticket again and expect one new message.

## Last updated

2026-10-16 - Ticket earned! releases the sender's downtime for every reason; documented stage-specific entry holds and removed the sneak bypass.
