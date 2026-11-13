# Sparkle Warping

Warp List discoveries refresh automatically while connected to Hypixel when a player's saved check is at least 24 hours old, including the first login after that interval. One player is fetched at a time with at least two seconds between request starts. This runs independently of Auto-party and the online-status filter. Confirmed acquired types are unchecked; remaining priority groups for that critter are compacted in their existing order with ties preserved. Other selections, player cards and always-invite preferences remain. Failed lookups retain all selections and are attempted again the next day; key/rate-limit failures pause the queue for five minutes. Checkpoints and resolved UUIDs persist in the existing configuration. Already-queued auto-party candidates are rechecked against current selections before inviting.

Sparkle Warping brings leechers into a Safari without using their tickets. Enable `/warping` and choose Solo or Duo in the Sparkle Warping tab. Leechers can stay in SkyBlock; the previous Prototype Lobby workflow is no longer required.

## Starting the first cycle

Run `/start` to enable warping and prepare the current party immediately, even if warping is already on. Turning `/warping` on only enables automation; it does not prepare or disband the party. Only `/start` explicitly prepares it, including a full party. Confirm leadership and resolve every member's IGN before changing the party. Save the leecher list, then disband in Solo, or remove only leechers in Duo while keeping the selected runner. Run `/start` again with the desired roster to replace the saved list. If already solo, the saved list is cleared and no disband is sent. Unknown party/name information waits; nonleaders take no action. Invalid/missing Duo runners leave the party untouched and show a local explanation.

Initial preparation happens only when explicitly running `/start`. `/start` respects the selected Solo/Duo mode, requires leadership (or solo status), and refuses while Leech is enabled. Merely logging in with it already enabled, opening settings, or entering a normal full-party run does not disband anyone. Disabling the feature, changing the warping configuration, switching to Leech or disconnecting cancels unfinished preparation. Once prepared, enter Safari normally; re-invites at 24 seconds and the configured warp deadline use the Safari progress clock.

## Leader cycle

The invite and warp deadlines use the Safari progress HUD clock. A cycle is available only for a confirmed solo player or party leader with fewer than four party members. Unknown party information waits for the existing party lookup. A normal run starting with four players does not start an invite/warp timer. Returning from Safari to Torrhus still prepares that party for the next run. Use `/start` to prepare an existing full party immediately.

1. During the first 24 seconds, optional Auto-party invites saved Warp List players for positively identified visible Sparkling critters. Unknown particle detections do not invite.
2. From 24 seconds, pending Warp List candidates give way to the previous leechers. Invite all missing returning players in one `/p <IGN> <IGN> <IGN>` command, filling only remaining spots up to four members including the leader and Duo runner. This batch does not wait for the Warp List two-second pacing.
3. At the configured delay (28 seconds for fresh installs), show **Warping Now** and send `/p warp` once. Players invited during this window can bring the party to four without cancelling that warp. Once armed, the timer sends the command at the deadline regardless of roster size, refresh state, or Duo membership. Hypixel determines whether the command succeeds.
4. Whenever the leader returns from Safari to Torrhus Canyon or Springs with warping enabled, save the current leechers and prepare the next run. **Solo** sends `/p disband`. **Duo** keeps the selected runner and sends `/p kick <IGN>` for each other member. The leader must still hold leadership; incomplete roster/name information waits rather than guessing. This cleanup also happens after a four-player run that never started the invite/warp timer.

Choose the Duo runner with `/duo <IGN>`; this also selects Duo mode. The same IGN can be edited in the tab. Duo cleanup requires that runner to be in the party. Switch the dropdown to Solo to run alone.

Warp List invitations before 24 seconds are sent one at a time, two seconds apart. Returning leechers are invited together in one command. Joined/current members are skipped. The previous leecher list is session memory, replaced when the next disband/kick cycle has players to remove; an empty cleanup does not erase it. A disconnect clears it. Existing invites follow Hypixel's normal expiry.

## Leech

`/leech` or the Leech toggle enables automatic acceptance of genuine Hypixel party invites **only from IGNs in the Leech whitelist**. Edit the whitelist in the Sparkle Warping tab using spaces, commas or semicolons. Matching is exact and case-insensitive; invalid names and duplicate entries are omitted. The fresh-install whitelist is `JohnCEO JohnFounder _cj Rqach Serkosea johnkuudra`. Existing configs without this setting use `JohnCEO _cj Rqach johnkuudra`; explicit saved lists remain unchanged. An explicitly empty list accepts nobody. An unlisted invite neither leaves the current party nor replaces a pending approved invite. If already in a party, send `/p leave`, wait for leave/roster confirmation, then `/p accept <inviter>`. If solo, accept immediately once party state is known. A newer invite replaces the pending target; turning Leech off, editing the whitelist, disconnecting or a 15-second timeout cancels pending acceptance. Player-chat quotes cannot trigger it.

Leech works in Hypixel lobbies as well as SkyBlock and does not require `/warping` or Auto-party. While Leech is enabled, this client does not run the leader cycle. It defaults off for fresh installs.

## Controls and persistence

| Control | Default | Effect |
| --- | --- | --- |
| `/start` | — | Enable/restart warping and save/prepare the current party, even if already enabled. |
| `/warping` / Auto warping | Off | Toggle invitations, delayed warps and return cleanup without immediately changing the party; use `/start` to prepare it. |
| Leader mode | Solo | Disband on returning from Safari to Torrhus, or keep the Duo runner. |
| `/duo <IGN>` / Duo runner | Empty | Save the runner; the command also chooses Duo. |
| `/autoparty` / Auto-party | On | Prioritize detected critters' Warp List players before 24 seconds. Returning leechers are invited even with this off. |
| `/leech` / Leech | Off | Accept whitelisted party invites, leaving the current party first. |
| Leech whitelist | `JohnCEO JohnFounder _cj Rqach Serkosea johnkuudra` for fresh installs | Approved inviter IGNs; empty accepts nobody. |
| `/warpingdelay <seconds>` / Delay | 28 seconds for fresh installs | Range 1–86400. Existing saved custom delays, including 23 seconds, stay unchanged. The missing-key fallback is 28. |

A delay below 24 seconds ends before returning-leecher invitations can begin. The tab explains the cycle and keeps the normal full-width description text.

Timers bind to the Safari run, world and client connection. Each run arms at most one timer. Disabling warping, changing its delay/mode/runner, enabling Leech, disconnecting, or a stale run/world cancels the warp. Invitations stop on a manual/confirmed party warp. Automatic return cleanup follows Safari-to-Torrhus travel, even if the last run did not start a timer; it does not trigger just on login. Explicit enable preparation remains the exception that can prepare a party immediately in another Hypixel area.

No new ShinyHunter/profile requests are introduced. Party membership uses the existing Hypixel Mod API roster; verified names use loaded profiles, the existing name cache or the existing asynchronous name resolver. Mode, runner, toggles, delay and Warp List persist in the main configuration. Run timers, cleanup queues, leecher lists and pending invites do not persist.

Enabling Warping turns Leech off, and enabling Leech turns Warping off. Auto-party keeps its saved setting and runs only while Warping is enabled.

## Discovery retention and contest pause

Before a managed disband or kick, retain the current party UUIDs and their loaded discoveries. Leech-enabled clients also retain their current party data before separation. Rejoining the same group reuses that data, including confirmed local catch updates, rather than fetching each member again. A newly needed or never-loaded member still needs an initial baseline. Account, API-provider and Alpha/main-realm changes invalidate cached data; disconnect releases the warping reservation. Ordinary party members not reserved by this workflow keep the normal cache policy.

While leading the warping cycle, mode requirements include the actual party plus expected returning members in saved order, up to four total. Thus a leader who already owns Doomspiral still sees Incense ESP if a planned leecher needs it. New actual members occupy slots first, and a full actual party excludes previous leechers who will not fit. A transient unknown roster during disband keeps the last accepted requirements. Cached expected members do **not** receive capture credit until they are actual participants in the run. Mode requirements return to the actual roster when warping is disabled or leadership is lost; Leech clients cache data but do not pretend former members are present.

For the warping leader only, the next run waits until Uncommon is earned in the current Miria contest, independently of the ticket-announcer setting. A new contest resets this eligibility. Return cleanup may still prepare the party, but Safari Manager/ticket entry is held and no new invite/warp timer starts while the goal is missing. An already-running Safari/timer can finish. Normal non-warping play and leechers are unaffected. Existing Shift entry override remains; entering manually while paused does not start a warp timer. Earning the goal releases the entry hold automatically without toggling warping.

## Warp List

The editor shows player cards with per-biome selection flags for the 26 supported early-detection critters. Add/select an IGN, then enable the species that should trigger an invitation. All/None applies to that player. Existing per-critter invitation order is retained; disabling every flag keeps the player card for later editing. Invalid names are omitted; repeated names are invited at most once per window. Disabling Auto-party only stops pre-24-second candidates, not returning leechers.

**Import CSV** adds players and critter invite flags to the Warp List without duplicating names or resetting existing preferences. Row 1 supplies case-insensitive critter headers and only rows 2–11 supply names. Blank or invalid IGN cells and unsupported columns are ignored. CSV quoting and UTF-8 BOMs are supported. Cancelling or a failed import leaves the current list unchanged.

Players with priority 1 are invited first, with UUID order breaking ties. CSV priorities follow each critter column from 1 to 10, including gaps. Importing a saved IGN updates its priority for that critter without creating another player card. Right-click a critter tile to edit that player's priority for that critter. Its current rank appears in the tile's bottom-right corner. Online-only mode checks Hypixel status every two hours using a personal API key, in groups of up to 20 single-player requests. Players with unknown or offline status are skipped unless their **Always invite** override is enabled.

## Validation

Focused tests cover Solo/Duo cleanup selection, missing/invalid runners, leader/capacity gating, the 19-second phase boundary, spaced invitations and deadline expiry, exact multiline invitation parsing, leave-before-accept, cancellation/expiry, and settings persistence. Server-side party permissions, transport and invite acceptance need live Hypixel testing.

## Last updated

2026-11-07 (America/New_York) — Refreshed fresh-install and missing-key Leech whitelist defaults; explicit saved lists stay authoritative.

2026-11-04 — Warp timing uses the Safari progress clock. Warp List invites end and returning-leecher invites begin at 24 seconds, with a 28-second default warp.

2026-11-01 — Player-card Warp List with per-species invite flags and replacement CSV import; existing per-critter order and Auto-party timing preserved. Empty player cards persist.

2026-10-31 — Leech defaults on with JohnCEO, _cj and Rqach whitelisted; existing saved choices remain unchanged.

2026-10-31 — Reserved discovery caches and expected-leecher mode requirements across reparty cycles; warping-specific contest ticket pause; whitelist-only Leech invites.

2026-10-31 — Added `/start` to explicitly save and prepare the current party without toggling warping off first.

2026-10-31 — Returning leechers use a single batch invitation from 19 seconds, and return-to-Torrhus cleanup no longer depends on the previous run having started a timer.

2026-10-31 — Explicit enabling now saves and prepares the current party immediately, resolving the full-party startup deadlock. Normal run eligibility and subsequent cycle timing remain unchanged.

2026-10-30 — Solo/Duo leader cycles, previous-leecher fallback from 19 seconds, 25-second fresh/reset delay, `/duo` and `/leech`. Only parties smaller than four start cycles; filling the party through invites still permits the scheduled warp.

2026-10-29 - Fresh installations seed the maintainer-supplied JohnFounder Warp List (115 ordered name assignments). Existing configuration files keep their saved or empty lists; Auto-party remains off by default.

2026-10-29 — The Sparkle Warping introduction uses standalone full-width wrapped text at normal font size, with no option box or automatic shrinking.

2026-10-29 (America/New_York) — Added Warp List CSV replacement import using the header and ten name rows. Renamed controls to Sparkle Warping, `/warping` and `/warpingdelay`, changed the fresh default to 23 seconds, and added bounded Warp List auto-party invitations.

Contest handling: leader warp entry requires Uncommon, regardless of the ticket-announcer goal. The party contest guard owns the New Contest Started! announcement. Shift still overrides the entry guard.

2026-11-03 — Fresh profiles retain the copied 26-second delay and Auto-party preference, with master warping and Leech off. Existing saved choices remain unchanged.

## Entry and command reliability

Once inside an entered run, a new contest or unearned Uncommon reward does not prevent arming invitations and the returning-player phase. The contest guard before entry remains independent. Early sparkling discoveries are retained for the current run until the invitation cycle starts. All automatic game commands share a minimum 500 ms send interval, including party chat, invites, leave/accept, cleanup, warp, profile confirmation and automated quest replies. Leech acceptance waits at least 500 ms after initiating leave and still requires confirmed departure. Queued commands expire after five seconds and are dropped on connection/run changes; warp/cleanup and leech commands recheck their mode before sending.
