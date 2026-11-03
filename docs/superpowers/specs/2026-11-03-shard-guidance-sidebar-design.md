# Saved Hunting Box guidance list

Status: approved directly by the human in voice on 2026-11-03 ("I approve of it"). Minecraft 26.1.2 only. Local work, no GitHub push.

## Intent and chosen approach

Add a compact list to the right of the Hunting Box, showing owned supported shards across visited pages with green SELL, red HOLD or gray unavailable guidance. Keep existing slot borders and tooltips.

Two approaches were discussed: clear the combined list after leaving the box, or retain it between openings and update entries when revisited. The human's latest choice is retention; older ownership observations may remain stale. A box opening refreshes the visible page, and navigating refreshes each additional page. Unopened server pages cannot be read automatically.

## Behavior

- Remember recognized shard names and owned counts across page changes, box openings and restarts. Combine by product identity, never add repeated observations together. Explicit zero-count evidence removes an entry; filters, missing rows and unvisited pages do not prove zero ownership.
- Reuse the current 37 supported Safari-shard products and existing experimental range rule. Broader Hunting Box product support is outside this increment.
- Display an alphabetical, scrollable right-side list of remembered positive-count shards, with aligned HOLD/SELL text and a compact stale/last-observed indication for entries not refreshed during this opening. Fit within the window and preserve inventory interaction.
- Saved ownership may be old; market advice must satisfy the existing freshness and coverage rules. Unknown/stale prices show gray unavailable guidance. Opening the box requests fresh advice for remembered supported products within the existing request throttles and bounds; rendering makes no requests.
- Save locally per verified account/profile, bounded to four scopes and 37 entries each, with bounded atomic worker-thread persistence. Never show another profile's saved list. Until profile identity is confirmed, use current-connection observations only and do not restore private saved ownership into an unknown profile.
- Keep one guidance switch for slot borders, tooltips and the list. Disabling hides the list and stops its demand; closing the box hides it but retains observations.

## Constraints and validation

No automatic menu navigation, trading, changes to sell-offer alerts, forecast claims or higher network caps. Retain the NoFrills crash and short-label recognition fixes. Test page accumulation/revisits, explicit zero versus omission, scope isolation/restart restore, stale ownership versus stale prices, and bounded persistence. Build/test 26.1.2 and verify native layout/scrolling with the user's mod combination where possible. No instance replacement or game closure without applicable authorization.

Implementation details and exact spacing are delegated to the builder. Approval is required before product code changes.
