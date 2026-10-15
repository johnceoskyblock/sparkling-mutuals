# Honeybug

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Forest. Use the **actual bee-producing nest count**, never a fixed four-bee threshold. Nest highlighting, Honeybug ESP and capture completion are independent.

## Evidence

- **Uncommon: guaranteed catch; no capsule escape.** A UUID identifies an observed entity; repeated sightings do not add another Honeybug.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Forest; subtract them from the remaining requirement once, as described in the shared rules.
- Track **C** = confirmed shared Honeybug catches, **U** = distinct currently loaded uncaptured bees, **N** = identified bee-producing nests. **B = C + U** accounts for bees already caught or currently present; never add historical/captured UUID sightings again.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One shared catch; if anyone still needs this sparkling variant, also satisfy the species' Sparkling evidence. The missing-species row may disappear after the catch. |
| Full Clear | All required Honeybugs captured; spawning all bees only completes nest handling. |
| Sparkling | Resolve every nest opportunity so all Honeybugs have spawned. Catching ordinary bees is unnecessary for this check. |

## Specific deductions

- On mid-run entry, this client cannot know which nests a teammate already punched. Use C + U as alternative spawn evidence; do not require this player's own punch history.
- B reaching N means all required bees are accounted for as spawned: remove all nest highlights regardless of mode or outstanding catches. Confirmed handling of every actual nest is another way to establish this state.
- In Unique, the first shared catch secures the catch requirement. If anyone still needs Sparkling Honeybug, retain nest highlights and a plain Honeybug checking row until all actual nests are confirmed punched or B reaches N. Retain the separate Bee nests to punch footer while those needed nests remain unresolved.
- If the party still needs its sparkling variant, overall Unique progress requires both the first shared catch and the completed nest-spawn check; hiding the species row/bee ESP does not complete that remaining source work.
- Keep nest completion requirements and highlights independent of panel presentation. The nest footer follows the shared mode/discovery requirements; Full Clear still requires its nest work regardless of discoveries.
- Ordinary Honeybug ESP: **Full Clear** keeps remaining uncaptured bees highlighted until all are caught; **Unique** stops after one shared Honeybug catch, even if someone still needs its sparkling discovery; **Sparkling** never shows ordinary Honeybug ESP.
- Actual sparkling messages/detection remain independent of ordinary bee ESP. A missing discovery requires spawning every bee, not keeping ordinary bee outlines on after Unique's first catch.
- N counts bee-producing nests, not all 13 potential nest/hive sites. Newly identified nests can increase N and reopen remaining nest work; an unseen/unclassified site is not a proven beehive.
- A bee released outside this client's entity range remains outstanding until catch or current bee evidence accounts for it. A spawn/punch is never a catch.
- The nest counter waits for distinct spawned bees to match the actual punched nest total, even after the last click. Keep this counter separate from nest highlight gating and capture quotas. Mid-run population evidence can also resolve it when all sites are known.

## Last updated

2026-10-15 - Preserved distinct spawned-bee requirements for the nest counter alongside mid-run population recovery and separate nest highlight gating.
