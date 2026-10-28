package net.johnceo.sparklingmutuals.safari.automation

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.screens.Screen
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.safari.SafariAreaMap
import net.johnceo.sparklingmutuals.safari.SafariIsland
import net.johnceo.sparklingmutuals.safari.SafariTicketType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Items

/** Recognize the entry menu in a verified Safari instance; no chat/click timing dependency. */
object SafariTicketSelect {
    data class Option(val slot: Int, val tier: SafariTicketType, val available: Boolean?)
    private var world: Any? = null
    private var connection: Any? = null
    private var menu: Any? = null
    private var shownAt = 0L
    private var revealed = false
    private var clicked = false
    private var lastAttempt = 0L
    internal fun enabledOnIsland(mode: String? = SafariIsland.currentMode) =
        ConfigManager.autoSelectSafariTicket && SafariIsland.isSafari(mode)

    internal fun validLocation(mode: String?, mappedArea: Int?, lines: List<String>) =
        ConfigManager.autoSelectSafariTicket && mappedArea in 0..4 &&
            (SafariRules.isSafari(lines) || SafariRules.isEntrance(lines) || lines.isEmpty() && SafariIsland.isSafari(mode))

    internal fun entryMenu(title: String) = SafariRules.strip(title).trim().equals("Critter Safari Entry",true)
    private fun syncSession(client: Minecraft) {
        if (world !== client.level || connection !== client.connection) {
            reset(); world=client.level; connection=client.connection
        }
    }
    internal fun validParty(known: Boolean, leaderKnown: Boolean, size: Int, localLeader: Boolean) =
        known && leaderKnown && size > 1 && !localLeader

    private fun locationAllows(client: Minecraft): Boolean {
        val player = client.player ?: return false
        val lines = SkyblockSidebar.areaName(client)?.let { listOf("Area: $it") }.orEmpty() + SkyblockSidebar.lines(client)
        return validLocation(SafariIsland.currentMode, SafariAreaMap.areaAt(player.x, player.y, player.z), lines)
    }
    private fun contextAllows(client: Minecraft): Boolean {
        if (client.level == null || client.connection == null) return false
        val player = client.player ?: return false
        return locationAllows(client) && validParty(PartyManager.hasInfo,
            PartyManager.getMembers().any(PartyManager::isLeader),
            PartyManager.withLocal(player.uuid.toString())?.size ?: 0, PartyManager.isLeader(player.uuid.toString()))
    }

    fun register() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen is AbstractContainerScreen<*>) ScreenMouseEvents.allowMouseClick(screen).register { _, _ ->
                val hidden=hideScreen(screen)
                // A visible menu belongs to the user once they begin clicking it.
                if (!hidden && entryMenu(screen.title.string) && contextAllows(Minecraft.getInstance())) {
                    clicked=true; revealed=true
                }
                !hidden
            }
        }
    }
    private fun observeMenu(current: Any, now: Long) {
        if (current !== menu) { menu = current; shownAt = now; clicked = false; revealed = false }
    }
    /** Keep the real menu open for its normal guarded click; suppress only its presentation. */
    fun hideScreen(screen: Screen): Boolean {
        val client = Minecraft.getInstance()
        val now = System.currentTimeMillis()
        syncSession(client)
        if (!contextAllows(client) || client.smScreen !== screen || screen !is AbstractContainerScreen<*> ||
            !entryMenu(screen.title.string)) return false
        observeMenu(client.player!!.containerMenu, now)
        // Reveal on server rejection/slow data rather than leave the user facing an invisible menu.
        return !revealed && now - shownAt < 2000
    }

    internal fun available(lines: List<String>): Boolean? {
        val text = lines.joinToString(" ") { SafariRules.strip(it).replace(',', ' ') }.lowercase()
        if (Regex("(?:don't|do not|doesn't|not enough|no tickets|no .*tickets|cannot|can't|insufficient|not own)").containsMatchIn(text)) return false
        val count = lines.firstNotNullOfOrNull { raw ->
            val plain = SafariRules.strip(raw).replace(",", "")
            Regex("(?i)(?:owned|available|amount|tickets?(?: owned)?|you (?:have|own))\\s*:?\\s*([0-9]+)")
                .find(plain)?.groupValues?.get(1)?.toLongOrNull()
        }
        if (count != null) return count > 0
        if (Regex("(?i)\\bclick to (?:enter|use|select|redeem|consume)").containsMatchIn(text)) return true
        return null
    }
    internal fun choose(options: List<Option>, preference: Int): Option? {
        val tiers = if (preference == 1) SafariTicketType.entries else SafariTicketType.entries.reversed()
        // Unknown availability might be the preferred ticket while menu data is still loading.
        for (tier in tiers) {
            val option = options.singleOrNull { it.tier == tier } ?: return null
            if (option.available == null) return null
            if (option.available) return option
        }
        return null
    }
    fun managerClicked(client: Minecraft) {
        // Preload the roster; opening the entry menu itself is the trigger.
        if (locationAllows(client) && !PartyManager.hasInfo) PartyManager.requestPartyInfo()
    }
    fun tick(client: Minecraft) {
        val now = System.currentTimeMillis()
        syncSession(client)
        if (!ConfigManager.autoSelectSafariTicket) { reset(); return }
        val player = client.player ?: return
        val screen = client.smScreen as? AbstractContainerScreen<*> ?: run {
            menu=null; clicked=false; revealed=false; return
        }
        if (!entryMenu(screen.title.string) || !locationAllows(client)) return
        if (!PartyManager.hasInfo || PartyManager.getMembers().none(PartyManager::isLeader)) PartyManager.requestPartyInfo()
        if (!contextAllows(client)) return
        val current = player.containerMenu
        observeMenu(current, now)
        if (clicked || now - shownAt < 250 || now - lastAttempt < 250) return
        val options = current.slots.mapIndexedNotNull { index, slot ->
            if (slot.container === player.inventory || !slot.item.`is`(Items.NAME_TAG)) return@mapIndexedNotNull null
            val name = SafariRules.strip(slot.item.hoverName.string).replace('-', ' ')
            val tier = SafariTicketType.entries.firstOrNull { name.contains(it.label, true) } ?: return@mapIndexedNotNull null
            Option(index, tier, available(slot.item.get(DataComponents.LORE)?.lines().orEmpty().map { it.string }))
        }
        if (options.map { it.tier }.toSet().size != SafariTicketType.entries.size) return
        val option = choose(options, ConfigManager.safariTicketPreference) ?: run {
            if (options.all { it.available != null }) revealed = true
            return
        }
        lastAttempt = now
        if (SafariManagerGuard.shouldBlockEntry(current.containerId, option.slot)) { revealed = true; return }
        val gameMode = client.gameMode ?: return
        clicked = true
        // The normal packet path performs the entry guard again before sending.
        gameMode.handleContainerInput(current.containerId, option.slot, 0, ContainerInput.PICKUP, player)
    }
    private fun reset() { world = null; connection = null; menu = null; clicked = false; revealed = false; shownAt = 0; lastAttempt = 0 }
}
