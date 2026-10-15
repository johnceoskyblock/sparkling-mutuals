package net.johnceo.sparklingmutuals.alerts

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SmallAlerts
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.minecraft.client.Minecraft
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object AlertManager {

    internal fun isTrigger(raw: String) = net.johnceo.sparklingmutuals.safari.SafariMessages.lines(raw).any {
        Regex("^HOTSPOT! Your Hunting Hotspot is the (?:Forest|Cavern|Icy|Haunted) Biome!$").matches(it)
    }

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "Sparkling Mutuals warp reminder").apply { isDaemon = true }
    }

    private var pendingAlert: ScheduledFuture<*>? = null
    private val expiry = WarpAlertTimer()

    fun toggleAlert(): Boolean {
        ConfigManager.warpAlertsEnabled = !ConfigManager.warpAlertsEnabled
        ConfigManager.save()
        cancelPendingAlert()
        return ConfigManager.warpAlertsEnabled
    }

    fun setDelay(seconds: Int) {
        ConfigManager.warpDelaySeconds = seconds.coerceIn(1, 86400)
        ConfigManager.save()
        cancelPendingAlert()
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
        net.johnceo.sparklingmutuals.safari.SafariTracking.ensureRun(client)
        val run = net.johnceo.sparklingmutuals.safari.SafariTracking.ledger.current ?: return
        val scheduledGeneration = expiry.arm(run)
        pendingAlert = scheduler.schedule({
            client.execute {
                val ready = client.connection === connection && client.level === level && client.player != null &&
                    isEnabled() && net.johnceo.sparklingmutuals.safari.SafariAssist.inSafari &&
                    net.johnceo.sparklingmutuals.safari.SafariTracking.ledger.current?.let { it.endedAt == null } == true
                if (expiry.consume(scheduledGeneration, ready, net.johnceo.sparklingmutuals.safari.SafariTracking.ledger.current)) {
                    pendingAlert = null
                    triggerAlert()
                }
            }
        }, getDelay().toLong(), TimeUnit.SECONDS)
    }

    private fun triggerAlert() {
        val minecraft = Minecraft.getInstance()
        if (minecraft.player == null || !isEnabled()) return

        SmallAlerts.show(SafariHud.WARP)
        minecraft.connection?.sendCommand("p warp")
    }

    fun cancelPendingAlert() {
        expiry.cancel()
        pendingAlert?.cancel(false)
        pendingAlert = null
        SmallAlerts.clear(SafariHud.WARP)
    }

    fun isEnabled(): Boolean = ConfigManager.warpAlertsEnabled

    fun getDelay(): Int = ConfigManager.warpDelaySeconds
}
