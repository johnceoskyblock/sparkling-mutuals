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
    private var setupRequested=false
    private var setupCleanup=false
    private var requestedNames: Set<String>?=null
    private var resolvedNames=emptyMap<String,String>()
    private var session=0L
    private val cleanup=ArrayDeque<String>()
    private var nextCleanupAt=0L
    private var returning=emptyList<String>()
    private val leech=LeechInvitation()
    private val contestReminder=WarpContestReminder()
    internal val preparingParty get()=setupRequested || setupCleanup
    fun returningPlayers()=returning.filter { ConfigManager.sparkleWarpMode == 0 || !it.equals(ConfigManager.warpDuo,true) }
    fun beginRun() { wasSafari=true }
    /** Only /start explicitly prepares a full party before the first run. */
    fun prepareOnEnable() {
        setupRequested=true;setupCleanup=false;cleanupRequested=false;wasSafari=false;cleanup.clear()
        org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/warping").info("Initial party preparation requested: mode={} leech={}",ConfigManager.sparkleWarpMode,ConfigManager.leechMode)
    }
    fun cancelSetup() { setupRequested=false;setupCleanup=false;cleanup.clear();cleanupRequested=false;wasSafari=false }
    fun canLead(client: Minecraft): Boolean {
        val player=client.player ?: return false
        return !ConfigManager.leechMode && PartyManager.hasInfo &&
            (PartyManager.getMembers().size <= 1 || PartyManager.isLeader(player.uuid.toString()))
    }
    fun canWarp(client: Minecraft) = canLead(client) && (ConfigManager.sparkleWarpMode == 0 ||
        ConfigManager.warpDuo.isNotEmpty() && names(client)?.any { it.equals(ConfigManager.warpDuo,true) } == true)
    fun contestPaused(client: Minecraft): Boolean = SparkleWarpPolicy.contestPause(
        ConfigManager.warpAlertsEnabled,ConfigManager.leechMode,
        if (PartyManager.hasInfo) canLead(client) else null,
        net.johnceo.sparklingmutuals.contest.ContestTracker.hasUncommon())
    fun retainingRequirements(client: Minecraft) = ConfigManager.warpAlertsEnabled && !ConfigManager.leechMode &&
        (!PartyManager.hasInfo || canLead(client)) && net.johnceo.sparklingmutuals.commands.SafariLookup.retainedWarpParty().isNotEmpty()
    fun requirementMembers(client: Minecraft, actual: Set<String>): Set<String> =
        if (retainingRequirements(client)) SparkleWarpPolicy.requirementRoster(actual,
            net.johnceo.sparklingmutuals.commands.SafariLookup.retainedWarpParty().mapTo(linkedSetOf()) { raw ->
                java.util.UUID.fromString(raw.replace(Regex("(.{8})(.{4})(.{4})(.{4})(.{12})"),"$1-$2-$3-$4-$5")).toString()
            }) else actual
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
                it.source.sendFeedback(LocalChat.component("[SM] Leech ${if(ConfigManager.leechMode) {
                    if(WarpList.parse(ConfigManager.leechWhitelist).isEmpty()) "on. Add approved IGNs to the Leech whitelist in Sparkle Warping settings." else "on: auto-accept whitelisted invites, leaving your current party first."
                } else "off."}"));1
            })
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute {
                if (!ConfigManager.leechMode) return@execute
                val client=Minecraft.getInstance()
                if (!net.johnceo.sparklingmutuals.JoinWelcomeState.hypixel(client.currentServer?.ip)) return@execute
                SparkleWarpPolicy.inviter(message.string)?.takeIf { SparkleWarpPolicy.inviteAllowed(it,ConfigManager.leechWhitelist) }
                    ?.let { leech.offer(it,System.currentTimeMillis());PartyManager.requestPartyInfo() }
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
    /** Resolve and save the roster before sending any disband/kick command. False means wait. */
    private fun prepareParty(client: Minecraft): Boolean {
        val members=names(client) ?: return false
        val mode=SparkleWarpMode.entries[ConfigManager.sparkleWarpMode.coerceIn(0,1)]
        val plan=SparkleWarpPolicy.cleanup(mode,client.player!!.name.string,ConfigManager.warpDuo,members)
        if (plan == null) {
            client.player?.sendSystemMessage(LocalChat.component("[SM] Duo warping needs your /duo runner in the party. Set the runner, then run /start."))
            return true
        }
        if (plan.commands.isNotEmpty()) {
            returning=plan.leechers
            val self=client.player!!
            retainPartyData(client,PartyManager.withLocal(self.uuid.toString()).orEmpty())
        }
        cleanup.addAll(plan.commands)
        org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/warping").info("Party prepared: mode={} returning={} commands={}",mode,returning,plan.commands)
        return true
    }
    private fun retainPartyData(client: Minecraft, members: Set<String>) {
        val self=client.player ?: return
        val lookup=net.johnceo.sparklingmutuals.commands.SafariLookup
        lookup.useAccount(self.uuid.toString()+":"+net.johnceo.sparklingmutuals.history.HistoryServerPolicy.alpha(client))
        lookup.retainWarpParty(members)
    }
    private fun sendCleanup(client: Minecraft, now: Long) {
        if (cleanup.isNotEmpty() && now >= nextCleanupAt) {
            client.connection?.sendCommand(cleanup.removeFirst());nextCleanupAt=now+750
        }
    }
    fun tick(client: Minecraft) {
        // Transfers/respawns can briefly have no player; the disconnect hook owns session resets.
        if (client.player == null || client.connection == null) return
        val now=System.currentTimeMillis()
        val onHypixel=net.johnceo.sparklingmutuals.JoinWelcomeState.hypixel(client.currentServer?.ip)
        val leader=if (PartyManager.hasInfo) canLead(client) else null
        if (contestReminder.poll(net.johnceo.sparklingmutuals.contest.ContestTracker.absoluteWindow(java.time.LocalDateTime.now()),
                onHypixel && ConfigManager.warpAlertsEnabled && !ConfigManager.leechMode,leader,
                PartyManager.getMembers().size)) {
            client.connection?.sendCommand("pc New contest! Grab contest between warps while the party is disbanded.")
        }
        if (!ConfigManager.leechMode) leech.clear() else {
            PartyManager.withLocal(client.player!!.uuid.toString())?.takeIf { it.size > 1 }?.let {
                retainPartyData(client,it)
            }
            val inParty=if (PartyManager.hasInfo) PartyManager.getMembers().size > 1 || PartyManager.isLeader(client.player!!.uuid.toString()) else null
            leech.next(now,inParty)?.let { client.connection?.sendCommand(it) }
            if (leech.pending && !PartyManager.hasInfo) PartyManager.requestPartyInfo()
        }
        if (!ConfigManager.warpAlertsEnabled || ConfigManager.leechMode) { cancelSetup();return }
        if (!net.johnceo.sparklingmutuals.JoinWelcomeState.hypixel(client.currentServer?.ip)) return
        if (setupRequested || setupCleanup) {
            if (!PartyManager.hasInfo) { PartyManager.requestPartyInfo();return }
            if (!canLead(client)) { cancelSetup();return }
            if (setupRequested) {
                if (!prepareParty(client)) return
                setupRequested=false;setupCleanup=cleanup.isNotEmpty()
            }
            if (setupCleanup) {
                sendCleanup(client,now)
                setupCleanup=cleanup.isNotEmpty()
                return
            }
        }
        if (SafariAssist.inSafari) {
            if (PartyManager.hasInfo) wasSafari=canLead(client)
            cleanupRequested=false;cleanup.clear()
            if (canLead(client)) names(client) // Warm verified names before the next transfer.
            return
        }
        val inCanyon=net.johnceo.sparklingmutuals.safari.SafariIsland.currentMode.equals("foraging_3",true) ||
            SkyblockSidebar.areaName(client) in setOf("Torrhus Canyon","Torrhus Springs")
        if (!inCanyon) return
        if (wasSafari) { wasSafari=false;cleanupRequested=true }
        if (!PartyManager.hasInfo) { if (cleanupRequested || cleanup.isNotEmpty()) PartyManager.requestPartyInfo();return }
        if (!canLead(client)) { cleanupRequested=false;cleanup.clear();return }
        if (cleanupRequested) {
            if (!prepareParty(client)) return
            cleanupRequested=false
        }
        sendCleanup(client,now)
    }
    fun configurationChanged() { AlertManager.cancelPendingAlert();cancelSetup();leech.clear() }
    fun whitelistChanged() { leech.clear() }
    fun reset() {
        net.johnceo.sparklingmutuals.commands.SafariLookup.retainWarpParty(emptySet())
        session++;cancelSetup();requestedNames=null;resolvedNames=emptyMap();contestReminder.reset()
        cleanup.clear();returning=emptyList();nextCleanupAt=0;leech.clear()
    }
}
