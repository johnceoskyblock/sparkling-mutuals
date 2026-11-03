# Bazaar Safari offer sidebar

## Requested behavior

The human requests text to the left of the Bazaar menu explaining which Safari shards are fine and which should be taken down, because blue alone is unclear. This is a presentation addition to the approved own-offer guidance. Use the opened Your/Co-op Bazaar Orders page; no automatic navigation or trading. Retain current top-ten-or-owned eligibility and explicit-owner/personal fallback rules. Duplicate offers group by shard. Confirmed HOLD says TAKE DOWN in the same blue as its border; confirmed SELL says KEEP LISTED in green. Missing/stale/invalid advice says UNAVAILABLE in gray. No absent advice is represented as safe to keep.

Use the established saved Safari Progress panel appearance and actual container bounds, leaving thirty GUI pixels for adjacent Bazaar controls. A scrollable left panel shrinks no lower than75% and hides when it cannot fit without overlap. Empty/awaiting-profile states are short neutral text. It lists current visible offers only, not historical order watches. Existing blue outlines, change-only chat, box sidebar, automatic confirmation and pricing rules remain.

Local Minecraft26.1.2 only; no GitHub writes, installation, game closure or .devAuth reads. Existing isolated worktree and Native execution retained. Latest GitHub profit-ranking and archival box-value owners read; local feature docs/source control this unpublished feature.

## Task 1: Cached decisions and safe rows

- [ ] Add failing session and snapshot tests for SELL/HOLD/UNKNOWN, stale/invalid fields, duplicate products, ownership, scope and item/menu invalidation; retain Safari/eligibility filters.
- [ ] Expose confirmed advice for every qualified current offer separately from the existing HOLD-only marker map. Build one shared guarded snapshot for borders and list rows. Test clear/reset behavior.
- [ ] Run full26.1.2 suite and commit locally.

## Task 2: Left panel and delivery

- [ ] Add failing left-layout and action-label tests. Implement read-only left panel, saved appearance, bounded scrolling and contextual empty text using the existing container render hook. Preserve other scrolling and NoFrills field compatibility.
- [ ] Update concise feature docs; run full26.1.2 tests/verifyTargetJar and extend the controlled native fixture for clear action text, rendering, scrolling, narrow layout and all previous74 checks.
- [ ] Obtain one independent review, TDD-fix Important/Critical findings, verify/package local JAR and retain worktree. Live Hypixel confirmation remains pending.

## Review focus

The panel cannot label UNKNOWN/stale data KEEP LISTED. Foreign/BUY/completed/non-Safari/ineligible rows cannot enter it. Duplicates do not create repeated shard rows. Menu/revision/item replacement invalidates rows as well as borders. Left placement must reserve controls, avoid inventory overlap and keep scroll handling limited to the panel; existing Hunting Box render/scroll callbacks and NoFrills compatibility remain.
