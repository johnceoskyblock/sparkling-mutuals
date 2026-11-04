# Hotspot assignment compatibility

## Shared backend

Updated clients use Serkosea's `hotspots-v1` authority with the SafariUtils v2 objective relay. Both mods follow the backend's assignments and fairness history. Exact lobby/roster matching, confirmed attendance, locked assignments on recovery, and leader-only non-display instructions follow the [shared sync contract](PARTY_SYNC.md).

While backend authority is active, hotspot reports, final assignment maps and automatic biome completion codes do not go to party chat. If the backend is unavailable before authority is adopted, the leader-owned chat protocol below remains the fallback. Once choices are finalized, neither reconnect nor a transport change may reshuffle them. A backend-owned visit never starts a competing chat allocation during an outage.

## What changed

Previously, each client kept its own hotspot-win history and calculated its assignment independently. Missed reports, reconnects or different versions could produce conflicting assignments.

Now the **party leader owns the pre-run counts and final assignments**. Updated clients follow the leader's final result even if their local history or received hotspot reports differ.

## Leader messages

The leader's immediate hotspot announcement includes the counts **before this run**:

```text
[SM] Hotspot: Haunted | Hotspot wins: JohnFounder=3, metalhat46=2, m3fi=3, Pyruuuu=1
```

A hotspot win means the player was assigned their actual Hypixel hotspot in an earlier run of this party session. It does not mean runs played or off-hotspot assignments. Other members still announce their actual hotspot using `[SM] Hotspot: <Biome>` or the equivalent `[SU]` format.

After allocating, the leader sends one final map:

```text
[SM] Biomes: JohnFounder=Haunted, metalhat46=Icy, m3fi=Cavern, Pyruuuu=Forest
```

Every updated client uses its entry in this map for its local assignment alert. **The leader's result overrides a conflicting local calculation.** It works even if the client missed the initial count snapshot.

## Assignment rules

- Announce actual hotspots immediately when Hypixel sends its hotspot message.
- Include verified party members' manual reports, abbreviations and close typos, such as `forest`, `haunt` and `hvant`. Keep the first report per member.
- Assign two seconds after the third distinct player reports. A fourth report within that window is included without restarting it. For smaller parties, wait two seconds after everyone reports. If too few report, use a five-second fallback from the first report. Solo players keep their hotspot.
- A sole claimant gets their hotspot. For duplicate claims, the leader prioritizes fewer pre-run hotspot wins.
- Break equal-count ties using lowercase UUID without hyphens, falling back to lowercase IGN; sort by Java's signed `String.hashCode`, then identity text.
- Sort displaced players by that identity ordering alone and assign unused biomes in **Forest → Cavern → Icy → Haunted** order. Then assign any remaining non-reporters using that ordering.
- Add one win for each player assigned their actual hotspot after allocation, once per run. Keep counts between runs; clear them when the party UUID set changes or the session resets.

Manual reports reserve biomes on the leader's allocation too. If Haunted, Cavern, Cavern and a plain `forest` are reported, the Forest reporter keeps Forest and the displaced Cavern player takes Icy.

## Receiving leader messages

- Accept coordination only from the **current party leader**, verified through the party roster. Support `[SM]`, `[SU]` and `[SafariUtils]` prefixes.
- Normalize IGNs to lowercase internally. Counts and final maps must contain the entire current roster, with no duplicate names. Final assignments must use distinct valid biomes.
- Replace local pre-run counts with the leader's snapshot. Do not add the current run twice when replacing provisional results or receiving duplicate messages.
- After receiving the leader's snapshot, wait for the final map rather than showing an independent assignment. The current implementation accepts snapshots within 15 seconds of local hotspot allocation starting, and final maps within 20 seconds. Without a final map, local fallback becomes available after 15 seconds; a valid final map still overrides it.
- Freeze the first valid final map for the run. Do not interpret count snapshots or final maps as manual biome reports.

Only the leader additionally sends `<IGN> you are assigned <Biome> biome!` to members without an SM/SU/SafariUtils hotspot announcement, once per member per run. Mod users receive their assignment locally from the shared final map.

Both mods need support for these messages. Older versions can still announce hotspots, but cannot be made to follow the leader's final map without updating. No API requests are needed for this coordination.
