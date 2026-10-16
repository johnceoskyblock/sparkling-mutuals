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
import net.minecraft.world.item.Items
import org.slf4j.LoggerFactory

/** The same decision runs before every Manager, ticket and entry-menu packet boundary. */
object SafariManagerGuard {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/entry-guard")
    private val presence = SafariEntryPresence()
    private val runState = SafariEntryRunState()
    private val kick = EntryKickHold()
    private var requestedRosterAt = 0L
    private var messagedAt = 0L
    private var lastDecision = ""
    fun register() { SafariPartyInput.register() }
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
    fun waitingInSafari(client: Minecraft) = stage() == EntryStage.SAFARI && !activeRun(client)
    fun onClientTick(client: Minecraft) {
        SafariPartyInput.tick(client)
        if (!ConfigManager.holdManagerOnKick) kick.reset()
        val player = client.player ?: return
        val connection = client.connection ?: return
        val now = System.currentTimeMillis()
        val waiting = waitingInSafari(client)
        if (waiting && ConfigManager.holdManagerOnKick && !PartyManager.hasInfo && now - requestedRosterAt > 5000) {
            requestedRosterAt = now; PartyManager.requestPartyInfo()
        }
        val roster = PartyManager.withLocal(player.uuid.toString())
        val present = connection.listedOnlinePlayers.mapTo(HashSet()) { it.profile.id.toString() }
        presence.update(now, waiting, false, client.level, roster, present, player.uuid.toString())
        if (waiting) kick.observe(client.level, roster?.mapTo(HashSet(), SafariEntryPresence::normalize),
            present.mapTo(HashSet(), SafariEntryPresence::normalize), presence.ready)
    }
    fun callsKick(text: String) = PartyChat.parse(text)?.body?.contains("kick", true) == true
    /** Only called after current-party UUID verification, and only for the original waiting instance. */
    fun partyKick(chat: PartyChat, senderId: String) {
        val client = Minecraft.getInstance()
        if (!ConfigManager.holdManagerOnKick || !waitingInSafari(client) || !chat.body.contains("kick", true)) return
        // A generic request does not identify its victim. Never recover it using somebody else's return.
        val selfReport = Regex("\\bi\\s+(?:(?:was|got|am|have been)\\s+)?kicked\\b", RegexOption.IGNORE_CASE)
            .containsMatchIn(chat.body)
        kick.report(client.level, senderId.takeIf { selfReport })
    }
    fun reset() { messagedAt = 0; requestedRosterAt = 0; lastDecision = ""; presence.reset(); runState.reset(); kick.reset() }
    fun entryItem(title: String, nameTag: Boolean, playerInventory: Boolean) =
        SafariRules.strip(title).trim().equals("Critter Safari Entry", true) && nameTag && !playerInventory
    fun shouldBlockEntry(containerId: Int, slotId: Int): Boolean {
        val client = Minecraft.getInstance()
        val player = client.player ?: return false
        val screen = client.smScreen as? AbstractContainerScreen<*> ?: return false
        val menu = player.containerMenu
        if (menu.containerId != containerId) return false
        val slot = menu.slots.getOrNull(slotId) ?: return false
        if (!entryItem(screen.title.string, slot.item.`is`(Items.NAME_TAG), slot.container === player.inventory)) return false
        val blocked = blocks(client, "entry menu")
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
        return entity != null && isManager(client, entity) && blocks(client, "manager ${entity.id}")
    }
    private fun blocks(client: Minecraft, route: String): Boolean {
        val player = client.player ?: return false
        onClientTick(client)
        val guard = ConfigManager.holdManagerOnKick
        val downtime = SafariDowntime.entryHold
        val active = activeRun(client)
        val blocked = (guard || ConfigManager.downtimeCommand) && EntryGuardDecision.block(stage(), active,
            downtime, guard && kick.active, !guard || presence.ready,
            when (stage()) {
                EntryStage.TORRHUS -> ConfigManager.downtimeCommand && SafariPartyInput.pendingDowntime
                EntryStage.SAFARI -> guard && SafariPartyInput.pendingKick
                else -> SafariPartyInput.pending
            })
        val reason = when (stage()) {
            EntryStage.TORRHUS -> if (SafariPartyInput.overflowReadiness) "readiness input limit reached; each current member must say r or Ticket earned!"
                else if (SafariPartyInput.pendingDowntime) "verifying party readiness" else "${SafariDowntime.holders()} asked for downtime"
            EntryStage.UNKNOWN -> "waiting for server location"
            else -> when {
                SafariPartyInput.overflowKick -> "kick input limit reached; revalidate in a new Safari instance"
                SafariPartyInput.pendingKick -> "verifying party message identity"
                !PartyManager.hasInfo -> "waiting for the current party roster"
                !presence.ready -> "party presence/loading incomplete (${presence.missing.size} missing)"
                kick.active -> "kick recovery needs a fresh return, membership change or verified new instance"
                else -> "ready"
            }
        }
        val detail = "$route: blocked=$blocked stage=${stage()} run=$active guard=$guard downtime=$downtime " +
            "rosterKnown=${PartyManager.hasInfo} presenceReady=${presence.ready} missing=${presence.missing} " +
            "kick=${kick.active} identityPending=${SafariPartyInput.pending} reason=$reason"
        if (detail != lastDecision) { logger.info(detail); lastDecision = detail }
        val now = System.currentTimeMillis()
        if (blocked && now - messagedAt >= 3000) {
            messagedAt = now
            player.sendSystemMessage(LocalChat.component("[SM] Blocked Safari entry: $reason."))
        }
        return blocked
    }
    fun debug() = listOf("Entry guard: stage=${stage()} setting=${ConfigManager.holdManagerOnKick} " +
        "downtime=${ConfigManager.downtimeCommand} holders=${SafariDowntime.holders()} rosterKnown=${PartyManager.hasInfo} " +
        "ready=${presence.ready} missing=${presence.missing} kick=${kick.active} identityPending=${SafariPartyInput.pending}", lastDecision)
    private fun isManager(client: Minecraft, entity: Entity): Boolean {
        fun named(e: Entity) = SafariRules.strip(e.customName?.string ?: e.name.string).equals("Safari Manager", true)
        if (named(entity)) return true
        // Only an unlisted NPC player with its own closely aligned nameplate, never arbitrary nearby entities.
        if (entity !is Player || client.connection?.listedOnlinePlayers?.any { it.profile.id == entity.uuid } == true) return false
        return client.level?.getEntities(entity, entity.boundingBox.inflate(.5, 2.5, .5))?.any {
            named(it) && kotlin.math.abs(it.x - entity.x) <= .5 && kotlin.math.abs(it.z - entity.z) <= .5 && it.y >= entity.y
        } == true
    }
}
