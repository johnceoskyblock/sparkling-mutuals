package net.johnceo.sparklingmutuals.alerts

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.SmallAlerts
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.minecraft.client.Minecraft

object AlertManager {

    internal fun isTrigger(raw: String) = SafariMessages.lines(raw).any {
        Regex("^HOTSPOT! Your Hunting Hotspot is the (?:Forest|Cavern|Icy|Haunted) Biome!$").matches(it)
    }

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "Sparkling Mutuals warp reminder").apply { isDaemon = true }
    }

    private var pendingAlert: ScheduledFuture<*>? = null
    private val expiry = WarpAlertTimer()
    private var scheduleRevision = 0L

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

    fun onInitialize() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay && isEnabled() && isTrigger(message.string)) Minecraft.getInstance().execute { scheduleAlert() }
        }
    }

    private fun scheduleAlert() {
        cancelPendingAlert()
        val client = Minecraft.getInstance()
        val connection = client.connection ?: return
        val level = client.level ?: return
        SafariTracking.ensureRun(client)
        val run = SafariTracking.ledger.current ?: return
        val revision = scheduleRevision
        val startedAt = System.currentTimeMillis()
        val deadline = startedAt + getDelay() * 1000L
        net.johnceo.sparklingmutuals.party.PartyManager.withPartyInfo {
            if (revision != scheduleRevision || client.connection !== connection || client.level !== level ||
                SafariTracking.ledger.current !== run || !isEnabled() || SparkleWarping.contestPaused(client) ||
                !SparkleWarpPolicy.canStart(SparkleWarping.canLead(client),net.johnceo.sparklingmutuals.party.PartyManager.getMembers().size) ||
                System.currentTimeMillis() >= deadline) return@withPartyInfo
            val scheduledGeneration = expiry.arm(run)
            SparkleWarping.beginRun()
            SparkleAutoParty.start(client, deadline, startedAt)
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
    }

    private fun triggerAlert() {
        SparkleAutoParty.stop()
        val minecraft = Minecraft.getInstance()
        if (minecraft.player == null || !isEnabled() || !SparkleWarping.canWarp(minecraft)) return

        SmallAlerts.show(SafariHud.WARP)
        minecraft.connection?.sendCommand("p warp")
    }

    fun cancelPendingAlert() {
        scheduleRevision++
        expiry.cancel()
        SparkleAutoParty.stop()
        pendingAlert?.cancel(false)
        pendingAlert = null
        SmallAlerts.clear(SafariHud.WARP)
    }

    fun isEnabled(): Boolean = ConfigManager.warpAlertsEnabled

    fun getDelay(): Int = ConfigManager.warpDelaySeconds
}
