# Safari WebSocket implementation handoff

JohnKuudra, extend Safari party sync so Sparkling Mutuals users see the same confirmed critter and biome progress across the run. Coordinate the shared protocol with Serkosea so SafariUtils keeps working in mixed parties. Use the Sparkling Mutuals WebSocket as a backup when the shared service is unavailable. Keep sync automatic for eligible parties.

## 1. Agree on the shared protocol first

- Start with [the existing Party Sync documentation](PARTY_SYNC.md) and the `SafariPartySync`, `PartySyncProtocol`, and `PartySyncTransport` code. Check the deployed protocol with Serkosea before adding fields. The client checkout I reviewed still points to the Serkosea service, so verify the backup endpoint, authentication, and capabilities with whoever runs it.
- Ask Serkosea for a capability-gated, per-critter completion report and a way for the room to acknowledge the first accepted biome report. Reuse existing capabilities if they already cover either need. Agree on species IDs, mode and evidence fields, run identity, and replay and deduplication behavior. Keep existing clients working if they lack the new capability.
- Prefer the shared service for mixed-mod parties. Treat the backup as a separate authority unless the two services explicitly support shared room state. Use one write authority per visit at a time. On reconnect or failover, keep confirmed progress and finalized hotspot assignments, and reconcile before publishing new decisions. If the backup cannot coordinate with SafariUtils peers, keep existing chat and local behavior available for those peers. Never assume a client connected to one service can see the other service's room.

## 2. Sync confirmed critter completion

- Share positive, locally confirmed results. The main gaps are a finished Sparkling check and a finished Full Clear render check. Unique still requires a capture of each required type, with the existing exemptions for types that can have zero spawns. Keep Sparkling checks separate from captures so a Sparkling result cannot satisfy a Full Clear quota or a Unique capture requirement.
- Report the species, relevant completion check, reporting player, and current run. Keep any capture totals backed by real capture evidence. Sync reports may update the Progress HUD, Missing or Unchecked panel, Biome Objectives, and `/captures`. They must never fabricate captures, profits, PBs, or history. A pending detected Sparkling still has to be caught.
- Preserve positive confirmations when entities unload or players change biomes. Reject stale or out-of-room reports, dedupe retries and self-echoes, and never publish a received result as fresh local evidence. A late joiner should get the same confirmed progress from the current room snapshot.

## 3. Keep biome completion and party chat in sync

- Publish a valid local biome clear to the backend even if the user's biome-reporting chat toggle is off. Send at most one automatic `fd`, `cd`, `id`, or `hd` per biome per run, through the first accepted reporter if their chat toggle is on. Keep the Unique Icy exception and the existing Wumpa rules. Verify the server can distinguish the winning report from a replay before relying on this for chat deduplication.
- A backend biome clear should flip the Progress HUD's X to the biome icon and fill the bar regardless of the reporting player's mode. This is biome-level display and coordination evidence. Keep mode-specific critter evidence separate.
- Accept exact manual biome codes as a fallback so players without either mod can participate. Trust that report outside the biome. Entering the biome should preserve it until positive contradictory evidence is seen. Missing or unloaded entities alone should never reopen a reported biome.

## 4. Preserve objective evidence and existing integrations

- Keep nest punches, resolved mounds, and broken walls keyed by canonical location. Hide the resolved waypoint and update its count across clients even if its entity unloads. Never infer that a source is broken solely from an out-of-range disappearance. A manual `fd` should clear nest ESP. A manual `cd` can be contradicted by a positive observation of a remaining wall.
- Keep actual inventory, starting grants, confirmed floor pickups, and consumed items distinct. Only publish or block completion on unused starting bird food and Shining Coins when starting-item announcements are enabled, subject to the existing local Forest rule for early entrants. Always share gems and incense. Preserve current local mode rules for floor drops and spawned birds/Gimmiegolds.
- In Biome Objectives, show teammate gems only for unfilled podium slots, and only show per-player **starting** bird food when shared and still unused. Keep per-player coins and incense out of that HUD. Preserve existing feeder and coin snapshot compatibility.
- Preserve the existing hotspot assignment contract, including finalized assignments through reconnects and one designated sender for non-mod instructions. Preserve Sparkling detection deduplication between local and shared sightings and keep caught or stale sightings from triggering auto-entry.

## Acceptance checks

1. In a mixed Sparkling Mutuals and SafariUtils party, verify existing objectives, Sparkling discoveries, and hotspot assignments still agree. A client without the new capability should continue to work.
2. In a party with two Sparkling Mutuals clients in different biomes, verify that confirmed Sparkling and Full Clear checks update both HUDs. Verify Unique captures and Full Clear quotas still reflect actual capture evidence.
3. Clear a biome on two clients nearly simultaneously. Verify one chat code, a biome icon on both HUDs, and backend completion even with chat disabled. Then test a manual code from a non-mod player and verify that entering the biome only reopens it after contradictory evidence.
4. Test late ticket use, reconnect, a shared-service outage, backup recovery, and a new run on a reused lobby ID. Verify no duplicate announcements, stale results, reassigned hotspots, or competing writes.
5. Test Forest with starting-item announcements on and off, and Cavern with two players breaking sources from opposite sides. Confirm held gems disappear from the objectives after placement and resolved waypoints stay gone after unloading and returning.
