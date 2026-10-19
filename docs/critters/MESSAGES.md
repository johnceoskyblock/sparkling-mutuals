# Hypixel Safari message reference

## Scope

Authoritative owner of exact supplied server messages, audience qualifiers and message provenance. Quotations supplied on 2026-10-14 are preserved verbatim, including formatting examples. [Tracking](TRACKING.md) owns evidence semantics and [mode completion](../../SAFARI_LOGIC.md) owns completion policy.

## Definitions

Angle-bracket fields are placeholders. Square-bracket head fields represent optional Chat Heads log text, not a required vanilla prefix. Formatting-stripped equivalents support semantic matching; quoted original messages must not be rewritten to match a naming/style convention.

An observed server message and an implemented parser are different evidence. This reference preserves supplied messages even where they only provide context or the client deliberately ignores them. A source/spawn/activity transition is not a catch.

## Implemented rules

### Entry and starting items

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

### Feeder and resource events

**Bird food placement:** no chat message, including when food is added to an active feeder. No placement confirmation is inferred from attraction chat; implemented bird spawning uses qualified captures/current population. Inventory loss alone is not confirmed placement. Other players' inventory/placement remains unobserved.

| Event / audience | Exact message | Meaning |
| --- | --- | --- |
| Gem placer only | `You placed the <Gem color> Gem on its podium!` | That color was placed, not a capture |
| Global Gemzie spawning | `A rumbling sound can be heard, and the door at the back of the chamber opens...` | Checks Gemzie five seconds after this exact server message, including the needed Sparkling check in Unique; does not supply a capture |
| Coin consumption/spawn | `A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!` | One coin used and one Gimmiegold spawned |

The canonical spelling is **Gimmiegold**. Additional previously recorded bird and mound quotations are preserved below; their terminal outcome variants remain incomplete.

### Boss events

| Boss / audience | Exact message | Meaning |
| --- | --- | --- |
| Doomspiral summoner only | `Your ritual summoned a Doomspiral into this world. Stay Still.` | Personal ritual/spawn signal |
| Doomspiral global | `Something stirs in the Haunted Biome...` | Spawn signal before capture |
| Doomspiral global | `The darkness in the Haunted Biome fades away...` | Slain signal after capture |
| Wumpa global | `You hear the sound of massive footsteps echoing through the Icy Biome... What could it be?` | Spawning signal |
| Wumpa global | `The Wumpa has awoken.` | Awoken state |
| Wumpa global | `The cave opens up again...` | Slain signal after capture |

Record these transitions separately. Spawn/awoken signals do not mean caught. Slain signals confirm the global post-capture state; personal/shared catch attribution and reward counts come from capture/loot-share confirmations. `The cave is collapsing...` is not the supplied Wumpa slain message.

### Snooper walls

For each wall, three collisions produce these stages:

1. `You ran headfirst into the wall. Some more cracks appeared in it...`
2. `Keep running into the wall! More cracks are forming!`
3. `You destroyed the wall! Maybe something was hiding behind it...`

Only stage 3 establishes wall destruction. It does not itself confirm a Snoozle spawn/catch. Associate progress with the wall; identical text alone does not identify which of the five walls was hit.

### Shyworm

| Event | Exact message |
| --- | --- |
| Starts eight-cycle rotation | `The Shyworm surfaced nearby! Don't let it see you!` |
| Normal end | `The Shyworm hid back into the ground.` |
| Player in path; returns to start | `The Shyworm saw a player and fled!` |
| Missed nearby capsule startles it; returns to start | `The Shyworm fled because it was startled!` |

These are activity states, not catches. A missed throw/startle is not a capsule escape; Shyworm is Common.

### Captures, loot shares and rewards

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

### Capsule outcomes

```text
You threw a Critter Capsule at the <Critter>!
The <Critter> escaped your Critter Capsule!
```

The first line confirms a hit/attempt; a throw can miss without that confirmation. The second confirms the attempt failed. Retries repeat these events until a capture confirmation. Neither attempt nor escape increments catches.

**Supplied server observation: Masterful Critter Capsules guarantee capture once hit**, regardless of rarity. No Masterful message variants are needed for escape tracking. Ordinary Common/Uncommon catches are also guaranteed once hit; only standard Rare/Epic/Legendary attempts can escape. A missed throw still is not a capture.
### Previously recorded mound and feeder text

The existing observations preserve these exact progress lines:

- Small cracks begin to form in the mound...
- The cracks seem to be getting larger, keep hitting it!
- Chunks of the mound begin falling away...
- The mound is about to fall to pieces! Keep going!
- Two Macaws were attracted to the Birdfeeder!

Implemented terminal-prefix matchers, taken from source rather than asserted as complete supplied server quotations:

| Outcome | Matched prefix |
| --- | --- |
| Empty mound | The mound falls apart, but nothing is inside |
| Released Rockmite | The mound fell apart, revealing a Rockmite hidden inside |

Mound progress is not a terminal reveal/empty result. Exact terminal variants need separate evidence. The supplied explanation associated the last progress line with an empty outcome; the implemented terminal matcher is still the source of the recorded outcome. Bird attraction text is informational and ignored for spawn/feeding/capture accounting.

### Honeyhive observations

| Event/status | Preserved exact text | Qualification |
| --- | --- | --- |
| Queen Bee proc | QUEEN BEE! The Honeyhive instantly refilled with loot! | Supplied exact observation; parser recognizes the QUEEN BEE! prefix |
| Ready label | Click to loot! | Status label; case-insensitive matching |
| Refill countdown | Refill in: ... | The ellipsis denotes the variable countdown suffix |
| Reward line | HIVE! | Recognized prefix; reward suffix varies |

[Honeyhive rules](../HONEYHIVES.md) own cooldown, profile and refill semantics.
## Exceptions

- First Class adds no grant message; initial inventory supplies its resource evidence.
- Hideyho uses finding/reward text instead of ordinary catching text.
- Global boss slain messages establish an activity transition; Sparkling boss checks still require actual catch evidence, with the accepted global Sparkling-catch override documented in the mode owner.
- Player quotations must not become server confirmations. Party completion words are a separate trusted party-report path.
- A Sparkling reward/global announcement pair describes one confirmed Sparkling catch, not two ordinary capture increments.

## Examples

A loot-share line awarding several shards records one shared capture. A MACAW bonus feather line adds no capture. A hit followed by escape records an unsuccessful attempt; only later successful capture text increments the ledger.

## Source and validation references

Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee.

- [SafariMessages](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariMessages.kt): server/player-chat separation, multiline entry and party-report routing.
- [SafariCatch](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariCatch.kt): personal/shared catch parsing and optional head text; [SafariTracking](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariTracking.kt): registered GAME callback and event consumers.
- [SafariEspRules](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/esp/SafariEspRules.kt), [SparklingChecks](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SparklingChecks.kt), [SafariBirdLedger](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariBirdLedger.kt): hit/escape, exact timer text and ignored feeder text.
- Existing [SafariCatchTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/SafariCatchTest.kt) and [SafariPartyCheckRoutingTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/SafariPartyCheckRoutingTest.kt) are validation references, not fresh test receipts. No tests/live server checks were run for this audit.

## Open questions

Exact mound terminal variants and server prerequisite-to-spawn relationships remain incompletely supplied. Capsule guarantees are reported server observations, not guarantees created by the client's parser. Parser differences and unverified mechanics belong to [the consolidated audit](../IMPLEMENTATION_AUDIT.md).

## Last updated

2026-10-19 (America/New_York) — Preserved exact message evidence and consolidated previously scattered mound/feeder quotations. Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee; static source/caller review, no live Minecraft validation.
