package net.johnceo.sparklingmutuals

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

    private var enabled: Boolean = false
    private var delaySeconds: Int = DEFAULT_DELAY_SECONDS
    private var pendingAlert: ScheduledFuture<*>? = null

    fun toggleAlert(): Boolean {
        enabled = !enabled
        cancelPendingAlert()
        val state = if (enabled) "enabled" else "disabled"
        return enabled
    }

    fun setDelay(seconds: Int) {
        delaySeconds = seconds
    }

    fun onInitialize() {
        registerListener()
    }

    private fun registerListener() {
        ClientReceiveMessageEvents.GAME.register { message, _ ->
            val text = message.string.trim()
            if (enabled && text.contains(TRIGGER_MESSAGE)) {
                onTriggerDetected()
            }
        }
    }

    private fun onTriggerDetected() {
        cancelPendingAlert()
        pendingAlert = scheduler.schedule({
            Minecraft.getInstance().execute {
                triggerAlert()
            }
        }, delaySeconds.toLong(), TimeUnit.SECONDS)
    }

    private fun triggerAlert() {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player
        if (player == null) return
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

    private fun sendClientMessage(message: String) {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().player?.sendSystemMessage(Component.literal(message))
        }
    }
}