package net.johnceo.sparklingmutuals.safari.completion

import java.util.Locale
import java.util.UUID
import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.resources.FloorPickup

/** Run-owned check evidence, independent of captures and personal bests. */
class SparklingChecks(private val clock: () -> Long = System::currentTimeMillis) {
    private var gemzieChamberOpenedAt: Long? = null
    val gemzieChamberOpened get() = gemzieChamberOpenedAt != null
    private fun gemzieChamberChecked() = gemzieChamberOpenedAt?.let { clock() - it >= 5000L } == true
    private val completed = mutableSetOf<String>()
    private val partyCompleted = mutableSetOf<String>()
    fun locallyChecked(name: String) = if (requiresCapture(name)) (captured[name] ?: 0) > 0
        else name in completed || name == "Gemzie" && gemzieChamberChecked()
    fun partyChecked(name: String) = name in partyCompleted
    fun cavernSourceEvidence(moundsCleared: Boolean, wallsCleared: Boolean) {
        if (moundsCleared) completed.add("Rockmite")
        if (wallsCleared) completed.add("Snoozle")
    }
    fun nestEvidence(checked: Boolean) {
        if (checked) completed.add("Honeybug") else completed.remove("Honeybug")
    }
    fun reconcileStructures(biome: SafariBiome, moundsRemain: Boolean, wallsRemain: Boolean) {
        if (biome != SafariBiome.CAVERN) return
        if (moundsRemain) { completed.remove("Rockmite"); partyCompleted.remove("Rockmite") }
        if (wallsRemain) { completed.remove("Snoozle"); partyCompleted.remove("Snoozle") }
    }
    private val observed = mutableMapOf<String, MutableSet<UUID>>()
    private var validatedCoinDemand = 0
    private var extraCoinUuidBaseline: Set<UUID>? = null
    private var extraCoinChecksNeeded = 0
    private fun addExtraCoinChecks(quantity: Int) {
        if (extraCoinUuidBaseline == null) extraCoinUuidBaseline = seenAtCaptureCount["Gimmiegold"].orEmpty().keys.toSet()
        extraCoinChecksNeeded += quantity
    }
    private fun extraCoinChecksResolved() = extraCoinUuidBaseline == null ||
        (captured["Gimmiegold"] ?: 0) >= gimmiegoldMinimum ||
        seenAtCaptureCount["Gimmiegold"].orEmpty().keys.count { it !in extraCoinUuidBaseline.orEmpty() } >= extraCoinChecksNeeded
    private var startingCoins = 0
    fun startingCoins(amount: Int) {
        if (amount > startingCoins) {
            if (locallyChecked("Gimmiegold") || extraCoinUuidBaseline != null) addExtraCoinChecks(amount - startingCoins)
            if (locallyChecked("Gimmiegold")) validatedCoinDemand = maxOf(validatedCoinDemand, minOf(checkCount("Gimmiegold"), gimmiegoldMinimum) + amount - startingCoins)
            completed.remove("Gimmiegold")
        }
        startingCoins = maxOf(startingCoins, amount)
    }
    private var coinsPicked = 0
    private var coinsSpent = 0
    private var heldCoins: Int? = null
    private var goldEnvironmentReady = false
    private var confirmedCoinSource = false
    /** Actual baseline floor pickups and consumption outrank speculative reward typing. */
    private fun confirmedFloorCoinsResolved() = coinsPicked >= BASE_COIN_FLOOR_DROPS &&
        coinsSpent >= coinsPicked + startingCoins && heldCoins == 0
    private val captured = mutableMapOf<String, Int>()
    private val currentCounts = mutableMapOf<String, Int>()
    // Remember qualified UUID sightings for the run. Capture baselines prevent counting
    // a previously seen critter again when its later catch arrives without a UUID.
    private val seenAtCaptureCount = mutableMapOf<String, MutableMap<UUID, Int>>()
    private fun retainedCheckCount(name: String): Int {
        val catches = captured[name] ?: 0
        val firstSeen = seenAtCaptureCount[name].orEmpty().values
        return maxOf(catches, firstSeen.distinct().maxOfOrNull { baseline ->
            baseline + firstSeen.count { it >= baseline }
        } ?: 0)
    }
    fun checkCount(name: String) = if (name == "Gemzie" && gemzieChamberChecked()) maxOf(3, retainedCheckCount(name))
        else retainedCheckCount(name)
    fun localScan(biome: SafariBiome) { partyCompleted.removeAll(biome.critters.map { it.name }.toSet()) }
    fun reenter(biome: SafariBiome) {
        if (biome == SafariBiome.HAUNTED) { goldEnvironmentReady = false; confirmedCoinSource = false }
        val names = biome.critters.map { it.name }.toSet()
        // Revalidate spawn sources, but never erase qualified UUID checks on biome entry.
        completed.removeAll(names.intersect(sourceChecks)); partyCompleted.removeAll(names)
        names.forEach { currentCounts.remove(it) }
    }
    // Captures/current loaded population can establish spawns missed by the coin-use chat listener.
    // Empty inventory alone is insufficient; source absence and the full population are also required.
    private fun coinUseResolved(population: Int) = coinsPicked + startingCoins == 0 ||
        coinsSpent >= coinsPicked + startingCoins || heldCoins == 0 && population >= gimmiegoldMinimum
    fun goldComplete(catches: Int, absent: Boolean) = goldEnvironmentReady && absent &&
        catches >= gimmiegoldMinimum && (heldCoins ?: 0) == 0 && coinUseResolved(catches)
    val gimmiegoldMinimum get() = maxOf(3, coinsPicked + startingCoins, coinsSpent, validatedCoinDemand)
    val hasHeldCoins get() = (heldCoins ?: 0) > 0
    val knownCoinsUnresolved get() = hasHeldCoins ||
        coinsPicked + startingCoins > coinsSpent && !gimmiegoldReady()
    fun gimmiegoldReady() = goldEnvironmentReady && heldCoins == 0 && coinUseResolved(checkCount("Gimmiegold")) && extraCoinChecksResolved()
    fun gimmiegoldDebug() = "coinSourcesVerified=$goldEnvironmentReady sourcePath=${if (confirmedCoinSource) "confirmed-coins" else if (goldEnvironmentReady) "marker-scan" else "pending"} " +
        "starting=$startingCoins picked=$coinsPicked spent=$coinsSpent held=$heldCoins required=$gimmiegoldMinimum " +
        "accounted=${minOf(checkCount("Gimmiegold"), gimmiegoldMinimum)} retainedChecks=${checkCount("Gimmiegold")} " +
        "retainedUuids=${seenAtCaptureCount["Gimmiegold"]?.size ?: 0} liveUuids=${observed["Gimmiegold"]?.size ?: 0} " +
        "captures=${captured["Gimmiegold"] ?: 0} checked=${locallyChecked("Gimmiegold")}"
    fun capture(name: String) { captured.merge(name, 1, Int::plus) }
    fun checked(name: String) = locallyChecked(name) || !requiresCapture(name) && name in partyCompleted
    fun wumpaPrerequisite(name: String, party: PartySparklingState) =
        SafariRoster.named(name)?.biome == SafariBiome.ICY && party.needs("Wumpa")
    fun scan(biome: SafariBiome, x: Double, z: Double, seen: List<Pair<String, UUID>>,
        nestsChecked: Boolean = false, moundsCleared: Boolean = false, wallsCleared: Boolean = false,
        birdsSpawned: Boolean = false, hauntedDropsRemaining: Int = -1,
        hauntedCoinDropsRemaining: Int = hauntedDropsRemaining) {
        observed.clear()
        seen.filter { SafariRoster.named(it.first)?.biome == biome }.forEach { (name, uuid) -> observed.getOrPut(name) { mutableSetOf() }.add(uuid) }
        val (cx, cz) = centers.getValue(biome)
        val atCenter = (x - cx) * (x - cx) + (z - cz) * (z - cz) <= 225
        observed.forEach { (name, ids) ->
            val zone = SafariCompletionZones.forBiome(biome)[name]
            val qualifies = if (zone != null) zone.contains(x, z, sparkling = true)
                else name != "Gimmiegold" || atCenter
            if (qualifies && name !in birds) ids.forEach { uuid ->
                seenAtCaptureCount.getOrPut(name) { mutableMapOf() }.putIfAbsent(uuid, captured[name] ?: 0)
            }
        }
        fun total(name: String) = retainedCheckCount(name)
        fun enough(name: String, amount: Int = 1) = total(name) >= amount
        fun check(name: String, ready: Boolean) { if (ready) completed.add(name) else completed.remove(name) }
        SafariCompletionZones.forBiome(biome).forEach { (name, zone) ->
            if (zone.contains(x, z, sparkling = true)) {
                currentCounts[name] = total(name)
                check(name, enough(name, SafariFullClear.minimum(name)))
            }
        }
        SafariCompletionZones.uuidMinimums.filterKeys { SafariRoster.named(it)?.biome == biome }.forEach { (name, minimum) ->
            currentCounts[name] = total(name)
            check(name, currentCounts.getValue(name) >= minimum)
        }
        when (biome) {
            SafariBiome.FOREST -> {
                nestEvidence(nestsChecked)
                birds.forEach { currentCounts[it] = total(it) }
                birds.forEach { check(it, birdsSpawned) }
            }
            SafariBiome.CAVERN -> {
                check("Rockmite", moundsCleared); check("Snoozle", wallsCleared)
            }
            SafariBiome.ICY -> Unit
            SafariBiome.HAUNTED -> {
                confirmedCoinSource = confirmedFloorCoinsResolved()
                val sourceResolved = hauntedCoinDropsRemaining == 0 || confirmedCoinSource
                goldEnvironmentReady = sourceResolved && (atCenter || goldEnvironmentReady)
                val alreadyChecked = locallyChecked("Gimmiegold")
                if (atCenter) currentCounts["Gimmiegold"] = if (alreadyChecked)
                    maxOf(checkCount("Gimmiegold"), total("Gimmiegold")) else total("Gimmiegold")
                val retainedCheck = alreadyChecked && sourceResolved && heldCoins == 0 && extraCoinChecksResolved() &&
                    coinUseResolved(checkCount("Gimmiegold")) && checkCount("Gimmiegold") >= gimmiegoldMinimum
                check("Gimmiegold", retainedCheck ||
                    goldComplete(captured["Gimmiegold"] ?: 0, observed["Gimmiegold"].isNullOrEmpty()) ||
                    gimmiegoldReady() && enough("Gimmiegold", gimmiegoldMinimum))
            }
        }
    }
    fun chat(raw: String, manualAllowed: Boolean = false) {
        val text = SafariRules.strip(raw)
        if (text == "A rumbling sound can be heard, and the door at the back of the chamber opens..." &&
            gemzieChamberOpenedAt == null) gemzieChamberOpenedAt = clock()
        if (text.startsWith("FLOOR DROP!") && Regex("\\bShining Coin\\b(?! Shard)").containsMatchIn(text)) {
            val quantity = FloorPickup.parse(text)?.quantity ?: 1
            if (locallyChecked("Gimmiegold") || extraCoinUuidBaseline != null) addExtraCoinChecks(quantity)
            if (locallyChecked("Gimmiegold")) validatedCoinDemand = maxOf(validatedCoinDemand, minOf(checkCount("Gimmiegold"), gimmiegoldMinimum)) + quantity
            else if (validatedCoinDemand > 0) validatedCoinDemand += quantity
            coinsPicked += quantity; heldCoins = null; completed.remove("Gimmiegold")
        }
        if (text == "A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!") {
            coinsSpent++; heldCoins = null; completed.remove("Gimmiegold")
        }
        if (manualAllowed) {
            globalCompletions[text]?.let(completed::add)
            partyCompleted.addAll(doneBiome(text)?.critters?.map { it.name } ?: emptyList())
        }
    }
    fun inventory(stacks: List<Pair<String, Int>>) {
        heldCoins = stacks.filter { SafariRules.strip(it.first).trim() == "Shining Coin" }.sumOf { it.second.coerceAtLeast(0) }
        if (heldCoins != 0) completed.remove("Gimmiegold")
    }
    companion object {
        /** These bosses require an actual shared catch, even when their UUID was already checked. */
        fun requiresCapture(name: String) = name == "Wumpa" || name == "Doomspiral"
        private const val BASE_COIN_FLOOR_DROPS = 3
        private val globalCompletions = mapOf(
            "The darkness in the Haunted Biome fades away..." to "Doomspiral",
            "The cave opens up again..." to "Wumpa")
        private val sourceChecks = setOf("Honeybug", "Rockmite", "Snoozle", "Bluebird", "Parakeet", "Macaw", "Gimmiegold")
        private val birds = setOf("Bluebird", "Parakeet", "Macaw")
        private val centers = mapOf(SafariBiome.FOREST to (6.0 to 51.0), SafariBiome.CAVERN to (-114.0 to 49.0),
            SafariBiome.ICY to (-112.0 to -54.0), SafariBiome.HAUNTED to (-4.0 to -64.0))
        fun doneBiome(raw: String): SafariBiome? {
            val word = PartyChat.parse(SafariRules.strip(raw))?.body?.trim()?.removeSuffix(".")?.lowercase(Locale.ROOT)
            return doneWords[word]
        }
        private val doneWords = mapOf("fd" to SafariBiome.FOREST, "cd" to SafariBiome.CAVERN,
            "id" to SafariBiome.ICY, "hd" to SafariBiome.HAUNTED)
    }
}
