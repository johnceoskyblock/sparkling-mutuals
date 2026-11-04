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
        ConfigManager.warpAlertsEnabled = !ConfigManager.warpAlertsEnabled
        ConfigManager.save()
        cancelPendingAlert()
        SparkleWarping.cancelSetup()
        return ConfigManager.warpAlertsEnabled
    }

    fun setDelay(seconds: Int) {
        ConfigManager.warpDelaySeconds = seconds.coerceIn(1, 86400)
        ConfigManager.save()
        cancelPendingAlert()
    }
    /** Explicit restart works even when the master toggle is already enabled. */
    fun startCycle() {
        ConfigManager.warpAlertsEnabled = true
        ConfigManager.save()
        cancelPendingAlert()
        SparkleWarping.prepareOnEnable()
    }

    fun tick(client: Minecraft) {
        val run = SafariTracking.ledger.current ?: return
        if (!isEnabled() || !SafariIsland.onIsland || !SafariAssist.inSafari || run === armedRun || SparkleWarping.preparingParty) return
        val connection = client.connection ?: return
        val level = client.level ?: return
        val deadline = SparkleWarpPolicy.warpDeadline(run.startedAt, getDelay())
        if (System.currentTimeMillis() >= deadline) return
        if (!PartyManager.hasInfo) { PartyManager.requestPartyInfo(); return }
        if (SparkleWarping.contestPaused(client) ||
            !SparkleWarpPolicy.canStart(SparkleWarping.canLead(client), PartyManager.getMembers().size)) return
        cancelPendingAlert()
        armedRun = run
        val scheduledGeneration = expiry.arm(run)
        SparkleWarping.beginRun()
        SparkleAutoParty.start(client, deadline, run.startedAt)
        pendingAlert = scheduler.schedule({
            client.execute {
                val ready = client.connection === connection && client.level === level && client.player != null &&
                    isEnabled() && SparkleWarping.canWarp(client) && SafariAssist.inSafari &&
                    SafariTracking.ledger.current?.let { it.endedAt == null } == true
                if (expiry.consume(scheduledGeneration, ready, SafariTracking.ledger.current)) {
                    pendingAlert = null
                    triggerAlert()
                }
            }
        }, (deadline-System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
    }

    private fun triggerAlert() {
        SparkleAutoParty.stop()
        val minecraft = Minecraft.getInstance()
        if (minecraft.player == null || !isEnabled() || !SparkleWarping.canWarp(minecraft)) return

        SmallAlerts.show(SafariHud.WARP)
        minecraft.connection?.sendCommand("p warp")
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
