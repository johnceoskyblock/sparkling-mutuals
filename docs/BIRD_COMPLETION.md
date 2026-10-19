# Forest bird completion

These rules apply only to Bluebird, Parakeet and Macaw. Other Forest species retain their own capture, source and checking rules. The user clarified this policy on 2026-10-18, including that the one-Macaw Full Clear exception remains.

## Expected population and evidence

- Start with the verified nine-floor-food baseline/slot evidence, then add the common absolute per-player starting-food total learned at HOTSPOT+2s. Local grant witnesses, own echoes, repeats and legacy unused notices overlap by sender/type. An ordinary pickup is part of the floor baseline, regardless of the player’s biome; it is never another starting grant.
- Birdfeeder attraction chat is ignored for spawning, species totals, food use and Macaw pairs. Actual capture/loot-share messages still count catches.
- Observe current rendered birds only while inside the existing bird scan circle: X/Z **6/51**, radius **15 blocks**. Count shared captures plus current rendered birds, never accumulated UUID histories. One Macaw pair accounts for one food opportunity:

  `Bluebird captures + live Bluebirds + Parakeet captures + live Parakeets + floor((Macaw captures + live Macaws) / 2)`

- Keep species population requirements established by qualified renders. Escape/replacement UUIDs do not increase the population when the current live count is unchanged. Once a qualified captures/current-render observation proves spawning with no remaining or held food, retain that proven population through unloading, travel and Forest reentry. Incomplete observations do not accumulate into a complete population. New food blockers still apply; newly reported additional quantities revoke older local population proof and require fresh applicable proof; an older fd target cannot cover larger demand.
- The user reports that Forest food floor drops remain rendered Safari-wide throughout the run. Inspect their markers from every biome and the central boat, independently of ESP selection. Count bird-food groups and untyped/partial markers as blockers; exclude complete active reward triples classified as non-food (including the generic XP/essence/shard tail). Match group members by entity IDs, including triples crossing block boundaries. Any remaining food marker or locally held bird food prevents bird completion. Travel cannot erase a known remaining drop; a subsequent complete zero-food-marker observation can establish collection. Raw Forest marker counts also contain unrelated rewards and must not be used directly.

Missing party reports or incomplete initial source coverage are unknown, not zero. Local completion requires known source/report coverage; outside Forest, an applicable remote fd may provide source/spawning trust, with known held/remaining food still blocking. See [starting synchronization and mixed-client limits](critters/TRACKING.md#early-party-starting-resource-synchronization).

## Completion by mode

| Mode | Inside Forest | Outside Forest |
| --- | --- | --- |
| Sparkling | Qualified captures + current renders reach the expected opportunities, with no remaining floor food or held bird food; retain earned proof after reentry. No bird catch is required. | Retain earned local spawning proof, or trust a received `fd`, subject to known food blockers and newly reported extra demand. Capture totals can establish that extra demand was subsequently satisfied. |
| Unique | The Sparkling spawning requirement plus one shared Bluebird, Parakeet and Macaw catch. If the complete qualified population establishes no Macaws, waive only the Macaw catch. | Retain earned local completion, including proven no-Macaw absence. Without local spawning proof, require `fd` plus each bird's shared catch. If Bluebird and Parakeet are caught and Macaw alone remains among the birds, `fd` may waive that Macaw catch. |
| Full Clear | Actual bird captures must cover expected spawning and known Bluebird/Parakeet population. The first shared Macaw catch supplies the retained Macaw-pair exception, so extra ordinary Macaws may remain. | Use the same capture requirement. `fd` never supplies missing captures or waives extra expected food. |

A Forest report does not waive another Forest species' normal requirements. Confirmed Sparkling bird catches are still real captures, but cannot bypass the shared bird spawning/source requirement. Personal Full Clear PBs still require the corresponding personal capture evidence.

## Sending fd

Only a Forest clearer may automatically send `fd`: they must not already have received bird catches before first entering Forest, and must personally collect at least six Forest food pickups. Visiting another biome first does not disqualify them. A late entrant who already received bird catches before first entering Forest can use valid evidence for progress but cannot report Forest completion. Merely passing through another biome is not evidence that someone else cleared Forest.

The clearer must have personally received **at least six separate Forest bird-food pickup events**, have no bird food in inventory, have no known remaining Forest food markers, and satisfy local bird spawning plus the selected mode's other Forest completion rules. Six items in a single pickup message are not six pickup events. The existing all-food alert still requires three of each food; it is separate from report eligibility.

Check inventory before queuing reports, then revalidate queued `fd` eligibility before dispatch. Owning every bird discovery does not bypass these report restrictions. Local starting food increases expected spawning even if that clearer never sends an unused-food party notice.

## Validation

Regression tests cover ignored attraction chat, qualified versus out-of-radius renders, mixed captures/renders, mid-run arrival, zero-Macaw exceptions, one-Macaw Full Clear credit, remote `fd` trust, incomplete captures after a report, Safari-wide remaining drops, held food, extra-food notice deduplication and the six-pickup report gate. Live Hypixel rendering and reporting still need gameplay verification.

The 2026-10-18 run log confirmed a false blocker: nine personal food pickups, seven Bluebird and three Parakeet captures, zero held food, but eight raw Forest floor markers. All eight remaining groups were classified as generic rewards. Regression coverage now reproduces this case through the floor predictor and verifies all three modes complete, while a remaining bird-food marker still blocks them.

`/sm debug` logs expected opportunities, qualified population, remaining floor markers, held food, local pickup events, the accepted report target and current `fd` eligibility. The `Forest report` line also shows clearer eligibility, supplies/spawn readiness, current biome, discovery readiness and the individual critters still blocking the active mode's local completion.

## Last updated

2026-10-19 (America/New_York) — Early absolute starting-resource reports at HOTSPOT+2s, explicit unknown coverage and local-only later reminders (issue68).

2026-10-18 - Replaced bird spawn-chat accounting with qualified render/capture evidence and restricted automatic Forest reports to qualified initial clearers.
