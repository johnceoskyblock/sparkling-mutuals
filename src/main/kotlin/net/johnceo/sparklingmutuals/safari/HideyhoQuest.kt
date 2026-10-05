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
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen is ChatScreen) ScreenMouseEvents.allowMouseClick(screen).register { _, click ->
                click.button() != 0 || !accept()
            }
        }
    }
    private fun receive(message: Component) {
        if (!ConfigManager.hideyhoQuestClicks || !SafariAssist.inHaunted) { reset(); return }
        val text = SafariRules.strip(message.string)
        val now = System.currentTimeMillis()
        if (text.startsWith("[NPC]")) pending = null
        dialogue.observe(text, now)
        if (!text.startsWith("Select an option:")) return
        pending = null
        if (!dialogue.acceptsOptions(now)) return
        // Styled visits preserve inherited click events. Only the exact Sure button is eligible.
        val found = message.visit<ClickEvent>({ style, segment ->
            val event = style.clickEvent
            if (SafariRules.strip(segment).trim('[', ']').equals("Sure", true) &&
                (event is ClickEvent.RunCommand || event is ClickEvent.Custom)) java.util.Optional.of(event)
            else java.util.Optional.empty()
        }, net.minecraft.network.chat.Style.EMPTY)
        pending = found.orElse(null) ?: return
        offeredAt = now
        connection = Minecraft.getInstance().connection
        level = Minecraft.getInstance().level
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal("[SM] Click anywhere with chat open to accept Hideyho."))
    }
    private fun accept(): Boolean {
        val client = Minecraft.getInstance()
        val event = pending ?: return false
        if (!ConfigManager.hideyhoQuestClicks || !SafariAssist.inHaunted ||
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
        if (!ConfigManager.hideyhoQuestClicks || !SafariAssist.inHaunted || (pending != null && client.level !== level)) reset()
    }
    fun reset() { pending = null; dialogue.reset(); connection = null; level = null; offeredAt = 0 }
}
