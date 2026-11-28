package net.johnceo.sparklingmutuals.config

import java.util.Properties
import net.johnceo.sparklingmutuals.commands.SafariLookup
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun

open class UniqueRemainingCheckSettings {
    @JvmField @ConfigOption(name = "Show remaining Sparkling checks after biome completion", desc =
        "After a biome clears in Unique mode, list the Sparkling checks your party still needs in Biome Objectives.")
    @ConfigEditorBoolean var remainingChecks = SafariEspConfig.uniqueRemainingChecks
    private var applied = remainingChecks

    internal fun applyRemainingChecks(): Boolean {
        if (remainingChecks == applied) remainingChecks = SafariEspConfig.uniqueRemainingChecks
        applied = remainingChecks
        return SafariEspConfig.setRemainingChecks(remainingChecks)
    }
}

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
    const val OPTIONAL_CHECKS_MIGRATION = "migration.optionalSparklingChecksOff"
    val optionalSpecies = SafariRoster.all.map { it.name }
        .filter { SafariLookup.isManualCheck(it) || it in setOf("Scrappy", "Troodon", "Hideyho") }
    private val birdChecks = optionalSpecies.filter(SafariLookup::isBird)
    private val hiddenRows = mutableSetOf<BiomeObjectiveRow>()
    private val skipped = optionalSpecies.toMutableSet()
    fun show(row: BiomeObjectiveRow) = row !in hiddenRows
    fun check(name: String) = name !in skipped
    fun setCheck(name: String, enabled: Boolean): Boolean {
        if (name !in optionalSpecies) return false
        return if (enabled) skipped.remove(name) else skipped.add(name)
    }
    fun needs(name: String, party: PartySparklingState, mode: SafariMode) =
        (mode != SafariMode.UNIQUE || check(name)) && party.needs(name)
    fun requiresUniqueCheck(name: String, party: PartySparklingState) =
        name in optionalSpecies && check(name) && party.needs(name)
    fun birdsRelaxed() = birdChecks.any { !check(it) }

    /** An opted-out objective still highlights its source and critter when a capture is required. */
    fun allowsEsp(name: String, mode: SafariMode, run: SafariRun?): Boolean =
        mode != SafariMode.UNIQUE || check(name) ||
            ((run?.captureProgress(name) ?: 0) == 0 && run?.uniqueUnavailable(name) != true)

    fun migrateOptionalChecks(properties: Properties): Boolean {
        if (properties.getProperty(OPTIONAL_CHECKS_MIGRATION) == "true") return false
        optionalSpecies.forEach { properties.remove(checkKey(it)) }
        properties.setProperty(OPTIONAL_CHECKS_MIGRATION, "true")
        return true
    }

    fun load(properties: Properties) {
        hiddenRows.clear()
        hiddenRows.addAll(BiomeObjectiveRow.entries.filter { properties.getProperty(rowKey(it)) == "false" })
        skipped.clear()
        skipped.addAll(optionalSpecies.filter { properties.getProperty(checkKey(it)) != "true" })
    }
    fun save(properties: Properties) {
        for (row in BiomeObjectiveRow.entries) properties.setProperty(rowKey(row), show(row).toString())
        for (name in optionalSpecies) properties.setProperty(checkKey(name), check(name).toString())
    }
    private fun rowKey(row: BiomeObjectiveRow) = "objectives.rows.${row.name.lowercase()}"
    private fun checkKey(name: String) = "objectives.checks.${name.lowercase()}"

    fun apply(rows: BiomeObjectiveToggles, checks: BiomeObjectiveToggles): Boolean {
        val nextHidden = BiomeObjectiveRow.entries.filterTo(mutableSetOf()) { !rows.enabled(it.name) }
        val nextSkipped = checks.entries.filterNot { it.mergeCheck(check(it.key)) }.mapTo(mutableSetOf()) { it.key }
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
    private var applied = enabled

    // An unchanged settings screen must pick up party changes instead of restoring its old value.
    internal fun mergeCheck(current: Boolean): Boolean {
        if (enabled == applied) enabled = current
        applied = enabled
        return enabled
    }
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
