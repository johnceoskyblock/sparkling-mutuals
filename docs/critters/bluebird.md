# Bluebird

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Forest. **Recorded Full Clear baseline:** Variable; determine from actual spawns. Baseline counts are carried from earlier notes, not newly verified server guarantees.

## Evidence

- **Uncommon: guaranteed catch; no capsule escape.** A UUID identifies an observed entity; repeated sightings do not add another Bluebird.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Forest; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One confirmed shared catch immediately completes Unique and removes the missing row, without a local Sparkling scan/radius requirement. Detected uncaptured Sparklings still block completion. |
| Full Clear | Local capture/source proof and a qualified empty scan advance progress and remove the missing row without a report; retain that proof after leaving. Other biomes without local proof use party report plus minimum captures, with existing fixed-count exceptions. Positive remaining critters reopen local completion. See the [per-critter panel reference](../MISSING_PANEL.md). |
| Sparkling | Resolve all required feeder events with no raw Forest drops or held bird food; ordinary captures are not required; use food evidence and current bird counts without distinct UUID checks. |

## Specific deductions

- Local Full Clear checks advance progress and remove the missing row without a report. Remote progress without local proof retains its report gate and existing fixed-count exceptions.

- Food use requires at least nine feeding equivalents plus known starting/extra food, no raw Forest floor drops and no bird food held. Account for birds with shared captures plus currently scanned nearby birds (two Macaws equal one food), using feeder announcements as overlapping evidence rather than adding them again. Never accumulate distinct bird UUIDs or past scans. Unknown scans/inventory stay pending. Recheck floor drops on Forest reentry.

- Forest highlights may select predicted Wriggleworm/Yogi Berry/Bag of Seeds drops via the [floor-drop predictor](FLOOR_DROPS.md). A prediction is not food collected or used; all shared-feeder checks remain unchanged.
- After a party unused-food notice, keep Forest's extra-food gate pending until that food is used and additional birds are evidenced by new feeder announcements or an increase in shared captures plus current nearby bird counts. Reusing the previous bird total is insufficient; Full Clear also requires the new birds captured.
- If any bird discovery is needed, a player finishing another biome with unused starting bird food outside Forest receives a local alert and sends a party notice. That notice reopens the relevant bird/feeding checks; no reminders if all bird discoveries are owned. See [starting-food reminders](TRACKING.md#unused-starting-foodcoin-reminders).
- Partial-clear entry retains received spawn/capture chat. Full Clear still needs confirmed species captures and no live birds of this species; shared captures plus current nearby bird counts can recover missed feeding evidence without distinct UUIDs.
- A matching birdfeeder announcement proves a spawn and a food use. Record its species and stated quantity; Two Macaws means two critters but one feeding.
- Count personal starting food + Forest pickups as minimum known food. Teammates may collect food or stack their own starting food in the feeder; accept announcements beyond the personal food total.
- Distinct bird UUID histories are not completion evidence; shared catches plus current nearby bird counts account for spawning. Ordinary live-species presence still blocks Full Clear for Bluebird and Parakeet, independently of food accounting.
- Species spawn totals come from exact feeder announcements. Each announcement uses one food; a Macaw announcement spawns two birds. Capture totals remain separate. UUID replacements cannot add food use or spawn counts.
- Capture-GUI color: keep this row red while known food remains unused/queued. Once food use is resolved, turn it green if no live Bluebird remains in a qualifying scan; other bird species need not be complete. Reevaluate on new food/spawns. See [shared feeder rules](TRACKING.md#forest-birds-shared-feeder-independent-completion).

## Last updated

2026-10-17 - Local Full Clear completion also advances progress; other biomes without local proof retain report requirements.

2026-10-17 - Separated local Full Clear missing-panel proof from report-based progress.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-16 - Bird completion uses shared captures plus current nearby bird counts, feeder evidence, ground drops and inventory; no distinct UUID accounting.

2026-10-15 - Added mid-run Forest feeding recovery; retained independent species completion and the one-catch Macaw exception.
