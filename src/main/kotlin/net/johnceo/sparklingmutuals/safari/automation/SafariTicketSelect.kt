package net.johnceo.sparklingmutuals.safari.automation

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.screens.Screen
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.SafariTicketType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Items

/** Only a menu opened after an allowed Manager interaction can spend a ticket. */
object SafariTicketSelect {
    data class Option(val slot: Int, val tier: SafariTicketType, val available: Boolean?)
    private var world: Any? = null
    private var connection: Any? = null
    private var armedUntil = 0L
    private var menu: Any? = null
    private var shownAt = 0L
    private var revealed = false
    private var clicked = false
    private var lastAttempt = 0L

    fun register() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen is AbstractContainerScreen<*>) ScreenMouseEvents.allowMouseClick(screen).register { _, _ ->
                !hideScreen(screen)
            }
        }
    }
    private fun eligible(client: Minecraft, now: Long): Boolean {
        val player = client.player ?: return false
        return ConfigManager.autoSelectSafariTicket && now <= armedUntil &&
            client.level === world && client.connection === connection && PartyManager.hasInfo &&
            PartyManager.getMembers().any(PartyManager::isLeader) &&
            (PartyManager.withLocal(player.uuid.toString())?.size ?: 0) > 1 && !PartyManager.isLeader(player.uuid.toString())
    }
    private fun observeMenu(current: Any, now: Long) {
        if (current !== menu) { menu = current; shownAt = now; clicked = false; revealed = false }
    }
    /** Keep the real menu open for its normal guarded click; suppress only its presentation. */
    fun hideScreen(screen: Screen): Boolean {
        val client = Minecraft.getInstance()
        val now = System.currentTimeMillis()
        if (!eligible(client, now) || client.smScreen !== screen || screen !is AbstractContainerScreen<*> ||
            !SafariRules.strip(screen.title.string).trim().equals("Critter Safari Entry", true)) return false
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
        if (!ConfigManager.autoSelectSafariTicket) return
        world = client.level; connection = client.connection
        armedUntil = System.currentTimeMillis() + 15_000
        menu = null; clicked = false; revealed = false
        if (!PartyManager.hasInfo) PartyManager.requestPartyInfo()
    }
    fun tick(client: Minecraft) {
        val now = System.currentTimeMillis()
        if (!ConfigManager.autoSelectSafariTicket || now > armedUntil || client.level !== world || client.connection !== connection) { reset(); return }
        val player = client.player ?: return
        if (!eligible(client, now)) return
        val screen = client.smScreen as? AbstractContainerScreen<*> ?: return
        if (!SafariRules.strip(screen.title.string).trim().equals("Critter Safari Entry", true)) return
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
    private fun reset() { world = null; connection = null; armedUntil = 0; menu = null; clicked = false; revealed = false; shownAt = 0; lastAttempt = 0 }
}
