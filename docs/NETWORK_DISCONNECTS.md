# Unknown packet disconnect investigation

## Last updated

2026-10-21 (America/New_York) — Read both Prism instances’ available latest/archived logs and reviewed the mod’s packet-related hooks. No networking behavior was changed.

## Confirmed local incidents

The observed error is `io.netty.handler.codec.DecoderException: Received unknown packet id 24`. No packet-ID-14 incident was found in the available logs searched.

| Instance | Log | Time in log |
| --- | --- | --- |
| 26.1.2 JohnFounder | 2026-10-16-4.log.gz | 17:13:13 |
| 26.1.2 JohnFounder | 2026-10-17-3.log.gz | 16:13:23 |
| 26.1.2 Skyblock | 2026-10-17-3.log.gz | 07:14:38 |

Each incident is immediately preceded by Hypixel’s `You are no longer allowed to access this instance!`, followed by `A kick occurred in your connection, so you were put in the SkyBlock lobby!` and `send:lobby`. These were sessions with Sparkling Mutuals 3.3.0, before the latest 3.4.0 fixes/features.

The Skyblock trace fails in `IdDispatchCodec.decode` via `PacketDecoder.decode`, on the incoming Netty decoding path. Fabric’s decoder wrapper and other ordinary pipeline components appear in the trace; their presence does not establish responsibility. Sparkling Mutuals is not named in that trace. Absence from a stack trace does not rule out an indirect trigger.

## Source review

- ShotPacketMixin and SafariParticleMixin observe already-decoded typed game packets after vanilla handler thread handoff; they do not register raw numeric vanilla packet IDs or replace the packet decoder.
- PartyManager/SafariIsland use the Hypixel Mod API’s custom payload/event interfaces, not a replacement vanilla packet registry.
- The entry guard cancels selected outgoing gameplay interactions, not protocol-transition acknowledgements.
- Ticket/gem selection uses vanilla container/use-item methods; Hideyho acceptance forwards the server-supplied action. No source in the reviewed mod hooks replaces IdDispatchCodec, PacketDecoder, or the configuration/play transition.

## Assessment and limits

The repeated timing suggests an incoming protocol/state mismatch during Hypixel’s forced instance-to-lobby transfer. This is an inference from the logs, not a confirmed root cause. The logs do not establish whether Hypixel, Fabric, another mod, Sparkling Mutuals, or an interaction is responsible. The working hypothesis does not justify swallowing unknown packets or changing the protocol state.

For an affected user reporting ID 14, collect the exact full disconnect text, Minecraft/mod versions, and the adjacent latest.log or Minecraft network-protocol error report. Do not assume 14 and 24 are the same failure.

A useful isolation test keeps Minecraft/Fabric/other mods identical in a copied test instance, toggling only Sparkling Mutuals and exercising the same instance-to-lobby transfer. One successful transfer is not conclusive for an intermittent failure. Preserve the original instance/configuration.
