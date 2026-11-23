# Hotspot assignment compatibility

Enable `/hotspot` when the party wants biome assignments. Sparkling Mutuals and compatible SafariUtils clients use the shared backend for assignments and fairness history. Final assignments stay fixed through reconnects in the same run. [Party sync protocol](PARTY_SYNC.md)

Players can report an actual hotspot in party chat early in a run. The leader’s client uses assignment messages to tell teammates without a connected assignment display where to go. The party chat toggle for automatic biome completion codes is separate from hotspot assigning. [Hotspot assigning](HOTSPOT_ASSIGNMENTS.md)

If the backend is unavailable before taking ownership of the visit, Sparkling Mutuals can fall back to leader-owned chat coordination. A visit that has already adopted backend authority keeps its established assignment state during an outage. Older clients can still use individual assignment messages and manual biome reports.
