package net.johnceo.sparklingmutuals.history

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariSparklingMode
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/**
 * Every Safari run, saved. A run is whatever the tracker's ledger calls one (arriving to leaving); when it closes it
 * is written to `sparkling-mutuals-runs.json` with its captures, shards, feathers, sparklings and biome splits.
 */
object RunHistory {
    class Data(var nextNumber: Int = 1, var runs: MutableList<RunRecord> = mutableListOf(),
               var sessionStarts: List<Long> = emptyList(),
               var pricedInstaSell: Boolean? = null, var legacyOwnerUuid: String? = null)
    private var data = Data()
    private var hotspotRunCount: Pair<Pair<Set<String>, Long?>, Int>? = null
    private var cachedSessionStats: SafariSessionStats? = null
    private val session = LiveHistorySession()
    private val tally get() = session.tally
    private val splits get() = session.splits
    private val tickets get() = session.tickets
    private val essenceLine=Regex("^Safari Essence: ([0-9,]+)$")
    private var ticks = 0
    private var finishedSummary: Pair<RunRecord, RunTally>? = null
    private var finishedSummaryAt = 0L
    private var finishedSummaryOwner: java.util.UUID? = null
    private var liveOwner: String? = null
    private var accountRuns: Pair<String, List<RunRecord>>? = null
    private var captureCountCache: Pair<Long, Long>? = null

    private fun belongs(run: RunRecord, owner: String) =
        (run.ownerUuid ?: data.legacyOwnerUuid ?: owner) == owner

    val runs: List<RunRecord> get() {
        val owner = ConfigManager.historyOwnerUuid
        return accountRuns?.takeIf { it.first == owner }?.second ?: data.runs.filter { belongs(it, owner) }
            .also { accountRuns = owner to it; cachedSessionStats = null; hotspotRunCount = null; captureCountCache = null }
    }
    internal fun claimLegacy(owner: String) {
        if (data.legacyOwnerUuid != null) return
        data.legacyOwnerUuid = owner
        if (data.runs.isNotEmpty()) save()
    }
    internal fun knownAccountNames() = data.runs.asSequence().flatMap { it.partyMembers?.entries.orEmpty().asSequence() }
        .mapNotNull { (uuid, name) -> HistoryAccountId.normalize(uuid)?.let { it to name } }
        .groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
    val sessionStarts: List<Long> get() = data.sessionStarts
    internal fun currentSessionStats(): SafariSessionStats {
        val history = runs
        return cachedSessionStats ?: SafariSessionStats.of(
            ProfitPeriods.currentRuns(history, SafariProfitPeriod.SESSION, sessionStarts.lastOrNull(), System.currentTimeMillis())
        ).also { cachedSessionStats = it }
    }
    internal val priceChangePending get() = data.pricedInstaSell?.let { it != ConfigManager.runCoinsInstaSell } == true
    internal fun hotspotRunsFor(roster: Set<String>?, run: SafariRun?): Int {
        val runParty = run?.runParty?.takeIf { it.presentTogether }?.members?.keys
        val party = runParty ?: roster ?: return 0
        val liveStart = run?.takeIf { it.runParty.hotspotUsed && it.entry.admitted && !it.historyExcluded }?.startedAt
        val key = party to liveStart
        hotspotRunCount?.takeIf { it.first == key }?.let { return it.second }
        return HistoryQueries.consecutiveHotspotRuns(data.runs, party, liveStart).also { hotspotRunCount = key to it }
    }

    fun newSession(now: Long = System.currentTimeMillis()): Result<Unit> {
        val previous = data.sessionStarts
        data.sessionStarts = previous + maxOf(now, (previous.lastOrNull() ?: 0L) + 1)
        return save().onFailure { data.sessionStarts = previous }
    }

    internal fun startNewSession(feedback: (Component) -> Unit, error: (Component) -> Unit): Int =
        newSession().fold(
            { feedback(LocalChat.component("[SM] Previous session saved. New session started.")); 1 },
            { error(LocalChat.component("[SM] Could not save the new session. See latest.log.")); 0 },
        )
    var lastImportUpdated = 0
        private set

    fun register() {
        data = HistoryStore.load("runs", Data::class.java, ::Data).also { d -> d.runs.sortByDescending { it.start } }
        accountRuns = null
        cachedSessionStats = null
        if (data.pricedInstaSell == null) data.pricedInstaSell = ConfigManager.runCoinsInstaSell
        val overlong = HistoryMerge.removeOverlongRuns(data.runs)
        val duplicates = HistoryMerge.removeExactDuplicates(data.runs)
        if (overlong + duplicates > 0) {
            data.nextNumber = data.runs.size + 1
            runCatching { HistoryStore.backup("runs", if (overlong > 0) "pre-duration-cleanup" else "pre-deduplication") }
                .onSuccess { save() }
                .onFailure { org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/history").warn("Couldn't back up run history repair", it) }
        }
        BazaarPrices.ensureFresh()
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { chat(message.string) }
        }
    }

    private fun chat(raw: String) {
        val client = Minecraft.getInstance()
        HistoryServerPolicy.allows(client)
        val run = SafariTracking.ledger.current.takeIf { HistoryAccountId.selected(client) != null }
        syncLive(run, client)
        if (HistoryAccountId.selected(client) == null) return
        val previousCaught = tally.caught
        session.chat(run, raw, client.player?.gameProfile?.name.orEmpty()) {}
        session.active?.takeUnless { it.historyExcluded }?.let { run ->
            if (tally.caught > previousCaught) Achievements.onCritterCapture()
            tally.allTypesAt?.let { Achievements.onAllTypes((it - run.startedAt).coerceAtLeast(0)) }
        }
        // Some servers deliver the summary rows just after the world/ledger closes.
        if (session.active == null && client.player?.uuid == finishedSummaryOwner &&
            System.currentTimeMillis() - finishedSummaryAt in 0..15000) {
            finishedSummary?.let { (record, endedTally) ->
                SafariMessages.lines(raw).forEach(endedTally::summaryLine)
                if (applySummary(record, endedTally)) save()
            }
        }
    }

    internal fun capturesBetween(after: Long, through: Long = Long.MAX_VALUE): Long {
        val history = runs
        val saved = if (through == Long.MAX_VALUE) {
            captureCountCache?.takeIf { it.first == after }?.second
                ?: CaptureProgress.savedCaptures(history, after, through).also { captureCountCache = after to it }
        } else CaptureProgress.savedCaptures(history, after, through)
        if (liveOwner != ConfigManager.historyOwnerUuid || !session.hasActiveCaptureTally) return saved
        return saved + tally.captureTimes.count { it > after && it <= through }
    }

    /** HUD estimate reads only the existing cache; rendering never triggers a request. */
    fun liveProfit(run: SafariRun): Long {
        if(session.active!==run) return runs.firstOrNull { it.start==run.startedAt }?.coins ?: 0L
        val earnedEssence=if(tally.safariEssence>=0) tally.safariEssence else tally.liveEssence
        if(tally.shards.isEmpty() && tally.feathers==0 && earnedEssence<=0) return 0L
        val prices=BazaarPrices.cachedPrices
        val value=BazaarPrices.value(tally.shards,tally.feathers,table=prices)
        if(value<0) return -1L
        val essence=prices[BazaarPrices.SAFARI_ESSENCE]?.getOrNull(if(ConfigManager.runCoinsInstaSell) 0 else 1) ?: 0.0
        return value+Math.round(earnedEssence*essence)
    }

    fun onTicket() {
        if (HistoryAccountId.selected(Minecraft.getInstance()) != null && HistoryServerPolicy.allows())
            session.onTicket(SafariTracking.ledger.current)
    }

    private fun syncLive(run: SafariRun?, client: Minecraft) {
        if (HistoryAccountId.selected(client) == null) session.discardPendingTickets()
        if (session.active === run) return
        val endingOwner = liveOwner
        session.sync(run) { if (endingOwner != null && endingOwner == ConfigManager.historyOwnerUuid &&
            (HistoryAccountId.active(client)?.let { it == endingOwner } != false)) finish(it, client, endingOwner) }
        liveOwner = HistoryAccountId.selected(client).takeIf { run != null }
    }

    fun tick(client: Minecraft) {
        HistoryServerPolicy.allows(client)
        syncLive(SafariTracking.ledger.current.takeIf { HistoryAccountId.selected(client) != null }, client)
        val current = session.active ?: return
        if(current.endedAt==null) net.johnceo.sparklingmutuals.hud.SkyblockSidebar.lines(client).firstNotNullOfOrNull { line ->
            essenceLine.matchEntire(line.trim())?.groupValues?.get(1)?.replace(",","")?.toIntOrNull()
        }?.let(tally::observeEssence)
        if (++ticks % 10 != 0) return
        val now = System.currentTimeMillis()
        val party = SafariSparklingMode.state
        for (biome in SafariBiome.entries) if (biome.label !in splits &&
            current.biomeComplete(SafariFullClear.mode, biome, party, includeShared = false))
            splits[biome.label] = now - current.startedAt
    }

    private fun finish(run: SafariRun, client: Minecraft, owner: String) {
        if (run.historyExcluded || tickets == 0 &&
            tally.party.isEmpty() && tally.summaryShards < 0 && tally.safariEssence < 0 && !tally.rewardSummarySeen) return
        // The run start is stable across callbacks; never insert or award the same live run twice.
        if (data.runs.any { !it.imported && it.start == run.startedAt && it.ownerUuid.let { id -> id == null || id == owner } }) return
        val length = run.elapsed(System.currentTimeMillis())
        if (!HistoryQueries.keepRunLength(length)) return
        val record = RunRecord(start = run.startedAt, length = length,
            mode = SafariFullClear.mode.name, party = tally.party.size, you = tally.catches.size,
            caught = tally.caught, lootShares = tally.lootShares,
            partySize = run.runParty.members?.size ?: client.player?.uuid?.toString()?.let(PartyManager::withLocal)?.size ?: 1,
            tickets = tickets, ticketAt = session.ticketAt, shards = LinkedHashMap(tally.shards), catches = LinkedHashMap(tally.catches),
            feathers = tally.feathers, sparklings = tally.sparklings.toMutableList(), ownSparklings = tally.ownSparklings,
            macaws = tally.macaws, splits = LinkedHashMap(splits), safariEssence = tally.safariEssence,
            summaryShards = tally.summaryShards, huntingXp = tally.huntingXp, hotspot = run.entry.hotspot?.label.orEmpty(),
            macawChecked = run.count("Macaw") > 0 || run.birds.feedingResolved,
            allBirdFoodUsed = run.birds.allNineFloorFoodUsed,
            macawsSeen = maxOf(run.count("Macaw"),run.birds.spawnedCount("Macaw")),
            allTypesMillis = tally.allTypesAt?.let { (it - run.startedAt).coerceAtLeast(0) } ?: -1,
            partyMembers = run.runParty.members, hotspotRun = run.runParty.hotspotUsed,
            fullPartyPresent = run.runParty.presentTogether, rewardSummarySeen = tally.rewardSummarySeen, ownerUuid = owner,
            captureOffsetsMillis = CaptureProgress.offsets(tally.captureTimes, run.startedAt))
        // A manual import during this run may have read it from latest.log already; the live record replaces that.
        val account = data.runs.filter { belongs(it, owner) }.toMutableList()
        val imported = HistoryMerge.removeLiveDuplicate(account, record)
        if (imported != null) data.runs.remove(imported)
        record.number = imported?.number ?: data.nextNumber++
        record.coins = BazaarPrices.value(record.shards, record.feathers)
        addEssenceValue(record)
        data.runs.add(0, record)
        finishedSummary = record to tally; finishedSummaryAt = System.currentTimeMillis(); finishedSummaryOwner = client.player?.uuid
        save()
        Achievements.onRunEnd(record)
    }

    /**
     * Adds runs rebuilt from old logs, preserving matched native and imported records.
     * Starts or original ticket times match within ninety seconds; display numbering is rebuilt oldest first.
     */
    fun reconcileImport(found: List<RunRecord>, alpha: List<RunRecord>, foreign: List<RunRecord> = emptyList()): Int {
        val account = runs.toMutableList()
        val count = net.johnceo.sparklingmutuals.history.importing.HistoryImportRepair.runs(account, found, alpha, foreign)
        if (count > 0) replaceAccountRuns(account)
        return count
    }

    fun importRuns(found: List<RunRecord>): Int {
        val account = runs.toMutableList()
        val existing = account.toSet()
        val updated = mutableSetOf<RunRecord>()
        val added = HistoryMerge.merge(account, found) { updated += it }
        account.filter { it.imported }.forEach { if (addEssenceValue(it) && it in existing) updated += it }
        replaceAccountRuns(account)
        lastImportUpdated = updated.size
        data.nextNumber = data.runs.size + 1
        save().getOrThrow()
        return added
    }

    private fun replaceAccountRuns(account: List<RunRecord>) {
        data.runs.removeIf { belongs(it, ConfigManager.historyOwnerUuid) }
        data.runs.addAll(account)
        data.runs.sortByDescending { it.start }
        accountRuns = null
    }

    /** Fills in coins for runs saved before the prices had loaded. */
    fun fillMissingCoins() {
        if (priceChangePending) { reprice(); return }
        val missing = data.runs.filter { !it.coinsEstimated && it.coins < 0 }
        missing.forEach { it.coins = BazaarPrices.value(it.shards, it.feathers) }
        val essenceChanged = data.runs.map(::addEssenceValue).any { it }
        if (missing.any { it.coins >= 0 } || essenceChanged) save()
    }

    /** Add actual summary essence once; cache-only so imports cannot trigger API requests. */
    internal fun addEssenceValue(run: RunRecord): Boolean {
        if (run.safariEssence < 0 || run.essenceCoins >= 0 || run.coins < 0) return false
        val price = BazaarPrices.cachedPrices[BazaarPrices.SAFARI_ESSENCE]
            ?.getOrNull(if (ConfigManager.runCoinsInstaSell) 0 else 1)?.takeIf { it.isFinite() && it > 0 } ?: return false
        run.essenceCoins = Math.round(run.safariEssence * price)
        run.coins += run.essenceCoins
        return true
    }
    internal fun applySummary(run: RunRecord, tally: RunTally): Boolean {
        var changed = false
        if (tally.rewardSummarySeen && !run.rewardSummarySeen) { run.rewardSummarySeen = true; changed = true }
        if (tally.safariEssence >= 0 && run.safariEssence != tally.safariEssence) {
            if (run.coins >= 0) run.coins -= maxOf(0L, run.essenceCoins)
            run.essenceCoins = -1; run.safariEssence = tally.safariEssence; changed = true
        }
        if (tally.summaryShards >= 0 && run.summaryShards != tally.summaryShards) { run.summaryShards = tally.summaryShards; changed = true }
        if (tally.huntingXp >= 0 && run.huntingXp != tally.huntingXp) { run.huntingXp = tally.huntingXp; changed = true }
        return addEssenceValue(run) || changed
    }
    fun reprice() {
        BazaarPrices.ensureFresh()
        val quotes = BazaarPrices.cachedPrices
        if (quotes.isEmpty()) { save(); return }
        val reference = data.runs.map { RunRecord(caught = it.caught, lootShares = it.lootShares, party = it.party,
            feathers = it.feathers, sparklings = it.sparklings.toMutableList(), coins = it.coins, essenceCoins = it.essenceCoins) }
        data.runs.forEach { run ->
            if (run.imported) net.johnceo.sparklingmutuals.history.importing.HistoryEstimates.assignCoins(run, reference, quotes, ConfigManager.runCoinsInstaSell)
            else run.coins = BazaarPrices.value(run.shards, run.feathers, quotes)
            run.essenceCoins = -1; addEssenceValue(run)
        }
        data.pricedInstaSell = ConfigManager.runCoinsInstaSell
        save()
    }

    fun save(): Result<Unit> {
        accountRuns = null
        captureCountCache = null
        hotspotRunCount = null
        cachedSessionStats = null
        return HistoryStore.save("runs", data)
    }
}

/** Client-thread tally state shared by chat callbacks and ticks. Finish observes the old state before it resets. */
internal class LiveHistorySession {
    var active: SafariRun? = null
        private set
    var tally = RunTally()
        private set
    val splits = linkedMapOf<String, Long>()
    var tickets = 0
        private set
    var ticketAt = 0L
        private set
    private var pendingTickets = 0
    private var pendingTicketAt = 0L
    private var finishing = false
    val hasActiveCaptureTally get() = !finishing && active?.historyExcluded == false

    /** A later ticket listener may run after chat has already synchronized the new run. */
    fun onTicket(run: SafariRun?, now: Long = System.currentTimeMillis()) {
        if (!finishing && run != null && run === active) {
            tickets++
            if (ticketAt == 0L) ticketAt = now
        } else {
            pendingTickets++
            if (pendingTicketAt == 0L) pendingTicketAt = now
        }
    }

    fun discardPendingTickets() {
        pendingTickets = 0
        pendingTicketAt = 0
    }

    fun sync(run: SafariRun?, finish: (SafariRun) -> Unit) {
        if (finishing || run === active) return
        val previous = active
        active = run
        finishing = true
        try {
            previous?.let(finish)
        } finally {
            tally = RunTally(); splits.clear()
            tickets = if (run == null) 0 else pendingTickets
            ticketAt = if (run == null) 0 else pendingTicketAt
            if (run != null) discardPendingTickets()
            finishing = false
        }
    }

    fun chat(run: SafariRun?, raw: String, self: String, finish: (SafariRun) -> Unit) {
        // A local achievement/chat message during finish must not close or modify the departing run again.
        if (finishing) return
        sync(run, finish)
        if (active == null) return
        SafariMessages.lines(raw).forEach { tally.line(it, self, System.currentTimeMillis()) }
    }
}

/** The merge behind imports, separate so it can be tested. */
object HistoryMerge {
    const val SAME_RUN_MILLIS = 90_000L
    fun removeOverlongRuns(runs: MutableList<RunRecord>): Int {
        val before = runs.size
        runs.removeAll { !HistoryQueries.keepRunLength(it.length) }
        if (runs.size != before) {
            runs.sortBy { it.start }
            runs.forEachIndexed { index, run -> run.number = index + 1 }
            runs.sortByDescending { it.start }
        }
        return before - runs.size
    }
    /** Only the display number is excluded. Close timestamps or equal capture totals alone are not duplicates. */
    private fun signature(run: RunRecord): List<Any?> = with(run) {
        listOf(start, length, mode, party, you, caught, lootShares, partySize, tickets, shards.toMap(), catches.toMap(),
            feathers, sparklings.toList(), ownSparklings, macaws, splits.toMap(), imported, coins, ticketAt, coinsEstimated, modeEstimated,
            safariEssence, summaryShards, huntingXp, hotspot, essenceCoins, macawChecked ?: "unknown", allTypesMillis,
            partyMembers, hotspotRun, fullPartyPresent, rewardSummarySeen, ownerUuid, captureOffsetsMillis)
    }
    fun removeExactDuplicates(runs: MutableList<RunRecord>): Int {
        val seen = mutableSetOf<List<Any?>>()
        val before = runs.size
        runs.removeAll { it.start > 0 && !seen.add(signature(it)) }
        val removed = before - runs.size
        if (removed > 0) {
            runs.sortBy { it.start }
            runs.forEachIndexed { index, run -> run.number = index + 1 }
            runs.sortByDescending { it.start }
        }
        return removed
    }
    /** Replace one overlapping import of this run; nearby, already-finished runs remain separate. */
    fun removeLiveDuplicate(runs: MutableList<RunRecord>, live: RunRecord): RunRecord? {
        val imported = runs.filter { it.imported && sameAccount(it, live) && kotlin.math.abs(it.start - live.start) <= SAME_RUN_MILLIS &&
            (Math.floorDiv(it.start, 1000L) == Math.floorDiv(live.start, 1000L) ||
                it.start < live.start + live.length && live.start < it.start + it.length) }
            .minByOrNull { kotlin.math.abs(it.start - live.start) }
        if (imported != null) runs.remove(imported)
        return imported
    }
    private fun sameAccount(a: RunRecord, b: RunRecord) = a.ownerUuid == null || b.ownerUuid == null || a.ownerUuid == b.ownerUuid
    /**
     * Merges [found] into [runs] (newest first afterwards), renumbering oldest first; returns how many were new.
     * Each saved run matches at most one found run, the closest in time; runs found in this batch never match
     * each other, so two short runs close together both survive.
     */
    fun merge(runs: MutableList<RunRecord>, found: List<RunRecord>, onUpdated: (RunRecord) -> Unit = {}): Int {
        val imports = runs.filter { it.imported }.toMutableList()
        removeOverlongRuns(imports)
        removeExactDuplicates(imports)
        val kept = imports.toSet()
        runs.removeIf { it.imported && it !in kept }
        val incoming = found.filter { HistoryQueries.keepRunLength(it.length) }.distinctBy { if (it.start > 0) signature(it) else it }
        data class Match(val found: Int, val saved: Int, val distance: Long)
        val sortedSaved = runs.withIndex().sortedBy { it.value.start }
        val candidates = mutableMapOf<Pair<Int, Int>, Long>()
        fun lowerBound(at: Long): Int {
            var lo = 0; var hi = sortedSaved.size
            while (lo < hi) { val mid = (lo + hi) ushr 1; if (sortedSaved[mid].value.start < at) lo = mid + 1 else hi = mid }
            return lo
        }
        incoming.forEachIndexed { fi, run ->
            listOf(run.start, run.ticketAt).filter { it > 0 }.distinct().forEach { anchor ->
                var index = lowerBound(anchor - SAME_RUN_MILLIS)
                while (index < sortedSaved.size && sortedSaved[index].value.start <= anchor + SAME_RUN_MILLIS) {
                    val saved = sortedSaved[index++]
                    if (!sameAccount(run, saved.value)) continue
                    val pair = fi to saved.index
                    val previous = saved.value
                    val distance = minOf(kotlin.math.abs(previous.start - anchor),
                        previous.ticketAt.takeIf { it > 0 }?.let { kotlin.math.abs(it - anchor) } ?: Long.MAX_VALUE)
                    val overlap = run.start < previous.start + previous.length && previous.start < run.start + run.length
                    if (distance <= 1_500 || overlap || previous.length == 0L) {
                        candidates[pair] = minOf(candidates[pair] ?: Long.MAX_VALUE, distance)
                    }
                }
            }
        }
        val matchedFound = mutableSetOf<Int>(); val matchedSaved = mutableSetOf<Int>()
        candidates.map { (pair, distance) -> Match(pair.first, pair.second, distance) }.sortedBy { it.distance }.forEach {
            if (it.found !in matchedFound && it.saved !in matchedSaved) {
                matchedFound += it.found; matchedSaved += it.saved
                val saved = runs[it.saved]; val extra = incoming[it.found]
                if (!saved.imported) return@forEach
                val before = signature(saved)
                if (saved.ownerUuid == null) saved.ownerUuid = extra.ownerUuid
                if (saved.importSource == null) saved.importSource = extra.importSource
                if (saved.allTypesMillis < 0 && extra.allTypesMillis >= 0) saved.allTypesMillis = extra.allTypesMillis
                if (saved.safariEssence < 0) saved.safariEssence = extra.safariEssence
                if (saved.summaryShards < 0) saved.summaryShards = extra.summaryShards
                if (saved.huntingXp < 0) saved.huntingXp = extra.huntingXp
                if (saved.hotspot.isEmpty()) saved.hotspot = extra.hotspot
                if (saved.modeEstimated && !extra.modeEstimated) { saved.mode = extra.mode; saved.modeEstimated = false }
                extra.splits.forEach { (biome, time) -> saved.splits.putIfAbsent(biome, time) }
                if (saved.imported) {
                    if (extra.caught >= saved.caught && extra.captureOffsetsMillis?.size == extra.caught)
                        saved.captureOffsetsMillis = extra.captureOffsetsMillis
                    extra.catches.forEach { (species, count) -> saved.catches[species] = maxOf(saved.catches[species] ?: 0, count) }
                    extra.shards.forEach { (species, count) -> saved.shards[species] = maxOf(saved.shards[species] ?: 0, count) }
                    saved.caught = maxOf(saved.caught, extra.caught)
                    saved.lootShares = maxOf(saved.lootShares, extra.lootShares)
                    saved.party = maxOf(saved.party, extra.party); saved.you = maxOf(saved.you, extra.you)
                    saved.partySize = maxOf(saved.partySize, extra.partySize)
                    if (extra.ticketAt > 0) {
                        saved.ticketAt = extra.ticketAt
                        saved.tickets = extra.tickets
                    }
                    saved.feathers = maxOf(saved.feathers, extra.feathers); saved.macaws = maxOf(saved.macaws, extra.macaws)
                    saved.ownSparklings = maxOf(saved.ownSparklings, extra.ownSparklings)
                    extra.sparklings.forEach { spark -> if (spark !in saved.sparklings) saved.sparklings.add(spark) }
                    if (saved.length <= 0) saved.length = extra.length
                    if (saved.coinsEstimated && (!extra.coinsEstimated ||
                        saved.coins - maxOf(0L, saved.essenceCoins) <= 0 && extra.coins > 0)) {
                        saved.coins = extra.coins; saved.coinsEstimated = extra.coinsEstimated; saved.essenceCoins = extra.essenceCoins
                    }
                }
                if (signature(saved) != before) onUpdated(saved)
            }
        }
        incoming.forEachIndexed { index, run -> if (index !in matchedFound) runs += run }
        val added = incoming.size - matchedFound.size
        // Saved native and imported details are preserved; only chronological numbering changes.
        runs.sortBy { it.start }
        runs.forEachIndexed { index, run -> run.number = index + 1 }
        runs.sortByDescending { it.start }
        return added
    }
}
