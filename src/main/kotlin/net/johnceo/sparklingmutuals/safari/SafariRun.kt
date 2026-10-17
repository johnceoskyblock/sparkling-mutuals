package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.history.RunTally
import net.johnceo.sparklingmutuals.safari.completion.BiomeClearEvidence
import net.johnceo.sparklingmutuals.safari.completion.SafariCompletionEvents
import net.johnceo.sparklingmutuals.safari.completion.SafariCompletionZones
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.completion.SparklingChecks
import net.johnceo.sparklingmutuals.safari.esp.EspEntity
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import net.johnceo.sparklingmutuals.safari.resources.SafariBirdLedger
import net.johnceo.sparklingmutuals.safari.resources.SafariResources
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingCaptureState
import net.johnceo.sparklingmutuals.safari.structures.MoundSurvey
import net.johnceo.sparklingmutuals.safari.structures.SafariMounds

/** Capture, source and completion evidence owned by a single Safari run. */
class SafariRun(val startedAt: Long) {
    var historyExcluded = false
        private set
    fun excludeFromHistory() { historyExcluded = true }
    val entry = SafariEntryEvidence()
    val resources = SafariResources()
    val gimmiegoldCheck = net.johnceo.sparklingmutuals.safari.completion.GimmiegoldSparklingCheck()
    private var localPlayerName: String? = null
    fun localPlayer(name: String) { localPlayerName = name }
    private fun syncGimmiegoldContributions() {
        gimmiegoldCheck.contributions(resources.startingCount(FloorDropType.COIN), localPlayerName, resources.coinContributions())
    }
    private fun gimmiegoldCaptureMinimum(): Int {
        syncGimmiegoldContributions()
        return maxOf(sparklingChecks.gimmiegoldMinimum, gimmiegoldCheck.required,
            resources.requiredPopulation(SafariBiome.HAUNTED))
    }
    fun scanGimmiegolds(live: Collection<java.util.UUID>, qualified: Boolean) {
        syncGimmiegoldContributions()
        gimmiegoldCheck.scan(count("Gimmiegold"), live, qualified)
    }
    val resourceReminders = net.johnceo.sparklingmutuals.safari.resources.StartingResourceDelivery(resources)
    val sparklingParticles = net.johnceo.sparklingmutuals.safari.sparkling.SparklingParticleLocations()
    val sparklingCaptures = SparklingCaptureState()
    fun observeSparkling(id: java.util.UUID, name: String, aliases: Set<java.util.UUID> = emptySet()): java.util.UUID {
        if (sparklingCaptures.observe(id, name, aliases)) SafariRoster.named(name)?.biome?.let(completion::reopen)
        return sparklingCaptures.identity(id)
    }
    fun sparklePending(name: String) = sparklingCaptures.pending(name)
    fun sparklingBroadcast(raw: String, now: Long = System.currentTimeMillis()) {
        val match = RunTally.sparkling.matchEntire(raw) ?: return
        val species = SafariRoster.named(match.groupValues[2].trim())?.name ?: return
        sparklingCaptures.capture(species, match.groupValues[1], broadcast = true, at = now)
    }
    private var liveBees = 0
    val beeNestCountKnown get() = foundNests > 0 && (nestSitesUnknown == 0 || nestsChecked)
    private val beePopulationAccountsForNests get() = beeNestCountKnown && count("Honeybug") + liveBees >= foundNests
    val nestsResolved get() = nestsChecked || beePopulationAccountsForNests
    private var honeybugLocalCheck = false
    private fun refreshNestChecking() {
        if (lastBiome == SafariBiome.FOREST || nestsChecked) honeybugLocalCheck = nestsResolved
        sparklingChecks.nestEvidence(honeybugLocalCheck)
    }
    fun liveHoneybugs(amount: Int) { liveBees = amount; refreshNestChecking() }
    var hadFourIncense = false
        private set
    private var firstInventoryAt: Long? = null
    fun inStartingInventoryWindow(now: Long) = firstInventoryAt?.let { now - it in 0..3000 } == true
    fun inventoryEvidence(stacks: List<Pair<String, Int>>, now: Long = System.currentTimeMillis()) {
        if (firstInventoryAt == null && now >= startedAt) firstInventoryAt = now
        resources.inventory(stacks, inStartingInventoryWindow(now))
        gimmiegoldCheck.inventory(stacks.filter { SafariRules.strip(it.first).trim() == "Shining Coin" }.sumOf { it.second.coerceAtLeast(0) })
        syncGimmiegoldContributions()
        birds.inventory(stacks)
        sparklingChecks.inventory(stacks)
        sparklingChecks.startingCoins(resources.startingCount(FloorDropType.COIN))
        if (endedAt == null && firstInventoryAt != null) birds.startingInventory(stacks, now - firstInventoryAt!!)
        if (stacks.filter { SafariRules.strip(it.first).trim() == "Soothing Incense" }.sumOf { it.second.coerceAtLeast(0) } >= 4)
            hadFourIncense = true
    }
    var brokenMounds = 0
        private set
    var rockmiteMounds = 0
        private set
    var endedAt: Long? = null
    var lastBiome: SafariBiome? = null
    private val counts = mutableMapOf<String, Int>()
    private val personal = mutableMapOf<String, Int>()
    private val catchEvents = mutableListOf<SafariCatch>()
    val catches: List<SafariCatch> get() = catchEvents.toList()
    private val knownPopulation = mutableMapOf<String, Int>()
    /** Current uncaptured UUIDs, never the union of pre/post-escape identities. */
    fun observePopulation(live: List<Pair<String, java.util.UUID>>) {
        val remaining = live.filterNot { sparklingCaptures.resolved(it.second) }
        remaining.groupBy({ it.first }, { it.second }).forEach { (name, ids) ->
            val critter = SafariRoster.named(name) ?: return@forEach
            if ((critter.biome in biomeReports || name in SafariFullClear.fixedCaptureTargets) && name != "Macaw") {
                val remaining = if (name in birdSpecies) ids.size else ids.distinct().size
                fullClearObservedTargets[name] = maxOf(fullClearObservedTargets[name] ?: 0, count(name) + remaining)
            }
        }
        remaining.filter { it.first !in birdSpecies }.groupBy({ it.first }, { it.second }).forEach { (name, ids) ->
            knownPopulation[name] = maxOf(knownPopulation[name] ?: 0, count(name) + ids.distinct().size)
        }
    }
    private val visited = mutableSetOf<SafariBiome>()
    private var startingBiome: SafariBiome? = null
    private val partyFullClears = mutableSetOf<SafariBiome>()
    private val communalClears = mutableSetOf<SafariBiome>()
    private val biomeReports = mutableSetOf<SafariBiome>()
    private var remoteForestReport = false
    val honeybugRemoteComplete get() = remoteForestReport && !sparklePending("Honeybug")
    private val fullClearObservedTargets = mutableMapOf<String, Int>()
    fun uniqueReportReceived(biome: SafariBiome) = biome in biomeReports
    fun fullClearReportReceived(biome: SafariBiome) = biome in biomeReports
    private val uniqueSourceContradictions = mutableSetOf<String>()
    fun partyFullClear(raw: String) {
        SparklingChecks.doneBiome(raw)?.let {
            partyFullClears.add(it); communalClears.add(it)
            biomeReports.add(it)
            if (it == SafariBiome.FOREST) remoteForestReport = true
            if (it == SafariBiome.CAVERN && lastBiome != SafariBiome.CAVERN) {
                remainingMounds = false; remainingWalls = false
            }
        }
    }
    fun fullClearProgress(critters: List<SafariCritter>) = critters.count { fullClearComplete(it.name) }
    /** Local proof advances progress directly; remote reports use capture minima. PBs remain personal. */
    fun fullClearComplete(name: String): Boolean {
        val critter = SafariRoster.named(name) ?: return false
        if (sparklePending(name) || sourcePending(name)) return false
        if (name in birdSpecies && birds.knownFoodUnresolved ||
            name == "Gimmiegold" && sparklingChecks.knownCoinsUnresolved) return false
        if (name == "Honeybug") {
            return fullClearMissingComplete(name)
        }
        if (name in uniqueSourceContradictions && !sparklingChecks.locallyChecked(name)) return false
        // The local checklist already requires qualified coverage, captures and resolved sources.
        // Its retained proof remains valid after leaving the scan radius or biome.
        if (fullClearMissingComplete(name)) return true
        if (name == "Macaw" && uniqueUnavailable(name)) return true
        val fixed = SafariFullClear.fixedCaptureTargets[name]
        if (fixed == null && critter.biome !in biomeReports) return false
        val minimum = when (name) {
            "Macaw" -> 1
            "Bluebird", "Parakeet" -> birds.spawnedCount(name)
            "Rockmite" -> maxOf(rockmiteMounds, if (encountered(name)) 1 else 0)
            "Snoozle" -> if (encountered(name)) 1 else 0
            "Gimmiegold" -> maxOf(SafariFullClear.minimum(name), gimmiegoldCaptureMinimum())
            else -> SafariFullClear.minimum(name)
        }
        return count(name) >= maxOf(fixed ?: minimum, fullClearObservedTargets[name] ?: 0)
    }
    private val inheritedBiomes = mutableSetOf<SafariBiome>()
    fun visitBiome(biome: SafariBiome) {
        if (startingBiome == null) startingBiome = biome
        if (lastBiome != biome) {
            partyFullClears.remove(biome); communalClears.remove(biome)
            sparklingChecks.reenter(biome)
            if (biome == SafariBiome.HAUNTED) gimmiegoldCheck.reenter()
            if (biome == SafariBiome.FOREST) { birds.reenter(); forestBirdsSurveyed = false }
            zoneScanConfirmed.removeAll(biome.critters.map { it.name }.toSet())
        }
        lastBiome = biome
        if (visited.add(biome) && biome in priorLootShare) inheritedBiomes.add(biome)
    }
    private val priorLootShare = mutableSetOf<SafariBiome>()
    private var remainingMounds = false
    private var remainingWalls = false
    /** Loaded Cavern sources can be observed from any Safari biome; unknown space is not broken. */
    fun observeCavernSources(moundsRemain: Boolean, wallsRemain: Boolean, allWallsBroken: Boolean) {
        if (wallsRemain) wallsChecked = false else if (allWallsBroken) wallsChecked = true
        reconcileStructures(SafariBiome.CAVERN, moundsRemain, wallsRemain)
        sparklingChecks.cavernSourceEvidence(allMoundsBroken && !moundsRemain, allWallsBroken)
    }
    fun inherited(biome: SafariBiome) = biome in inheritedBiomes
    val partyNestsHandled get() = nestsResolved
    val inheritedMounds get() = inherited(SafariBiome.CAVERN) || SafariBiome.CAVERN in communalClears
    private val inheritedGold get() = inherited(SafariBiome.HAUNTED) || SafariBiome.HAUNTED in communalClears
    fun reconcileStructures(biome: SafariBiome, moundsRemain: Boolean, wallsRemain: Boolean) {
        if (biome == SafariBiome.CAVERN) {
            if (moundsRemain) uniqueSourceContradictions.add("Rockmite")
            else if (allMoundsBroken) uniqueSourceContradictions.remove("Rockmite")
            if (wallsRemain) uniqueSourceContradictions.add("Snoozle")
            else if (wallsChecked) uniqueSourceContradictions.remove("Snoozle")
        }
        if (biome == SafariBiome.CAVERN) { remainingMounds = moundsRemain; remainingWalls = wallsRemain }
        if (biome == SafariBiome.CAVERN && (moundsRemain || wallsRemain)) partyFullClears.remove(biome)
        sparklingChecks.reconcileStructures(biome, moundsRemain, wallsRemain)
    }
    private fun cavernSourcesChecked(name: String) = when (name) {
        "Rockmite" -> allMoundsBroken && !remainingMounds
        "Snoozle" -> wallsChecked && !remainingWalls
        else -> false
    }
    /** Party reports never resolve a positively observed intact source. */
    private fun partyCompletion(name: String, party: PartySparklingState): Boolean? {
        if (name == "Honeybug") return if (honeybugRemoteComplete) true else null
        val critter = SafariRoster.named(name) ?: return null
        if (critter.biome !in communalClears && !sparklingChecks.partyChecked(name)) return null
        if (critter.biome == SafariBiome.CAVERN && lastBiome != SafariBiome.CAVERN) return true
        return when (name) {
            "Rockmite" -> !party.needs(name) || !remainingMounds
            "Snoozle" -> !party.needs(name) || !remainingWalls
            else -> true
        }
    }
    fun sparklingComplete(name: String, party: PartySparklingState, localOnly: Boolean = false): Boolean {
        val critter = SafariRoster.named(name) ?: return false
        if (sparklePending(name)) return false
        if (sparklingCaptures.captured(name)) return true
        if (SparklingChecks.requiresCapture(name)) return count(name) > 0
        if (name == "Rockmite" || name == "Snoozle") {
            if (name in uniqueSourceContradictions && !sparklingChecks.locallyChecked(name)) return false
            if (cavernSourcesChecked(name) || sparklingChecks.locallyChecked(name)) return true
            return !localOnly && partyCompletion(name, party) == true
        }
        if (name == "Honeybug") return (!localOnly && honeybugRemoteComplete) || honeybugLocalCheck ||
            lastBiome == SafariBiome.FOREST && sparklingChecks.locallyChecked(name)
        if (name == "Gimmiegold") {
            if (!localOnly && lastBiome != SafariBiome.HAUNTED) partyCompletion(name, party)?.let { return it }
            syncGimmiegoldContributions()
            return gimmiegoldCheck.complete
        }
        if (sourcePending(name)) return false
        if (name in birdSpecies && !birds.sparklingSpawned) return false
        if (name == "Macaw" && !encountered(name) && !birds.hasMacaws(count(name), nearbyMacaws) && !uniqueUnavailable(name)) return false
        if (name in birdSpecies && birds.accountedFeedings < resources.requiredPopulation(SafariBiome.FOREST)) return false
        if (!localOnly) partyCompletion(name, party)?.let { return it }
        return sparklingChecks.locallyChecked(name)
    }
    fun completed(mode: SafariMode, critters: List<SafariCritter>, party: PartySparklingState, localOnly: Boolean = false): Int =
        critters.count { !sparklePending(it.name) && when (mode) {
            SafariMode.UNIQUE -> uniqueComplete(it.name, party, localOnly) &&
                (it.name != "Honeybug" || !party.needs(it.name) || sparklingComplete(it.name, party, localOnly))
            SafariMode.FULL_CLEAR -> if (localOnly) captureComplete(it.name) else fullClearComplete(it.name)
            SafariMode.SPARKLING -> sparklingComplete(it.name, party, localOnly)
        } }
    val completion = SafariCompletionEvents()
    private fun uniqueReportedComplete(name: String): Boolean {
        if (name !in setOf("Rockmite", "Snoozle") || SafariBiome.CAVERN !in biomeReports) return false
        return name !in uniqueSourceContradictions || cavernSourcesChecked(name)
    }
    /** Cavern species can combine one shared catch with safari-wide source proof; cd can confirm no spawn. */
    fun uniqueComplete(name: String, party: PartySparklingState, localOnly: Boolean = false): Boolean {
        if (sparklePending(name)) return false
        if (sparklingCaptures.captured(name)) return true
        if (name == "Macaw" && uniqueUnavailable(name)) return true
        if (name == "Rockmite" || name == "Snoozle") {
            if (name in uniqueSourceContradictions && !sparklingChecks.locallyChecked(name)) return false
            val checked = cavernSourcesChecked(name) || sparklingChecks.locallyChecked(name)
            if (count(name) > 0) return checked || !localOnly && uniqueReportedComplete(name)
            // Empty sources do not fabricate a catch or prove no spawn without the party report.
            return !localOnly && checked && !encountered(name) && uniqueReportReceived(SafariBiome.CAVERN)
        }
        return count(name) > 0
    }
    /** A completed local survey can prove a variable critter never spawned; a sighting revokes it. */
    fun uniqueUnavailable(name: String): Boolean {
        if (name == "Macaw") {
            if (!forestBirdsSurveyed) return false
            val remaining = nearby[SafariBiome.FOREST] ?: return false
            return !encountered(name) && name !in remaining && !birds.hasMacaws(count(name), nearbyMacaws) &&
                birds.feedingResolved && birds.accountedFeedings >= resources.requiredPopulation(SafariBiome.FOREST) && !sourcePending(name)
        }
        val remaining = nearby[SafariBiome.CAVERN] ?: return false
        if (encountered(name) || name in remaining) return false
        return when (name) {
            "Rockmite" -> allMoundsBroken && !remainingMounds && "Rockmite Mound" !in remaining
            "Snoozle" -> wallsChecked && !remainingWalls
            else -> false
        }
    }
    private val sightings = mutableSetOf<String>()
    private val observedCritters = mutableMapOf<String, MutableSet<Int>>()
    private val observedUuids = mutableMapOf<String, MutableSet<java.util.UUID>>()
    private val zoneScanConfirmed = mutableSetOf<String>()
    // Missing-panel coverage survives leaving a zone/biome; a positive remaining sighting revokes it.
    private val missingScanConfirmed = mutableSetOf<String>()
    private val zonesRequired = mutableSetOf<SafariBiome>()
    val birds = SafariBirdLedger()
    val sparklingChecks = SparklingChecks()
    private val nearby = mutableMapOf<SafariBiome, Set<String>>()
    private var wallsChecked = false
    private var nestsChecked = false
    private var punchedNests = 0
    private var foundNests = 0
    var nestSitesUnknown: Int? = null
        private set
    val nestSurveyComplete get() = nestsResolved
    val honeybugCaptureTarget get() = if (beeNestCountKnown) foundNests else
        maxOf(SafariFullClear.minimum("Honeybug"), foundNests, punchedNests, observedUuids["Honeybug"]?.size ?: 0)
    val nestCaptureQuotaMet get() = beeNestCountKnown && count("Honeybug") >= honeybugCaptureTarget
    /** Full Clear cannot trust reports or rendered bees to retire unpunched source work. */
    fun nestCounterNeeded(mode: SafariMode, party: PartySparklingState): Boolean {
        if (mode == SafariMode.FULL_CLEAR) return !nestsChecked && !nestCaptureQuotaMet
        if (nestsResolved || honeybugRemoteComplete) return false
        return when (mode) {
            SafariMode.FULL_CLEAR -> true
            SafariMode.UNIQUE -> count("Honeybug") == 0 || party.needs("Honeybug")
            SafariMode.SPARKLING -> party.needs("Honeybug")
        }
    }
    /** Block evidence updates immediately, including when the entity scanner has no fresh snapshot. */
    fun updateNestEvidence(checked: Boolean, punched: Int, found: Int = punched, unknown: Int? = null) {
        val expanded = found > foundNests
        nestsChecked = checked || nestsChecked && !expanded
        punchedNests = maxOf(punchedNests, punched.coerceAtLeast(0))
        foundNests = maxOf(foundNests, found.coerceAtLeast(0))
        if (unknown != null) nestSitesUnknown = minOf(nestSitesUnknown ?: Int.MAX_VALUE, unknown.coerceAtLeast(0))
        if (expanded) honeybugLocalCheck = false
        refreshNestChecking()
        if (!checked || personalCount("Honeybug") < honeybugCaptureTarget) biomeClears.remove(SafariBiome.FOREST)
    }
    private val birdSpecies = setOf("Bluebird", "Parakeet", "Macaw")
    private var nearbyMacaws = 0
    private var forestBirdsSurveyed = false
    val birdFoodsComplete get() = birds.foodsComplete
    fun recordBirdFood(raw: String, biome: SafariBiome? = SafariBiome.FOREST, now: Long = System.currentTimeMillis()) =
        birds.pickup(raw, biome, now - startedAt)
    fun updateCaptureEvidence(biome: SafariBiome, speciesInRange: Set<String>, allWallsChecked: Boolean, allNestsChecked: Boolean = false,
        macawsInRange: Int = if ("Macaw" in speciesInRange) 1 else 0,
        playerX: Double? = null, playerZ: Double? = null, nestsPunched: Int = 0, nestsFound: Int = nestsPunched) {
        nearby[biome] = speciesInRange
        speciesInRange.filter { it != "Macaw" && SafariRoster.named(it) != null }.forEach { name ->
            if (biome in biomeReports || name in SafariFullClear.fixedCaptureTargets)
                fullClearObservedTargets[name] = maxOf(fullClearObservedTargets[name] ?: 0, count(name) + 1)
        }
        partyFullClears.remove(biome); communalClears.remove(biome); sparklingChecks.localScan(biome)
        zoneScanConfirmed.removeAll(speciesInRange)
        missingScanConfirmed.removeAll(speciesInRange - "Macaw")
        if (speciesInRange.isNotEmpty()) partyFullClears.remove(biome)
        if (biome == SafariBiome.CAVERN) wallsChecked = allWallsChecked
        if (biome == SafariBiome.FOREST) updateNestEvidence(allNestsChecked, nestsPunched, nestsFound)
        if (biome == SafariBiome.FOREST) nearbyMacaws = macawsInRange
        if (SafariCompletionZones.forBiome(biome).isNotEmpty() && playerX != null && playerZ != null) {
            zonesRequired.add(biome)
            SafariCompletionZones.forBiome(biome).forEach { (name, zone) ->
                if (name !in speciesInRange && zone.contains(playerX, playerZ, sparkling = false) &&
                    count(name) >= SafariFullClear.minimum(name)) {
                    zoneScanConfirmed.add(name)
                    missingScanConfirmed.add(name)
                }
            }
            SafariCompletionZones.uuidMinimums.filterKeys { SafariRoster.named(it)?.biome == biome }.forEach { (name, minimum) ->
                if (name !in speciesInRange && count(name) >= minimum) {
                    zoneScanConfirmed.add(name)
                    missingScanConfirmed.add(name)
                }
            }
        }
    }
    /** Shared captures + current nearby bird counts, never a history of distinct UUIDs. */
    fun scanBirdSources(dropsRemaining: Int, live: List<String> = emptyList()) {
        if (lastBiome == SafariBiome.FOREST) forestBirdsSurveyed = true
        nearbyMacaws = live.count { it == "Macaw" }
        sightings.addAll(live.filter { it in birdSpecies })
        fun population(name: String) = count(name) + live.count { it == name }
        val population = population("Bluebird") + population("Parakeet") + population("Macaw") / 2
        birds.scan(dropsRemaining, population)
        resources.scan(SafariBiome.FOREST, birds.accountedFeedings)
    }
    fun hasCaptureEvidence(biome: SafariBiome) = biome in nearby
    /** Local checklist proof can advance progress without a party report. */
    fun fullClearMissingComplete(name: String): Boolean {
        val critter = SafariRoster.named(name) ?: return false
        if ((name in SafariCompletionZones.forBiome(critter.biome) || name in SafariCompletionZones.uuidMinimums) &&
            name !in missingScanConfirmed) return false
        return captureCompleteWithZones(name, false, nearby[critter.biome], null, missingScanConfirmed)
    }
    fun missing(critter: SafariCritter, fullClear: Boolean) = if (fullClear)
        !fullClearMissingComplete(critter.name) else count(critter.name) == 0
    fun birdCount(personalOnly: Boolean = false) = birdSpecies.sumOf { if (personalOnly) personalCount(it) else count(it) }
    private fun birdComplete(species: String, speciesInRange: Set<String>?, personalOnly: Boolean = false): Boolean {
        if (speciesInRange == null || !birds.feedingResolved || birds.accountedFeedings < resources.requiredPopulation(SafariBiome.FOREST) || sourcePending(species)) return false
        val captured = if (personalOnly) personalCount(species) else count(species)
        return if (species == "Macaw") captured >= 1 || uniqueUnavailable(species)
            else species !in speciesInRange && captured >= birds.spawnedCount(species)
    }
    fun birdsComplete(speciesInRange: Set<String>?, personalOnly: Boolean = false) =
        birdSpecies.all { birdComplete(it, speciesInRange, personalOnly) }
    fun sourcePending(name: String) = when (name) {
        "Bluebird", "Parakeet", "Macaw" -> resources.pending(SafariBiome.FOREST)
        "Gimmiegold" -> resources.pending(SafariBiome.HAUNTED)
        else -> false
    }
    fun captureComplete(species: String, personalOnly: Boolean = false,
        speciesInRange: Set<String>? = nearby[SafariRoster.named(species)!!.biome], structures: BiomeClearEvidence? = null): Boolean =
        captureCompleteWithZones(species, personalOnly, speciesInRange, structures, zoneScanConfirmed)

    private fun captureCompleteWithZones(species: String, personalOnly: Boolean,
        speciesInRange: Set<String>?, structures: BiomeClearEvidence?, confirmedZones: Set<String>): Boolean {
        if (sourcePending(species) || sparklePending(species)) return false
        val captured = if (personalOnly) personalCount(species) else count(species)
        // The initial known nest total and actual catch messages suffice from any biome.
        if (species == "Honeybug" && beeNestCountKnown)
            return captured >= maxOf(honeybugCaptureTarget, knownPopulation[species] ?: 0)
        val remaining = speciesInRange ?: return false
        if (species in birdSpecies) return birdComplete(species, remaining, personalOnly)
        val minimum = if (species == "Honeybug") honeybugCaptureTarget else SafariFullClear.minimum(species)
        if (captured < maxOf(minimum, if (species == "Macaw") 0 else knownPopulation[species] ?: 0)) return false
        if (species == "Gimmiegold" && captured < gimmiegoldCaptureMinimum()) return false
        val biome = SafariRoster.named(species)!!.biome
        if (biome in zonesRequired && species == "Gimmiegold" &&
            !sparklingChecks.goldComplete(captured, species !in remaining)) return false
        if (biome in zonesRequired && (species in SafariCompletionZones.forBiome(biome) || species in SafariCompletionZones.uuidMinimums) &&
            species !in confirmedZones) return false
        if (species in remaining) return false
        return when (species) {
            "Honeybug" -> ((structures?.nestsChecked ?: nestsChecked) || beePopulationAccountsForNests) &&
                captured >= maxOf(honeybugCaptureTarget, structures?.nestsFound ?: 0)
            "Snoozle" -> structures?.wallsCleared ?: wallsChecked
            "Rockmite" -> (structures?.moundsCleared ?: moundsComplete(remaining)) &&
                captured >= rockmiteMounds && "Rockmite Mound" !in remaining
            else -> true
        }
    }
    val biomeClears = mutableSetOf<SafariBiome>()
    val uniqueBiomeClears = mutableSetOf<SafariBiome>()
    val partyUniqueClears = mutableSetOf<SafariBiome>()
    val moundSurvey = MoundSurvey()
    val allMoundsBroken get() = moundSurvey.allBroken
    fun moundsComplete(remaining: Set<String>? = nearby[SafariBiome.CAVERN]) = remaining != null &&
        allMoundsBroken && moundSurvey.remaining == 0 && "Rockmite Mound" !in remaining
    fun encountered(name: String) = count(name) > 0 || name in sightings || name == "Rockmite" && rockmiteMounds > 0
    fun observe(entity: EspEntity) {
        if (entity.type == "silverfish" || entity.type == "sniffer") SafariEspRules.identify(entity)?.let { sightings.add(it.name) }
    }
    fun observeCritter(id: Int, species: String, uuid: java.util.UUID? = null) {
        observedCritters.getOrPut(species) { mutableSetOf() }.add(id); sightings.add(species)
        if (uuid != null) observedUuids.getOrPut(species) { mutableSetOf() }.add(uuid)
    }
    fun observedCount(species: String) = observedCritters[species]?.size ?: 0
    fun record(catch: SafariCatch, now: Long = System.currentTimeMillis(), self: String? = null) {
        if (catch.sparkling) sparklingCaptures.capture(catch.critter.name, if (catch.personal) self else catch.catcherName, broadcast = false, at = now)
        catchEvents.add(catch)
        resources.capture(catch.critter.name)
        if (!catch.personal && catch.critter.biome !in visited) priorLootShare.add(catch.critter.biome)
        sparklingChecks.capture(catch.critter.name)
        counts.merge(catch.critter.name, 1, Int::plus)
        if (catch.personal) personal.merge(catch.critter.name, 1, Int::plus)
    }
    fun recordMound(raw: String): Boolean {
        val found = SafariMounds.outcome(raw) ?: return false
        brokenMounds++
        if (found) rockmiteMounds++
        return true
    }
    fun count(name: String) = counts[name] ?: 0
    fun personalCount(name: String) = personal[name] ?: 0
    fun elapsed(now: Long) = ((endedAt ?: now) - startedAt).coerceAtLeast(0)
    fun complete(critter: SafariCritter, unique: Boolean, fullClear: Boolean = false) = count(critter.name) >= critter.required(unique, fullClear)
    fun progress(critters: List<SafariCritter>, unique: Boolean, fullClear: Boolean = false) = critters.count { complete(it, unique, fullClear) }
}
