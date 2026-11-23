package net.johnceo.sparklingmutuals.safari

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.compat.smSetScreen
import net.johnceo.sparklingmutuals.safari.hideyho.HideyhoButton
import net.johnceo.sparklingmutuals.safari.hideyho.HideyhoPromptWindow
import net.johnceo.sparklingmutuals.config.HideyhoKeySetupScreen
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/** Each shortcut performs a single action in response to a key press. */
internal object StandardSafariKeys {
    private val prompt = HideyhoPromptWindow()
    private data class Offer(val event: ClickEvent, val run: SafariRun?, val world: Any?, val connection: Any?, val at: Long)
    private var offer: Offer? = null
    private var hideyhoKey: KeyMapping? = null
    private var setupOffered = false

    fun register() {
        val category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("sparkling-mutuals", "safari"))
        val hideyho = KeyMappingHelper.registerKeyMapping(
            KeyMapping("key.sparkling-mutuals.accept_hideyho", InputConstants.UNKNOWN.value, category)
        )
        hideyhoKey = hideyho
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!setupOffered && hideyho.isUnbound && client.smScreen is TitleScreen) openSetup(client)
            val hideyhoPressed = pressed(hideyho)
            if (hideyhoPressed && client.player != null && client.connection != null && client.smScreen == null && SafariAssist.inSafari)
                acceptHideyho(client)
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) receiveOnClient(message)
        }
        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ -> receiveOnClient(message) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            prompt.reset()
            offer = null
        }
    }

    fun openSetup(client: Minecraft) {
        val key = hideyhoKey ?: return
        setupOffered = true
        client.smSetScreen(HideyhoKeySetupScreen(client.smScreen, key))
    }

    private fun pressed(key: KeyMapping): Boolean {
        val pressed = key.consumeClick()
        while (key.consumeClick()) { }
        return pressed
    }

    private fun receiveOnClient(message: Component) {
        val client = Minecraft.getInstance()
        client.execute {
            if (!SafariAssist.inSafari) return@execute
            val now = System.currentTimeMillis()
            val plain = SafariRules.strip(message.string)
            if (SafariRules.isDialogue(plain)) offer = null
            prompt.observe(plain, now, client.level, client.connection)
            if (!plain.startsWith("Select an option:")) return@execute
            offer = HideyhoButton.find(message)?.takeIf { prompt.acceptsOptions(now) }
                ?.let { Offer(it, SafariTracking.ledger.current, client.level, client.connection, now) }
        }
    }

    private fun acceptHideyho(client: Minecraft) {
        val active = offer
        offer = null
        if (active == null || active.run !== SafariTracking.ledger.current || active.world !== client.level || active.connection !== client.connection ||
            System.currentTimeMillis() - active.at !in 0..30_000) {
            feedback(client, "Talk to Hideyho first to open the quest prompt.")
            return
        }
        prompt.reset()
        ChatPromptClick.accept(active.event, client)
    }

    private fun feedback(client: Minecraft, text: String) {
        client.player?.sendSystemMessage(LocalChat.component("[SM] $text"))
    }

    private class ChatPromptClick : Screen(Component.empty()) {
        companion object {
            fun accept(event: ClickEvent, client: Minecraft) = defaultHandleGameClickEvent(event, client, null)
        }
    }
}
