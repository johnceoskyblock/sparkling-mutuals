# Miria contest and downtime

## Purpose

Track Miria's contest window, warn while incomplete, and coordinate party readiness before the next Safari entry. [Safari entry guard](SAFARI_ENTRY_GUARD.md) owns interaction interception and party-presence checks; this page owns contest requests and downtime release.

## Controls and defaults

| Control in `/sm` settings | Default | Effect |
| --- | --- | --- |
| **Miria contest → Contest HUD and tracking** | On | Enables contest HUD/tracking presentation and warning processing. |
| **Auto !dt contest** | On | Allows one automatic leader request at a newly observed contest start. |
| **5 / 3 / 1 minute warning** | All on | Selects warning thresholds while below Uncommon. |
| Warning title/sound | On; `minecraft:block.bell.use`, volume 100 | Local incomplete-contest warning. |
| **Party commands → !dt downtime** | On | Records readiness requests and holds Torrhus entry. |

Contest controls persist in `config/sparkling-mutuals-contest.properties`; downtime and [Honeyhive beacons](HONEYHIVES.md) use the general configuration. Ticket detection continues independently of HUD/tracking, warnings, beacons and automatic requests. **Hold either physical Shift key** to bypass the current entry interaction without clearing requests.

## Implemented behavior

### Schedule, warnings and automatic requests

Every ten client ticks, the tracker uses the computer's local clock: twenty-minute windows offset to minute 15, with a thirty-second end gap. Absolute window keys distinguish dates. This is the implemented schedule, not an independently synchronized server clock. Warning thresholds are only 5, 3 and 1 minutes. Repeated scans warn once per selected threshold; joining late coalesces elapsed thresholds into one warning.

The automatic request sends literal `/pc !dt contest` only when a different window is observed in its first ten seconds, with a loaded player/world, SkyBlock detection, a known party of more than one member and a server-confirmed local Party Leader role. The first observation establishes a baseline. Disabled, solo, unknown-leader, member/moderator and non-SkyBlock transitions are consumed without sending; enabling/joining/becoming leader mid-window does not catch up.

### Requests and readiness

Any verified current party member may send `!dt [reason]`. Requests during an active run are banked; outside a run they hold the next Torrhus entry immediately. A verified leader's exact `!dt contest` creates separate contest holds for every current party member, including self. Other members' requests remain individual, and preexisting other-person reasons are retained.

Each sender releases only their own outstanding request by a party message containing `ticket` (case-insensitive, for any reason), or an accepted affirmative readiness message. Accepted examples: `r`, `rdy`, `ready`, `I'm ready`, `ready to go`, `ready when you are`, punctuation/case variants, short r-spam and a single adjacent-key typo such as `r/`, `rrr`, `rt` or `er`. Questions, `nr`, `not ready`, `almost ready`, `I will be ready`, `party`, `red` and `brb` do not count. The same parser is used while identity verification is pending.

A current Miria scoreboard tier of **Uncommon or higher**, observed at least ten seconds into the window, establishes local contest completion. This immediately clears only the local player's **contest** request. With known party membership of more than one, it also sends exact party text **Ticket earned!**, once per player/window. Receiving that verified party text can release its sender's hold for any reason. Therefore local scoreboard detection and receiving a ticket-containing party message have different scopes. Higher tiers and profile refreshes cannot repeat the announcement in that window.

### Combined reminder

Contest-only readiness also has a travel fallback: after the exact server notice ` » SharkboardMC is traveling to Torrhus Canyon FOLLOW`, clear that player's `contest` hold **20 seconds after the first matching notice**. Each member has their own timer; duplicate notices do not extend it. Only outstanding contest holds can expire this way. Quoted party/public messages, other destinations and unrelated players cannot release another member's request. Existing ready/ticket messages can still clear a hold earlier. Non-contest reasons continue waiting for readiness. Transfers preserve the timer for the same party; party reset/disable clears it, and a new contest request discards previous-trip deadlines. This changes downtime only, not the independent Safari presence/kick protection.

A server-confirmed `SkyBlock Party Warp (N players)` while the local player is party leader in Torrhus also starts the twenty-second timer for every other outstanding contest requester at once. Per-member FOLLOW notices are not required for that warp path. The leader's own hold still releases through their ticket/readiness evidence. A quoted header, a non-leader client, or a warp in Safari/another location cannot start these timers. Repeated confirmations do not extend an already-running timer. Earlier readiness/ticket messages still release individual members immediately; food and other manual reasons are not timed out. UUID-to-name resolution retains deadlines.

Entry-block messages list each player with their reason, such as `Alex (contest), Steve (food)`. Contest-only local notices and completion messages say **Contest**, rather than labeling the request as generic downtime. Manual downtime keeps its own label.

On returning to Torrhus, clients coordinate one party message naming all outstanding requesters and reasons, even if the reporting client requested no downtime: `Downtime requested: Alex (food); Steve (contest); Bob (no reason)`.

Reports wait for the destination, stagger by party UUID order and suppress a matching report already heard, including one heard inside Safari. Resume/departure/ticket changes update pending content. Long reasons share the available message budget and use `...` truncation to preserve names. Local notices list holders and explain the Shift override. A ready message does not clear unrelated missing-member or kick guards.

## Lifecycle and persistence

Settings persist; requests, warning state and automatic-request observation state are in memory. Same-party transfers retain requests and activate banked holds; the party inbox discards old-instance kick context. Confirmed departures prune only that member; party replacement, disband or disabling downtime clears requests. Unknown member names remain held by UUID until resolved.

Profile scope changes reset contest completion, tier, points and warnings, but do not reset the automatic transition observer or per-player/window announcement deduplication. A new window clears completion/warning state and permits a new ticket announcement. The announcement state is not disk-backed across full client restarts.

## Edge cases

- The start request and ticket announcement do not require **Respond to party commands**; downtime's own switch controls whether the echoed request becomes a hold.
- A ticket-containing message releases only its verified sender, even if another player's name is quoted in it. Guild/private/public text cannot release holds.
- Disabling HUD/tracking does not stop local earned-ticket detection or automatic requests.
- Known party roles and identity matter; current tab presence is a separate [Safari-stage guard](SAFARI_ENTRY_GUARD.md#presence-and-kick-recovery).

## Source and validation references

- Tick registration: [SparklingMutuals](../src/main/kotlin/net/johnceo/sparklingmutuals/SparklingMutuals.kt). Schedule, sidebar and output: [ContestTracker](../src/main/kotlin/net/johnceo/sparklingmutuals/contest/ContestTracker.kt), [MiriaContestFilter](../src/main/kotlin/net/johnceo/sparklingmutuals/contest/MiriaContestFilter.kt), [ContestDowntimeState](../src/main/kotlin/net/johnceo/sparklingmutuals/contest/ContestDowntimeState.kt), [ContestTicketState](../src/main/kotlin/net/johnceo/sparklingmutuals/contest/ContestTicketState.kt).
- Requests/releases and reports: [SafariDowntime](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/automation/SafariDowntime.kt), [SafariPartyInput](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/automation/SafariPartyInput.kt), [DowntimeAnnouncement](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/automation/DowntimeAnnouncement.kt); settings: [ContestConfig](../src/main/kotlin/net/johnceo/sparklingmutuals/config/ContestConfig.kt).
- Existing secondary coverage: [ContestTicketAndAutoWarpTest](../src/test/kotlin/net/johnceo/sparklingmutuals/contest/ContestTicketAndAutoWarpTest.kt), [SafariDowntimeTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/automation/SafariDowntimeTest.kt), [DowntimeAnnouncementTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/automation/DowntimeAnnouncementTest.kt). No tests or live session ran in this audit. Live checks should cross starts with each role/setting, earn tiers across rollovers, exercise multiple independent holders, transfers, staggered reports and same-window profile refreshes.

## Last updated

2026-10-26 — Confirmed leader party warps in Torrhus start all other contest timers together; readiness remains an early release and local guard messages show contest-specific reasons.

2026-10-20 - Added per-member twenty-second contest-hold release from Torrhus server travel notices, allowing non-mod party members to resume without readiness chat.

2026-10-19 (America/New_York) — Audited schedule, request gates, ticket/readiness release and lifecycle against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`. See [documentation conventions](DOCUMENTATION_GUIDE.md).
