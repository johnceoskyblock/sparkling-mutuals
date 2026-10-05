package net.johnceo.sparklingmutuals.safari

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket

/** Click-anywhere quest acceptance adapted from ShinyHunter's QuestAccepter (MIT). */
object HideyhoQuest {
    private val dialogue = HideyhoPromptWindow()
    private var pending: ClickEvent? = null
    private var offeredAt = 0L
    private var connection: Any? = null
    private var level: Any? = null

    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { receive(message) }
        }
        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ ->
            Minecraft.getInstance().execute { receive(message) }
        }
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen is ChatScreen) ScreenMouseEvents.allowMouseClick(screen).register { _, _ ->
                !accept()
            }
        }
    }
    private fun receive(message: Component) {
        if (!ConfigManager.hideyhoQuestClicks) { reset(); return }
        val client = Minecraft.getInstance()
        if (hasContext() && !sameContext(client)) reset()
        val text = SafariRules.strip(message.string)
        val now = System.currentTimeMillis()
        if (SafariRules.isDialogue(text)) pending = null
        dialogue.observe(text, now, client.level, client.connection)
        if (SafariRules.isHideyhoDialogue(text)) { connection = client.connection; level = client.level }
        if (!text.startsWith("Select an option:")) return
        pending = null
        if (!dialogue.acceptsOptions(now)) return
        pending = HideyhoButton.find(message) ?: return
        offeredAt = now
        connection = Minecraft.getInstance().connection
        level = Minecraft.getInstance().level
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal("[SM] Click anywhere with chat open to accept Hideyho."))
    }
    private fun accept(): Boolean {
        val client = Minecraft.getInstance()
        val event = pending ?: return false
        if (!ConfigManager.hideyhoQuestClicks ||
            client.connection !== connection || client.level !== level || System.currentTimeMillis() - offeredAt > 30000) { reset(); return false }
        val active = client.connection ?: return false
        reset()
        when (event) {
            is ClickEvent.RunCommand -> active.sendCommand(event.command().removePrefix("/"))
            is ClickEvent.Custom -> active.send(ServerboundCustomClickActionPacket(event.id(), event.payload()))
            else -> return false
        }
        client.player?.sendSystemMessage(Component.literal("[SM] Hideyho quest accepted."))
        return true
    }
    fun onClientTick(client: Minecraft) {
        dialogue.synchronize(client.level, client.connection)
        if (!ConfigManager.hideyhoQuestClicks || (hasContext() && !sameContext(client)) ||
            (pending != null && System.currentTimeMillis() - offeredAt > 30000)) reset()
    }
    private fun hasContext() = connection != null || level != null
    private fun sameContext(client: Minecraft) = client.connection === connection && client.level === level
    fun reset() { pending = null; dialogue.reset(); connection = null; level = null; offeredAt = 0 }
}
