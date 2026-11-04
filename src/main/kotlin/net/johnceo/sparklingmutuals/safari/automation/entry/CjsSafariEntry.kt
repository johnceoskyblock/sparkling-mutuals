package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.azureaaron.hmapi.events.HypixelPacketEvents
import net.azureaaron.hmapi.network.packet.v1.s2c.LocationUpdateS2CPacket
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.JoinWelcomeState
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.SafariAreaMap
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariIsland
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

/** 26.1.2 experiment. Its only outgoing network operation is a bounded party status command. */
object CjsSafariEntry {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/cj-entry-dryrun")
    private val origin = System.nanoTime()
    private fun now() = (System.nanoTime() - origin) / 1_000_000
    private var supported = false
    private var environment = EntryDryRunEnvironment()
    private var session = EntryDryRunSession()
    private var transport = EntryDryRunTransport(::now)
    private var nextRosterAt = 0L
    private var wasEnabled = false
    private val trace = ArrayDeque<String>()

    fun register() {
        supported = FabricLoader.getInstance().getModContainer("minecraft").orElse(null)
            ?.metadata?.version?.friendlyString == "26.1.2"
        if (!supported) return
        // SafariIsland owns the existing subscription; this listener retains its fresh server claim.
        HypixelPacketEvents.LOCATION_UPDATE.register { packet ->
            if (packet is LocationUpdateS2CPacket) Minecraft.getInstance().execute {
                val client = Minecraft.getInstance()
                sync(client, now())
                val mode = packet.mode().orElse(null).takeIf {
                    packet.serverType().orElse("SKYBLOCK").equals("SKYBLOCK", true)
                }
                environment.location(mode, packet.serverName(), realm(client))
            }
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { receive(message.string) }
        }
    }

    private fun realm(client: Minecraft): String = if (client.currentServer?.ip?.substringBefore(':')
            .equals("alpha.hypixel.net", true) || SkyblockSidebar.lines(client).any {
                SafariRules.strip(it).equals("alpha.hypixel.net", true)
            }) "alpha" else "main"

    private fun sync(client: Minecraft, at: Long) = environment.world(client.connection, client.level, at)

    private fun frame(client: Minecraft, at: Long): EntryDryRunFrame {
        sync(client, at)
        val enabled = supported && ConfigManager.safariEntryDryRun
        val hypixel = JoinWelcomeState.hypixel(client.currentServer?.ip)
        val player = client.player
        val ids = PartyManager.withLocal(player?.uuid?.toString().orEmpty())?.mapNotNull {
            runCatching { UUID.fromString(it) }.getOrNull()
        }?.toSet()
        val leader = ids?.singleOrNull { PartyManager.isLeader(it.toString()) }
        val party = if (leader != null && ids.size in 1..4) EntryParty(leader, ids, PartyManager.continuity) else null
        // Observing unknown party information invalidates the old context even when the player is absent.
        val context = if (player != null && hypixel) environment.context(player.uuid, player.name.string, party)
            else { environment.context(UUID(0, 0), "Unavailable", null); null }
        if (enabled && hypixel && player != null && SafariIsland.onIsland && !PartyManager.hasInfo && at >= nextRosterAt) {
            nextRosterAt = at + 5000
            PartyManager.requestPartyInfo()
        }
        val mapped = player?.let { SafariAreaMap.areaAt(it.x, it.y, it.z) }
        return EntryDryRunFrame(context, enabled, SafariAssist.atEntrance && mapped == 0,
            SafariAssist.inSafari && mapped in 0..4, environment.arrivedAt)
    }

    private fun receive(text: String) {
        if (!supported) return
        val client = Minecraft.getInstance()
        val at = now()
        val frame = frame(client, at)
        if (!frame.enabled) return
        // Global capture evidence must also withdraw advice while this player is still outside.
        EntryCaptureNotice.species(text)?.let {
            session.captured(frame, it, at); record("capture received: $it", at)
        }
        val current = frame.context ?: return
        val (peer, message) = EntryPartySender.resolve(text, profiles(client), current.party.members) ?: return
        record("${if (message is EntryMessage.Query) "query" else "reply"} received from ${peer.name}", at)
        session.receive(frame, peer, message, at)
    }

    private fun profiles(client: Minecraft) = client.connection?.onlinePlayers.orEmpty().mapNotNull {
        if (!EntryWire.ign.matches(it.profile.name)) null else EntryPeer(it.profile.id, it.profile.name)
    }

    /** Called once per completed positive detector scan, including an empty result. */
    fun scanned(observed: List<EntryDryRunObservation>) {
        if (!supported) return
        val at = now()
        session.scan(frame(Minecraft.getInstance(), at), observed, at)
    }

    fun tick(client: Minecraft) {
        if (!supported) return
        val at = now()
        val frame = frame(client, at)
        session.retainLoaded(frame, SafariAssist.entryVisibleIdentities(), at)
        session.tick(frame, at).forEach { output ->
            when (output) {
                is EntryDryRunOutput.Notice -> if (frame.enabled || wasEnabled) {
                    record(output.text, at)
                    client.player?.sendSystemMessage(LocalChat.component("[CJ dry run +${output.elapsedMs}ms] ${output.text}."))
                }
                is EntryDryRunOutput.Party -> {
                    // No background work or queue between proposal and this current-frame check.
                    val sent = transport.send(output, { frame(client, now()) }, { profiles(client) }) {
                        checkNotNull(client.connection).sendCommand(it)
                    }
                    record(if (sent) "${if (output.message is EntryMessage.Query) "query" else "reply"} send attempted (acceptance unverified)"
                        else "proposal dropped or send uncertain; no retry", now())
                }
            }
        }
        wasEnabled = frame.enabled
    }

    private fun record(text: String, at: Long) {
        val line = "t=${at}ms arrival+${(at - environment.arrivedAt).coerceAtLeast(0)}ms $text"
        if (trace.size >= 64) trace.removeFirst()
        trace.addLast(line)
        logger.info("CJ_ENTRY_DRY_RUN {}", line)
    }

    fun debug() = listOf("CJ entry: supported=$supported setting=${ConfigManager.safariEntryDryRun} actions=disabled") +
        session.debug() + trace.toList()

    fun reset() {
        environment = EntryDryRunEnvironment(); session = EntryDryRunSession()
        transport = EntryDryRunTransport(::now)
        nextRosterAt = 0; wasEnabled = false; trace.clear()
    }
}
