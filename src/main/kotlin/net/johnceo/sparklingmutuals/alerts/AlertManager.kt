package net.johnceo.sparklingmutuals.alerts

import net.johnceo.sparklingmutuals.JoinWelcomeState
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.SmallAlerts
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariIsland
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener

object AlertManager {

    private data class PendingWarp(val run: SafariRun, val connection: ClientPacketListener,
        val level: ClientLevel, val token: Long, val deadline: Long)
    private var pendingWarp: PendingWarp? = null
    private val expiry = WarpAlertTimer()
    private var armedRun: SafariRun? = null

    fun toggleAlert(): Boolean {
        val enabled = !ConfigManager.warpAlertsEnabled
        SparkleWarping.setModes(enabled, !enabled && ConfigManager.leechMode)
        ConfigManager.save()
        return enabled
    }

    fun setDelay(seconds: Int) = setTimings(ConfigManager.repartyDelaySeconds,seconds)
    fun setTimings(repartySeconds: Int,warpSeconds: Int) {
        val warp=warpSeconds.coerceIn(1,86400)
        val reparty=ConfigManager.validRepartyDelay(repartySeconds,warp)
        if(warp==ConfigManager.warpDelaySeconds && reparty==ConfigManager.repartyDelaySeconds)return
        ConfigManager.warpDelaySeconds=warp;ConfigManager.repartyDelaySeconds=reparty
        ConfigManager.saveSettings()
    }
    /** Explicit restart works even when the master toggle is already enabled. */
    fun startCycle() {
        SparkleWarping.setModes(warping = true, leeching = false)
        ConfigManager.save()
        cancelPendingAlert()
        SparkleWarping.prepareOnEnable()
    }

    fun tick(client: Minecraft) {
        val now = System.currentTimeMillis()
        val current = SafariTracking.ledger.current
        pendingWarp?.let { pending ->
            val run = pending.run
            if (current !== run || client.connection !== pending.connection || client.level !== pending.level ||
                client.player == null || !isEnabled() || ConfigManager.leechMode || !SafariIsland.onIsland ||
                !SafariAssist.inSafari || SafariAssist.departing || run.endedAt != null) {
                cancelPendingAlert()
            } else {
                val deadline = SparkleWarpPolicy.warpDeadline(run.startedAt, getDelay())
                val windowEnd = run.startedAt + SparkleWarpPolicy.WARP_WINDOW_MILLIS
                if ((deadline < windowEnd && now >= windowEnd) || (deadline != pending.deadline && now >= deadline)) {
                    cancelPendingAlert()
                } else if (deadline != pending.deadline) {
                    pendingWarp = pending.copy(deadline = deadline)
                    SparkleAutoParty.start(client, deadline, run.startedAt)
                } else if (now >= expiry.dueAt(deadline, run.startedAt) &&
                    AutomatedCommands.tryWarp(pending.connection) { expiry.consume(pending.token, true, run) }) {
                    pendingWarp = null
                    SparkleAutoParty.stop()
                    SmallAlerts.show(SafariHud.WARP)
                }
                return
            }
        }
        val run = current ?: return
        if (!isEnabled() || !SafariIsland.onIsland || !SafariAssist.inSafari || SafariAssist.departing ||
            run === armedRun || SparkleWarping.preparingParty) return
        val connection = client.connection ?: return
        val level = client.level ?: return
        val deadline = SparkleWarpPolicy.warpDeadline(run.startedAt, getDelay())
        if (now >= deadline) return
        if (!PartyManager.hasInfo) { PartyManager.requestPartyInfo(); return }
        val leader=SparkleWarping.canLead(client)
        if (!SparkleWarpPolicy.canStart(leader, PartyManager.getMembers().size)) return
        armedRun = run
        val scheduledGeneration = expiry.arm(run)
        SparkleWarping.beginRun()
        SparkleAutoParty.start(client, deadline, run.startedAt)
        pendingWarp = PendingWarp(run, connection, level, scheduledGeneration, deadline)
    }

    internal fun observeTravel(client: Minecraft, raw: String, joined: Set<String>) {
        val pending = pendingWarp ?: return
        if (client.connection !== pending.connection || client.level !== pending.level ||
            SafariTracking.ledger.current !== pending.run || !JoinWelcomeState.hypixel(client.currentServer?.ip)) return
        val name = SparkleWarpPolicy.travelingPlayer(raw) ?: return
        val player = client.player ?: return
        if (name.equals(player.name.string, true)) return
        val members = PartyManager.getMembers()
        if (name.lowercase() in joined || members.any { name.equals(SafariPartyInput.nameOf(it), true) } ||
            pending.connection.onlinePlayers.any { it.profile.id.toString() in members && name.equals(it.profile.name, true) })
            expiry.traveled(pending.token, System.currentTimeMillis())
    }

    fun cancelPendingAlert() {
        expiry.cancel()
        SparkleAutoParty.stop()
        pendingWarp = null
        SmallAlerts.clear(SafariHud.WARP)
    }

    fun isEnabled(): Boolean = ConfigManager.warpAlertsEnabled

    fun getDelay(): Int = ConfigManager.warpDelaySeconds
}
