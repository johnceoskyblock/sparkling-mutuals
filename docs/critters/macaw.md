# Macaw

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Forest. **Recorded Full Clear baseline:** Variable; determine from actual spawns. Baseline counts are carried from earlier notes, not newly verified server guarantees.

## Evidence

- **Legendary: can escape a standard capsule; Masterful catches are guaranteed once hit.** An escaped critter may return with a replacement UUID; that alone does not prove an additional Macaw spawn.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Forest; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One confirmed shared catch immediately completes Unique and removes the missing row, without a local Sparkling scan/radius requirement. Detected uncaptured Sparklings still block completion. |
| Unique + Sparkling check | The same first-shared-catch rule applies; party-needed discoveries do not add a local food/radius gate. The no-Macaw exception below remains available. |
| Full Clear | Local resolved-food proof plus one shared catch advances progress without a report; remote progress accepts a report plus one shared catch even while ordinary Macaws remain. The missing row requires locally resolved food plus that one catch, or the proven no-Macaw case. Pending Sparklings and food notices block completion. See the [panel reference](../MISSING_PANEL.md). |
| Sparkling | Resolve all required feeder events with no raw Forest drops or held bird food; ordinary captures are not required; use food evidence and current bird counts without distinct UUID checks. |

## Specific deductions

- Local Full Clear capture/source and empty-scan proof advances progress and removes the missing row without a report. Remote progress without local proof uses report rules.

- Food use requires at least nine feeding equivalents plus known starting/extra food, no raw Forest floor drops and no bird food held. Account for birds with shared captures plus currently scanned nearby birds (two Macaws equal one food), using feeder announcements as overlapping evidence rather than adding them again. Never accumulate distinct bird UUIDs or past scans. Unknown scans/inventory stay pending. Recheck floor drops on Forest reentry.

- Forest highlights may select predicted Wriggleworm/Yogi Berry/Bag of Seeds drops via the [floor-drop predictor](FLOOR_DROPS.md). A prediction is not food collected or used; all shared-feeder checks remain unchanged.
- After a party unused-food notice, keep Forest's extra-food gate pending until that food is used and additional birds are evidenced by new feeder announcements or an increase in shared captures plus current nearby bird counts. Reusing the previous bird total is insufficient. Full Clear must resolve the additional food/spawn evidence; its one-Macaw capture exception still applies.
- If any bird discovery is needed, a player finishing another biome with unused starting bird food outside Forest receives a local alert and sends a party notice. That notice reopens the relevant bird/feeding checks; no reminders if all bird discoveries are owned. See [starting-food reminders](TRACKING.md#unused-starting-foodcoin-reminders).
- Full Clear exception: one own capture or party loot share is enough after all known starting/floor/party-reported food has been used. Remaining Macaws do not block Forest completion or its Full Clear message. A personal Full Clear PB still needs a personal Macaw catch. Reopen feeding work for newly known unused food, then apply the same one-catch exception after it is used.
- A matching birdfeeder announcement proves a spawn and a food use. Record its species and stated quantity; Two Macaws means two critters but one feeding.
- One shared Macaw capture plus one currently scanned Macaw accounts for one food: `(1 + 1) / 2 = 1`. Add captured and currently scanned Macaws before dividing; do not round an incomplete pair up or add the overlapping spawn announcement again.
- Count personal starting food + Forest pickups as minimum known food. Teammates may collect food or stack their own starting food in the feeder; accept announcements beyond the personal food total.
- Distinct bird UUID histories are not completion evidence; shared catches plus current nearby bird counts account for spawning. Ordinary live-species presence still blocks Full Clear for Bluebird and Parakeet, independently of food accounting.
- Species spawn totals come from exact feeder announcements. Each announcement uses one food; a Macaw announcement spawns two birds. Capture totals remain separate. UUID replacements cannot add food use or spawn counts.
- Full Clear panels: keep Macaw red/missing while food remains unresolved, or when Macaws spawned but none was caught. Locally resolved food plus one shared Macaw catch turns its capture row green, removes the missing row and advances progress without a report even if other Macaws remain. Without local proof, a report plus one real catch can advance remote progress, unless known unused food or delivery contradicts it. When none spawned or were seen, locally resolved food while in Forest gives the same panel result with zero catches. Other Forest requirements still apply.
- A positively detected Sparkling Macaw remains a capture obligation in every mode; resolved food and ordinary catches cannot clear it. Once all detected Sparkling Macaws are confirmed caught, Unique and Sparkling immediately complete Macaw and remove its missing row, even if food work remains. Full Clear still needs resolved food and its normal one-Macaw capture exception; clearing the Sparkling obligation must not leave it stuck incomplete. Ordinary ESP remains independent: Full Clear can outline remaining Macaws after completion; Unique stops ordinary first-catch ESP after one shared catch; Sparkling uses the shared ten-second-per-UUID display timer, which never contributes spawning/completion evidence.
- No-Macaw evidence needs every feeding opportunity resolved with no Macaw spawn/sighting and no known held food or pending extra-food notice. While in Forest, this completes Macaw in all modes and removes it from the missing panel, including Full Clear progress/capture panels and Unique with a needed Sparkling check, without adding shared/personal catches. New food or a Macaw spawn/sighting reopens the requirement. Party `fd` alone cannot establish this exception in any Full Clear, Unique or Sparkling progress path. A bonus Rainbow Feather message is a reward, not another Macaw capture.
- Reported in Sparkling testing: a Macaw escaped a capsule, reappeared and regained ESP. A replacement UUID may explain this; it is not another feeder spawn, successful catch or automatically new discovery opportunity. See [escape identity](TRACKING.md#capsule-escape-and-replacement-identity).

## Last updated

2026-10-17 - Prevented reports from inventing the zero-catch outcome and clarified local versus remote completion.

2026-10-17 - Distinguished local Full Clear missing rows from progress completion.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-16 - A confirmed Sparkling Macaw completes Unique/Sparkling immediately; Full Clear retains its normal capture/food requirements.

2026-10-16 - Bird completion uses shared captures plus current nearby bird counts, feeder evidence, ground drops and inventory; no distinct UUID accounting.

2026-10-16 - Unique accepts resolved no-Macaw runs for Forest progress and removes Macaw from the missing panel without creating a catch.

2026-10-15 - Added mid-run Forest feeding recovery; retained independent species completion and the one-catch Macaw exception.
