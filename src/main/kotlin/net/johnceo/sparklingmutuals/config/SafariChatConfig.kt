package net.johnceo.sparklingmutuals.config

import java.util.Properties

/** SafariUtils chat categories (MIT); existing hidden categories retain their defaults. */
enum class SafariChatCategory(val label: String, val defaultHidden: Boolean) {
    RUN_START("Run Start Information", false),
    CATCH_ATTEMPTS("Catch Attempts", true),
    CRITTER_CATCHES("Critter Catches", true),
    LOOT_SHARES("Loot Shares", true),
    FLOOR_DROPS("Floor Drops", false),
    MANAGER("Safari Manager", false),
    NPCS("NPCs", false),
    DROP_ITEMS("Drop Items", false),
    GEMZIE("Gemzie", false),
    CHUCKWALLA("Chuckwalla", true),
    ROCKMITE("Rockmite", true),
    SCRAPPY("Scrappy", true),
    SHYWORM("Shyworm", true),
    SNOOZLE("Snoozle", true),
    WUMPA("Wumpa", false),
    TROODON("Troodon", true),
    COLD("Cold", false),
    DOOMSPIRAL("Doomspiral", false),
    BLOODBAT("Bloodbat", false),
    DUPLICO("Duplico", true),
    GAZER("Gazer", false),
    GIMMIEGOLD("Gimmiegold", false),
    HIDEYHO("Hideyho", false),
    BIRD_SPAWNS("Bird Spawns", false),
    EMPTY_NESTS("Empty Nest", true)
}

object SafariChatConfig {
    val toggles = SafariChatCategory.entries.associateWith { it.defaultHidden }.toMutableMap()
    fun hidden(category: SafariChatCategory) = toggles[category] == true
    fun load(properties: Properties) {
        SafariChatCategory.entries.forEach {
            toggles[it] = properties.getProperty("chat.hide.${it.name.lowercase()}")?.toBooleanStrictOrNull() ?: it.defaultHidden
        }
    }
    fun save(properties: Properties) {
        toggles.forEach { (category, enabled) -> properties.setProperty("chat.hide.${category.name.lowercase()}", enabled.toString()) }
    }
}
