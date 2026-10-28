package net.johnceo.sparklingmutuals.history

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariSparklingMode
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.minecraft.client.Minecraft

/**
 * Every Safari run, saved. A run is whatever the tracker's ledger calls one (arriving to leaving); when it closes it
 * is written to `sparkling-mutuals-runs.json` with its captures, shards, feathers, sparklings and biome splits.
 */
object RunHistory {
    class Data(var nextNumber: Int = 1, var runs: MutableList<RunRecord> = mutableListOf())
    private var data = Data()
    private val session = LiveHistorySession()
    private val tally get() = session.tally
    private val splits get() = session.splits
    private val tickets get() = session.tickets
    private var ticks = 0
    private var finishedSummary: Pair<RunRecord, RunTally>? = null
    private var finishedSummaryAt = 0L
    private var finishedSummaryOwner: java.util.UUID? = null

    val runs: List<RunRecord> get() = data.runs
    var lastImportUpdated = 0
        private set

    fun register() {
        data = HistoryStore.load("runs", Data::class.java, ::Data).also { d -> d.runs.sortByDescending { it.start } }
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
        session.chat(SafariTracking.ledger.current, raw, client.player?.name?.string.orEmpty()) { finish(it, client) }
        session.active?.takeUnless { it.historyExcluded }?.let { run ->
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

    fun onTicket() { if (HistoryServerPolicy.allows()) session.onTicket(SafariTracking.ledger.current) }

    fun tick(client: Minecraft) {
        HistoryServerPolicy.allows(client)
        session.sync(SafariTracking.ledger.current) { finish(it, client) }
        val current = session.active ?: return
        if (++ticks % 10 != 0) return
        val now = System.currentTimeMillis()
        val party = SafariSparklingMode.state
        for (biome in SafariBiome.entries) if (biome.label !in splits &&
            current.completed(SafariFullClear.mode, biome.critters, party) == biome.critters.size)
            splits[biome.label] = now - current.startedAt
    }

    private fun finish(run: SafariRun, client: Minecraft) {
        if (run.historyExcluded || !ConfigManager.runHistory ||
            tally.party.isEmpty() && tally.summaryShards < 0 && tally.safariEssence < 0) return
        // The run start is stable across callbacks; never insert or award the same live run twice.
        if (data.runs.any { !it.imported && it.start == run.startedAt }) return
        val length = run.elapsed(System.currentTimeMillis())
        if (!HistoryQueries.keepRunLength(length)) return
        val record = RunRecord(start = run.startedAt, length = length,
            mode = SafariFullClear.mode.name, party = tally.party.size, you = tally.catches.size,
            caught = tally.caught, lootShares = tally.lootShares,
            partySize = client.player?.uuid?.toString()?.let(PartyManager::withLocal)?.size ?: 1,
            tickets = tickets, shards = LinkedHashMap(tally.shards), catches = LinkedHashMap(tally.catches),
            feathers = tally.feathers, sparklings = tally.sparklings.toMutableList(), ownSparklings = tally.ownSparklings,
            macaws = tally.macaws, splits = LinkedHashMap(splits), safariEssence = tally.safariEssence,
            summaryShards = tally.summaryShards, huntingXp = tally.huntingXp, hotspot = run.entry.hotspot?.label.orEmpty(),
            macawChecked = run.count("Macaw") > 0 || run.birds.feedingResolved,
            allTypesMillis = tally.allTypesAt?.let { (it - run.startedAt).coerceAtLeast(0) } ?: -1)
        // A manual import during this run may have read it from latest.log already; the live record replaces that.
        val imported = HistoryMerge.removeLiveDuplicate(data.runs, record)
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
    fun reconcileImport(found: List<RunRecord>, alpha: List<RunRecord>): Int =
        net.johnceo.sparklingmutuals.history.importing.HistoryImportRepair.runs(data.runs, found, alpha)

    fun importRuns(found: List<RunRecord>): Int {
        val existing = data.runs.toSet()
        val updated = mutableSetOf<RunRecord>()
        val added = HistoryMerge.merge(data.runs, found) { updated += it }
        data.runs.forEach { if (addEssenceValue(it) && it in existing) updated += it }
        lastImportUpdated = updated.size
        data.nextNumber = data.runs.size + 1
        save().getOrThrow()
        return added
    }

    /** Fills in coins for runs saved before the prices had loaded. */
    fun fillMissingCoins() {
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
        if (quotes.isEmpty()) return
        val reference = data.runs.map { RunRecord(caught = it.caught, lootShares = it.lootShares, party = it.party,
            feathers = it.feathers, sparklings = it.sparklings.toMutableList(), coins = it.coins, essenceCoins = it.essenceCoins) }
        data.runs.forEach { run ->
            if (run.imported) net.johnceo.sparklingmutuals.history.importing.HistoryEstimates.assignCoins(run, reference, quotes, ConfigManager.runCoinsInstaSell)
            else run.coins = BazaarPrices.value(run.shards, run.feathers, quotes)
            run.essenceCoins = -1; addEssenceValue(run)
        }
        save()
    }

    fun save() = HistoryStore.save("runs", data)
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
    private var pendingTickets = 0
    private var finishing = false

    /** A later ticket listener may run after chat has already synchronized the new run. */
    fun onTicket(run: SafariRun?) {
        if (!finishing && run != null && run === active) tickets++ else pendingTickets++
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
            if (run != null) pendingTickets = 0
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
    private fun signature(run: RunRecord): List<Any> = with(run) {
        listOf(start, length, mode, party, you, caught, lootShares, partySize, tickets, shards.toMap(), catches.toMap(),
            feathers, sparklings.toList(), ownSparklings, macaws, splits.toMap(), imported, coins, ticketAt, coinsEstimated, modeEstimated,
            safariEssence, summaryShards, huntingXp, hotspot, essenceCoins, macawChecked ?: "unknown", allTypesMillis)
    }
    fun removeExactDuplicates(runs: MutableList<RunRecord>): Int {
        val seen = mutableSetOf<List<Any>>()
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
        val imported = runs.filter { it.imported && kotlin.math.abs(it.start - live.start) <= SAME_RUN_MILLIS &&
            (Math.floorDiv(it.start, 1000L) == Math.floorDiv(live.start, 1000L) ||
                it.start < live.start + live.length && live.start < it.start + it.length) }
            .minByOrNull { kotlin.math.abs(it.start - live.start) }
        if (imported != null) runs.remove(imported)
        return imported
    }
    /**
     * Merges [found] into [runs] (newest first afterwards), renumbering oldest first; returns how many were new.
     * Each saved run matches at most one found run, the closest in time; runs found in this batch never match
     * each other, so two short runs close together both survive.
     */
    fun merge(runs: MutableList<RunRecord>, found: List<RunRecord>, onUpdated: (RunRecord) -> Unit = {}): Int {
        removeOverlongRuns(runs)
        removeExactDuplicates(runs)
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
                    val pair = fi to saved.index
                    candidates[pair] = minOf(candidates[pair] ?: Long.MAX_VALUE, kotlin.math.abs(saved.value.start - anchor))
                }
            }
        }
        val matchedFound = mutableSetOf<Int>(); val matchedSaved = mutableSetOf<Int>()
        candidates.map { (pair, distance) -> Match(pair.first, pair.second, distance) }.sortedBy { it.distance }.forEach {
            if (it.found !in matchedFound && it.saved !in matchedSaved) {
                matchedFound += it.found; matchedSaved += it.saved
                val saved = runs[it.saved]; val extra = incoming[it.found]
                val before = signature(saved)
                if (saved.allTypesMillis < 0 && extra.allTypesMillis >= 0) saved.allTypesMillis = extra.allTypesMillis
                if (saved.safariEssence < 0) saved.safariEssence = extra.safariEssence
                if (saved.summaryShards < 0) saved.summaryShards = extra.summaryShards
                if (saved.huntingXp < 0) saved.huntingXp = extra.huntingXp
                if (saved.hotspot.isEmpty()) saved.hotspot = extra.hotspot
                if (saved.modeEstimated && !extra.modeEstimated) { saved.mode = extra.mode; saved.modeEstimated = false }
                extra.splits.forEach { (biome, time) -> saved.splits.putIfAbsent(biome, time) }
                if (saved.imported) {
                    extra.catches.forEach { (species, count) -> saved.catches[species] = maxOf(saved.catches[species] ?: 0, count) }
                    extra.shards.forEach { (species, count) -> saved.shards[species] = maxOf(saved.shards[species] ?: 0, count) }
                    saved.caught = maxOf(saved.caught, extra.caught)
                    saved.lootShares = maxOf(saved.lootShares, extra.lootShares)
                    saved.party = maxOf(saved.party, extra.party); saved.you = maxOf(saved.you, extra.you)
                    saved.partySize = maxOf(saved.partySize, extra.partySize); saved.tickets = maxOf(saved.tickets, extra.tickets)
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
