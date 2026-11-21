package net.johnceo.sparklingmutuals.config

import java.util.Properties
import net.johnceo.sparklingmutuals.commands.SafariLookup
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun

internal enum class BiomeObjectiveRow(val label: String, val description: String = "") {
    COMPLETE("All objectives complete"),
    CONFIRM_CLEAR("Confirm biome clear"),
    STARTING_ITEMS("Use starting items"),
    STARTING_FOOD_SYNC("Party starting food", "Show unused starting food reported by party sync."),
    GEM_SYNC("Party-held gems", "Show gems reported by party sync that are still needed on the podiums."),
    NESTS("Bee nests"),
    FOOD_TOTAL("Bird food left to use"),
    FOOD_TYPES("Bird food by type"),
    FEEDER("Food in feeder"),
    BIRDS("Spawned birds"),
    PODIUMS("Podium gems"),
    WALLS("Snoozle walls"),
    MOUNDS("Rockmite mounds"),
    WUMPA("Capture Wumpa"),
    COINS("Coins left to use"),
    GAZERS("Gazers left"),
}

internal object BiomeObjectiveConfig {
    val optionalSpecies = SafariRoster.all.map { it.name }
        .filter { SafariLookup.isManualCheck(it) || it in setOf("Scrappy", "Troodon", "Hideyho") }
    private val birdChecks = optionalSpecies.filter(SafariLookup::isBird)
    private val hiddenRows = mutableSetOf<BiomeObjectiveRow>()
    private val skipped = mutableSetOf<String>()
    fun show(row: BiomeObjectiveRow) = row !in hiddenRows
    fun check(name: String) = name !in skipped
    fun needs(name: String, party: PartySparklingState) = check(name) && party.needs(name)
    fun birdsRelaxed() = birdChecks.any { !check(it) }

    /** An opted-out objective still highlights its source and critter when a capture is required. */
    fun allowsEsp(name: String, mode: SafariMode, run: SafariRun?): Boolean =
        check(name) || mode == SafariMode.FULL_CLEAR ||
            mode == SafariMode.UNIQUE && ((run?.captureProgress(name) ?: 0) == 0 && run?.uniqueUnavailable(name) != true)

    fun load(properties: Properties) {
        hiddenRows.clear()
        hiddenRows.addAll(BiomeObjectiveRow.entries.filter { properties.getProperty(rowKey(it)) == "false" })
        skipped.clear()
        skipped.addAll(optionalSpecies.filter { properties.getProperty(checkKey(it)) == "false" })
    }
    fun save(properties: Properties) {
        for (row in BiomeObjectiveRow.entries) properties.setProperty(rowKey(row), show(row).toString())
        for (name in optionalSpecies) properties.setProperty(checkKey(name), check(name).toString())
    }
    private fun rowKey(row: BiomeObjectiveRow) = "objectives.rows.${row.name.lowercase()}"
    private fun checkKey(name: String) = "objectives.checks.${name.lowercase()}"

    fun apply(rows: BiomeObjectiveToggles, checks: BiomeObjectiveToggles): Boolean {
        val nextHidden = BiomeObjectiveRow.entries.filterTo(mutableSetOf()) { !rows.enabled(it.name) }
        val nextSkipped = optionalSpecies.filterTo(mutableSetOf()) { !checks.enabled(it) }
        if (nextHidden == hiddenRows && nextSkipped == skipped) return false
        hiddenRows.clear()
        hiddenRows.addAll(nextHidden)
        skipped.clear()
        skipped.addAll(nextSkipped)
        return true
    }
}

class BiomeObjectiveToggle internal constructor(val key: String, val label: String, val description: String, enabled: Boolean) {
    @JvmField @ConfigOption(name = "", desc = "") @ConfigEditorBoolean var enabled = enabled
}

class BiomeObjectiveToggles internal constructor(internal val entries: List<BiomeObjectiveToggle>) {
    internal fun enabled(key: String) = entries.first { it.key == key }.enabled
    companion object {
        internal fun rows() = BiomeObjectiveToggles(BiomeObjectiveRow.entries.map {
            BiomeObjectiveToggle(it.name, it.label, it.description, BiomeObjectiveConfig.show(it))
        })
        internal fun checks() = BiomeObjectiveToggles(BiomeObjectiveConfig.optionalSpecies.map {
            BiomeObjectiveToggle(it, it, "", BiomeObjectiveConfig.check(it))
        })
    }
}
