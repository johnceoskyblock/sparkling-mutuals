package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.azureaaron.hmapi.events.HypixelPacketEvents
import net.azureaaron.hmapi.network.packet.v1.s2c.LocationUpdateS2CPacket
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.JoinWelcomeState
import net.johnceo.sparklingmutuals.compat.smScreen
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
import net.johnceo.sparklingmutuals.sync.SafariPartySync
import net.johnceo.sparklingmutuals.sync.PartySyncProtocol

/** Opt-in Sparkling auto entry. Positive coordination uses only the existing shared WebSocket. */
object CjsSafariEntry {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/cj-entry")
    private val origin = System.nanoTime()
    private fun now() = (System.nanoTime() - origin) / 1_000_000
    private var supported = false
    private var environment = EntryDryRunEnvironment()
    private var admission = CjsAdmissionState()
    private var warp = CjsWarpGate()
    private val approach = CjsApproachInput()
    private var flow=CjsEntryFlow()
    private val entryInput=CjsEntryInput(approach)
    private var lastApproachWarp=-1L
    private var lastStatus=""
    private var nextRosterAt = 0L
    private val trace = ArrayDeque<String>()

    fun register() {
        supported = FabricLoader.getInstance().getModContainer("minecraft").orElse(null)
            ?.metadata?.version?.friendlyString in setOf("26.1.2", "26.2", "26.3")
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

    private fun sync(client: Minecraft, at: Long) {
        environment.world(client.connection, client.level, at)
        admission.world(client.level)
        warp.world(client.level,at)
    }

    private fun frame(client: Minecraft, at: Long): EntryDryRunFrame {
        sync(client, at)
        val enabled = supported && ConfigManager.safariEntryAutoEnter
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
        return admission.frame(context, enabled, mapped, SafariAssist.inSafari && mapped in 0..4, environment.arrivedAt)
    }

    private fun receive(text: String) {
        if (!supported) return
        val client = Minecraft.getInstance()
        val at = now()
        frame(client, at)
        admission.chat(text)
        // Observing a later genuine warp retires old cancellation ownership even while switched off.
        if (JoinWelcomeState.hypixel(client.currentServer?.ip)) warp.observe(text,at)
    }

    private fun profiles(client: Minecraft) = client.connection?.onlinePlayers.orEmpty().mapNotNull {
        if (!EntryWire.ign.matches(it.profile.name)) null else EntryPeer(it.profile.id, it.profile.name)
    }

    private fun signal(client: Minecraft, base: EntryDryRunFrame): CjsRoomSignal? {
        val c=base.context ?: return null
        val own=PartySyncProtocol.canonicalLobby(c.server) ?: return null
        val shared=SafariIsland.serverName?.let(PartySyncProtocol::canonicalLobby) ?: return null
        if(!own.equals(shared,true)) return null
        return SafariPartySync.entryRoom(client)?.let { CjsRoomSignal(c,it.generation,it.active) }
    }
    fun tick(client: Minecraft) {
        if(supported) frame(client,now()) // Lifecycle only. All CJ inputs run at START.
    }
    fun manualInput() {
        if(!supported || !flow.suppressesAutomaticMenus) return
        if(flow.inputEvent(true)) approach.release(Minecraft.getInstance())
    }
    /** Called only by actual Minecraft input callbacks, never KeyMapping writes. */
    fun gameplayInput(window: Long, action: Int) {
        if(!supported || action<=0 || !flow.suppressesAutomaticMenus) return
        val client=Minecraft.getInstance()
        if(window==client.window.handle() && client.smScreen==null) manualInput()
    }
    fun suppressesAutomaticMenu(client: Minecraft): Boolean {
        val claim = flow.claimedContext ?: return false
        return supported && environment.matchesWorld(claim, client.connection, client.level)
    }

    /** Drain known room changes and revalidate before vanilla consumes walking input. */
    fun startTick(client: Minecraft) {
        if(!supported) return
        val at=now()
        fun base()=frame(client,now())
        val initial=frame(client,at)
        val allowed=warp.allows(initial.context,profiles(client),at)
        if(allowed && lastApproachWarp!=warp.generation) {
            approach.reset(client);lastApproachWarp=warp.generation
        }
        fun current(): CjsEntryFrame {
            val b=base()
            return entryInput.frame(client,b,signal(client,b),ConfigManager.safariEntryAutoEnter,
                warp.allows(b.context,profiles(client),now()),warp.generation,flow)
        }
        try {
            if(ConfigManager.safariEntryAutoEnter || flow.suppressesAutomaticMenus) {
                val f=current()
                when(val effect=flow.tick(f,at)) {
                    is CjsEntryEffect.Approach -> {
                        val room=SafariPartySync.entryRoom(client)
                        val driven=room!=null && room.generation==effect.generation && SafariPartySync.withEntryRoom(client,room) {
                            val fresh=current()
                            if(!flow.matches(effect) || !fresh.safe || !fresh.guardAllows || !fresh.enabled ||
                                fresh.context!=effect.context || fresh.signal?.generation!=effect.generation ||
                                fresh.signal.active.isEmpty() || !fresh.warpAllowed) false
                            else {
                                approach.tick(client,base(),fresh.signal,true,true,warp.generation,now()) { record(it,now()) }
                                true
                            }
                        }
                        if(!driven) { flow.cancel("Approach permission withdrawn");approach.release(client) }
                    }
                    is CjsEntryEffect.Manager, is CjsEntryEffect.Ticket ->
                        entryInput.dispatch(client,flow,effect,::base,::current,now())
                    null -> approach.release(client)
                }
                if(flow.status!=lastStatus) {
                    lastStatus=flow.status;record(lastStatus,at)
                    if (flow.confirmedAutomaticAdmission) {
                        client.player?.sendSystemMessage(LocalChat.component("[SM] Safari ticket automatically used."))
                        PartyManager.sendPartyChat(client, "[SM] In the run!")
                    }
                }
            } else approach.release(client)
        } catch(error: Exception) {
            flow.cancel("Automatic entry cancelled: ${error.javaClass.simpleName}")
            approach.release(client);record(flow.status,now())
        }
    }

    private fun record(text: String, at: Long) {
        val line = "t=${at}ms arrival+${(at - environment.arrivedAt).coerceAtLeast(0)}ms $text"
        if (trace.size >= 64) trace.removeFirst()
        trace.addLast(line)
        logger.info("CJ_ENTRY {}", line)
    }

    fun debug() = listOf("CJ entry: supported=$supported sparklingAutoEntry=${ConfigManager.safariEntryAutoEnter}",
        "CJ phase=${flow.phase} status=${flow.status} genericMenuSuppressed=${flow.suppressesAutomaticMenus} warp=${warp.generation}") + trace.toList()

    fun reset() {
        approach.reset(Minecraft.getInstance());admission=CjsAdmissionState();warp=CjsWarpGate()
        environment=EntryDryRunEnvironment();flow=CjsEntryFlow();lastApproachWarp=-1;lastStatus=""
        nextRosterAt=0;trace.clear()
    }
}
