package net.johnceo.sparklingmutuals.safari.automation

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
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
    internal fun choose(options: List<Option>, preference: Int): Option? =
        if (preference == 1) chooseLowest(options) else chooseInOrder(options, SafariTicketType.entries.reversed())

    internal fun chooseLowest(options: List<Option>): Option? = chooseInOrder(options, SafariTicketType.entries)

    private fun chooseInOrder(options: List<Option>, tiers: List<SafariTicketType>): Option? {
        // Unknown availability might be the preferred ticket while menu data is still loading.
        for (tier in tiers) {
            val option = options.singleOrNull { it.tier == tier } ?: return null
            if (option.available == null) return null
            if (option.available) return option
        }
        return null
    }
    internal fun options(client: Minecraft): List<Option> {
        val player=client.player ?: return emptyList()
        val current=player.containerMenu
        return current.slots.mapIndexedNotNull { index, slot ->
            if (slot.container === player.inventory || !slot.item.`is`(Items.NAME_TAG)) return@mapIndexedNotNull null
            val name = SafariRules.strip(slot.item.hoverName.string).replace('-', ' ')
            val tier = SafariTicketType.entries.firstOrNull { name.contains(it.label, true) } ?: return@mapIndexedNotNull null
            Option(index, tier, available(slot.item.get(DataComponents.LORE)?.lines().orEmpty().map { it.string }))
        }
    }
    fun managerClicked(client: Minecraft) {
        // Preload the roster; opening the entry menu itself is the trigger.
        if (locationAllows(client) && !PartyManager.hasInfo) PartyManager.requestPartyInfo()
    }
}
