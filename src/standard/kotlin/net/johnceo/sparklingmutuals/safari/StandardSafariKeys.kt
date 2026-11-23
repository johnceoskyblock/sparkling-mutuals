package net.johnceo.sparklingmutuals.safari

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.alerts.WarpList
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.compat.smSetScreen
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.hideyho.HideyhoButton
import net.johnceo.sparklingmutuals.safari.hideyho.HideyhoPromptWindow
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters
import net.johnceo.sparklingmutuals.sync.PartySyncProtocol
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/** Each shortcut performs a single action in response to a key press. */
internal object StandardSafariKeys {
    private val prompt = HideyhoPromptWindow()
    private data class Offer(val event: ClickEvent, val run: SafariRun?, val world: Any?, val connection: Any?, val at: Long)
    private var offer: Offer? = null
    private var inviteRun: SafariRun? = null
    private var inviteKey: KeyMapping? = null
    private val invited = mutableSetOf<String>()
    private val inviteCommand = Regex("(?:p|party) invite ([A-Za-z0-9_]{1,16})", RegexOption.IGNORE_CASE)

    fun register() {
        val category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("sparkling-mutuals", "safari"))
        fun key(name: String) = KeyMappingHelper.registerKeyMapping(
            KeyMapping("key.sparkling-mutuals.$name", InputConstants.UNKNOWN.value, category)
        )
        val invite = key("invite_sparkling").also { inviteKey = it }
        val warp = key("warp_party")
        val hideyho = key("accept_hideyho")
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // Physical press hooks handle invites. Drain vanilla's repeat click counter.
            pressed(invite)
            val warpPressed = pressed(warp)
            val hideyhoPressed = pressed(hideyho)
            if (client.player == null || client.connection == null || client.smScreen != null || !SafariAssist.inSafari)
                return@register
            when {
                hideyhoPressed -> acceptHideyho(client)
                warpPressed -> client.smSetScreen(ChatScreen("/party warp", false))
            }
        }
        ClientSendMessageEvents.COMMAND.register { command ->
            inviteCommand.matchEntire(command.trim())?.let {
                currentInviteRun()
                invited.add(it.groupValues[1].lowercase())
            }
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) receiveOnClient(message)
        }
        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ -> receiveOnClient(message) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            prompt.reset()
            offer = null
            inviteRun = null
            invited.clear()
        }
    }

    fun inviteOnPress(window: Long, event: KeyEvent) {
        if (inviteKey?.matches(event) == true) inviteIfReady(window)
    }

    fun inviteOnPress(window: Long, event: MouseButtonEvent) {
        if (inviteKey?.matchesMouse(event) == true) inviteIfReady(window)
    }

    private fun inviteIfReady(window: Long) {
        val client = Minecraft.getInstance()
        if (window == client.window.handle() && client.isWindowActive &&
            client.smScreen == null && SafariAssist.inSafari) inviteNext(client)
    }

    private fun pressed(key: KeyMapping): Boolean {
        val pressed = key.consumeClick()
        while (key.consumeClick()) { }
        return pressed
    }

    private fun currentInviteRun(): SafariRun? {
        val run = SafariTracking.ledger.current
        if (inviteRun !== run) {
            inviteRun = run
            invited.clear()
        }
        return run
    }

    private fun inviteNext(client: Minecraft) {
        val connection = client.connection ?: return
        val self = client.player?.name?.string ?: return
        val run = currentInviteRun()?.takeIf { it.endedAt == null } ?: return
        if (System.currentTimeMillis() - run.startedAt >= 30_000) {
            feedback(client, "The Safari warp window has closed.")
            return
        }
        val critter = SparklingEncounters.visibleCritter()
        if (critter == null) {
            feedback(client, "Look at a visible sparkling critter to invite a player.")
            return
        }
        val members = PartyManager.getMembers().mapNotNull(PartySyncProtocol::uuid).toSet()
        val profiles = connection.onlinePlayers.associate { it.profile.name.lowercase() to it.profile.id.toString().replace("-", "") }
        fun uuid(name: String) = profiles[name.lowercase()] ?: WarpList.uuid(name)
        val candidate = WarpList.names(critter)
            .filterNot { it.equals(self, true) || it.lowercase() in invited || uuid(it) in members }
            .minWithOrNull(compareBy<String> { WarpList.priority(it, critter) }
                .thenBy { uuid(it) ?: "~" }.thenBy(String::lowercase))
        if (candidate == null) feedback(client, "No remaining invite-list players for $critter.")
        else {
            invited.add(candidate.lowercase())
            connection.sendCommand("party invite $candidate")
        }
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
