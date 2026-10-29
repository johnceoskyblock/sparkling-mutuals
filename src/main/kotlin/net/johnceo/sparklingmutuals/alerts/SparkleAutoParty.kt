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
    fun register() {
        ClientSendMessageEvents.COMMAND.register { command ->
            if (command.trim().matches(Regex("(?:p|party)\\s+warp", RegexOption.IGNORE_CASE))) stop()
        }
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(ClientCommands.literal("autoparty").executes {
                ConfigManager.autoParty = !ConfigManager.autoParty
                ConfigManager.save()
                if (!ConfigManager.autoParty) invitations.discardPending()
                it.source.sendFeedback(LocalChat.component("[SM] Auto-party ${if (ConfigManager.autoParty) "on" else "off"}."))
                1
            })
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay && net.johnceo.sparklingmutuals.safari.automation.SafariDowntime.partyWarpMessage(message.string))
                Minecraft.getInstance().execute { stop() }
            if (!overlay) SafariMessages.lines(message.string).forEach { line ->
                Regex("^(?:\\[[^]\\r\\n]+]\\s*)*([A-Za-z0-9_]{1,16}) joined the party[.!]$")
                    .matchEntire(line)?.groupValues?.get(1)?.let { name ->
                        Minecraft.getInstance().execute { joined.add(name.lowercase()) }
                    }
            }
        }
    }
    fun start(client: Minecraft, deadline: Long) {
        stop()
        run = SafariTracking.ledger.current
        level = client.level
        connection = client.connection
        invitations.start(deadline)
    }
    fun detected(name: String) {
        if (ConfigManager.autoParty && ConfigManager.warpAlertsEnabled && SafariAssist.inSafari &&
            run === SafariTracking.ledger.current) invitations.add(name, WarpList.names(name))
    }
    fun tick(client: Minecraft) {
        if (invitations.deadline == 0L) return
        if (!ConfigManager.warpAlertsEnabled || !SafariAssist.inSafari || client.level !== level ||
            client.connection !== connection || SafariTracking.ledger.current !== run ||
            SafariTracking.ledger.current?.endedAt != null) { stop(); return }
        if (!ConfigManager.autoParty) { invitations.discardPending(); return }
        val now = System.currentTimeMillis()
        if (now >= invitations.deadline) { stop(); return }
        if (!PartyManager.hasInfo) { PartyManager.requestPartyInfo(); return }
        val player = client.player ?: return
        val members = PartyManager.getMembers().toSet()
        // A confirmed solo player can create a party; existing parties require leadership.
        if (members.size > 1 && !PartyManager.isLeader(player.uuid.toString())) { stop(); return }
        val names = client.connection?.onlinePlayers.orEmpty()
            .filter { it.profile.id.toString() in members }.map { it.profile.name.lowercase() }.toSet()
        val cached = members.mapNotNull(net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput::nameOf)
            .map(String::lowercase)
        invitations.next(now, names + cached + joined + player.name.string.lowercase())?.let {
            client.connection?.sendCommand("p $it")
        }
    }
    fun stop() { invitations.stop(); joined.clear(); run = null; level = null; connection = null }
}
