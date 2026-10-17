# Shyworm

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Cavern. **Recorded Full Clear baseline:** 4. Baseline counts are carried from earlier notes, not newly verified server guarantees.

## Evidence

- **Common: guaranteed catch; no capsule escape.** A UUID identifies an observed entity; repeated sightings do not add another Shyworm.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Cavern; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One confirmed shared catch immediately completes Unique and removes the missing row, without a local Sparkling scan/radius requirement. Detected uncaptured Sparklings still block completion. |
| Full Clear | Local capture/source proof and a qualified empty scan advance progress and remove the missing row without a report; retain that proof after leaving. Other biomes without local proof use party report plus minimum captures, with existing fixed-count exceptions. Positive remaining critters reopen local completion. See the [per-critter panel reference](../MISSING_PANEL.md). |
| Sparkling | Record distinct UUIDs seen inside this species' qualifying scan area and retain their checked progress for the run, including after unloading and biome reentry. Reconcile shared captures without counting a later catch of a seen UUID twice; source requirements remain separate. |

## Specific deductions

- Local Full Clear checks advance progress and remove the missing row without a report. Remote progress without local proof retains its report gate and existing fixed-count exceptions.

- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- `The Shyworm surfaced nearby! Don't let it see you!` starts its eight-cycle rotation; do not assume eight seconds. [Exact end/interrupt messages](MESSAGES.md#shyworm) distinguish normal hiding, player obstruction and a missed capsule startling it.
- Hid-back-into-the-ground marks the normal end. Startled means an interrupted attempt after a missed capsule; saw-a-player-and-fled means a player blocked its path.
- Shyworm is Common and cannot escape a capsule. Fleeing after a missed throw is an interrupted activity cycle, not an escape from a capsule. Hiding/resurfacing alone is neither a capture nor proof of another worm.
- Full Clear still requires catches; seeing a complete movement cycle does not supply one.

## Last updated

2026-10-17 - Local Full Clear completion also advances progress; other biomes without local proof retain report requirements.

2026-10-17 - Separated local Full Clear missing-panel proof from report-based progress.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

2026-10-17 - Added focused body diagnostics for capsule-contact investigation; tracking and completion rules are unchanged.

2026-10-17 - Added provisional separate body-piece contact estimates from focused live snapshots; tracking and completion rules remain unchanged.

## Capsule body investigation

The human reported that a surfaced Shyworm can be caught by hitting its visible body, while the contact diamond appeared only near its head. The supplied clip shows green body pieces extending beyond that head-area proxy. The selected recorder export contains zero native head bounds and does not identify the other pieces or their relationship to the head; it does not establish the server's capture hitbox.

In Safari, `/sm debug` now appends local `SM_SHYWORM_BODY` JSON lines to `logs/latest.log`. Stand within roughly 8 blocks of one ordinary surfaced Shyworm, leave capsule hiding off, and run the command three times while its green body is visible and moving, about one second apart. No throw or shot recording is required. Stay in the same world for those snapshots. A later hidden snapshot can help distinguish the states if convenient, but no absence in a limited scan proves the worm has gone.

The capture considers at most 4,096 entries from the existing diagnostic list, selects the nearest two identified heads within 24 blocks, and retains at most 64 candidate parts. Visual/interaction entities within 8 blocks of a selected head and directly linked carriers/passengers are candidates. Heads and explicit links are prioritised; scan, head, candidate and passenger limits are reported. Candidates remain unverified membership, not new contact bounds.

Rows contain player-eye-relative positions/bounds, rotation, velocity, visibility, numeric runtime relationships, registry item/block IDs, texture hashes, armour-stand poses and display transforms when observed. Missing/nonfinite geometry stays unavailable. They omit player entities, names, UUIDs, raw profiles, arbitrary item data and text-display content. The existing general debug output is unchanged; share or inspect only the `SM_SHYWORM_BODY` lines for this investigation. Nothing uploads automatically. Four local snapshots now support a provisional contact-only extension: the repeated body texture occurs on seven separate marker armour-stand head-items, with a raised trailing section even while the head is low. The guide checks those pieces separately using the existing head-item visual box, requiring a bounded unambiguous connection to a current identified head. It does not use allocation IDs as ownership evidence, bridge gaps between parts, predict their motion, or register the pieces as extra worms. Missing/ambiguous connections and stale/unsupported geometry remain unavailable. These snapshots are fitting evidence; neither the visual proxy nor its connection thresholds establish the server capture hitbox. See the [contact guide](../CAPTURE_SHOT_RECORDING.md#experimental-capsule-path) for bounds and independent live validation limitations.
