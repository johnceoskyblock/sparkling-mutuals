# Miria contest and downtime

## Purpose

Track Miria's contest window, warn while incomplete, and coordinate party readiness before the next Safari entry. [Safari entry guard](SAFARI_ENTRY_GUARD.md) owns interaction interception and party-presence checks; this page owns contest requests and downtime release.

## Controls and defaults

| Control in `/sm` settings | Default | Effect |
| --- | --- | --- |
| **Miria contest → Contest tracking** | On | Enables contest tracking and warnings. |
| **HUDs → HUDs → Safari contest timer** | On | Shows the timer and score without affecting tracking or ticket logic. |
| **Party Contest Guard Entry** | On | Holds Torrhus entry while the leader waits for ticket confirmations. |
| **5 / 3 / 1 minute warning** | All on | Selects warning thresholds while below Uncommon. |
| Warning title/sound | On; `minecraft:block.bell.use`, volume 100 | Local incomplete-contest warning. |
| **Party Interactions → Respect !dt requests** | On | Records readiness requests and holds Torrhus entry. |

Contest controls persist in `config/sparkling-mutuals-contest.properties`; downtime and [Honeyhive beacons](HONEYHIVES.md) use the general configuration. Ticket detection continues independently of HUD/tracking, warnings, beacons and automatic requests. **Hold either physical Shift key** to bypass the current entry interaction without clearing requests.

## Implemented behavior

### Schedule, warnings and automatic requests

Every ten client ticks, the tracker uses the computer's local clock: twenty-minute windows offset to minute 15, with a thirty-second end gap. Absolute window keys distinguish dates. This is the implemented schedule, not an independently synchronized server clock. Warning thresholds are only 5, 3 and 1 minutes. Repeated scans warn once per selected threshold; joining late coalesces elapsed thresholds into one warning.

The leader announces `New Contest Started!` only when a different window is observed in its first ten seconds, with a loaded player/world, SkyBlock detection, a known party of more than one member and a server-confirmed local Party Leader role. The first observation establishes a baseline. Disabled, solo, unknown-leader, member/moderator and non-SkyBlock transitions are consumed without sending; enabling/joining/becoming leader mid-window does not catch up.

The guard waits for the other players on the roster at announcement time. The 30-second timer starts in Torrhus while the local player is leader. After an unrecognized return from an active Safari, it waits until a teammate is on the same server. It sends one reminder naming up to three teammates still waiting. Shift-clicking a Safari entry interaction clears this guard for the current contest. Each verified party member clears their own hold with readiness or ticket chat. Departures remove that member from the wait. The legacy manual downtime flow below is separate.

### Requests and readiness

Any verified current party member may send `!dt [reason]`. Requests during an active run are banked; outside a run they hold the next Torrhus entry immediately. A verified leader's exact `!dt contest` creates separate contest holds for every current party member, including self. Other members' requests remain individual, and preexisting other-person reasons are retained.

Each sender releases only their own outstanding request by a party message containing `ticket` (case-insensitive, for any reason), or an accepted affirmative readiness message. Accepted examples: `r`, `rdy`, `ready`, `I'm ready`, `ready to go`, `ready when you are`, punctuation/case variants, short r-spam and a single adjacent-key typo such as `r/`, `rrr`, `rt` or `er`. Questions, `nr`, `not ready`, `almost ready`, `I will be ready`, `party`, `red` and `brb` do not count. The same parser is used while identity verification is pending.

Under Miria Contest, **Ticket mode** selects Basic (default: Uncommon or higher, announcing **Ticket earned!**) or First-Class (Legendary / 5,000 points, announcing **First-Class Ticket earned!**). The announcer is always enabled, including with the contest HUD disabled. Current Miria scoreboard evidence at least ten seconds into the window establishes the selected target and clears the local contest hold. An already-earned tier on the first score read within ten seconds of arriving in Torrhus restores completion silently. Announcements are deduplicated per player/window/target: changing to First-Class can announce the higher reward, while changing back cannot repeat an already announced Basic reward. Contest warnings and hive completion visibility follow the selected target. Received ticket-containing party messages retain their existing readiness behavior. The selector persists in the contest config and is independent of the automatic Safari entry ticket preference.

### Combined reminder

Contest-only readiness also has a travel fallback: after the exact server notice ` » SharkboardMC is traveling to Torrhus Canyon FOLLOW`, clear that player's `contest` hold **20 seconds after the first matching notice**. Each member has their own timer; duplicate notices do not extend it. Only outstanding contest holds can expire this way. Quoted party/public messages, other destinations and unrelated players cannot release another member's request. Existing ready/ticket messages can still clear a hold earlier. Non-contest reasons continue waiting for readiness. Transfers preserve the timer for the same party; party reset/disable clears it, and a new contest request discards previous-trip deadlines. This changes downtime only, not the independent Safari presence/kick protection.

A server-confirmed `SkyBlock Party Warp (N players)` while the local player is party leader in Torrhus also starts the twenty-second timer for every other outstanding contest requester at once. Per-member FOLLOW notices are not required for that warp path. The leader's own hold still releases through their ticket/readiness evidence. A quoted header, a non-leader client, or a warp in Safari/another location cannot start these timers. Repeated confirmations do not extend an already-running timer. Earlier readiness/ticket messages still release individual members immediately; food and other manual reasons are not timed out. UUID-to-name resolution retains deadlines.

Entry-block messages list each player with their reason, such as `Alex (contest), Steve (food)`. No unsolicited local notices accompany these changes; holds and release rules remain unchanged.

With an outstanding contest request, queue `[Reminder] Everyone get your ticket!`. Only the current party leader sends it, after Torrhus Canyon is confirmed. A request made while already in Torrhus is eligible immediately. Send once per request cycle; readiness/ticket resolution cancels an unsent reminder. Regular downtime and named request details remain local and no longer produce verbose party summaries. Entry holds, Shift overrides and release timers are unchanged.

## Chat volume

The automatic contest guard sends one start message and at most one follow-up. Manual contest requests retain their separate Torrhus reminder. Routine local bookkeeping notices have been removed. Contest warnings retain their existing title/sound controls; the automatic warning chat line is also removed. No new API requests are introduced.

## Lifecycle and persistence

Settings persist; requests, warning state and automatic-request observation state are in memory. Same-party transfers retain requests and activate banked holds; the party inbox discards old-instance kick context. Confirmed departures prune only that member; party replacement, disband or disabling downtime clears requests. Unknown member names remain held by UUID until resolved.

Contest completion, tier, score and warnings are retained in memory per account/profile and absolute contest window. A temporary missing scope during a kick/disconnect hides the current state without deleting it; returning to the same scope restores it. Another account/profile starts with its own evidence. Lower reconnect scoreboard placeholders cannot reduce the retained score. A new contest window invalidates old progress. Automatic-request observation and announcement deduplication still survive reconnects, preventing a second ticket message. These states are not disk-backed across full client restarts.

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

2026-11-04 — Replaced the automatic downtime request with leader-owned ticket confirmations.

2026-11-01 — Independent contest HUD visibility; short leader-only ticket reminders replace verbose party summaries.


2026-10-29 — Renamed the receiver toggle to Respect !dt requests and removed unsolicited local downtime bookkeeping messages without changing requests, readiness, timers or party announcements.

2026-10-27 — Retain same-window contest progress across kicks, disconnects and temporary profile loss; isolate accounts/profiles and reject lower scoreboard placeholders after reconnect.

2026-10-27 — Added always-enabled Basic / First-Class contest ticket announcement targets and persisted Miria Contest selector.

2026-10-26 — Confirmed leader party warps in Torrhus start all other contest timers together; readiness remains an early release and local guard messages show contest-specific reasons.

2026-10-20 - Added per-member twenty-second contest-hold release from Torrhus server travel notices, allowing non-mod party members to resume without readiness chat.

2026-10-19 (America/New_York) — Audited schedule, request gates, ticket/readiness release and lifecycle against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`. See [documentation conventions](DOCUMENTATION_GUIDE.md).
