package net.johnceo.sparklingmutuals.safari

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import com.mojang.blaze3d.platform.InputConstants
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.Items
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.lwjgl.glfw.GLFW

/**
 * Guards the manager, tickets and entry GUI after a kick, during downtime, or while party members are absent.
 * Only on the Safari island itself, never at the Critter Safari zone on Torrhus Canyon.
 * Sneaking goes anyway. The right-click is cancelled in SafariManagerMixin, since Fabric fires no client event for it.
 */
object SafariManagerGuard {
    const val HOLD_MILLIS = 30_000L
    private const val MESSAGE_COOLDOWN = 3_000L
    private const val MANAGER = "Safari Manager"
    private const val TICKET = "Critter Safari Ticket"
    /** "kick" as its own word, so "kickstart" doesn't count. */
    private val kickWord = Regex("\\bkick\\b", RegexOption.IGNORE_CASE)
    var heldUntil = 0L
        private set
    private var messagedAt = 0L
    private val presence = SafariEntryPresence()
    private val runState = SafariEntryRunState()
    private var requestedRosterAt = 0L

    private fun inRun(client: Minecraft): Boolean {
        val player = client.player ?: return false
        return runState.active(client.level, SafariIsland.onIsland, SafariAssist.location,
            SafariAreaMap.areaAt(player.x, player.y, player.z))
    }

    fun onClientTick(client: Minecraft) {
        val player = client.player
        val connection = client.connection
        val now = System.currentTimeMillis()
        val activeRun = inRun(client)
        val waiting = ConfigManager.holdManagerOnKick && SafariIsland.onIsland && !activeRun
        if (!waiting || player == null || connection == null) { presence.reset(); return }
        if (!PartyManager.hasInfo &&
            (requestedRosterAt == 0L || now - requestedRosterAt >= 5_000L)) {
            requestedRosterAt = now
            PartyManager.requestPartyInfo()
        }
        val changed = presence.update(now, waiting, false, client.level,
            PartyManager.withLocal(player.uuid.toString()),
            connection.onlinePlayers.mapTo(HashSet()) { it.profile.id.toString() },
            player.uuid.toString())
        if (changed && presence.missing.isNotEmpty()) {
            messagedAt = now
            player.sendSystemMessage(LocalChat.component(
                "[SM] Blocked Safari entry: ${presence.missing.size} party member(s) missing from this instance. Shift-click to go anyway."))
        }
    }

    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { chat(message.string, System.currentTimeMillis()) }
        }
        AttackEntityCallback.EVENT.register { player, _, _, entity, _ ->
            if (player === Minecraft.getInstance().player && shouldBlock(entity)) InteractionResult.FAIL else InteractionResult.PASS
        }
        UseItemCallback.EVENT.register { player, _, hand ->
            val client = Minecraft.getInstance()
            val name = SafariRules.strip(player.getItemInHand(hand).hoverName.string)
            if (player === client.player && name.contains(TICKET, true) && blocks(client)) InteractionResult.FAIL else InteractionResult.PASS
        }
    }

    fun callsKick(text: String) = PartyChat.parse(text)?.let { kickWord.containsMatchIn(it.body) } == true
    fun chat(text: String, now: Long) {
        if (!ConfigManager.holdManagerOnKick || !callsKick(text)) return
        heldUntil = now + HOLD_MILLIS
        Minecraft.getInstance()?.player?.sendSystemMessage(LocalChat.component(
            "[SM] Kick called: Safari Manager held for ${HOLD_MILLIS / 1000}s. Sneak to go anyway."))
    }
    fun holding(now: Long = System.currentTimeMillis()) = now < heldUntil
    fun reset() { heldUntil = 0L; messagedAt = 0L; requestedRosterAt = 0L; presence.reset(); runState.reset() }

    /** Whether clicking this entity should be refused. Cheap for everything that isn't the manager. */
    fun shouldBlock(entity: Entity?): Boolean {
        val client = Minecraft.getInstance()
        return entity != null && isManager(client, entity) && blocks(client)
    }
    /** The screenshot's exact entry menu, never an ordinary inventory's name tags or its close button. */
    fun entryItem(title: String, nameTag: Boolean, playerInventory: Boolean) =
        SafariRules.strip(title).trim().equals("Critter Safari Entry", true) && nameTag && !playerInventory

    fun shouldBlockEntry(containerId: Int, slotId: Int): Boolean {
        val client = Minecraft.getInstance()
        val player = client.player ?: return false
        val screen = client.screen as? AbstractContainerScreen<*> ?: return false
        val menu = player.containerMenu
        if (menu.containerId != containerId) return false
        val slot = menu.slots.getOrNull(slotId) ?: return false
        if (!entryItem(screen.title.string, slot.item.`is`(Items.NAME_TAG), slot.container === player.inventory)) return false
        // GUI Shift-click does not necessarily set the player's sneaking flag.
        val shift = InputConstants.isKeyDown(client.window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
            InputConstants.isKeyDown(client.window, GLFW.GLFW_KEY_RIGHT_SHIFT)
        return blocks(client, shift)
    }

    private fun blocks(client: Minecraft, shiftOverride: Boolean? = null): Boolean {
        val player = client.player ?: return false
        // Recheck immediately before entry as well as each tick, so late arrivals release the GUI too.
        onClientTick(client)
        val kick = ConfigManager.holdManagerOnKick && holding()
        val downtime = SafariDowntime.holding // false whenever the downtime setting is off
        val missing = ConfigManager.holdManagerOnKick && presence.missing.isNotEmpty()
        if (!SafariEntryPresence.blocks(SafariIsland.onIsland, inRun(client), kick, downtime, missing, false)) return false
        if (!SafariEntryPresence.blocks(true, false, kick, downtime, missing, shiftOverride ?: player.isShiftKeyDown)) {
            // Deliberate override; it also spends the downtime hold, which is a one-shot.
            if (downtime) SafariDowntime.clear()
            return false
        }
        val now = System.currentTimeMillis()
        if (now - messagedAt > MESSAGE_COOLDOWN) {
            messagedAt = now
            val reason = if (downtime) "${SafariDowntime.holders()} asked for downtime"
                else if (missing) "${presence.missing.size} party member(s) missing from this instance"
                else "a kick was called (${(heldUntil - now + 999) / 1000}s left)"
            player.sendSystemMessage(LocalChat.component("[SM] Blocked: $reason. Shift-click to go anyway."))
        }
        return true
    }
    /** Hypixel NPCs carry their visible name on separate nameplate stands, so those nearby count too. */
    private fun isManager(client: Minecraft, entity: Entity): Boolean {
        fun named(e: Entity) = SafariRules.strip(e.customName?.string ?: e.name.string).contains(MANAGER, true)
        if (named(entity)) return true
        return client.level?.getEntities(entity, entity.boundingBox.inflate(3.0))?.any(::named) == true
    }
}
