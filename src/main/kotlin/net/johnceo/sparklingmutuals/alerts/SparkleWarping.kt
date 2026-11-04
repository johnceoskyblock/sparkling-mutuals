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
    private var setupDeadline = 0L
    private var requestedPartyList = false
    private val setupRoster = StartPartyRoster()
    private var requestedNames: Set<String>?=null
    private var resolvedNames=emptyMap<String,String>()
    private var session=0L
    private val cleanup=ArrayDeque<String>()
    private var nextCleanupAt=0L
    private var returning=emptyList<String>()
    private var returningIds = emptySet<String>()
    private var returningNamesById = emptyMap<String, String>()
    private var returningPinned = false
    private val leech=LeechInvitation()
    internal val preparingParty get()=setupRequested || setupCleanup
    fun returningPlayers(): List<String> {
        val lookup = net.johnceo.sparklingmutuals.commands.SafariLookup
        val counts = returningIds.mapNotNull { id ->
            val name = returningNamesById[id] ?: SafariPartyInput.nameOf(id) ?: resolvedNames[id.replace("-", "")]
            val count = lookup.known(id)?.size
            if (name == null || count == null) null else name.lowercase() to count
        }.toMap()
        return returning.filter { ConfigManager.sparkleWarpMode == 0 || !it.equals(ConfigManager.warpDuo,true) }
            .sortedBy { counts[it.lowercase()] ?: Int.MAX_VALUE }
    }
    fun beginRun() { wasSafari=true }
    /** /start saves the current roster and prepares it for the next run. */
    fun prepareOnEnable() {
        setupRequested=true;setupCleanup=false;cleanupRequested=false;wasSafari=false;cleanup.clear()
        setupDeadline = System.currentTimeMillis() + 10_000
        requestedPartyList = false
        setupRoster.clear()
        nextCleanupAt = 0
        org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/warping").info("Initial party preparation requested: mode={} leech={}",ConfigManager.sparkleWarpMode,ConfigManager.leechMode)
    }
    fun cancelSetup() { setupRequested=false;setupCleanup=false;cleanup.clear();cleanupRequested=false;wasSafari=false;setupRoster.clear() }
    fun canLead(client: Minecraft): Boolean {
        val player=client.player ?: return false
        return !ConfigManager.leechMode && PartyManager.hasInfo &&
            (PartyManager.getMembers().size <= 1 || PartyManager.isLeader(player.uuid.toString()))
    }
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
                val enabled = !ConfigManager.leechMode
                setModes(!enabled && ConfigManager.warpAlertsEnabled, enabled)
                ConfigManager.save()
                it.source.sendFeedback(LocalChat.component("[SM] Leech ${if(ConfigManager.leechMode) {
                    if(WarpList.parse(ConfigManager.leechWhitelist).isEmpty()) "on. Add approved IGNs to the Leech whitelist in Sparkle Warping settings." else "on: auto-accept whitelisted invites, leaving your current party first."
                } else "off."}"));1
            })
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute {
                if (setupRequested && requestedPartyList) {
                    if (net.johnceo.sparklingmutuals.party.PartyRosterSignals.changed(message.string)) {
                        setupRoster.clear()
                        requestedPartyList = false
                    } else SafariMessages.lines(message.string).forEach(setupRoster::observe)
                }
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
    private fun prepareParty(client: Minecraft, members: List<String>? = names(client)): Boolean {
        if (members == null) return false
        val mode=SparkleWarpMode.entries[ConfigManager.sparkleWarpMode.coerceIn(0,1)]
        val plan=SparkleWarpPolicy.cleanup(mode,client.player!!.name.string,ConfigManager.warpDuo,members)
        if (plan == null) {
            client.player?.sendSystemMessage(LocalChat.component("[SM] Duo warping needs your /duo runner in the party. Set the runner, then run /start."))
            return true
        }
        if (setupRequested || (plan.commands.isNotEmpty() && !returningPinned)) {
            returning=plan.leechers
            returningPinned = setupRequested
            val self=client.player!!
            returningIds = PartyManager.withLocal(self.uuid.toString()).orEmpty() - self.uuid.toString()
            val online = client.connection?.onlinePlayers.orEmpty().associate { it.profile.id.toString() to it.profile.name }
            returningNamesById = returningIds.mapNotNull { id ->
                (online[id] ?: SafariPartyInput.nameOf(id) ?: resolvedNames[id.replace("-", "")])?.let { id to it }
            }.toMap()
            resolveReturningNames(client)
            retainPartyData(client, returningIds + self.uuid.toString())
        }
        cleanup.addAll(plan.commands)
        org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/warping").info("Party prepared: mode={} returning={} commands={}",mode,returning,plan.commands)
        return true
    }
    private fun resolveReturningNames(client: Minecraft) {
        val saved = returning
        val roster = returningIds.associateBy { it.replace("-", "") }
        val knownNames = returningNamesById.values.mapTo(mutableSetOf(), String::lowercase)
        saved.filter { it.lowercase() !in knownNames }.forEach { name ->
            java.util.concurrent.CompletableFuture.supplyAsync {
                runCatching { net.johnceo.sparklingmutuals.api.HypixelApi.getPlayerUuid(name) }.getOrNull()
            }.thenAccept { uuid -> client.execute {
                if (returning === saved) roster[uuid?.replace("-", "")]?.let { id ->
                    returningNamesById = returningNamesById + (id to name)
                }
            } }
        }
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
        if (!ConfigManager.leechMode) leech.clear() else {
            PartyManager.withLocal(client.player!!.uuid.toString())?.takeIf { it.size > 1 }?.let {
                retainPartyData(client,it)
            }
            if (leech.pending) {
                val self = client.player!!
                val members = PartyManager.getMembers()
                val inParty = if (PartyManager.hasInfo) members.isNotEmpty() else null
                val protected = if (inParty == true) {
                    val leader = members.firstOrNull(PartyManager::isLeader)
                    val name = if (leader == self.uuid.toString()) self.name.string else leader?.let { id ->
                        client.connection?.onlinePlayers?.firstOrNull { it.profile.id.toString().equals(id, true) }?.profile?.name
                            ?: SafariPartyInput.nameOf(id) ?: resolvedNames[id.replace("-", "")]
                    }
                    if (name == null && leader != null) names(client)
                    name?.let { SparkleWarpPolicy.inviteAllowed(it, ConfigManager.leechWhitelist) }
                } else false
                leech.next(now, inParty, protected)?.let { client.connection?.sendCommand(it) }
                if (leech.pending && (!PartyManager.hasInfo || inParty == true && members.none(PartyManager::isLeader)))
                    PartyManager.requestPartyInfo()
            }
        }
        if (!ConfigManager.warpAlertsEnabled || ConfigManager.leechMode) { cancelSetup();return }
        if (!net.johnceo.sparklingmutuals.JoinWelcomeState.hypixel(client.currentServer?.ip)) return
        if (setupRequested || setupCleanup) {
            if (now >= setupDeadline) {
                cancelSetup()
                client.player?.sendSystemMessage(LocalChat.component("[SM] Could not verify your party for /start. Try again."))
                return
            }
            if (!PartyManager.hasInfo) { PartyManager.requestPartyInfo();return }
            if (!canLead(client)) { cancelSetup();return }
            if (setupRequested) {
                val self = client.player!!
                val rosterSize = PartyManager.withLocal(self.uuid.toString())?.size ?: return
                val members = setupRoster.members(self.name.string, rosterSize) ?: names(client)
                if (members == null) {
                    if (!requestedPartyList) {
                        client.connection?.sendCommand("p list")
                        requestedPartyList = true
                    }
                    return
                }
                if (!prepareParty(client, members)) return
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
    internal fun setModes(warping: Boolean, leeching: Boolean): Boolean {
        if (ConfigManager.warpAlertsEnabled == warping && ConfigManager.leechMode == leeching) return false
        ConfigManager.warpAlertsEnabled = warping
        ConfigManager.leechMode = leeching
        configurationChanged()
        return true
    }
    fun configurationChanged() { AlertManager.cancelPendingAlert();cancelSetup();leech.clear() }
    fun whitelistChanged() { leech.clear() }
    fun reset() {
        net.johnceo.sparklingmutuals.commands.SafariLookup.retainWarpParty(emptySet())
        session++;cancelSetup();requestedNames=null;resolvedNames=emptyMap()
        cleanup.clear();returning=emptyList();returningIds=emptySet();returningNamesById=emptyMap();returningPinned=false;nextCleanupAt=0;leech.clear()
    }
}
