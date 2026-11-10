package net.johnceo.sparklingmutuals.alerts

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.SmallAlerts
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariIsland
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.party.PartyManager
import net.minecraft.client.Minecraft

object AlertManager {

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "Sparkling Mutuals warp reminder").apply { isDaemon = true }
    }

    private var pendingAlert: ScheduledFuture<*>? = null
    private val expiry = WarpAlertTimer()
    private var armedRun: SafariRun? = null

    fun toggleAlert(): Boolean {
        val enabled = !ConfigManager.warpAlertsEnabled
        SparkleWarping.setModes(enabled, !enabled && ConfigManager.leechMode)
        ConfigManager.save()
        return enabled
    }

    fun setDelay(seconds: Int) {
        ConfigManager.warpDelaySeconds = seconds.coerceIn(1, 86400)
        ConfigManager.save()
        cancelPendingAlert()
    }
    /** Explicit restart works even when the master toggle is already enabled. */
    fun startCycle() {
        SparkleWarping.setModes(warping = true, leeching = false)
        ConfigManager.save()
        cancelPendingAlert()
        SparkleWarping.prepareOnEnable()
    }

    fun tick(client: Minecraft) {
        val run = SafariTracking.ledger.current ?: return
        if (!isEnabled() || !SafariIsland.onIsland || !SafariAssist.inSafari || SafariAssist.departing ||
            run === armedRun || SparkleWarping.preparingParty) return
        val connection = client.connection ?: return
        val level = client.level ?: return
        val deadline = SparkleWarpPolicy.warpDeadline(run.startedAt, getDelay())
        if (System.currentTimeMillis() >= deadline) return
        if (!PartyManager.hasInfo) { PartyManager.requestPartyInfo(); return }
        if (!SparkleWarpPolicy.canStart(SparkleWarping.canLead(client), PartyManager.getMembers().size)) return
        cancelPendingAlert()
        armedRun = run
        val scheduledGeneration = expiry.arm(run)
        SparkleWarping.beginRun()
        SparkleAutoParty.start(client, deadline, run.startedAt)
        pendingAlert = scheduler.schedule({
            client.execute {
                val current = SafariTracking.ledger.current
                val active = client.connection === connection && client.level === level && client.player != null &&
                    isEnabled() && !ConfigManager.leechMode && SafariIsland.onIsland &&
                    SafariAssist.inSafari && !SafariAssist.departing && run.endedAt == null
                // The roster authorizes arming. Hypixel handles party permissions when the command is sent.
                if (expiry.consume(scheduledGeneration, active, current)) {
                    pendingAlert = null
                    SparkleAutoParty.stop()
                    net.johnceo.sparklingmutuals.alerts.AutomatedCommands.send(connection,"p warp")
                    SmallAlerts.show(SafariHud.WARP)
                }
            }
        }, (deadline - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
    }

    fun cancelPendingAlert() {
        expiry.cancel()
        SparkleAutoParty.stop()
        pendingAlert?.cancel(false)
        pendingAlert = null
        SmallAlerts.clear(SafariHud.WARP)
    }

    fun isEnabled(): Boolean = ConfigManager.warpAlertsEnabled

    fun getDelay(): Int = ConfigManager.warpDelaySeconds
}
