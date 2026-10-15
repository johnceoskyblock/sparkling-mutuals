package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig

/** Per-run pickups survive item consumption; a manual choice lasts only for this biome visit. */
class FloorDropState {
    private val acquired = mutableMapOf<FloorDropType, Int>()
    private val inventoryPeak = mutableMapOf<FloorDropType, Int>()
    private val heldMaximum = mutableMapOf<FloorDropType, Int>()
    fun startingGrant(type: FloorDropType) { heldMaximum[type] = maxOf(heldMaximum[type] ?: 0, 1) }
    fun needed(type: FloorDropType, mode: SafariMode, party: PartySparklingState): Boolean = when (type) {
        FloorDropType.LIME, FloorDropType.PURPLE, FloorDropType.ORANGE -> amount(type) == 0
        FloorDropType.ICEBREAKER -> mode == SafariMode.FULL_CLEAR && amount(type) == 0
        FloorDropType.COIN -> party.needs("Gimmiegold") || mode != SafariMode.SPARKLING && amount(type) == 0
        FloorDropType.INCENSE -> (acquired[type] ?: 0) < 4 && (inventoryPeak[type] ?: 0) < 4 &&
            if (mode == SafariMode.SPARKLING) party.needs("Doomspiral")
            else party.needs("Gimmiegold") || amount(type) < 4
        FloorDropType.WORM, FloorDropType.BERRY, FloorDropType.SEEDS -> true
        FloorDropType.GENERIC -> false
    }
    /** Rank all known incense locations, including unloaded/collected ones, without replacing picked spawns. */
    fun incenseDropIds(known: List<PredictedFloorDrop>): Set<Int> {
        val limit = if ((heldMaximum[FloorDropType.INCENSE] ?: 0) > 0) 2 else 4
        return known.filter { it.biome == SafariBiome.HAUNTED && it.type == FloorDropType.INCENSE }
            .sortedWith(compareBy<PredictedFloorDrop> {
                (it.x - 4) * (it.x - 4) + (it.y - 47) * (it.y - 47) + (it.z + 27) * (it.z + 27)
            }.thenBy { it.id }).take(limit).map { it.id }.toSet()
    }
    private fun amount(type: FloorDropType) = maxOf((acquired[type] ?: 0) + (heldMaximum[type] ?: 0), inventoryPeak[type] ?: 0)
    private var gemsReady = false
    private val gems = mutableSetOf<String>()
    private val gemNames = setOf("Purple Gem", "Lime Gem", "Orange Gem")
    private var pickaxes = 0
    private var inventoryPickaxes = 0
    private var incensePickups = 0
    private var incenseReady = false
    private var biome: SafariBiome? = null
    private var override: Boolean? = null
    fun visit(biome: SafariBiome?) {
        if (this.biome != biome) { override = null; this.biome = biome }
    }
    fun record(text: String, biome: SafariBiome?) {
        if (!text.startsWith("FLOOR DROP!")) return
        FloorPickup.parse(text)?.let { acquired.merge(it.type, it.quantity, Int::plus) }
        gems.addAll(gemNames.filter(text::contains))
        if (text.contains("Soothing Incense", true)) incensePickups++
        if (biome == SafariBiome.ICY && Regex("\\bIcebreaker\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) pickaxes++
    }
    fun inventory(stacks: List<Pair<String, Int>>, initial: Boolean = true) {
        FloorDropType.entries.forEach { type ->
            val held = stacks.filter { SafariRules.strip(it.first).trim().equals(type.label, true) }.sumOf { it.second.coerceAtLeast(0) }
            inventoryPeak[type] = maxOf(inventoryPeak[type] ?: 0, held)
            if (initial) heldMaximum[type] = maxOf(heldMaximum[type] ?: 0, held - (acquired[type] ?: 0))
        }
        if (stacks.filter { SafariRules.strip(it.first).equals("Soothing Incense", true) }.sumOf { it.second.coerceAtLeast(0) } >= 4) incenseReady = true
        val heldGems = stacks.filter { it.second > 0 }.map { SafariRules.strip(it.first) }.toSet().intersect(gemNames)
        gems.addAll(heldGems)
        if (heldGems.containsAll(gemNames)) gemsReady = true
        inventoryPickaxes = maxOf(inventoryPickaxes, stacks.filter { SafariRules.strip(it.first).equals("Icebreaker", true) }.sumOf { it.second.coerceAtLeast(0) })
    }
    fun force(enabled: Boolean) { if (biome in setOf(SafariBiome.CAVERN, SafariBiome.ICY)) override = enabled }
    fun enabled(biome: SafariBiome?, fullClear: Boolean, configured: Boolean, forestFoodComplete: Boolean = false,
        partyBirdsComplete: Boolean = false, partyGemzieComplete: Boolean = false,
        partyGimmiegoldComplete: Boolean = false, gemzieCaught: Boolean = false, doomCaught: Boolean = false): Boolean {
        if (biome == this.biome && override != null) return override!!
        return when (biome) {
            SafariBiome.CAVERN -> if (fullClear) !gemsReady else gems.size < 3 && !partyGemzieComplete && !gemzieCaught
            SafariBiome.FOREST -> configured && (fullClear || !partyBirdsComplete && !forestFoodComplete)
            SafariBiome.HAUNTED -> configured && (fullClear || !partyGimmiegoldComplete || !incenseReady && incensePickups < 4 && !doomCaught)
            SafariBiome.ICY -> fullClear && maxOf(pickaxes, inventoryPickaxes) < 1
            else -> configured
        }
    }
    fun enabledForMode(biome: SafariBiome?, mode: SafariMode, configured: Boolean, run: SafariRun?, party: PartySparklingState): Boolean {
        if (biome == this.biome && override != null) return override!!
        fun caught(name: String) = (run?.count(name) ?: 0) > 0
        val fullClear = mode == SafariMode.FULL_CLEAR
        return when (biome) {
            SafariBiome.CAVERN -> (fullClear || party.needs("Gemzie") || mode == SafariMode.UNIQUE && !caught("Gemzie")) &&
                listOf(FloorDropType.LIME, FloorDropType.PURPLE, FloorDropType.ORANGE).any { needed(it, mode, party) }
            SafariBiome.FOREST -> configured && (fullClear || listOf("Bluebird", "Parakeet", "Macaw").any {
                party.needs(it) || mode == SafariMode.UNIQUE && !caught(it)
            })
            SafariBiome.HAUNTED -> configured && (needed(FloorDropType.COIN, mode, party) || needed(FloorDropType.INCENSE, mode, party))
            SafariBiome.ICY -> fullClear && needed(FloorDropType.ICEBREAKER, mode, party)
            else -> false
        }
    }
    fun reset() { inventoryPeak.clear(); acquired.clear(); heldMaximum.clear(); gemsReady = false; gems.clear(); pickaxes = 0; inventoryPickaxes = 0; incensePickups = 0; incenseReady = false; biome = null; override = null }
}

object SafariFloorDrops {
    val state = FloorDropState()
    fun selected(drop: PredictedFloorDrop, incenseIds: Set<Int>): Boolean {
        val type = drop.type ?: return false
        val mode = SafariFullClear.mode
        val party = SafariSparklingMode.state
        return (type != FloorDropType.INCENSE || drop.id in incenseIds) && enabled(drop.biome) && state.needed(type, mode, party)
    }
    fun enabled(biome: SafariBiome? = SafariAssist.biome): Boolean {
        state.visit(SafariAssist.biome)
        val run = SafariTracking.ledger.current
        val party = SafariSparklingMode.state
        return state.enabledForMode(biome, SafariFullClear.mode, SafariEspConfig.groups.getValue("floor").enabled, run, party)
    }
}
