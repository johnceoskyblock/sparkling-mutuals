package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items
import org.slf4j.LoggerFactory

/** The same decision runs before every Manager, ticket and entry-menu packet boundary. */
object SafariManagerGuard {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/entry-guard")
    private val presence = SafariEntryPresence()
    private val runState = SafariEntryRunState()
    private val kick = EntryKickHold()
    private val managerTargets = EntryManagerTargets()
    private var requestedRosterAt = 0L
    private var messagedAt = 0L
    private var lastDecision = ""
    fun register() { SafariPartyInput.register(); SafariTicketSelect.register() }
    fun activeRun(client: Minecraft): Boolean {
        val player = client.player ?: return false
        return runState.active(client.level, SafariIsland.onIsland, SafariAssist.location,
            SafariAreaMap.areaAt(player.x, player.y, player.z))
    }
    private fun stage() = when {
        SafariIsland.onIsland -> EntryStage.SAFARI
        SafariIsland.currentMode.equals("foraging_3", true) -> EntryStage.TORRHUS
        SafariIsland.currentMode == null -> EntryStage.UNKNOWN
        else -> EntryStage.OTHER
    }
    fun inSafariInstance() = stage() == EntryStage.SAFARI
    fun onClientTick(client: Minecraft) {
        SafariPartyInput.tick(client)
        if (!ConfigManager.holdManagerOnKick) kick.reset()
        val player = client.player ?: return
        val connection = client.connection ?: return
        val now = System.currentTimeMillis()
        val safari = inSafariInstance()
        if (safari && ConfigManager.holdManagerOnKick && !PartyManager.hasInfo && now - requestedRosterAt > 5000) {
            requestedRosterAt = now; PartyManager.requestPartyInfo()
        }
        val roster = PartyManager.withLocal(player.uuid.toString())
        val present = connection.listedOnlinePlayers.mapTo(HashSet()) { it.profile.id.toString() }
        presence.update(now, safari, false, client.level, roster, present, player.uuid.toString())
        kick.observe(client.level)
    }
    fun callsKick(text: String) = PartyChat.parse(text)?.body?.contains("kick", true) == true
    /** Sender identity is verified before this callback. Use receipt time, not delayed lookup time. */
    fun partyKick(chat: PartyChat, receivedAt: Long = System.currentTimeMillis()) {
        val client = Minecraft.getInstance()
        if (!ConfigManager.holdManagerOnKick || !inSafariInstance() || !chat.body.contains("kick", true)) return
        kick.report(client.level, receivedAt)
    }
    fun reset() { messagedAt = 0; requestedRosterAt = 0; lastDecision = ""; presence.reset(); runState.reset(); kick.reset(); managerTargets.reset() }
    fun entryItem(title: String, nameTag: Boolean, playerInventory: Boolean) =
        SafariRules.strip(title).trim().equals("Critter Safari Entry", true) && nameTag && !playerInventory
    private fun entrySlot(containerId: Int, slotId: Int): Slot? {
        val client = Minecraft.getInstance()
        val player = client.player ?: return null
        val screen = client.smScreen as? AbstractContainerScreen<*> ?: return null
        val menu = player.containerMenu
        if (menu.containerId != containerId) return null
        val slot = menu.slots.getOrNull(slotId) ?: return null
        return slot.takeIf { entryItem(screen.title.string, it.item.`is`(Items.NAME_TAG), it.container === player.inventory) }
    }
    internal fun ticketInput(input: ContainerInput, shift: Boolean, entry: Boolean): ContainerInput =
        if (shift && entry && input == ContainerInput.QUICK_MOVE) ContainerInput.PICKUP else input
    fun entryInput(input: ContainerInput, containerId: Int, slotId: Int): ContainerInput =
        ticketInput(input, shiftDown(Minecraft.getInstance()), entrySlot(containerId, slotId) != null)
    fun shouldBlockEntry(containerId: Int, slotId: Int): Boolean {
        val slot = entrySlot(containerId, slotId) ?: return false
        val blocked = blocks(Minecraft.getInstance(), "entry menu")
        if (!blocked) SafariTracking.ticketSelected(slot.item.hoverName.string)
        return blocked
    }
    fun shouldBlockTicket(player: Player, hand: InteractionHand): Boolean {
        val client = Minecraft.getInstance()
        if (player !== client.player) return false
        val name = SafariRules.strip(player.getItemInHand(hand).hoverName.string)
        if (!name.contains("Critter Safari Ticket", true)) return false
        val blocked = blocks(client, "ticket $hand")
        if (!blocked) SafariTracking.ticketSelected(name)
        return blocked
    }
    fun shouldBlock(entity: Entity?): Boolean {
        val client = Minecraft.getInstance()
        if (entity == null || !isManager(client, entity)) return false
        val blocked = blocks(client, "manager ${entity.id}")
        if (!blocked) SafariTicketSelect.managerClicked(client)
        return blocked
    }
    private fun blocks(client: Minecraft, route: String): Boolean {
        val player = client.player ?: return false
        onClientTick(client)
        val guard = ConfigManager.holdManagerOnKick
        val downtime = SafariDowntime.entryHold
        val active = activeRun(client)
        val blocked = (guard || ConfigManager.downtimeCommand) && EntryGuardDecision.block(stage(),
            downtime, guard && kick.active, !guard || presence.ready,
            when (stage()) {
                EntryStage.TORRHUS -> ConfigManager.downtimeCommand && SafariPartyInput.pendingDowntime
                EntryStage.SAFARI -> guard && SafariPartyInput.pendingKick
                else -> SafariPartyInput.pending
            }, shiftDown(client))
        val reason = when (stage()) {
            EntryStage.TORRHUS -> if (SafariPartyInput.overflowReadiness) "Everyone must say r or Ticket earned! in party chat"
                else if (SafariPartyInput.pendingDowntime) "Checking party readiness" else "Waiting for ${SafariDowntime.waitingFor()}"
            EntryStage.UNKNOWN -> "Waiting for server info"
            else -> when {
                !PartyManager.hasInfo -> "Loading party members"
                !presence.ready -> missingMessage(presence.missing, SafariPartyInput::nameOf)
                kick.active -> "Kick reported; waiting 2 seconds"
                else -> "Waiting for party readiness"
            }
        }
        val detail = "$route: blocked=$blocked stage=${stage()} run=$active guard=$guard downtime=$downtime " +
            "rosterKnown=${PartyManager.hasInfo} presenceReady=${presence.ready} missing=${presence.missing} " +
            "kick=${kick.active} identityPending=${SafariPartyInput.pending} reason=$reason"
        if (detail != lastDecision) { logger.info(detail); lastDecision = detail }
        val now = System.currentTimeMillis()
        if (blocked && now - messagedAt >= 3000) {
            messagedAt = now
            player.sendSystemMessage(LocalChat.component("[SM] Safari: $reason. Hold Shift to enter anyway."))
        }
        return blocked
    }
    internal fun missingMessage(missing: Set<String>, nameOf: (String) -> String?): String {
        if (missing.isEmpty()) return "Waiting for the party to load"
        val names = missing.map { nameOf(it) ?: "name loading" }.sortedBy { it.lowercase() }
        return "${missing.size} missing: ${names.joinToString(", ")}"
    }
    fun debug() = listOf("Entry guard: stage=${stage()} setting=${ConfigManager.holdManagerOnKick} " +
        "downtime=${ConfigManager.downtimeCommand} holders=${SafariDowntime.holders()} rosterKnown=${PartyManager.hasInfo} " +
        "ready=${presence.ready} missing=${presence.missing} kick=${kick.active} identityPending=${SafariPartyInput.pending}", lastDecision)
    private fun isManager(client: Minecraft, entity: Entity): Boolean {
        fun named(e: Entity) = SafariRules.strip(e.customName?.string ?: e.name.string).equals("Safari Manager", true)
        val world = client.level ?: return false
        val partyPlayer = entity is Player && (entity === client.player ||
            PartyManager.withLocal(client.player?.uuid?.toString().orEmpty())?.any {
                SafariEntryPresence.normalize(it) == SafariEntryPresence.normalize(entity.uuid.toString())
            } == true)
        val proxy = entity is Player || entity is net.minecraft.world.entity.decoration.ArmorStand ||
            entity.type.toShortString() in setOf("interaction", "text_display")
        val labels = world.entitiesForRendering().filter { !it.isRemoved && world.getEntity(it.id) === it && named(it) }
            .map { Triple(it.x, it.y, it.z) }.toList()
        return managerTargets.recognize(world, entity.uuid.toString(), proxy, entity.x, entity.y, entity.z,
            named(entity), partyPlayer, labels)
    }
}
