# Critter logic notes

Concise rules based on Hypixel messages, UUIDs, resources and observed world state. [Shared evidence rules](TRACKING.md) define grants, catches, late biome entry and completion. [Mode summary](../../SAFARI_LOGIC.md) defines the chosen goals.

[Exact Hypixel messages](MESSAGES.md) records supplied quotations, recipient scope, optional Chat Heads text and event meanings.

[Typed floor drops](FLOOR_DROPS.md) records the supplied Shiny Hunter JAR's order-based predictor, biome-specific highlight choices and limits for the future implementation.

These are tracking rules to implement/review, not a description of every current code shortcut. Numeric baselines below are retained from the earlier source audit; they still need confirmation as server spawn guarantees. A minimum is never proof that all opportunities were observed. Zero means variable, not automatically complete.

Confirmed fixed-spawn check totals are **Scrappy 3, Gemzie 3, Troodon 3 and Gazer 4**. Only these species get per-critter X/X check suffixes; see the [mode-specific panel rules](TRACKING.md#missingcheck-panel-by-mode). Other numeric baselines remain unconfirmed.

Species checks combine into [biome completion](TRACKING.md#biome-completion-and-party-synchronization). Unique biomes require a catch of every species; party completion reports affect Full Clear/Sparkling progress only and are rechecked on local entry.

## Forest

| Critter | Recorded Full Clear baseline |
| --- | --- |
| [Foxtrot](foxtrot.md) | 6 |
| [Bluebird](bluebird.md) | Variable |
| [Honeybug](honeybug.md) | 3 |
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

## Last updated

2026-10-15 - Added revision tracking; aligned the Full Clear Macaw exception and documentation workflow where applicable.
