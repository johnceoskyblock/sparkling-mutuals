# CJ’s Revenge WebSocket automatic entry

## Scope

Local experiment **4.0.2-cjs-revenge.8**, Minecraft **26.1.2 only**, based on the supplied unpublished SM 4.0.2 source. The current WebSocket room’s uncaught Sparkling discoveries can authorize the full approach → Safari Manager interaction → configured ticket selection. The old five-second visible-sighting requirement and registered CJ SMSE1 party-chat exchange are removed. The normal shared detector still publishes observations through the existing shared socket; it is not a direct CJ input fallback.

## Setup

Use the separate CJ’s Revenge Fabric 26.1.2 profile with Java 25, matching Fabric API and Fabric Language Kotlin. Keep only one JAR with mod ID `sparkling-mutuals`; CJ and normal SM cannot coexist in the same mods folder. Existing settings migrate under `config/sparkling-mutuals/` and explicit saved choices remain.

On the joining nonleader, open `/sm` → **Party** → **Party warping**. The last control below the other warping settings is **sparkling auto entry**, with the red description **USE AT YOUR OWN RISK!!!!**. It defaults **off** and controls the complete WebSocket discovery → bounded approach → Manager interaction → configured ticket sequence.

Turn that one switch on for the test. There are no separate CJ discovery or approach-only gates. The existing `safariEntryAutoEnter` saved choice is retained; retired `safariEntryDryRun`/`safariEntryApproach` entries are ignored and disappear on the next actual config save. Existing explicit full-entry choices remain; merely having enabled an older approach test never opts into full entry.

Choose **Preferred ticket** under Safari → Helpers → Safari entry (Basic for an initial test). CJ reads actual menu lore and uses the existing preferred-tier/fallback policy. Unknown availability waits; no usable ticket means no click. The generic **Auto-select Safari ticket** helper remains independent and is not required by this switch.

Keep the existing party leader’s warp/auto-party configuration and joining client’s appropriate invite/leech settings. This increment changes neither timing nor invitations. The supplied source’s fresh warp default remains 28 seconds, with explicit saved timing preserved. Backend-compatible clients in the same canonical Safari lobby and exact party roster must provide discoveries; the joining client receives room state before personal ticket use. No special CJ party-chat commands are needed.

## Behavior and safety

Require a genuine current-leader party-warp notice bound to the next world, fresh Safari location association, verified nonleader roster, pre-admission entrance state and a ready shared socket advertising `room-sparklings-v1`. Welcome or legacy per-sender replay alone cannot authorize entry. Atomic found/caught state is applied before action; caught species stay withdrawn for the visit, including across reconnect. There is no five-second sighting-age limit. The warp notice must bind the next world within ten seconds. Once bound, authorization lasts the same fixed twelve seconds from world arrival as the entry attempt, rather than expiring ten seconds after the earlier notice. Repeated samples, roster loading and reconnect do not renew that deadline.

Keep Minecraft focused, game view grabbed and movement/mouse controls idle. CJ faces one uniquely nameplate-correlated Manager body and uses ordinary crouched walking within its one-block cumulative budget, with early release and stopping allowance. Overlapping players are checked with Minecraft’s actual hard-collision and team-aware push rules, so players on no-collision teams do not falsely block the corridor. Solid entities and players that can push the local player still block it. It releases owned keys, waits for coast to settle, then checks actual native interaction reach, aim and line of sight before one ordinary guarded Manager interaction. It never moves farther or extends reach if the Manager remains inaccessible.

The exact `Critter Safari Entry` menu stays visible. CJ owns the menu/container instance and rechecks current room, context, guard, tier, slot and availability before its one permitted normal selection. Menu replacement, closure/Escape, manual input, focus loss, guard/context/roster changes, last active discovery being caught, socket loss/replacement or timeout cancels remaining input. Cancellation states the specific lost permission (arrival context, entry guard, warp authority, room discovery/connection, Manager, or input/focus/movement). Cancellation cannot retry in the same warp. The generic helper remains suppressed for the claimed arrival, including late/reopened menus and switch-off, so it cannot finish a revoked CJ attempt. Manual guarded interaction remains available.

A sent Manager/ticket input is only an attempt. Exact personal ticket/Manager acceptance or personal Hotspot evidence confirms admission; another player entering does not. An uncertain outcome is not retried. A genuine new verified warp ends the prior arrival’s generic-helper suppression even if Automatic Safari entry is now off; that off choice grants no new CJ input. Reconnect or a generic world replacement does not clear the old suppression. Real gameplay key/button presses latch cancellation even if released between ticks; synthetic binding writes do not count as manual takeover, and vanilla user input is not swallowed.

## Test and diagnostics

Start with one leader and one joiner. Record both views from before the delayed warp through discovery, approach, visible ticket menu and personal admission. Capture the chosen ticket preference and saved warp delay. Run `/sm debug` on both clients before disconnecting, preserving `CJ_ENTRY` lines and relevant shared socket status from `latest.log`. Repeat with two joining players overlapping at spawn and delayed room readiness, then compare both joiners’ approach/admission. Also exercise a capture-before-entry, disconnect and manual cancellation; cancelled attempts should release movement and produce no later automatic ticket click.

Automated tests include real local HTTP/WebSocket fixtures, all 37 upstream species bits, atomic catch withdrawal, reconnect, complete policy/permit dispatch, native geometry, movement/coast ownership and configuration persistence. Offline tests/build/review do not establish live backend availability, accepted Hypixel NPC/menu interactions, server timing or final displacement under latency. Species masks cannot distinguish two Sparkling individuals of the same species or prove current visual presence; using outstanding room discoveries is the human-approved criterion.

This fix does not install, launch/close Minecraft, upload to GitHub or modify the server. The human separately requested sending the verified .8 build and instructions to Johnceo on Discord. Earlier .3–.6 report/dry-run/multiple-toggle instructions are historical; source and delivered packages remain retained. The [approved design](superpowers/specs/2026-11-04-cjs-websocket-entry-design.md) and [plan](superpowers/plans/2026-11-04-cjs-websocket-entry.md) describe the current increment.

Last updated: 2026-11-04 (Asia/Dubai).

Deferred review minor: full-entry terminal attempts still construct native Manager/guard evidence each tick. This can add entity-scanning overhead; it was not profiled.
