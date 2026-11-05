package net.johnceo.sparklingmutuals.config

class SafariChatToggleSettings {
    @JvmField @ConfigOption(name = "Run Start Information", desc = "")
    @ConfigEditorBoolean var run_start = SafariChatConfig.hidden(SafariChatCategory.RUN_START)
    @JvmField @ConfigOption(name = "Catch Attempts", desc = "")
    @ConfigEditorBoolean var catch_attempts = SafariChatConfig.hidden(SafariChatCategory.CATCH_ATTEMPTS)
    @JvmField @ConfigOption(name = "Critter Catches", desc = "")
    @ConfigEditorBoolean var critter_catches = SafariChatConfig.hidden(SafariChatCategory.CRITTER_CATCHES)
    @JvmField @ConfigOption(name = "Loot Shares", desc = "")
    @ConfigEditorBoolean var loot_shares = SafariChatConfig.hidden(SafariChatCategory.LOOT_SHARES)
    @JvmField @ConfigOption(name = "Floor Drops", desc = "")
    @ConfigEditorBoolean var floor_drops = SafariChatConfig.hidden(SafariChatCategory.FLOOR_DROPS)
    @JvmField @ConfigOption(name = "Safari Manager", desc = "")
    @ConfigEditorBoolean var manager = SafariChatConfig.hidden(SafariChatCategory.MANAGER)
    @JvmField @ConfigOption(name = "NPCs", desc = "")
    @ConfigEditorBoolean var npcs = SafariChatConfig.hidden(SafariChatCategory.NPCS)
    @JvmField @ConfigOption(name = "Drop Items", desc = "")
    @ConfigEditorBoolean var drop_items = SafariChatConfig.hidden(SafariChatCategory.DROP_ITEMS)
    @JvmField @ConfigOption(name = "Gemzie", desc = "")
    @ConfigEditorBoolean var gemzie = SafariChatConfig.hidden(SafariChatCategory.GEMZIE)
    @JvmField @ConfigOption(name = "Chuckwalla", desc = "")
    @ConfigEditorBoolean var chuckwalla = SafariChatConfig.hidden(SafariChatCategory.CHUCKWALLA)
    @JvmField @ConfigOption(name = "Rockmite", desc = "")
    @ConfigEditorBoolean var rockmite = SafariChatConfig.hidden(SafariChatCategory.ROCKMITE)
    @JvmField @ConfigOption(name = "Scrappy", desc = "")
    @ConfigEditorBoolean var scrappy = SafariChatConfig.hidden(SafariChatCategory.SCRAPPY)
    @JvmField @ConfigOption(name = "Shyworm", desc = "")
    @ConfigEditorBoolean var shyworm = SafariChatConfig.hidden(SafariChatCategory.SHYWORM)
    @JvmField @ConfigOption(name = "Snoozle", desc = "")
    @ConfigEditorBoolean var snoozle = SafariChatConfig.hidden(SafariChatCategory.SNOOZLE)
    @JvmField @ConfigOption(name = "Wumpa", desc = "")
    @ConfigEditorBoolean var wumpa = SafariChatConfig.hidden(SafariChatCategory.WUMPA)
    @JvmField @ConfigOption(name = "Troodon", desc = "")
    @ConfigEditorBoolean var troodon = SafariChatConfig.hidden(SafariChatCategory.TROODON)
    @JvmField @ConfigOption(name = "Cold", desc = "")
    @ConfigEditorBoolean var cold = SafariChatConfig.hidden(SafariChatCategory.COLD)
    @JvmField @ConfigOption(name = "Doomspiral", desc = "")
    @ConfigEditorBoolean var doomspiral = SafariChatConfig.hidden(SafariChatCategory.DOOMSPIRAL)
    @JvmField @ConfigOption(name = "Bloodbat", desc = "")
    @ConfigEditorBoolean var bloodbat = SafariChatConfig.hidden(SafariChatCategory.BLOODBAT)
    @JvmField @ConfigOption(name = "Duplico", desc = "")
    @ConfigEditorBoolean var duplico = SafariChatConfig.hidden(SafariChatCategory.DUPLICO)
    @JvmField @ConfigOption(name = "Gazer", desc = "")
    @ConfigEditorBoolean var gazer = SafariChatConfig.hidden(SafariChatCategory.GAZER)
    @JvmField @ConfigOption(name = "Gimmiegold", desc = "")
    @ConfigEditorBoolean var gimmiegold = SafariChatConfig.hidden(SafariChatCategory.GIMMIEGOLD)
    @JvmField @ConfigOption(name = "Hideyho", desc = "")
    @ConfigEditorBoolean var hideyho = SafariChatConfig.hidden(SafariChatCategory.HIDEYHO)
    @JvmField @ConfigOption(name = "Bird Spawns", desc = "")
    @ConfigEditorBoolean var bird_spawns = SafariChatConfig.hidden(SafariChatCategory.BIRD_SPAWNS)
    @JvmField @ConfigOption(name = "Empty Nest", desc = "")
    @ConfigEditorBoolean var empty_nests = SafariChatConfig.hidden(SafariChatCategory.EMPTY_NESTS)
    fun apply(): Boolean {
        var changed = false
        javaClass.fields.forEach { field ->
            val label = field.getAnnotation(ConfigOption::class.java)?.name ?: return@forEach
            val category = SafariChatCategory.entries.single { it.label == label }
            val enabled = field.getBoolean(this)
            if (SafariChatConfig.toggles.put(category, enabled) != enabled) changed = true
        }
        return changed
    }
}
