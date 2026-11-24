# Hotspot preferences protocol update

## WebSocket fields

- Add `hotspot-preferences-v1` to `welcome.features` when supported. Keep `hotspots-v1` and the existing hotspot payload version.
- Accept optional `hotspotPreference` on the existing version 1 `H` hello. It is an ordered array containing each of `FOREST`, `CAVERN`, `ICY`, and `HAUNTED` exactly once, highest preference first.

```json
{"hotspotPreference":["ICY","FOREST","HAUNTED","CAVERN"]}
```

- Bind the preference to the authenticated socket sender and canonical party UUID roster. Ignore malformed values without changing stored preferences. An omitted field leaves stored preferences intact.
- Store the last accepted order across Safari runs, lobby changes, and reconnects with the same roster. Include it as `welcome.hotspotPreference` for that sender only. Omit the field if there is no stored order for this sender and roster.
- Process the preference on a valid hello even if run reconciliation is pending, before evaluating a new assignment. The client sends it if the welcome does not already contain its selected order, and again only when the saved order changes. Treat duplicate values as idempotent.

## Starting-item evidence

- Use the player's confirmed starting grant for the current run. Forest uses the total of Wriggleworms, Yogi Berries, and Bags of Seeds. Haunted uses Shining Coins. Exclude later floor pickups. Spending or losing items does not change the original grant.
- Reuse existing per-player grant counts if available. If the backend only has inventory or discovery totals, those cannot identify the original grant reliably. In that case, add an optional sender-only `startingItems` object to `H` hello with the two independent counts `birdFood` and `shiningCoins`. Accept only nonnegative integers. Omit an unknown count and send an explicit zero for a confirmed zero. Associate the counts with the current run and clear them for the next run. This field would also require client support.
- Use the evidence available at the existing lock deadline. If any of the tied assignment clients has an unknown count for the contested biome, skip the starting-item step for that tie.

## Duplicate actual-hotspot claims

Rank eligible players claiming the same available hotspot by the following, in order.

1. Fewer pre-run hotspot wins.
2. Authenticated assignment client before a manual-only player. Use current-run connection evidence and retain it through reconnects. A chat report, roster membership, or an order cached from a previous run alone does not establish client status.
3. Among assignment clients tied on wins, more starting bird food for **Forest** or more starting Shining Coins for **Haunted**. Skip this step for **Icy** and **Cavern**.
4. Among assignment clients still tied, the earlier position of **this hotspot** in the player’s order. Use `ICY, FOREST, HAUNTED, CAVERN` for assignment clients without a stored order.
5. Higher UUID. Compare the lowercase, hyphen-free 32-character hexadecimal UUIDs in descending lexicographic order. This also resolves a tie between manual-only claimants.

A manual-only player can win a sole claim or a claim when they have fewer wins. They lose an equal-wins tie against any assignment client claiming that hotspot. Keep existing rules for eligibility, scheduling, and displaced players. Preference or item updates never reopen or modify existing assignment locks.
