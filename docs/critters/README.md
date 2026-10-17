# Critter logic notes

Concise rules based on Hypixel messages, UUIDs, resources and observed world state. [Shared evidence rules](TRACKING.md) define grants, catches, late biome entry and completion. [Mode summary](../../SAFARI_LOGIC.md) defines the chosen goals.

[Exact Hypixel messages](MESSAGES.md) records supplied quotations, recipient scope, optional Chat Heads text and event meanings.

[Missing-panel implementation reference](../MISSING_PANEL.md) lists all 37 critters in every mode and distinguishes current missing/progress behavior from rules still requiring implementation review.

[Typed floor drops](FLOOR_DROPS.md) records the supplied Shiny Hunter JAR's order-based predictor, biome-specific highlight choices and limits of the implemented predictor.

These are tracking rules to implement/review, not a description of every current code shortcut. Numeric baselines below are retained from the earlier source audit; they still need confirmation as server spawn guarantees. A minimum is never proof that all opportunities were observed. Zero means variable, not automatically complete.

Confirmed fixed-spawn check totals are **Scrappy 3, Gemzie 3, Troodon 3 and Gazer 4**. Only these species get per-critter X/X check suffixes; see the [mode-specific panel rules](TRACKING.md#missingcheck-panel-by-mode). Other numeric baselines remain unconfirmed.

Species checks combine into [biome completion](TRACKING.md#biome-completion-and-party-synchronization). Unique advances immediately per first shared species capture, without local discovery scans. Rockmite/Snoozle also need cd and respect needed unbroken-source evidence; loaded source checks work outside Cavern.

## Forest

| Critter | Recorded Full Clear baseline |
| --- | --- |
| [Foxtrot](foxtrot.md) | 6 |
| [Bluebird](bluebird.md) | Variable |
| [Honeybug](honeybug.md) | Actual verified nest count; 3 is the fallback while that count is unknown |
| [Treefrog](treefrog.md) | 3 |
| [Woodchucker](woodchucker.md) | 3 |
| [Fluffling](fluffling.md) | 1 |
| [Hideonfloor](hideonfloor.md) | 1 |
| [Parakeet](parakeet.md) | Variable |
| [Macaw](macaw.md) | One shared catch once all known food is used; additional Macaws may remain |

## Cavern

| Critter | Recorded Full Clear baseline |
| --- | --- |
| [Cavernfish](cavernfish.md) | 4 |
| [Flitter](flitter.md) | 6 |
| [Shyworm](shyworm.md) | 4 |
| [Driftling](driftling.md) | 3 |
| [Chuckwalla](chuckwalla.md) | 2 |
| [Rockmite](rockmite.md) | Variable |
| [Scrappy](scrappy.md) | 3 |
| [Snoozle](snoozle.md) | Variable |
| [Gemzie](gemzie.md) | 3 |

## Icy

| Critter | Recorded Full Clear baseline |
| --- | --- |
| [Strongarm](strongarm.md) | 4 |
| [Tepid](tepid.md) | 6 |
| [Polaris](polaris.md) | 2 |
| [Shuddersquid](shuddersquid.md) | 3 |
| [Billygoat](billygoat.md) | 2 |
| [Mantis Shrimp](mantis-shrimp.md) | 3 |
| [Nozzlenose](nozzlenose.md) | 2 |
| [Troodon](troodon.md) | 3 |
| [Wumpa](wumpa.md) | 1 |

## Haunted

| Critter | Recorded Full Clear baseline |
| --- | --- |
| [Areita](areita.md) | 3 |
| [Bloodbat](bloodbat.md) | 3 |
| [Duplico](duplico.md) | 2 |
| [Gazer](gazer.md) | 4 |
| [Litterbug](litterbug.md) | 4 |
| [Solsnatcher](solsnatcher.md) | 4 |
| [Gimmiegold](gimmiegold.md) | 3 |
| [Hideonwall](hideonwall.md) | 2 |
| [Hideyho](hideyho.md) | 1 |
| [Doomspiral](doomspiral.md) | 1 |

Edit the relevant page when new server evidence establishes a rule. State what was observed, what follows from it, and what remains unknown. Shared evidence belongs in [TRACKING.md](TRACKING.md).

Global exception for every critter page: detecting a Sparkling blocks all modes until caught. Once all detected Sparklings of that species are caught, Unique and Sparkling complete it immediately, overriding their ordinary checks. Full Clear retains normal capture/source requirements. See [shared capture rules](TRACKING.md#sparkling-captures-block-biome-completion-in-every-mode).

Full Clear local capture/source proof and a qualified empty scan advance progress without a report and remain valid after travel. Other biomes without local proof require the biome report plus each variable species' actual capture minimum. Fixed Scrappy/Gemzie/Troodon/Gazer counts can complete early; only positive remaining-critter/source evidence reopens reported completion. Exhaustive UUID render coverage is not required. Macaw retains its explicit exception.

## Last updated

2026-10-17 - Reconciled local Full Clear progress with remote report rules and the reviewed source baseline.

2026-10-17 - Clarified Honeybug's actual nest target and linked species rules for independent Unique nest work and remote Forest reports.

2026-10-17 - Linked the current missing/progress implementation reference for all 37 critters.

2026-10-16 - Documented Full Clear report trust and positive-only revalidation.

2026-10-16 - Aligned Unique party-report checking with real per-species captures and limited Cavern source contradictions.

2026-10-16 - Documented the confirmed-Sparkling completion override shared by every critter.

2026-10-15 - Added revision tracking; aligned the Full Clear Macaw exception and documentation workflow where applicable.
