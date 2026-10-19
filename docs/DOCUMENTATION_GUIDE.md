# Documentation vocabulary and style

## Purpose and audience

This is the common vocabulary and structure for the current implementation reference. Start at the [documentation index](README.md). The [audit report](IMPLEMENTATION_AUDIT.md) records the reviewed source and discrepancies. Behavior-changing code changes update their owning docs; older prose does not override a newer approved requirement.

## Terminology

| Term | Meaning in this implementation |
| --- | --- |
| Personal capture | A capture credited to the local player. Used separately for personal bests and history. |
| Shared capture | The run's capture count, including recognized party loot shares. A shared catch does not imply a local capture or a checked UUID. |
| Party loot share | A recognized server reward message attributed through the catch parser; reward shard quantity is not a number of captures. See [messages](critters/MESSAGES.md). |
| Live sighting | A currently observed/loaded species or model. Unloading removes visibility, not necessarily retained evidence. |
| Qualified UUID check | An identified entity UUID admitted by the applicable check/zone rules. Repeated observations of the same UUID do not add checks. |
| Retained check evidence | Run-local check evidence that survives the owner's specified unload/travel paths. Source/resource revalidation can differ from UUID retention. |
| Reconciled population | The species-specific combination of captures, qualified checks or current entities, with the overlap handling its owner implements. It is not universally captures plus every historical UUID. |
| Spawn opportunity | A resource or structure that may produce a critter; it does not itself prove a spawned entity. |
| Confirmed spawn | A recognized spawn event or identified critter, with the scope of evidence described by its owner. A client predicate does not certify server mechanics. |
| Source resolved | The relevant source predicate has no remaining obligation from its known evidence. Unknown/unloaded space is not automatically resolved. |
| Unknown source/coverage | The client has not established the relevant catalog, scan or association. This differs from confirmed absence. Some consumers accept weaker evidence; see their explicit rules. |
| Local completion | An owner accepts local capture/check/source evidence, without relying on a remote completion report. |
| Party-reported completion | A parsed biome report can satisfy particular remote branches. It does not fabricate personal captures, UUIDs or a personal PB. |
| Progress completion | The progress HUD's species/biome count, composed by `SafariRun.completed`. |
| Missing/unchecked-row removal | The panel no longer includes a species, through its own missing/unchecked predicate. It can differ from progress. |
| Ordinary ESP visibility | An entity passes the mode, discovery, capture and display filters. A completed row does not automatically disable every highlight. |
| Source-helper visibility | A nest, mound, wall, food or other helper remains useful under its separate source/display rules. |
| Announcement eligibility | The completion announcer's additional local/report/party gates allow an automatic message. Progress alone may be insufficient. |
| PB eligibility | A personal/party timing consumer has its required capture, timing and participation evidence. A party report alone does not grant personal proof. |
| Starting grant | A resource reconciled from ticket/Head Start/inventory evidence at entry, not a later floor pickup. |
| Floor pickup | A recognized collected-resource event. A predicted marker or held item alone is not a pickup. |
| Held resource | A currently counted inventory resource. Spending it does not erase the historical pickup. |
| Resource use | Recognized consumption/interaction evidence. It is distinct from the resulting spawn and successful capture. |
| Confirmed placement | Server/world evidence establishes the placed resource. Selection or outgoing use alone does not establish placement. |
| Confirmed fixed total | A supplied, narrowly scoped server-evidence claim. The fixed check targets are Scrappy 3, Gemzie 3, Troodon 3 and Gazer 4; this audit did not revalidate them live. |
| Implemented minimum | A lower bound in source, scoped to the relevant completion consumer. It is not a server guarantee. |
| Observed spawn range | A range from observations with their stated scope; it does not prove a maximum. |
| Statistical average | An average over specified samples, not a required completion count. No sample population is invented here. |
| Unverified assumption | A hypothesis retained with its uncertainty, such as contact proxies or broader-angle flight. |

Use **Sparkling**, **Unique**, **Full Clear** in that order. Biomes are **Forest**, **Cavern**, **Icy**, **Haunted**. Use blocks for distance, milliseconds or seconds explicitly for time, `(x, y, z)` for block coordinates and UUID only when identity actually matters. Exact server messages retain their original text. Use **None**, **Not applicable**, or **Unknown** instead of an empty table cell.

## Page families

| Family | Required section order |
| --- | --- |
| Species | Overview → What makes this critter different → Spawn prerequisites → Evidence → Completion by mode → Display and highlights → Party reports and late entry → Retention and reopening → Examples → Implementation and validation → Open questions → Last updated |
| Shared gameplay | Scope → Definitions → Implemented rules → Exceptions → Examples → Source and validation references → Open questions → Last updated |
| Feature | Purpose → Controls and defaults → Implemented behavior → Lifecycle and persistence → Edge cases → Source and validation references → Last updated |
| Developer guide | Purpose → Prerequisites → Procedure → Expected results → Troubleshooting → Last updated |
| Index | Purpose and audience → Categorized links → Rule ownership and navigation → Last updated |

Species tables use: Overview **Property / Value / Status**; Evidence **Evidence / What it establishes / What it does not establish**; Completion **Mode / Capture requirement / Checking or source requirement / Party-report effect**; Display **Outcome / Implemented behavior**. Display rows distinguish Progress, Missing/unchecked row, Counters, Ordinary ESP, Source helpers, Automatic completion announcements, PB eligibility. Ordinary cases link to a shared owner rather than restating its algorithm. Exceptions belong near the top and in the affected row.

README installation/credits, this vocabulary reference, the consolidated audit and exact-message catalog have purpose-specific tables. Legal notices and historical quotations are exempt from gameplay templates. The manual checklist uses developer sections and scenario subheadings. A separate packaged tools README remains necessary because it travels with the standalone ZIP.

## Rule ownership and navigation

| Rule | Authoritative owner |
| --- | --- |
| Mode composition and completion outcomes | [Safari logic](../SAFARI_LOGIC.md) |
| Run evidence, resource accounting, reports, retention | [Tracking](critters/TRACKING.md) |
| Exact server messages and event meanings | [Messages](critters/MESSAGES.md) |
| Drop classification and eligibility | [Floor drops](critters/FLOOR_DROPS.md) |
| Bird spawning and Forest announcements | [Bird completion](BIRD_COMPLETION.md) |
| Progress/panel/ESP distinction | [Missing panel](MISSING_PANEL.md) |
| Detection and notification identity | [Sparkling detection](SPARKLING_DETECTION.md) |
| Feature controls, lifecycle and caveats | The feature page linked from the [index](README.md) |
| Review dates, hashes, source baselines and validation | [Source synchronization](SOURCE_SYNC.md) |

## Last updated

2026-10-19 (America/New_York) — Consolidated navigation and audited contributor guidance. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; documentation-only changes, no local runtime differences or new live validation.
