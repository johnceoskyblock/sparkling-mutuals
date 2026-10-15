# Hypixel Safari message reference

[Tracking rules](TRACKING.md) · [Critters](README.md)

Exact quotations supplied by the user on 2026-10-14. Angle-bracket fields are placeholders. Square-bracket head fields represent optional Chat Heads text in logs, not a required vanilla prefix. Preserve the original text; strip Minecraft formatting for semantic matching. These messages define events, not automatic capture counts unless stated.

## Entry and starting items

| Audience / event | Exact message | Meaning |
| --- | --- | --- |
| Head Start recipient | `HEAD START! You started with a <Quest Item>!` | Named starting grant |
| First Class bonus | **No message** | Inspect inventory at run start, immediately after Head Start when present; do not require another grant message |
| Party leader queue | `Queuing... (Attempt 1/3)` | Queue attempt; not successful arrival |
| Server transfer | `Sending to server mini<4 character server name>...` | Transfer destination |
| Leader admission | `[NPC] Safari Manager: I already saw your ticket, so you're free to go.` | Manager admission acknowledgement |
| Member admission | `[NPC] Safari Manager: Looks good to me. Have fun out there!` | Manager admission acknowledgement |
| Hotspot | `HOTSPOT! Your Hunting Hotspot is the <Biome Name> Biome!` | Run's hunting hotspot |

Global entry example, including its separate lines:

```text
-----------------------------
[MVP++] JohnCEO entered Critter Safari!
-----------------------------
```

Chat Heads log variant:

```text
-----------------------------
[MVP++] [JohnCEO head]JohnCEO entered Critter Safari!
-----------------------------
```

Rank and IGN above are examples. A head decoration may occur between rank and IGN; it must not become part of the IGN. Entry, queue and transfer are distinct events, not multiple tickets/grants. First Class starting inventory includes the Head Start item when that perk applies: do not count the message's item a second time on top of the snapshot. Without a Head Start message, use initial run inventory rather than waiting for a nonexistent First Class message.

## Feeder and resource events

**Bird food placement:** no chat message, including when food is added to an active feeder. Inventory changes and later bird spawn announcements must be correlated; do not wait for placement dialogue. Other players' inventory/placement remains unobserved.

| Event / audience | Exact message | Meaning |
| --- | --- | --- |
| Gem placer only | `You placed the <Gem color> Gem on its podium!` | That color was placed, not a capture |
| Global Gemzie spawning | `A rumbling sound can be heard, and the door at the back of the chamber opens...` | Starts pending spawn check; wait for three accounted-for Gemzies (shared captures + reconciled live uncaptured scans); does not supply a capture |
| Coin consumption/spawn | `A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!` | One coin used and one Gimmiegold spawned |

The canonical spelling is **Gimmiegold**. Existing bird-announcement and mound patterns are retained in the tracking/species notes; they were not newly quoted in this batch.

## Boss events

| Boss / audience | Exact message | Meaning |
| --- | --- | --- |
| Doomspiral summoner only | `Your ritual summoned a Doomspiral into this world. Stay Still.` | Personal ritual/spawn signal |
| Doomspiral global | `Something stirs in the Haunted Biome...` | Spawn signal before capture |
| Doomspiral global | `The darkness in the Haunted Biome fades away...` | Slain signal after capture |
| Wumpa global | `You hear the sound of massive footsteps echoing through the Icy Biome... What could it be?` | Spawning signal |
| Wumpa global | `The Wumpa has awoken.` | Awoken state |
| Wumpa global | `The cave opens up again...` | Slain signal after capture |

Record these transitions separately. Spawn/awoken signals do not mean caught. Slain signals confirm the global post-capture state; personal/shared catch attribution and reward counts come from capture/loot-share confirmations. `The cave is collapsing...` is not the supplied Wumpa slain message.

## Snooper walls

For each wall, three collisions produce these stages:

1. `You ran headfirst into the wall. Some more cracks appeared in it...`
2. `Keep running into the wall! More cracks are forming!`
3. `You destroyed the wall! Maybe something was hiding behind it...`

Only stage 3 establishes wall destruction. It does not itself confirm a Snoozle spawn/catch. Associate progress with the wall; identical text alone does not identify which of the five walls was hit.

## Shyworm

| Event | Exact message |
| --- | --- |
| Starts eight-cycle rotation | `The Shyworm surfaced nearby! Don't let it see you!` |
| Normal end | `The Shyworm hid back into the ground.` |
| Player in path; returns to start | `The Shyworm saw a player and fled!` |
| Missed nearby capsule startles it; returns to start | `The Shyworm fled because it was startled!` |

These are activity states, not catches. A missed throw/startle is not a capsule escape; Shyworm is Common.

## Captures, loot shares and rewards

```text
CAPTURE! You caught a <Critter> and gained <# of shards>x <Critter> Shard!
LOOT SHARE! You received <# of shards>x <Critter> Shard from [<IGN> head]<IGN> catching a <Critter>!
FLOOR DROP! You found <Item> on the ground!
CAPTURE! You found the Hideyho, and as a reward it gave you <# of shards>x Hideyho Shard!
LOOT SHARE! You received <# of shards>x Hideyho Shard from [<IGN> head]<IGN> finding the Hideyho!
LOOT SHARE! You received a Rainbow Feather and <# of shards>x <Critter> Shard from [<IGN> head]<IGN> catching a SPARKLING <Critter>!
MACAW! You found a bonus Rainbow Feather!
```

The `[<IGN> head]` segment may be absent. A loot share must still identify the following IGN and distinguish `catching a` from `finding the Hideyho`. One catch yields one capture event regardless of shard quantity. A sparkling loot share is one shared capture plus its rewards; the Macaw bonus line adds a feather, not another capture.

Sparkling personal capture, preserving the supplied formatting codes:

```text
§a§lCAPTURE! §7You caught a §6§lSPARKLING §f<Critter>§7 and received a §5Rainbow Feather§7 and <# of shards>x §f<Critter> Shard§7!
```

Formatting-stripped equivalent:

```text
CAPTURE! You caught a SPARKLING <Critter> and received a Rainbow Feather and <# of shards>x <Critter> Shard!
```

This is one personal and shared capture. `SPARKLING` is a modifier, not part of the canonical species name.

## Capsule outcomes

```text
You threw a Critter Capsule at the <Critter>!
The <Critter> escaped your Critter Capsule!
```

The first line confirms a hit/attempt; a throw can miss without that confirmation. The second confirms the attempt failed. Retries repeat these events until a capture confirmation. Neither attempt nor escape increments catches.

**Masterful Critter Capsules guarantee capture once hit**, regardless of rarity. No Masterful message variants are needed for escape tracking. Ordinary Common/Uncommon catches are also guaranteed once hit; only standard Rare/Epic/Legendary attempts can escape. A missed throw still is not a capture.

## Last updated

2026-10-15 - Added document revision tracking.
