# Gimmiegold

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Haunted. **Recorded Full Clear baseline:** 3. Baseline counts are carried from earlier notes, not newly verified server guarantees.

## Evidence

- **Rare: can escape a standard capsule; Masterful catches are guaranteed once hit.** An escaped critter may return with a replacement UUID; that alone does not prove an additional Gimmiegold spawn.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Haunted; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One shared catch; if anyone still needs this sparkling variant, also satisfy the species' Sparkling evidence. The missing-species row may disappear after the catch. |
| Full Clear | At least 3 confirmed catches and no outstanding critters/spawn opportunities; apply the mid-run Haunted rule below. |
| Sparkling | Use all local inventory Shining Coins and establish at least three Gimmiegold spawns from reconciled catch/chat/UUID evidence, including additional known opportunities. The mid-run rule below is an alternative. |

### Confirmed coin evidence versus predicted markers

The supplied live-run log has `starting=0 picked=3 spent=3 held=0 required=3`, one confirmed catch, two current Gimmiegold models, eight retained UUID checks and eleven predicted coin-source markers. It must complete Unique/Sparkling checking. Full Clear remains pending until all three are actually captured.

- The runtime uses the observed **three floor-coin baseline** for this confirmed-collection path: at least three actual coin floor pickups, all acquired plus starting coins confirmed consumed, and a fresh empty coin inventory establish the baseline coin source. Use that authoritative evidence in the qualifying Haunted area even when the visual predictor still reports coin/unknown markers. Neither three observed critters nor an empty inventory alone establishes this path. Starting coins do not replace baseline floor pickups.
- The inspected predictor maps every Haunted group after its five-incense prefix to Coin without an upper bound. Its reported marker count is not a confirmed count of uncollected coins and cannot invent further coin requirements after the actual baseline has been collected/consumed. Preserve the marker-scan recovery path when complete local pickup/use history is unavailable.
- Additional actual pickups, starting coins, held coins and outstanding party resource notices still require their own use/spawn/capture work. Predictions alone do not supply those resources or change their confirmed counts.
- Retained UUID checking is separate from actual spawn/capture accounting. Eight retained UUIDs do not mean eight current Gimmiegolds, eight coin uses or eight required captures. Escapes/replacement models can change UUIDs. Base new coin demand on the prior required amount capped against verified checking, not the unbounded retained UUID count. After a new coin becomes known, require its new qualified UUID check or the full required capture total; surplus old UUID history cannot check that new opportunity.
- Debug output includes `sourcePath=confirmed-coins|marker-scan|pending`, target-capped `accounted`, raw `retainedChecks`, `retainedUuids`, `liveUuids`, and `captures`. Log the predicted marker count separately so its meaning is clear.

## Specific deductions

- Missing coin-use chat must not permanently block completion: with a fresh empty coin inventory, a qualifying source scan showing no remaining coin/unknown markers, and at least the full required population from retained qualified UUID checks plus safely reconciled shared captures, that population can establish the missing spawn evidence. Empty inventory alone remains insufficient. Full Clear uses confirmed captures for its entire required population; live entities cannot become captures.
- Retain a validated Gimmiegold check and verified coin-source absence during ordinary movement. New coin pickups, newly held coins, additional starting coin demand, positive/unknown coin markers when authoritative baseline collection/use is unavailable, and Haunted reentry reopen or revalidate the applicable work. A coin acquired after earlier completion requires another Gimmiegold; do not reuse the old three.
- Remaining-drop evidence uses active, visible STRING item displays, not merely surviving numeric display IDs. Invisible/empty/decorative survivors are not unopened rewards. Associate validated triple members by their group IDs, even when their block positions straddle a boundary; complete incense triples must not masquerade as remaining coins.
- `/sm debug` logs Gimmiegold starting/picked/spent/held coin counts, required and accounted population, completion/source state and remaining marker count. Use this to diagnose any remaining live-server mismatch; the sewer is within the existing Haunted center radius and that radius has not been enlarged for this fix.
- Coin-source scans are independent of incense: a complete currently loaded typed Soothing Incense display group does not count as a remaining Shining Coin marker. Predicted coin groups and unknown/partial/generic markers remain blockers on the marker-scan recovery path, even when hidden by ESP; they do not override fully confirmed baseline coin collection/use. This item-specific presence filter does not fabricate a pickup or coin-use event; the spawn-order typing still needs in-game validation.
- After coin drops are resolved and all acquired/starting coins have spawned their Gimmiegolds, Full Clear turns the capture row green once all required Gimmiegolds are confirmed captured and none remains. Incense drops/ESP must not keep it red. Spawning alone can satisfy Sparkling checking with qualifying current UUIDs, but does not count as a Full Clear capture.
- If anyone needs Sparkling Gimmiegold, collecting **all Shining Coin floor drops** is required. Keep their typed ESP while coin drops remain; reaching the three-spawn/capture minimum alone cannot waive remaining coin pickups. Then use the coins and apply the existing spawn/check evidence rules. This also applies to Unique's added Sparkling requirement.
- Haunted highlights may select predicted Shining Coin/Soothing Incense drops via the [floor-drop predictor](FLOOR_DROPS.md). This does not supply a coin pickup, consumption or spawn; known/unknown drop presence stays independent of visible highlights.
- A Shining Coin may come from a starting grant or a Haunted floor pickup. Count each actual coin once, then its consumption separately.
- The coin-gobbled server message proves one new Gimmiegold spawned. Observe its unique UUID when available; do not count the same spawn again when its entity loads.
- Catches, coin-gobbled spawn messages and UUID scans can establish the required three spawns together. Reconcile them as distinct spawn opportunities: a message, its UUID and its later catch are not three different Gimmiegolds; escape replacement UUIDs add none.
- For the general Sparkling count, retain distinct Gimmiegold UUIDs checked inside its qualifying area throughout the run and reconcile captures without double counting already-seen critters. Coin-use and mid-run rules remain additional/special evidence.
- Unused starting coin: after finishing another biome, alert locally and notify the party if still held outside Haunted and anyone needs Sparkling Gimmiegold. A party notice of an unused coin reopens this source check/Haunted completion despite an earlier empty-biome inference. Suppress when everyone has the discovery. See [reminder rules](TRACKING.md#unused-starting-foodcoin-reminders).
- After that notice, keep Haunted's extra-coin gate pending until the coin is used and an additional Gimmiegold spawn is evidenced by its message or new shared-capture + current-scan accounting. The old three-count total cannot discharge an extra coin. Then recheck the active mode; Full Clear still needs the new critter captured.
- In Unique, when anyone still needs Sparkling Gimmiegold, one shared catch secures the capture requirement but overall completion also needs the Sparkling coin/spawn check. Empty local inventory alone does not establish three spawns.
- Full Clear requires the recorded minimum plus any additional verified spawns, all captured, with remaining coin opportunities resolved. Sparkling requires those spawn opportunities resolved and each resulting critter checked.
- Mid-run entry: no Haunted floor drops + no Gimmiegolds + minimum shared captures met means completed/checked, even without local coin history or sightings of the captured UUIDs.
- With the same empty-world evidence but fewer than the minimum catches, remain pending: the earlier clearer may still hold Shining Coins. Their inventory is inferred, not observed; a partial catch total alone does not complete this case.
- Zero coins held alone proves neither that all coins were collected nor that all spawned critters were captured. Exact spawn: `A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!`.

## Last updated

2026-10-15 - Reproduced the supplied 3-picked/3-spent/11-predicted-marker run; prioritized confirmed baseline coin evidence and separated current, retained and required population diagnostics.
