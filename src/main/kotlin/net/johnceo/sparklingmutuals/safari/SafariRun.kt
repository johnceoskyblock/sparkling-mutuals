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
class SafariRun(val startedAt: Long, checkClock: () -> Long = System::currentTimeMillis) {
    var historyExcluded = false
        private set
    fun excludeFromHistory() { historyExcluded = true }
    val entry = SafariEntryEvidence()
    val resources = SafariResources()
    val birdFoodCheck = net.johnceo.sparklingmutuals.safari.resources.BirdFoodSparklingCheck()
    var biomeAtThirtySeconds: SafariBiome? = null
        private set
    private var thirtySecondSampled = false
    private var lastBiomeSample: Pair<Long,SafariBiome?>? = null
    private var firstForestVisit: Long? = null
    fun sampleBiome(now: Long, biome: SafariBiome?) {
        if(thirtySecondSampled || now < startedAt) return
        val mark=startedAt+30000
        if(now >= mark) {
            biomeAtThirtySeconds=if(now == mark) biome else lastBiomeSample
                ?.takeIf { it.first in (mark-1000)..mark && now-mark <= 1000 }?.second
            thirtySecondSampled=true
        }
        lastBiomeSample=now to biome
    }
    val trustLateForestNestReport get() = remoteForestReport && (firstForestVisit ?: Long.MIN_VALUE) > startedAt+30000

    val gimmiegoldCheck = net.johnceo.sparklingmutuals.safari.completion.GimmiegoldSparklingCheck()
    private var localPlayerName: String? = null
    fun localPlayer(name: String) { localPlayerName = name }
    private fun syncGimmiegoldContributions() {
        gimmiegoldCheck.contributions(resources.startingCount(FloorDropType.COIN), localPlayerName, resources.coinContributions())
    }
    val coinsLeftToUse: Int? get() {
        syncGimmiegoldContributions()
        return gimmiegoldCheck.leftToUse
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
    val partyResourceReminders = net.johnceo.sparklingmutuals.safari.resources.StartingResourcePartyReminder(resources)
    val startingAnnouncement = net.johnceo.sparklingmutuals.safari.resources.StartingResourceAnnouncement(resources)
    val sparklingParticles = net.johnceo.sparklingmutuals.safari.sparkling.SparklingParticleLocations()
    val sparklingCaptures = SparklingCaptureState()
    fun observeSparkling(id: java.util.UUID, name: String, aliases: Set<java.util.UUID> = emptySet()): java.util.UUID {
        sparklingCaptures.observe(id, name, aliases)
        return sparklingCaptures.identity(id)
    }
    /** Unresolved detection identities are diagnostic state, never a completion requirement. */
    fun sparklePending(name: String) = sparklingCaptures.pending(name)
    fun sparklingBroadcast(raw: String, now: Long = System.currentTimeMillis()) {
        val match = RunTally.sparkling.matchEntire(raw) ?: return
        val species = SafariRoster.named(match.groupValues[2].trim())?.name ?: return
        confirmedSparklingCapture(species, match.groupValues[1], broadcast = true, at = now)
    }
    private fun confirmedSparklingCapture(species: String, player: String?, broadcast: Boolean, at: Long) {
        if (!sparklingCaptures.capture(species, player, broadcast, at)) return
        val biome = SafariRoster.named(species)?.biome ?: return
        val retired = sparklingParticles.captured(biome, at)
        net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters.captured(species, retired)
    }
    private var liveBees = 0
    val beeNestCountKnown get() = foundNests > 0 && (nestSitesUnknown == 0 || nestsChecked)
    private val beePopulationAccountsForNests get() = beeNestCountKnown && count("Honeybug") + liveBees >= foundNests
    val nestsResolved get() = trustLateForestNestReport || nestsChecked || honeybugLocalCheck || beePopulationAccountsForNests
    private var honeybugLocalCheck = false
    private fun refreshNestChecking(renderConfirmed: Boolean = false) {
        // Only a fresh render sample may establish population proof. Block-survey updates can
        // otherwise mix a newly recorded catch with the previous scan's still-visible bee count.
        if (lastBiome == SafariBiome.FOREST || nestsChecked)
            honeybugLocalCheck = honeybugLocalCheck || nestsChecked || renderConfirmed && beePopulationAccountsForNests
        sparklingChecks.nestEvidence(honeybugLocalCheck)
    }
    fun liveHoneybugs(amount: Int) { liveBees = amount.coerceAtLeast(0); refreshNestChecking(renderConfirmed = true) }
    var hadFourIncense = false
        private set
    private var firstInventoryAt: Long? = null
    private var startingGrantAt: Long? = null
    fun startingGrantWindow(now: Long) {
        if (startingGrantAt != null) return
        startingGrantAt = now
        firstInventoryAt = now
    }
    fun inStartingInventoryWindow(now: Long) = firstInventoryAt?.let { now - it in 0..3000 } == true
    fun inventoryEvidence(stacks: List<Pair<String, Int>>, now: Long = System.currentTimeMillis()) {
        if (firstInventoryAt == null && now >= startedAt) firstInventoryAt = now
        resources.inventory(stacks, inStartingInventoryWindow(now))
        gimmiegoldCheck.inventory(stacks.filter { SafariRules.strip(it.first).trim() == "Shining Coin" }.sumOf { it.second.coerceAtLeast(0) })
        syncGimmiegoldContributions()
        birds.inventory(stacks)
        birdFoodCheck.inventory(stacks,now)
        sparklingChecks.inventory(stacks)
        sparklingChecks.startingCoins(resources.startingCount(FloorDropType.COIN))
        if (endedAt == null && firstInventoryAt != null) birds.startingInventory(stacks, now - firstInventoryAt!!)
        if (personalCount("Doomspiral") == 0 && stacks.filter { SafariRules.strip(it.first).trim() == "Soothing Incense" }.sumOf { it.second.coerceAtLeast(0) } >= 4)
            hadFourIncense = true
    }
    var brokenMounds = 0
        private set
    var rockmiteMounds = 0
        private set
    var endedAt: Long? = null
    var lastBiome: SafariBiome? = null
    private val counts = mutableMapOf<String, Int>()
    private var wumpaPrerequisitesConfirmed = false
    /** Server awakening is proof of shared captures, with no personal attribution or invented events. */
    fun recordWumpaSpawn(text: String) {
        if (SafariRules.strip(text) == "The Wumpa has awoken.") wumpaPrerequisitesConfirmed = true
    }
    private fun icyCaptureProof(name: String) = wumpaPrerequisitesConfirmed && name != "Wumpa" &&
        SafariRoster.named(name)?.biome == SafariBiome.ICY
    fun inferredCapture(name: String) = (counts[name] ?: 0) == 0 && icyCaptureProof(name)
    private val personal = mutableMapOf<String, Int>()
    private val catchEvents = mutableListOf<SafariCatch>()
    val catches: List<SafariCatch> get() = catchEvents.toList()
    private val populationClock = checkClock
    private val settlingRemainders = mutableSetOf<String>()
    private val lastPopulationCapture = mutableMapOf<String, Long>()
    private fun populationSettling(species: String) = species in setOf("Driftling", "Chuckwalla", "Flitter", "Shyworm") &&
        lastPopulationCapture[species]?.let { populationClock() - it in 0..2500 } == true
    private val knownPopulation = mutableMapOf<String, Int>()
    /** Current uncaptured UUIDs, never the union of pre/post-escape identities. */
    fun observePopulation(live: List<Pair<String, java.util.UUID>>) {
        val remaining = live.filterNot { sparklingCaptures.resolved(it.second) }
        settlingRemainders.clear()
        settlingRemainders.addAll(remaining.map { it.first }.filter(::populationSettling))
        remaining.groupBy({ it.first }, { it.second }).forEach { (name, ids) ->
            val critter = SafariRoster.named(name) ?: return@forEach
            if (populationSettling(name)) return@forEach
            if ((critter.biome in biomeReports || name in SafariFullClear.fixedCaptureTargets) && name != "Macaw") {
                val remaining = if (name in birdSpecies) ids.size else ids.distinct().size
                fullClearObservedTargets[name] = maxOf(fullClearObservedTargets[name] ?: 0, count(name) + remaining)
            }
        }
        remaining.filter { it.first !in birdSpecies }.groupBy({ it.first }, { it.second }).forEach { (name, ids) ->
            if (populationSettling(name)) return@forEach
            knownPopulation[name] = maxOf(knownPopulation[name] ?: 0, count(name) + ids.distinct().size)
        }
    }
    private val visited = mutableSetOf<SafariBiome>()
    private fun syncBirdContributions() { birds.contributions(localPlayerName, resources.foodContributions(), resources.startingReporters()) }
    fun observeEarlyBirdVisitor(name: String, x: Double, z: Double, now: Long) {
        if (name.equals(localPlayerName, true)) return
        birds.earlyVisitor(name, now - startedAt,
            net.johnceo.sparklingmutuals.safari.completion.ForestCompletionZones.nearBirdScanRadius(x, z),
            resources.startingReporters().any { it.equals(name, true) })
        syncBirdContributions()
    }
    /** Counts for compact Full Clear replies, independent of completion/report overrides. */
    fun capturesLeft(name: String): Int {
        val target = when (name) {
            "Honeybug" -> honeybugCaptureTarget
            "Macaw" -> if (encountered(name)) 1 else 0
            "Bluebird", "Parakeet" -> birds.spawnedCount(name)
            "Gimmiegold" -> gimmiegoldCaptureMinimum()
            else -> maxOf(SafariFullClear.minimum(name), knownPopulation[name] ?: 0)
        }
        return (target - count(name)).coerceAtLeast(0)
    }
    val knownBirdFoodLeftToUse: Int? get() = birdFoodCheck.leftToUse
    val birdFoodLeftToUse: Int get() { syncBirdContributions(); return birdFoodCheck.leftToUse ?: 0 }
    fun birdSpawnsComplete(localOnly: Boolean = false): Boolean {
        syncBirdContributions()
        return if (lastBiome == SafariBiome.FOREST) birdFoodCheck.complete else !localOnly &&
            (birdFoodCheck.complete || birds.hasReport && resources.heldCount(FloorDropType.SEEDS)+resources.heldCount(FloorDropType.WORM)+resources.heldCount(FloorDropType.BERRY) == 0)
    }
    fun hasUnusedStartingForBiome(biome: SafariBiome, party: PartySparklingState) =
        resources.startingNotice(party)?.items?.any { it.destination == biome } == true
    fun canReportBiome(biome: SafariBiome, party: PartySparklingState = SafariSparklingMode.state): Boolean {
        if (hasUnusedStartingForBiome(biome, party)) return false
        if (biome == SafariBiome.HAUNTED) return biome !in biomeReports
        if (biome != SafariBiome.FOREST) return true
        return biome !in biomeReports && birdSpawnsComplete(localOnly = true)
    }
    fun forestReportDebug(mode: SafariMode, party: PartySparklingState): String {
        val targets = SafariBiome.FOREST.critters.filter { mode != SafariMode.SPARKLING || party.needs(it.name) }
        val missing = targets.filter { completed(mode, listOf(it), party, localOnly = true) == 0 }.map { it.name }
        return "mode=$mode reportReceived=${SafariBiome.FOREST in biomeReports} suppliesReady=${birds.reportSuppliesReady} " +
            "spawnProof=${birdSpawnsComplete(localOnly = true)} ${birdFoodCheck.debug()} partyReady=${party.ready} " +
            "currentBiome=$lastBiome scanKnown=${hasCaptureEvidence(SafariBiome.FOREST)} missing=$missing"
    }
    private val partyFullClears = mutableSetOf<SafariBiome>()
    private val communalClears = mutableSetOf<SafariBiome>()
    private val biomeReports = mutableSetOf<SafariBiome>()
    private var remoteForestReport = false
    val honeybugRemoteComplete get() = remoteForestReport && lastBiome != SafariBiome.FOREST
    private val fullClearObservedTargets = mutableMapOf<String, Int>()
    fun uniqueReportReceived(biome: SafariBiome) = biome in biomeReports
    fun fullClearReportReceived(biome: SafariBiome) = biome in biomeReports
    private val uniqueSourceContradictions = mutableSetOf<String>()
    fun partyFullClear(raw: String) {
        SparklingChecks.doneBiome(raw)?.let {
            partyFullClears.add(it); communalClears.add(it)
            biomeReports.add(it)
            if (it == SafariBiome.FOREST) {
                remoteForestReport = true
                syncBirdContributions(); birds.report()
            }
            if (it == SafariBiome.HAUNTED) {
                syncGimmiegoldContributions(); gimmiegoldCheck.report()
            }
            if (it == SafariBiome.CAVERN && lastBiome != SafariBiome.CAVERN) {
                remainingMounds = false; remainingWalls = false
            }
        }
    }
    fun fullClearProgress(critters: List<SafariCritter>) = critters.count { fullClearComplete(it.name) }
    /** Local proof advances progress directly; remote reports use capture minima. PBs remain personal. */
    private fun remoteReport(biome: SafariBiome) = lastBiome != biome && biome in biomeReports
    private val optionalUniqueSpawns = setOf("Rockmite", "Snoozle", "Bluebird", "Parakeet", "Macaw")
    private fun remoteCaptureMinimum(name: String): Int = when (name) {
        "Honeybug" -> maxOf(SafariFullClear.minimum(name), foundNests, punchedNests)
        "Rockmite" -> maxOf(rockmiteMounds, if (encountered(name)) 1 else 0)
        "Snoozle" -> if (encountered(name)) 1 else 0
        "Gimmiegold" -> maxOf(SafariFullClear.minimum(name), gimmiegoldCaptureMinimum())
        else -> SafariFullClear.minimum(name)
    }
    fun fullClearComplete(name: String): Boolean {
        val critter = SafariRoster.named(name) ?: return false
        if (remoteReport(critter.biome)) {
            if (name in birdSpecies) return birdComplete(name, null)
            if (name == "Gimmiegold" && (sparklingChecks.hasHeldCoins || resources.pending(SafariBiome.HAUNTED))) return false
            return count(name) >= remoteCaptureMinimum(name)
        }
        if (name in birdSpecies) return birdComplete(name, nearby[SafariBiome.FOREST])
        if (sourcePending(name)) return false
        if (name == "Gimmiegold" && (sparklingChecks.hasHeldCoins || sparklingChecks.knownCoinsUnresolved && !gimmiegoldCheck.complete)) return false
        if (name == "Honeybug") {
            return fullClearMissingComplete(name)
        }
        if (name in uniqueSourceContradictions && !sparklingChecks.locallyChecked(name)) return false
        // The local checklist already requires qualified coverage, captures and resolved sources.
        // Its retained proof remains valid after leaving the scan radius or biome.
        if (fullClearMissingComplete(name)) return true
        if (populationSettling(name) && name in settlingRemainders) return false
        val fixed = SafariFullClear.fixedCaptureTargets[name]
        if (fixed == null && critter.biome !in biomeReports) return false
        val minimum = when (name) {
            "Rockmite" -> maxOf(rockmiteMounds, if (encountered(name)) 1 else 0)
            "Snoozle" -> if (encountered(name)) 1 else 0
            "Gimmiegold" -> maxOf(SafariFullClear.minimum(name), gimmiegoldCaptureMinimum())
            else -> SafariFullClear.minimum(name)
        }
        return count(name) >= maxOf(fixed ?: minimum, if (name == "Gimmiegold") 0 else fullClearObservedTargets[name] ?: 0)
    }
    private val inheritedBiomes = mutableSetOf<SafariBiome>()
    fun visitBiome(biome: SafariBiome, now: Long = startedAt) {
        if(biome == SafariBiome.FOREST && firstForestVisit == null) firstForestVisit=now
        if (lastBiome != biome) {
            partyFullClears.remove(biome); communalClears.remove(biome)
            sparklingChecks.reenter(biome)
            if (biome == SafariBiome.HAUNTED) gimmiegoldCheck.reenter()
            if (biome == SafariBiome.FOREST) birds.reenter()
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
    /** Current-biome sources require local proof; remote reports supply checking elsewhere. */
    private fun partyCompletion(name: String, party: PartySparklingState): Boolean? {
        val critter = SafariRoster.named(name) ?: return null
        if (lastBiome == critter.biome) return null
        if (name == "Honeybug") return if (honeybugRemoteComplete) true else null
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
        if (name !in birdSpecies && sparklingCaptures.captured(name)) return true
        if (!localOnly && remoteReport(critter.biome)) {
            if (SparklingChecks.requiresCapture(name)) return count(name) > 0
            if (name in birdSpecies) {
                syncBirdContributions()
                val held = listOf(FloorDropType.SEEDS, FloorDropType.WORM, FloorDropType.BERRY).any { resources.heldCount(it) > 0 }
                return !held
            }
            if (name == "Gimmiegold") {
                syncGimmiegoldContributions()
                return resources.heldCount(FloorDropType.COIN) == 0 && (gimmiegoldCheck.remoteReady || gimmiegoldCheck.complete)
            }
            return true
        }
        if (critter.biome == SafariBiome.ICY && count("Wumpa") > 0) return true
        if (name in birdSpecies) return birdSpawnsComplete(localOnly)
        if (SparklingChecks.requiresCapture(name)) return count(name) > 0
        if (name == "Rockmite" || name == "Snoozle") {
            if (name in uniqueSourceContradictions && !sparklingChecks.locallyChecked(name)) return false
            if (cavernSourcesChecked(name) || sparklingChecks.locallyChecked(name)) return true
            return !localOnly && partyCompletion(name, party) == true
        }
        if (name == "Honeybug") return (!localOnly && honeybugRemoteComplete) || honeybugLocalCheck ||
            lastBiome == SafariBiome.FOREST && sparklingChecks.locallyChecked(name)
        if (name == "Gimmiegold") {
            syncGimmiegoldContributions()
            if (!localOnly && lastBiome != SafariBiome.HAUNTED && partyCompletion(name, party) == true &&
                gimmiegoldCheck.remoteReady) return true
            return gimmiegoldCheck.complete
        }
        if (sourcePending(name)) return false
        if (!localOnly) partyCompletion(name, party)?.let { return it }
        return sparklingChecks.locallyChecked(name)
    }
    fun completed(mode: SafariMode, critters: List<SafariCritter>, party: PartySparklingState, localOnly: Boolean = false): Int =
        critters.count { when (mode) {
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
    /** A completed local source/absence survey can waive a variable species that never spawned. */
    fun uniqueComplete(name: String, party: PartySparklingState, localOnly: Boolean = false): Boolean {
        val critter = SafariRoster.named(name) ?: return false
        if (name !in birdSpecies && sparklingCaptures.captured(name)) return true
        if (!localOnly && remoteReport(critter.biome)) {
            if ((name in birdSpecies || name == "Gimmiegold") && hasUnusedStartingForBiome(critter.biome, party)) return false
            return count(name) > 0 || name in optionalUniqueSpawns && !encountered(name)
        }
        if (name in birdSpecies) return birdSpawnsComplete(localOnly) &&
            (count(name) > 0 || uniqueUnavailable(name) || name == "Macaw" &&
                lastBiome != SafariBiome.FOREST && !localOnly && count("Bluebird") > 0 && count("Parakeet") > 0 && birds.remoteReady)
        if (name in setOf("Gazer", "Gimmiegold") && party.needs(name))
            return count(name) > 0 && sparklingComplete(name, party, localOnly || lastBiome == SafariBiome.HAUNTED)
        if (name == "Rockmite" || name == "Snoozle") {
            if (count(name) > 0 && !party.needs(name)) return true
            if (name in uniqueSourceContradictions && !sparklingChecks.locallyChecked(name)) return false
            val checked = cavernSourcesChecked(name) || sparklingChecks.locallyChecked(name)
            if (count(name) > 0) return checked || !localOnly && uniqueReportedComplete(name)
            return uniqueUnavailable(name) ||
                !localOnly && checked && !encountered(name) && uniqueReportReceived(SafariBiome.CAVERN)
        }
        return count(name) > 0
    }
    /** A completed local survey can prove a variable critter never spawned; a sighting revokes it. */
    fun uniqueUnavailable(name: String): Boolean {
        if (name in birdSpecies) {
            syncBirdContributions()
            return forestBirdsSurveyed && !encountered(name) && birds.spawnedCount(name) == 0 &&
                (name != "Macaw" || !birds.hasMacaws(count(name), nearbyMacaws)) && birds.feedingResolved
        }
        // Mounds and walls are retained Safari-wide. Positive cleared-source proof does not
        // require entering Cavern or waiting for a party report when nothing ever spawned.
        val remaining = nearby[SafariBiome.CAVERN].orEmpty()
        if (encountered(name) || name in remaining) return false
        return when (name) {
            "Rockmite" -> allMoundsBroken && !remainingMounds
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
    val sparklingChecks = SparklingChecks(checkClock)
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
        if (trustLateForestNestReport) return false
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
        nestsChecked = trustLateForestNestReport || checked || nestsChecked && !expanded
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
        birds.pickup(raw, biome, now - (firstInventoryAt ?: startedAt))
    fun updateCaptureEvidence(biome: SafariBiome, speciesInRange: Set<String>, allWallsChecked: Boolean, allNestsChecked: Boolean = false,
        macawsInRange: Int = if ("Macaw" in speciesInRange) 1 else 0,
        playerX: Double? = null, playerZ: Double? = null, nestsPunched: Int = 0, nestsFound: Int = nestsPunched) {
        nearby[biome] = speciesInRange
        for (name in listOf("Driftling", "Chuckwalla", "Flitter", "Shyworm")) {
            if (SafariRoster.named(name)?.biome == biome) {
                if (name in speciesInRange && populationSettling(name)) settlingRemainders.add(name)
                else settlingRemainders.remove(name)
            }
        }
        speciesInRange.filter { it != "Macaw" && SafariRoster.named(it) != null }.forEach { name ->
            if (!populationSettling(name) && (biome in biomeReports || name in SafariFullClear.fixedCaptureTargets))
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
    fun scanBirdSources(dropsRemaining: Int, live: List<String> = emptyList(), qualified: Boolean = lastBiome == SafariBiome.FOREST) {
        syncBirdContributions()
        birds.floor(dropsRemaining)
        birdFoodCheck.floor(dropsRemaining)
        if (!qualified || lastBiome != SafariBiome.FOREST) return
        forestBirdsSurveyed = true
        nearbyMacaws = live.count { it == "Macaw" }
        sightings.addAll(live.filter { it in birdSpecies })
        birds.render(::count, live)
        birdFoodCheck.population(birds.accountedFeedings, birds.feedingTarget)
        resources.scan(SafariBiome.FOREST, birds.accountedFeedings)
    }
    fun hasCaptureEvidence(biome: SafariBiome) = biome in nearby
    /** Local checklist proof can advance progress without a party report. */
    fun fullClearMissingComplete(name: String): Boolean {
        if (name == "Hideyho") return count(name) >= SafariFullClear.minimum(name)
        val critter = SafariRoster.named(name) ?: return false
        if (name in birdSpecies) return birdComplete(name, nearby[SafariBiome.FOREST])
        if ((name in SafariCompletionZones.forBiome(critter.biome) || name in SafariCompletionZones.uuidMinimums) &&
            name !in missingScanConfirmed) return false
        return captureCompleteWithZones(name, false, nearby[critter.biome], null, missingScanConfirmed)
    }
    fun missing(critter: SafariCritter, fullClear: Boolean) = if (fullClear)
        !fullClearMissingComplete(critter.name) else count(critter.name) == 0
    fun birdCount(personalOnly: Boolean = false) = birdSpecies.sumOf { if (personalOnly) personalCount(it) else count(it) }
    private fun birdComplete(species: String, speciesInRange: Set<String>?, personalOnly: Boolean = false): Boolean {
        syncBirdContributions()
        return birds.capturesComplete { if (personalOnly) personalCount(it) else count(it) }
    }
    fun birdsComplete(speciesInRange: Set<String>?, personalOnly: Boolean = false) =
        birdSpecies.all { birdComplete(it, speciesInRange, personalOnly) }
    fun sourcePending(name: String) = when (name) {
        "Bluebird", "Parakeet", "Macaw" -> birds.knownFoodUnresolved
        "Gimmiegold" -> resources.pending(SafariBiome.HAUNTED)
        else -> false
    }
    fun captureComplete(species: String, personalOnly: Boolean = false,
        speciesInRange: Set<String>? = nearby[SafariRoster.named(species)!!.biome], structures: BiomeClearEvidence? = null): Boolean =
        captureCompleteWithZones(species, personalOnly, speciesInRange, structures, zoneScanConfirmed)

    private fun captureCompleteWithZones(species: String, personalOnly: Boolean,
        speciesInRange: Set<String>?, structures: BiomeClearEvidence?, confirmedZones: Set<String>): Boolean {
        if (species in birdSpecies) return birdComplete(species, speciesInRange, personalOnly)
        if (sourcePending(species)) return false
        val captured = if (personalOnly) personalCount(species) else count(species)
        // Finding Hideyho and receiving the reward is the quest's terminal success, not a live mob catch.
        if (species == "Hideyho") return captured >= SafariFullClear.minimum(species)
        // The initial known nest total and actual catch messages suffice from any biome.
        if (species == "Honeybug" && beeNestCountKnown)
            return captured >= maxOf(honeybugCaptureTarget, knownPopulation[species] ?: 0)
        val remaining = speciesInRange ?: return false
        if (species in birdSpecies) return birdComplete(species, remaining, personalOnly)
        val minimum = if (species == "Honeybug") honeybugCaptureTarget else SafariFullClear.minimum(species)
        // Gimmiegolds come from coins, not the number of model UUIDs seen across capsule transitions.
        if (species == "Gimmiegold") {
            if (captured < maxOf(minimum, gimmiegoldCaptureMinimum()) || species in remaining || sparklingChecks.hasHeldCoins) return false
            return SafariBiome.HAUNTED !in zonesRequired || gimmiegoldCheck.complete || sparklingChecks.goldComplete(captured, absent = true)
        }
        if (captured < maxOf(minimum, if (species == "Macaw") 0 else knownPopulation[species] ?: 0)) return false
        val biome = SafariRoster.named(species)!!.biome
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
        if (catch.critter.name == "Wumpa") wumpaPrerequisitesConfirmed = true
        if (catch.sparkling) confirmedSparklingCapture(catch.critter.name, if (catch.personal) self else catch.catcherName, broadcast = false, at = now)
        lastPopulationCapture[catch.critter.name] = now
        if (self != null) localPlayer(self)
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
    fun count(name: String) = maxOf(counts[name] ?: 0, if (icyCaptureProof(name)) 1 else 0)
    fun personalCount(name: String) = personal[name] ?: 0
    fun playerCount(species: String, player: String) = catches.count {
        it.critter.name == species && (if (it.personal) localPlayerName else it.catcherName).equals(player, true)
    }
    fun catchers(biome: SafariBiome) = catches.filter { it.critter.biome == biome }
        .mapNotNull { if (it.personal) localPlayerName else it.catcherName }.distinctBy { it.lowercase() }
    val observedUniqueClears = mutableSetOf<SafariBiome>()
    fun elapsed(now: Long) = ((endedAt ?: now) - startedAt).coerceAtLeast(0)
    fun complete(critter: SafariCritter, unique: Boolean, fullClear: Boolean = false) = count(critter.name) >= critter.required(unique, fullClear)
    fun progress(critters: List<SafariCritter>, unique: Boolean, fullClear: Boolean = false) = critters.count { complete(it, unique, fullClear) }
}
