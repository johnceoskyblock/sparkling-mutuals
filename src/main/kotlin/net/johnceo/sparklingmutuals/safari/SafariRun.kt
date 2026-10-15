package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.history.achievements.*
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
    val runId: String = java.util.UUID.randomUUID().toString()
    private var achievementSnapshot: DexSnapshot? = null
    private var achievementUniqueValid = true
    private var achievementScopeValid = true
    /** Attach once, at entry. A late API response cannot supply a before-run dex. */
    fun attachAchievementSnapshot(snapshot: DexSnapshot) {
        if (achievementSnapshot != null || snapshot.observedAt > startedAt) return
        achievementSnapshot = snapshot.copy(discoveries = snapshot.discoveries.toSet())
    }
    /** Null/scope change invalidates all provenance; changed discoveries invalidate unique input. */
    fun refreshAchievementDex(snapshot: DexSnapshot?) {
        val before = achievementSnapshot ?: return
        if (snapshot == null || snapshot.scope.key != before.scope.key) {
            achievementScopeValid = false
            achievementUniqueValid = false
        } else if (snapshot.discoveries != before.discoveries) achievementUniqueValid = false
    }
    fun achievementEvidence(id: String, provenanceComplete: Boolean): AchievementRunEvidence? {
        val before = achievementSnapshot ?: return null
        return AchievementRunEvidence(id, before.scope,
            if (achievementUniqueValid) AchievementCalibration.rates.keys - before.discoveries else null,
            personal.toMap(), catchEvents.filter { it.personal && it.sparkling }.map { it.critter.name }.toSet(),
            provenanceComplete && achievementScopeValid)
    }
    val entry = SafariEntryEvidence()
    val resources = SafariResources()
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
    private val beePopulationAccountsForNests get() = foundNests > 0 && nestSitesUnknown == 0 && count("Honeybug") + liveBees >= foundNests
    val nestsResolved get() = nestsChecked || beePopulationAccountsForNests
    fun liveHoneybugs(amount: Int) { liveBees = amount; sparklingChecks.nestEvidence(nestsResolved) }
    var hadFourIncense = false
        private set
    fun inventoryEvidence(stacks: List<Pair<String, Int>>, now: Long = System.currentTimeMillis()) {
        resources.inventory(stacks, now - startedAt in 0..3000)
        birds.inventory(stacks)
        sparklingChecks.inventory(stacks)
        sparklingChecks.startingCoins(resources.startingCount(FloorDropType.COIN))
        if (endedAt == null) birds.startingInventory(stacks, now - startedAt)
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
        live.groupBy({ it.first }, { it.second }).forEach { (name, ids) ->
            knownPopulation[name] = maxOf(knownPopulation[name] ?: 0, count(name) + ids.distinct().size)
        }
    }
    private val visited = mutableSetOf<SafariBiome>()
    private var startingBiome: SafariBiome? = null
    private val partyFullClears = mutableSetOf<SafariBiome>()
    private val communalClears = mutableSetOf<SafariBiome>()
    fun partyFullClear(raw: String) {
        SparklingChecks.doneBiome(raw)?.let {
            partyFullClears.add(it); communalClears.add(it)
            if (it == SafariBiome.CAVERN && lastBiome != SafariBiome.CAVERN) {
                remainingMounds = false; remainingWalls = false
            }
        }
    }
    fun fullClearProgress(critters: List<SafariCritter>) = critters.count {
        !sparklePending(it.name) && (it.biome in partyFullClears || captureComplete(it.name))
    }
    private val inheritedBiomes = mutableSetOf<SafariBiome>()
    fun visitBiome(biome: SafariBiome) {
        if (startingBiome == null) startingBiome = biome
        if (lastBiome != biome) {
            partyFullClears.remove(biome); communalClears.remove(biome)
            sparklingChecks.reenter(biome)
            if (biome == SafariBiome.FOREST) birds.reenter()
            zoneScanConfirmed.removeAll(biome.critters.map { it.name }.toSet())
        }
        lastBiome = biome
        if (visited.add(biome) && biome in priorLootShare) inheritedBiomes.add(biome)
    }
    private val priorLootShare = mutableSetOf<SafariBiome>()
    private var remainingMounds = false
    private var remainingWalls = false
    fun inherited(biome: SafariBiome) = biome in inheritedBiomes
    val partyNestsHandled get() = nestsResolved
    val inheritedMounds get() = inherited(SafariBiome.CAVERN) || SafariBiome.CAVERN in communalClears
    private val inheritedGold get() = inherited(SafariBiome.HAUNTED) || SafariBiome.HAUNTED in communalClears
    fun reconcileStructures(biome: SafariBiome, moundsRemain: Boolean, wallsRemain: Boolean) {
        if (biome == SafariBiome.CAVERN) { remainingMounds = moundsRemain; remainingWalls = wallsRemain }
        if (biome == SafariBiome.CAVERN && (moundsRemain || wallsRemain)) partyFullClears.remove(biome)
        sparklingChecks.reconcileStructures(biome, moundsRemain, wallsRemain)
    }
    /** Party done overrides progress, never captures; only needed Cavern structures can contradict it. */
    private fun partyCompletion(name: String, party: PartySparklingState): Boolean? {
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
        if (SparklingChecks.requiresCapture(name)) return count(name) > 0
        if (sourcePending(name)) return false
        if (name in birdSpecies && !birds.sparklingSpawned) return false
        if (name == "Gimmiegold" && sparklingChecks.checkCount(name) < resources.requiredPopulation(SafariBiome.HAUNTED)) return false
        if (name in birdSpecies && sparklingChecks.birdPopulation() < resources.requiredPopulation(SafariBiome.FOREST)) return false
        if (!localOnly) partyCompletion(name, party)?.let { return it }
        return sparklingChecks.locallyChecked(name)
    }
    fun completed(mode: SafariMode, critters: List<SafariCritter>, party: PartySparklingState, localOnly: Boolean = false): Int =
        critters.count { !sparklePending(it.name) && when (mode) {
            SafariMode.UNIQUE -> uniqueComplete(it.name, party, localOnly)
            SafariMode.FULL_CLEAR -> (!localOnly && it.biome in partyFullClears && !sourcePending(it.name)) || captureComplete(it.name)
            SafariMode.SPARKLING -> sparklingComplete(it.name, party, localOnly)
        } }
    val completion = SafariCompletionEvents()
    /** Unique progress retains sparkle checks; missing rows and PBs use their own capture evidence. */
    fun uniqueComplete(name: String, party: PartySparklingState, localOnly: Boolean = false): Boolean {
        if (count(name) == 0 || sparklePending(name)) return false
        return !party.needs(name) || sparklingComplete(name, party, localOnly = true)
    }
    /** A completed local survey can prove a variable critter never spawned; a sighting revokes it. */
    fun uniqueUnavailable(name: String): Boolean {
        if (name == "Macaw") {
            val remaining = nearby[SafariBiome.FOREST] ?: return false
            return !encountered(name) && name !in remaining && !birds.hasMacaws(count(name), nearbyMacaws) &&
                birds.feedingResolved && !sourcePending(name)
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
    val honeybugCaptureTarget get() = maxOf(SafariFullClear.minimum("Honeybug"), foundNests, punchedNests,
        observedUuids["Honeybug"]?.size ?: 0)
    /** The missing-panel nest counter tracks handling/spawning, never the capture quota. */
    fun nestCounterNeeded(mode: SafariMode, party: PartySparklingState): Boolean {
        val nests = maxOf(foundNests, punchedNests)
        val handledAndSpawned = nestsChecked && nests >= 3 && punchedNests >= nests &&
            (observedUuids["Honeybug"]?.size ?: 0) == nests
        if (handledAndSpawned || !nestsChecked && beePopulationAccountsForNests) return false
        return when (mode) {
            SafariMode.FULL_CLEAR -> true
            SafariMode.UNIQUE -> partyCompletion("Honeybug", party) != true &&
                (count("Honeybug") == 0 || party.needs("Honeybug"))
            SafariMode.SPARKLING -> partyCompletion("Honeybug", party) != true && party.needs("Honeybug")
        }
    }
    /** Block evidence updates immediately, including when the entity scanner has no fresh snapshot. */
    fun updateNestEvidence(checked: Boolean, punched: Int, found: Int = punched, unknown: Int? = null) {
        nestsChecked = checked
        punchedNests = punched.coerceAtLeast(0)
        foundNests = found.coerceAtLeast(0)
        if (unknown != null) nestSitesUnknown = unknown.coerceAtLeast(0)
        sparklingChecks.nestEvidence(nestsResolved)
        if (!checked || personalCount("Honeybug") < honeybugCaptureTarget) biomeClears.remove(SafariBiome.FOREST)
    }
    private val birdSpecies = setOf("Bluebird", "Parakeet", "Macaw")
    private var nearbyMacaws = 0
    val birdFoodsComplete get() = birds.foodsComplete
    fun recordBirdFood(raw: String, biome: SafariBiome? = SafariBiome.FOREST, now: Long = System.currentTimeMillis()) =
        birds.pickup(raw, biome, now - startedAt)
    fun updateCaptureEvidence(biome: SafariBiome, speciesInRange: Set<String>, allWallsChecked: Boolean, allNestsChecked: Boolean = false,
        macawsInRange: Int = if ("Macaw" in speciesInRange) 1 else 0,
        playerX: Double? = null, playerZ: Double? = null, nestsPunched: Int = 0, nestsFound: Int = nestsPunched) {
        nearby[biome] = speciesInRange
        partyFullClears.remove(biome); communalClears.remove(biome); sparklingChecks.localScan(biome)
        zoneScanConfirmed.removeAll(speciesInRange)
        if (speciesInRange.isNotEmpty()) partyFullClears.remove(biome)
        if (biome == SafariBiome.CAVERN) wallsChecked = allWallsChecked
        if (biome == SafariBiome.FOREST) updateNestEvidence(allNestsChecked, nestsPunched, nestsFound)
        if (biome == SafariBiome.FOREST) nearbyMacaws = macawsInRange
        if (SafariCompletionZones.forBiome(biome).isNotEmpty() && playerX != null && playerZ != null) {
            zonesRequired.add(biome)
            SafariCompletionZones.forBiome(biome).forEach { (name, zone) ->
                if (name !in speciesInRange && zone.contains(playerX, playerZ, sparkling = false) &&
                    count(name) >= SafariFullClear.minimum(name)) zoneScanConfirmed.add(name)
            }
            SafariCompletionZones.uuidMinimums.filterKeys { SafariRoster.named(it)?.biome == biome }.forEach { (name, minimum) ->
                if (name !in speciesInRange && count(name) >= minimum)
                    zoneScanConfirmed.add(name)
            }
        }
    }
    /** No historical UUID union or spawn-message totals are added to the current population. */
    fun scanBirdSources(x: Double, z: Double, live: List<Pair<String, java.util.UUID>>, dropsRemaining: Int) {
        fun population(name: String) = count(name) + live.filter { it.first == name }.map { it.second }.distinct().size
        val population = population("Bluebird") + population("Parakeet") + population("Macaw") / 2
        resources.scan(SafariBiome.FOREST, population)
        birds.scan(x, z, population, dropsRemaining)
    }
    fun hasCaptureEvidence(biome: SafariBiome) = biome in nearby
    fun missing(critter: SafariCritter, fullClear: Boolean) = if (fullClear)
        !captureComplete(critter.name) else count(critter.name) == 0
    fun birdCount(personalOnly: Boolean = false) = birdSpecies.sumOf { if (personalOnly) personalCount(it) else count(it) }
    private fun birdComplete(species: String, speciesInRange: Set<String>?, personalOnly: Boolean = false, macawsInRange: Int = nearbyMacaws): Boolean {
        if (speciesInRange == null || !birds.feedingResolved || sourcePending(species)) return false
        val captured = if (personalOnly) personalCount(species) else count(species)
        return if (species == "Macaw") captured >= 1 || !birds.hasMacaws(count(species), macawsInRange)
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
        speciesInRange: Set<String>? = nearby[SafariRoster.named(species)!!.biome], structures: BiomeClearEvidence? = null): Boolean {
        if (sourcePending(species) || sparklePending(species)) return false
        val remaining = speciesInRange ?: return false
        val captured = if (personalOnly) personalCount(species) else count(species)
        if (captured < maxOf(SafariFullClear.minimum(species), if (species == "Macaw") 0 else knownPopulation[species] ?: 0)) return false
        if (species == "Gimmiegold" && captured < resources.requiredPopulation(SafariBiome.HAUNTED)) return false
        val biome = SafariRoster.named(species)!!.biome
        if (biome in zonesRequired && species == "Gimmiegold" &&
            !sparklingChecks.goldComplete(captured, species !in remaining)) return false
        if (biome in zonesRequired && (species in SafariCompletionZones.forBiome(biome) || species in SafariCompletionZones.uuidMinimums) &&
            species !in zoneScanConfirmed) return false
        if (species in birdSpecies) return birdComplete(species, remaining, personalOnly, structures?.nearbyMacaws ?: nearbyMacaws)
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
