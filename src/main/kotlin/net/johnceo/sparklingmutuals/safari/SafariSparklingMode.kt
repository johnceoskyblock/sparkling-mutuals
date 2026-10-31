package net.johnceo.sparklingmutuals.safari

import java.util.concurrent.Executors
import java.util.concurrent.Future
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.api.ApiFailure
import net.johnceo.sparklingmutuals.api.ApiProvider
import net.johnceo.sparklingmutuals.api.ApiRetryWindow
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.commands.SafariLookup
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.minecraft.client.Minecraft

/** A partial or different-party lookup must never claim a discovery is shared by everyone. */
/** HMAPI roster refreshes run on the client thread; profile requests run on a daemon worker. */
object SafariSparklingMode {
    val state = PartySparklingState()
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "Sparkling mode lookups").apply { isDaemon = true } }
    private var task: Future<*>? = null
    private var generation = 0L
    private var roster = emptySet<String>()
    private var provider: ApiProvider? = null
    private var partyRefreshAt = 0L
    private var attemptedAt = 0L
    private var warning: String? = null
    private val retry = ApiRetryWindow()
    fun requestRefresh() { invalidate() }
    private fun local(client: Minecraft, message: String) {
        if (warning == message) return
        warning = message
        client.player?.sendSystemMessage(LocalChat.component("[SM] $message"))
    }
    private fun invalidate() { generation++; task?.cancel(false); task = null; attemptedAt = 0; retry.reset() }
    fun tick(client: Minecraft) {
        if (client.player == null || client.connection == null) return
        if (SafariLookup.useAccount(client.player!!.uuid.toString() + ":" + net.johnceo.sparklingmutuals.history.HistoryServerPolicy.alpha(client))) {
            invalidate(); state.reset(); roster = emptySet()
        }
        val active = SafariAssist.location == SafariLocation.INSIDE || ConfigManager.sparklingMode
        val currentProvider = HypixelApi.provider
        if (provider != currentProvider) {
            invalidate(); state.reset(); provider = currentProvider; warning = null
        }
        if (!active) return
        if (!currentProvider.enabled) {
            if (ConfigManager.sparklingMode) local(client, "No Hypixel API key. Use /apikey or settings.")
            return
        }
        val now = System.currentTimeMillis()
        if (now - partyRefreshAt >= 60_000) {
            partyRefreshAt = now
            PartyManager.requestPartyInfo()
        }
        // A confirmed empty roster is solo. Unknown membership cannot be treated as solo.
        val actualMembers = PartyManager.withLocal(client.player!!.uuid.toString())
        if (actualMembers == null) {
            if (net.johnceo.sparklingmutuals.alerts.SparkleWarping.retainingRequirements(client)) {
                PartyManager.requestPartyInfo();return
            }
            if (roster.isNotEmpty()) { invalidate(); roster = emptySet(); state.reset() }
            PartyManager.requestPartyInfo()
            return
        }
        val members = net.johnceo.sparklingmutuals.alerts.SparkleWarping.requirementMembers(client,actualMembers)
        if (members != roster) { invalidate(); roster = members; warning = null }
        SafariLookup.selectParty(members, currentProvider)
        state.select(members, currentProvider)
        val cached = members.mapNotNull { id -> SafariLookup.known(id, currentProvider)?.let { id to it } }.toMap()
        if (cached.size == members.size) {
            state.accept(members, cached, currentProvider)
            retry.reset(); warning = null
            return
        }
        if (task != null ||
            if (retry.pending) !retry.due(now, currentProvider) else now - attemptedAt < 60_000) return
        retry.reset()
        attemptedAt = now
        val revision = generation
        val connection = client.connection
        task = executor.submit {
            val result = runCatching { members.associateWith { SafariLookup.discoveries(it, currentProvider) } }
            client.execute {
                if (revision != generation) return@execute
                // This worker is finished even if a transient roster refresh makes its result unusable.
                task = null
                if (client.connection !== connection || HypixelApi.provider != currentProvider) return@execute
                if (client.player?.uuid?.toString()?.let(PartyManager::withLocal)?.let {
                        net.johnceo.sparklingmutuals.alerts.SparkleWarping.requirementMembers(client,it)
                    } != members) {
                    retry.failed(System.currentTimeMillis(), currentProvider)
                    return@execute
                }
                result.fold({ discoveries ->
                    if (state.accept(members, discoveries.mapValues { (id, value) -> SafariLookup.known(id, currentProvider) ?: value }, currentProvider)) { warning = null }
                    else { retry.failed(System.currentTimeMillis(), currentProvider) }
                }, { failure ->
                    retry.failed(System.currentTimeMillis(), currentProvider)
                    local(client, (failure as? ApiFailure)?.message ?: ApiFailure.unavailable().message!!)
                })
            }
        }
    }
    /** Server-confirmed catch; returns the local account's pre-catch discovery status for history. */
    fun caught(player: String, species: String): Boolean? {
        val client = Minecraft.getInstance()
        val self = client.player ?: return null
        if (!SafariAssist.inSafari) return null
        val members = PartyManager.withLocal(self.uuid.toString()) ?: return null
        members.firstOrNull { uuid ->
            val name = if (uuid == self.uuid.toString()) self.name.string else
                client.connection?.getPlayerInfo(java.util.UUID.fromString(uuid))?.profile?.name
                    ?: net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput.nameOf(uuid)
            name.equals(player, true)
        } ?: return null
        SafariLookup.useAccount(self.uuid.toString() + ":" + net.johnceo.sparklingmutuals.history.HistoryServerPolicy.alpha(client))
        val participants = members.filterTo(mutableSetOf()) { uuid ->
            uuid == self.uuid.toString() || client.connection?.getPlayerInfo(java.util.UUID.fromString(uuid)) != null
        }
        val before = SafariLookup.caught(members, species, participants)
        HypixelApi.recordSparkling(participants,species)
        val requirements=net.johnceo.sparklingmutuals.alerts.SparkleWarping.requirementMembers(client,members)
        state.select(requirements)
        val updated = requirements.mapNotNull { id -> SafariLookup.known(id)?.let { id to it } }.toMap()
        state.accept(requirements, updated)
        if (!net.johnceo.sparklingmutuals.history.HistoryServerPolicy.alpha(client))
            net.johnceo.sparklingmutuals.history.OwnStats.sparkling(species)
        val key = species.uppercase(java.util.Locale.ROOT).replace(' ', '_')
        return before[self.uuid.toString()]?.contains(key)
    }
    fun reset() {
        invalidate(); state.reset(); roster = emptySet(); provider = null; partyRefreshAt = 0
        warning = null
    }
}
