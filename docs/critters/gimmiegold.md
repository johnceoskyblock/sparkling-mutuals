# Gimmiegold

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Haunted | Implemented roster |
| Rarity | Rare | Supplied server notes; not live revalidated |
| Spawn source | Shining Coin slots plus starting/party coins | Implemented evidence path; mechanics unverified |
| Population model | Variable; Sparkling target F registered floor slots + E extra coins | Implemented rule, not server guarantee |
| Full Clear minimum | 3 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | Sparkling population radius 15 blocks around X/Z (-4, -64); slots learned Safari-wide | Implemented coverage |

## What makes this critter different

- Sparkling requires all registered slots resolved, sampled zero held coins, and qualified capture/check coverage of F + E. Qualified UUID checks are retained when critters leave render range; they are not captures.
- Full Clear retains a separate conservative recovery ledger: fallback minimum 3 plus larger known targets.

## Spawn prerequisites

Collect registered slots/use all known coins. Grants, pickups, held coins, use messages and contributions remain separate evidence; see [FLOOR_DROPS](FLOOR_DROPS.md).

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live textured item display / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Supplied notes say standard capsules can fail, replacement UUIDs may follow escape, and Masterful catches are guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Coin registry, inventory and qualified retained capture/check coverage | Slot collection and qualified capture/check coverage against F + E | Fixed three-slot guarantee, capture from resource use, or replacement UUID history as current population |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary models; confirmed Sparkling capture override applies. | All registered slots resolved, sampled 0 held coins, qualified capture/check coverage >= F + E; at least one registered slot required. | Outside Haunted, hd can supply checking trust. Inside Haunted local coin conditions apply. |
| Unique | One shared capture, or the confirmed-Sparkling capture override. | If anyone needs Gimmiegold, require the same resolved floor-slot/held-coin/qualified-population check as Sparkling before progress completes. First catch removes only the missing row. | Outside Haunted hd may supply inherited checking; inside Haunted local coin conditions apply. Never supplies a capture. |
| Full Clear | At least 3 shared captures, raised by source/contribution and observed-population targets. | Local recovery resolved, no known held coins/no remaining critters; resource/source contradictions may still block progress. | Without local proof, report plus capture minimum can complete progress; row still needs local proof. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear needs local capture/absence proof, separately from report-based progress. See [panel rules](../MISSING_PANEL.md). |
| Counters | No species-row counter; capture panel shows actual shared catches. Unique Haunted shows Coins left to use while its discovery check remains: max(uncollected slots + held coins, required floor/starting/party coins - qualified captured/live Gimmiegolds). Unknown catalog/inventory displays ?. |
| Ordinary ESP | Unique: until first shared capture. Full Clear: eligible uncaught models. Sparkling: no ordinary model ESP solely for automatic checking. Settings and model validity still apply. |
| Source helpers | Remaining eligible coin markers are independent of incense cutoffs; Sparkling requires needed Gimmiegold discovery or enabled profitable-shard ESP with Gimmiegold in the cached top ten. This exception does not add discovery checks or source reminders. |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of each spawned species; proven zero-spawn types exempt. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

Early starting-item reports overlap by sender/maximum quantity; own report overlaps starting coins. Legacy unused-item notices remain accepted without doubling an early contribution. Extras increase demand without catches. Bird food belongs to Forest. Outside Haunted, hd records the current F + E target; increased demand invalidates that report until a fresh report or local check. Inside Haunted, local slot/inventory/population proof applies. A late entrant may send hd after this proof and the other local active-mode checks pass, without personally collecting every coin. Do not echo a received hd. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Retain slot IDs/contribution demand through collection/unloading. Unobservable missing markers stay unresolved. Travel retains completed validation. Haunted reentry revalidates sources/inventory using retained qualified UUID checks. New slots, returning uncollected markers or changed contribution demand clear the prior check cohort, so old checks cannot automatically satisfy new work. Remaining markers and held coins still block completion. First-seen capture baselines prevent a later catch from counting the same checked critter twice; a UUID repeated in a scan counts once. New run resets all. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

F = 4 and E = 1 require qualified coverage of five captured or checked critters. Even with zero held coins, one remaining slot blocks checking. Once sources resolve, earlier qualified checks may complete the requirement even if some critters have left render range. Actual Full Clear catches remain a separate counter.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entry: `Gimmiegold` in `SafariRun` and `SafariEspRules.mobs`; dedicated owners below.

[GimmiegoldSparklingCheck](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/GimmiegoldSparklingCheck.kt)

[SafariResources / contribution notices](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariResources.kt)

Existing coverage: [GimmiegoldCoinFloorCheckTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/GimmiegoldCoinFloorCheckTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Larger repeated resource notices can diverge between Sparkling maxima and conservative Full Clear recovery; see [audit D3](../IMPLEMENTATION_AUDIT.md#d3).

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Coin ESP presentation

Unique and Full Clear keep Shining Coin waypoints for every remaining drop throughout the run, even when no party member needs its Sparkling variant. Sparkling mode retains the discovery-needs gate. Pickup tracking removes collected drops normally. This is an ESP-only rule: Unique capture/check requirements and Full Clear minimum/capture rules remain unchanged.

## Coins-left display

Coins left to use is the known remaining floor-coin slots plus this client's held coins. When both are zero the counter is zero even if teammates reported unused coins. Qualified Gimmiegold population/checking and capture requirements are separate from this display counter.

## Last updated

2026-10-29 - Simplified known-source replies and local Sparkling source requirements.


2026-10-27 — Personal Unique PBs exempt positively proven zero-spawn types.

2026-10-26 — Removed ordinary ESP added solely for automatic Sparkling checks and its per-UUID linger. Capture/source and profitable-highlight exceptions remain separate.

2026-10-25 — Keep Shining Coin ESP in Sparkle mode for top-ten profitable Gimmiegold when profitable ESP is enabled, even if its shiny is shared. No mode completion changes or new price fetches.

2026-10-25 — Fixed JohnFounder's six-coin Unique run: qualified UUID checks now survive partial unloading before the final pickup. New source/contribution demand clears prior check credit; actual capture counts and Full Clear minimums stay separate.

2026-10-22 — Kept needed coin-source checking after Unique capture, with a remaining-use footer that includes uncollected, held and party-extra demand.

2026-10-21 — Removed the Unique-mode coin ESP pickup cutoff; all remaining coins stay highlighted without changing completion logic.

2026-10-21 — Full Clear Gimmiegold capture targets are coin-derived, not raised by historical/replacement model UUID population estimates. A cleared dedicated coin-source ledger plus sufficient actual captures and no live remainder completes the species even if the older generic floor-marker scan remains blocked by unrelated rewards. Legacy coin proof remains a fallback; additional real coins and held coins still block completion. Added a three-coin whole-Haunted regression.

2026-10-19 (America/New_York) — Early coin quantities feed F + E; hd trust is target-scoped and late entrants can report local capture-plus-render proof. Reviewed source: working changes on `78885e0`; live validation pending.
