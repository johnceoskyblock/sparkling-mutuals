# Automatic Bazaar profile confirmation repair

## Scope and evidence

Repair the approved Bazaar guidance feature locally on Minecraft 26.1.2. The human reported Bluebird HOLD without an outline and asked why confirmation was manual and whether the repair was underway. The live session has guidance enabled, the exact delivered JAR, supported Co-op Bazaar Orders fields, and no old Profile ID line. Running /profileid produces exact system labels CLICK THIS TO SUGGEST IT IN CHAT [DASHES] and [NO DASHES]. The existing parser only reads the obsolete plain line.

Keep existing owner, top-ten-or-owned eligibility, confirmed advice, pricing, profile separation, alerts and UI rules. No GitHub writes, installation, game closure, automatic trading or unrelated Honeyhive edits. Current isolated worktree and Native execution retained.

## Approach

Automatically request the server's current profile while guidance is enabled in SkyBlock and its profile is unknown. Prefer the fresh server response over the shared profile API, whose documented one-minute cache can lag a profile switch. Read the UUID from a SuggestCommand click event only on the two exact system response labels, correlated with a pending request; continue accepting the existing exact plain line. Never execute a returned click command or send a UUID as chat. Delay initial requests one second, bound pending replies to fifteen seconds, retry at most three times at thirty-second intervals, retain a global five-second spacing across resets, and drain outstanding old replies before requesting again after profile/context changes. Leaving SkyBlock, disabling, disconnecting and profile changes cancel pending confirmation.

## Task 1: Reproduce, repair and verify

- [ ] Add failing component and request-lifecycle tests before implementation. Cover legacy response, inherited/split click style, malformed/conflicting IDs, wrong label/action/player text, request timeout, retries, disable, clock rollback, reset and delayed replies.
- [ ] Add narrow component parsing and a small pure request lifecycle; wire automatic query and response handling into existing ShardSaleGuidance tick/chat/reset paths. No new workers or web API calls.
- [ ] Run full 26.1.2 tests and verifyTargetJar; update concise feature documentation.
- [ ] Extend the controlled native fixture through the real Fabric game-message listener and current request lifecycle, retain existing69 render/ownership/alert checks. No live-game inputs.
- [ ] Obtain one independent final review, TDD-fix Important/Critical findings, then package and hash/class-verify a local JAR. Retain branch/worktree and preserve the user's running game.

## Review focus

Cancellation/profile changes must not reuse an old pending UUID; retry bounds survive repeated ticks and clock rollback. Click UUIDs are data, never commands to execute. Wrong actions, labels and conflicting identities remain unknown. Guidance-disabled/outside-SkyBlock contexts issue no queries. The real runtime must use the new parser rather than only unit tests.
