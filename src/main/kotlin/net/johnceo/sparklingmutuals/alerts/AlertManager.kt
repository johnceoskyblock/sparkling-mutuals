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

    private const val TRIGGER_MESSAGE = "HOTSPOT! Your Hunting Hotspot is"

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "Sparkling Mutuals warp reminder").apply { isDaemon = true }
    }

    private var pendingAlert: ScheduledFuture<*>? = null
    private var generation = 0L

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
        ClientReceiveMessageEvents.GAME.register { message, _ ->
            if (isEnabled() && message.string.trim().contains(TRIGGER_MESSAGE)) {
                scheduleAlert()
            }
        }
    }

    private fun scheduleAlert() {
        cancelPendingAlert()
        val client = Minecraft.getInstance()
        val connection = client.connection
        val scheduledGeneration = generation
        pendingAlert = scheduler.schedule({
            client.execute {
                if (scheduledGeneration == generation && client.connection === connection) triggerAlert()
            }
        }, getDelay().toLong(), TimeUnit.SECONDS)
    }

    private fun triggerAlert() {
        val minecraft = Minecraft.getInstance()
        if (minecraft.player == null || !isEnabled()) return

        SmallAlerts.show(SafariHud.WARP)
    }

    fun cancelPendingAlert() {
        generation++
        pendingAlert?.cancel(false)
        pendingAlert = null
        SmallAlerts.clear(SafariHud.WARP)
    }

    fun isEnabled(): Boolean = ConfigManager.warpAlertsEnabled

    fun getDelay(): Int = ConfigManager.warpDelaySeconds
}
