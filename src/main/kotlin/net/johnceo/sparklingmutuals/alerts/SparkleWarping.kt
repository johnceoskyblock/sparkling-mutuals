package net.johnceo.sparklingmutuals.alerts

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.commands.PartyNameSuggestions
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput
import net.minecraft.client.Minecraft

/** Retains the leader's last leecher roster across Safari/Torrhus world transfers. */
object SparkleWarping {
    private var wasSafari=false
    private var cleanupRequested=false
    private var requestedNames: Set<String>?=null
    private var resolvedNames=emptyMap<String,String>()
    private var session=0L
    private val cleanup=ArrayDeque<String>()
    private var nextCleanupAt=0L
    private var returning=emptyList<String>()
    private val leech=LeechInvitation()
    fun returningPlayers()=returning.filter { ConfigManager.sparkleWarpMode == 0 || !it.equals(ConfigManager.warpDuo,true) }
    fun beginRun() { wasSafari=true }
    fun canLead(client: Minecraft): Boolean {
        val player=client.player ?: return false
        return !ConfigManager.leechMode && PartyManager.hasInfo &&
            (PartyManager.getMembers().size <= 1 || PartyManager.isLeader(player.uuid.toString()))
    }
    fun canWarp(client: Minecraft) = canLead(client) && (ConfigManager.sparkleWarpMode == 0 ||
        ConfigManager.warpDuo.isNotEmpty() && names(client)?.any { it.equals(ConfigManager.warpDuo,true) } == true)
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(ClientCommands.literal("duo")
                .executes { it.source.sendFeedback(LocalChat.component("[SM] Duo runner: ${ConfigManager.warpDuo.ifEmpty { "not set" }}. Use /duo <IGN>."));1 }
                .then(ClientCommands.argument("ign",StringArgumentType.word()).suggests { _, builder -> PartyNameSuggestions.sparkling(builder) }.executes {
                    val name=StringArgumentType.getString(it,"ign")
                    if (!Regex("[A-Za-z0-9_]{1,16}").matches(name) || name.equals(Minecraft.getInstance().player?.name?.string,true)) {
                        it.source.sendError(LocalChat.component("[SM] Enter your duo runner's IGN."));return@executes 0
                    }
                    ConfigManager.warpDuo=name;ConfigManager.sparkleWarpMode=1
                    configurationChanged();ConfigManager.save()
                    it.source.sendFeedback(LocalChat.component("[SM] Duo warping: $name. /warping toggles the cycle."));1
                }))
            dispatcher.register(ClientCommands.literal("leech").executes {
                ConfigManager.leechMode=!ConfigManager.leechMode
                configurationChanged();ConfigManager.save()
                it.source.sendFeedback(LocalChat.component("[SM] Leech ${if(ConfigManager.leechMode) "on: auto-accept all party invites, leaving your current party first." else "off."}"));1
            })
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute {
                if (!ConfigManager.leechMode) return@execute
                val client=Minecraft.getInstance()
                if (!net.johnceo.sparklingmutuals.JoinWelcomeState.hypixel(client.currentServer?.ip)) return@execute
                SparkleWarpPolicy.inviter(message.string)?.let { leech.offer(it,System.currentTimeMillis());PartyManager.requestPartyInfo() }
                if (SafariMessages.lines(message.string).any { it.trim() in setOf("You left the party.","You are not currently in a party.") }) leech.leftParty()
            }
        }
    }
    private fun names(client: Minecraft): List<String>? {
        val self=client.player ?: return null
        val roster=PartyManager.withLocal(self.uuid.toString()) ?: return null
        val direct=client.connection?.onlinePlayers.orEmpty().associate { it.profile.id.toString().replace("-","") to it.profile.name }
        val found=roster.associateWith { id ->
            if (id.equals(self.uuid.toString(),true)) self.name.string else
                direct[id.replace("-","")] ?: SafariPartyInput.nameOf(id) ?: resolvedNames[id.replace("-","")]
        }
        if (found.values.all { it != null }) return found.values.filterNotNull()
        if (requestedNames != roster) {
            requestedNames=roster
            val revision=session
            PartyNameSuggestions.namesByIdAsync().thenAccept { resolved -> client.execute {
                if (revision == session) { resolvedNames=resolvedNames+resolved;requestedNames=null }
            } }
        }
        return null
    }
    fun tick(client: Minecraft) {
        // Transfers/respawns can briefly have no player; the disconnect hook owns session resets.
        if (client.player == null || client.connection == null) return
        val now=System.currentTimeMillis()
        if (!ConfigManager.leechMode) leech.clear() else {
            val inParty=if (PartyManager.hasInfo) PartyManager.getMembers().size > 1 || PartyManager.isLeader(client.player!!.uuid.toString()) else null
            leech.next(now,inParty)?.let { client.connection?.sendCommand(it) }
            if (leech.pending && !PartyManager.hasInfo) PartyManager.requestPartyInfo()
        }
        if (!ConfigManager.warpAlertsEnabled || ConfigManager.leechMode) { wasSafari=false;cleanupRequested=false;cleanup.clear();return }
        if (SafariAssist.inSafari) {
            if (PartyManager.hasInfo && !canLead(client)) wasSafari=false
            cleanupRequested=false;cleanup.clear()
            if (canLead(client)) names(client) // Warm verified names before the next transfer.
            return
        }
        val inCanyon=SkyblockSidebar.areaName(client) in setOf("Torrhus Canyon","Torrhus Springs")
        if (!inCanyon) return
        if (wasSafari) { wasSafari=false;cleanupRequested=true }
        if (!PartyManager.hasInfo) { if (cleanupRequested || cleanup.isNotEmpty()) PartyManager.requestPartyInfo();return }
        if (!canLead(client)) { cleanupRequested=false;cleanup.clear();return }
        if (cleanupRequested) {
            val members=names(client) ?: return
            val mode=SparkleWarpMode.entries[ConfigManager.sparkleWarpMode.coerceIn(0,1)]
            val plan=SparkleWarpPolicy.cleanup(mode,client.player!!.name.string,ConfigManager.warpDuo,members)
            cleanupRequested=false
            if (plan == null) {
                client.player?.sendSystemMessage(LocalChat.component("[SM] Duo warping needs your /duo runner in the party."));return
            }
            if (plan.commands.isNotEmpty()) returning=plan.leechers
            cleanup.addAll(plan.commands)
        }
        if (cleanup.isNotEmpty() && now >= nextCleanupAt) {
            client.connection?.sendCommand(cleanup.removeFirst());nextCleanupAt=now+750
        }
    }
    fun configurationChanged() { AlertManager.cancelPendingAlert();cleanup.clear();cleanupRequested=false;leech.clear() }
    fun reset() {
        session++;wasSafari=false;cleanupRequested=false;requestedNames=null;resolvedNames=emptyMap()
        cleanup.clear();returning=emptyList();nextCleanupAt=0;leech.clear()
    }
}
