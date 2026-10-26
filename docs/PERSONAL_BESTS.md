# Personal and observed PBs

Boss and unique biome records use local run start time. They use successful capture messages, never new API requests. Existing personal Full Clear biome PB behavior is unchanged; other players do not receive observed Full Clear biome records.

## Boss classification

- Local Doomspiral is Solo if the player held at least four Soothing Incense simultaneously before their capture. Consuming incense does not erase this evidence. Reaching four after capture cannot upgrade the record. Otherwise the capture is Duo.
- Local Wumpa is Solo when all eight other Icy types have personal capture evidence; otherwise Duo.
- Observed Doomspiral is assumed Solo.
- Observed Wumpa is Solo when the named catcher has captured every other Icy type in this run. Captures by another player do not satisfy that player's prerequisites. Otherwise it is Duo.
- A new boss time improves only its classified bucket. Legacy unrestricted personal boss records remain in Duo. Legacy observed Doomspiral records migrate to Solo; legacy observed Wumpa records remain Duo because they have no prerequisite attribution.
- Ordinary and explicitly SPARKLING boss capture/reward messages support the same classification.

## Observed unique biome PBs

Capture attribution is retained throughout the run, including the local player's identity. When the party first secures at least one of every biome type, the observed unique timer stops. Species positively proven absent use the existing unique-mode absence rules. Checks also run after scans that establish absence.

For each player with captures in that biome, store a Solo unique record if they were the only catcher and secured every available type. If any other player also captured a critter in the biome, store a Party record instead. Repeated scans do not advance the finished time. These records do not depend on a party biome-completion message and do not track observed Full Clear times.

## Commands

| Command | Result |
| --- | --- |
| `!pb doom`, `!pb wumpa` | Mod user's Solo boss PB. |
| `!pb doom duo`, `!pb wumpa duo` | Mod user's Duo boss PB. |
| `!pb doom IGN`, `!pb wumpa IGN` | Named player's observed Solo boss PB. |
| `!pb doom IGN duo`, `!pb wumpa IGN duo` | Named player's observed Duo boss PB. |
| `!pb forest IGN` | Named player's observed Solo unique Forest PB. Also accepts cavern, icy, haunted. |
| `!pb forest IGN party` | Named player's observed Party unique Forest PB. Also accepts cavern, icy, haunted. |

Current mode restrictions remain: boss commands are available in Unique/Sparkle, biome commands in Unique/Full Clear. Named biome queries always refer to observed unique records. Player name autocomplete includes named biome queries.

## Persistence

All categories persist in `sparkling-mutuals-pbs.properties`, with case-insensitive player lookup. Existing migration from older PB config locations remains automatic. Observed categories have independent keys ending in `Millis` so migration can preserve the faster duplicate record.

## Last updated

2026-10-26 — Added attributed Solo/Party observed unique biome records, split observed bosses into Solo/Duo, and froze local Doom incense eligibility at capture.
