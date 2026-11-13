package net.johnceo.sparklingmutuals.alerts

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.minecraft.client.Minecraft

object SparkleAutoParty {
    private val invitations = WarpInvitations()
    private val joined = mutableSetOf<String>()
    private var run: Any? = null
    private var level: Any? = null
    private var connection: Any? = null
    private var windowStartedAt = 0L
    private val returningInvites = ReturningPartyInvites()
    private var detectedRun: net.johnceo.sparklingmutuals.safari.SafariRun?=null
    private val earlyDetections=linkedSetOf<String>()
    fun register() {
        ClientSendMessageEvents.COMMAND.register { command ->
            if (command.trim().matches(Regex("(?:p|party)\\s+warp", RegexOption.IGNORE_CASE))) stop()
        }
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(ClientCommands.literal("autoparty").executes {
                ConfigManager.autoParty = !ConfigManager.autoParty
                ConfigManager.save()
                if (!ConfigManager.autoParty && !SparkleWarpPolicy.returningPhase(windowStartedAt,System.currentTimeMillis())) invitations.discardPending()
                it.source.sendFeedback(LocalChat.component("[SM] Auto-party ${if (ConfigManager.autoParty) "on" else "off"}."))
                1
            })
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay && net.johnceo.sparklingmutuals.safari.automation.SafariDowntime.partyWarpMessage(message.string))
                Minecraft.getInstance().execute { stop() }
            if (!overlay) SafariMessages.lines(message.string).forEach { line ->
                if (line == "You cannot invite that player since they're not online.") Minecraft.getInstance().execute {
                    val now = System.currentTimeMillis()
                    if (!SparkleWarpPolicy.returningPhase(windowStartedAt, now)) invitations.rejectedOffline(now)
                }
                Regex("^(?:\\[[^]\\r\\n]+]\\s*)*([A-Za-z0-9_]{1,16}) joined the party[.!]$")
                    .matchEntire(line)?.groupValues?.get(1)?.let { name ->
                        Minecraft.getInstance().execute { joined.add(name.lowercase()) }
                    }
                Regex("^(?:\\[[^]\\r\\n]+]\\s*)*([A-Za-z0-9_]{1,16}) has left the party[.!]$")
                    .matchEntire(line)?.groupValues?.get(1)?.let { name ->
                        Minecraft.getInstance().execute { joined.remove(name.lowercase()) }
                    }
            }
        }
    }
    fun start(client: Minecraft, deadline: Long, startedAt: Long = System.currentTimeMillis()) {
        val discoveries=earlyDetections.takeIf { detectedRun===SafariTracking.ledger.current }?.toList().orEmpty()
        stop()
        run = SafariTracking.ledger.current
        level = client.level
        connection = client.connection
        invitations.start(deadline)
        windowStartedAt = startedAt
        discoveries.forEach(::detected)
    }
    fun detected(name: String) {
        val current=SafariTracking.ledger.current
        if(detectedRun!==current) { detectedRun=current;earlyDetections.clear() }
        if(current!=null && ConfigManager.autoParty && ConfigManager.warpAlertsEnabled) earlyDetections.add(name)
        if (ConfigManager.autoParty && ConfigManager.warpAlertsEnabled && SafariAssist.inSafari &&
            run === SafariTracking.ledger.current && !SparkleWarpPolicy.returningPhase(windowStartedAt,System.currentTimeMillis()))
            invitations.add(name, WarpPlayerStatus.ordered(name))
    }
    fun tick(client: Minecraft) {
        val now = System.currentTimeMillis()
        WarpListRefresh.tick(client,now)
        WarpPlayerStatus.tick(client, now)
        if (invitations.deadline == 0L) return
        if (!ConfigManager.warpAlertsEnabled || !SafariAssist.inSafari || client.level !== level ||
            client.connection !== connection || SafariTracking.ledger.current !== run ||
            SafariTracking.ledger.current?.endedAt != null) { stop(); return }
        if (now >= invitations.deadline) { stop(); return }
        if (SparkleWarping.preparingParty) return
        if (!PartyManager.hasInfo) { PartyManager.requestPartyInfo(); return }
        val player = client.player ?: return
        val members = PartyManager.getMembers().toSet()
        // A confirmed solo player can create a party; existing parties require leadership.
        if (!SparkleWarping.canLead(client)) { stop(); return }
        if (ConfigManager.sparkleWarpMode == 1 && ConfigManager.warpDuo.isBlank()) { stop();return }
        if (!SparkleWarpPolicy.returningPhase(windowStartedAt,now) && !ConfigManager.autoParty) invitations.discardPending()
        val names = client.connection?.onlinePlayers.orEmpty()
            .filter { it.profile.id.toString() in members }.map { it.profile.name.lowercase() }.toSet()
        val cached = members.mapNotNull(net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput::nameOf)
            .map(String::lowercase)
        val present=names + cached + joined + player.name.string.lowercase()
        if (ConfigManager.sparkleWarpMode == 1 && ConfigManager.warpDuo.lowercase() !in present) return
        if (SparkleWarpPolicy.returningPhase(windowStartedAt,now)) {
            invitations.discardPending()
            val handler = client.connection ?: return
            returningInvites.next(now, invitations.deadline, SparkleWarping.returningPlayers(), present, members.size)?.let {
                net.johnceo.sparklingmutuals.alerts.AutomatedCommands.send(handler,it)
            }
            return
        }
        if (maxOf(members.size,present.size) >= SparkleWarpPolicy.PARTY_SIZE) return
        invitations.next(now, present) { candidate ->
            WarpPlayerStatus.eligible(candidate,now) && earlyDetections.any { critter -> WarpList.selected(candidate,critter) }
        }?.let {
            client.connection?.let { handler -> net.johnceo.sparklingmutuals.alerts.AutomatedCommands.send(handler,"p $it") }
        }
    }
    fun stop() { invitations.stop(); joined.clear(); run = null; level = null; connection = null;windowStartedAt=0;returningInvites.reset() }
}
