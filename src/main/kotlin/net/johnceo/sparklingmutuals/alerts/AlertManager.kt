package net.johnceo.sparklingmutuals.alerts

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object AlertManager {

    private const val DEFAULT_DELAY_SECONDS = 300
    private const val TRIGGER_MESSAGE = "HOTSPOT! Your Hunting Hotspot is"

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var enabled = false
    private var delaySeconds = DEFAULT_DELAY_SECONDS
    private var pendingAlert: ScheduledFuture<*>? = null

    fun toggleAlert(): Boolean {
        enabled = !enabled
        cancelPendingAlert()
        return enabled
    }

    fun setDelay(seconds: Int) {
        delaySeconds = seconds
    }

    fun onInitialize() {
        ClientReceiveMessageEvents.GAME.register { message, _ ->
            if (enabled && message.string.trim().contains(TRIGGER_MESSAGE)) {
                scheduleAlert()
            }
        }
    }

    private fun scheduleAlert() {
        cancelPendingAlert()
        pendingAlert = scheduler.schedule({
            Minecraft.getInstance().execute(::triggerAlert)
        }, delaySeconds.toLong(), TimeUnit.SECONDS)
    }

    private fun triggerAlert() {
        val minecraft = Minecraft.getInstance()
        if (minecraft.player == null) return

        val message = Component.literal("WARP REMINDER").withStyle(ChatFormatting.RED)
        minecraft.gui.setTitle(message)
        minecraft.gui.setOverlayMessage(message, false)
    }

    private fun cancelPendingAlert() {
        pendingAlert?.cancel(false)
        pendingAlert = null
    }

    fun isEnabled(): Boolean = enabled

    fun getDelay(): Int = delaySeconds
}