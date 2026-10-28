package net.johnceo.sparklingmutuals.history.importing

import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Path
import java.time.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.history.Achievements
import net.johnceo.sparklingmutuals.history.HistoryStore
import net.johnceo.sparklingmutuals.history.RunHistory
import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.history.SparklingEntry
import net.johnceo.sparklingmutuals.history.SparklingLog
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

/** One explicit backfill imports both histories. Normal sessions are recorded live, without startup rescans. */
object LogImporter {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/history-import")
    private val dated = Regex("^(\\d{4})-(\\d{2})-(\\d{2})-(\\d+)\\.(?:log(?:\\.gz)?|json)$", RegexOption.IGNORE_CASE)
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "Sparkling Mutuals history import").apply { isDaemon = true } }
    private val busy = AtomicBoolean()
    val running get() = busy.get()
    @Volatile private var filesTotal = 0
    @Volatile private var filesDone = 0
    private var progressTicks = 0

    data class Failure(val file: String, val reason: String)
    class Result(val runs: List<RunRecord>, val sparklings: List<SparklingEntry>, val files: Int, val newest: Long,
        val failures: List<Failure> = emptyList(), val ticketTimes: List<Long> = emptyList(), val skippedAccounts: Int = 0,
        val alphaRuns: List<RunRecord> = emptyList(), val alphaSparklings: List<SparklingEntry> = emptyList())
    private data class LogFile(val path: Path, val day: LocalDate, val order: Int, val modified: Long)
    fun defaultLogs(): Path = FabricLoader.getInstance().gameDir.resolve("logs")

    /** Client-independent parser. Histories are rebuilt chronologically across all supplied files. */
    class Parser(private val self: String, private val requireOwner: Boolean = true) {
        private val events = mutableListOf<HistoryBackfill.Event>()
        private var serial = 0
        private var cached: HistoryBackfill.Rebuilt? = null
        private fun result() = cached ?: HistoryBackfill.rebuild(events, self).also { cached = it }
        val runs get() = result().runs
        val sparklings get() = result().sparklings
        val ticketTimes get() = result().tickets
        val alphaRuns get() = result().alphaRuns
        val alphaSparklings get() = result().alphaSparklings
        fun read(reader: BufferedReader, startDay: LocalDate, closeAtEnd: Boolean = true, source: String = "log-${serial++}") {
            // Commit only completely read files: a broken gzip must not leave half a file's events behind.
            val read = HistoryBackfill.log(reader, startDay, source, active = !closeAtEnd, expectedSelf = self.takeIf { requireOwner })
            events.addAll(read); cached = null
        }
        fun readJson(reader: BufferedReader, startDay: LocalDate, source: String = "json-${serial++}") {
            val read = HistoryBackfill.json(reader, startDay, source, expectedSelf = self.takeIf { requireOwner })
            events.addAll(read); cached = null
        }
        fun handle(text: String, at: Long) {
            events += HistoryBackfill.Event(at, HistoryBackfill.body(text), "direct", serial++)
            cached = null
        }
    }

    fun startDay(times: Iterable<LocalTime>, modified: LocalDateTime): LocalDate {
        var crossings = 0L; var previous: LocalTime? = null
        for (time in times) {
            if (previous?.let { time.toSecondOfDay() < it.toSecondOfDay() - 3600 } == true) crossings++
            previous = time
        }
        var endDay = modified.toLocalDate()
        if (previous?.let { it.toSecondOfDay() > modified.toLocalTime().toSecondOfDay() + 3600 } == true) endDay = endDay.minusDays(1)
        return endDay.minusDays(crossings)
    }
    private fun inferredStartDay(path: Path, modified: Long): LocalDate = Files.newInputStream(path).use { raw ->
        (if (path.toString().endsWith(".gz", true)) GZIPInputStream(raw) else raw).bufferedReader().useLines { lines ->
            startDay(lines.mapNotNull(HistoryBackfill::time).asIterable(), LocalDateTime.ofInstant(Instant.ofEpochMilli(modified), ZoneId.systemDefault()))
        }
    }

    private fun files(folder: Path, includeLatest: Boolean, newerThan: Long): List<LogFile> {
        val all = Files.list(folder).use { paths -> paths.filter { Files.isRegularFile(it) }.toList() }.mapNotNull { path ->
            val name = path.fileName.toString()
            if (!name.endsWith(".log", true) && !name.endsWith(".log.gz", true) && !name.endsWith(".json", true)) return@mapNotNull null
            if (!includeLatest && name.equals("latest.log", true)) return@mapNotNull null
            val modified = Files.getLastModifiedTime(path).toMillis()
            val match = dated.matchEntire(name)
            val day = match?.let { LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
                ?: Instant.ofEpochMilli(modified).atZone(ZoneId.systemDefault()).toLocalDate()
            LogFile(path, day, match?.groupValues?.get(4)?.toIntOrNull() ?: Int.MAX_VALUE, modified)
        }.sortedWith(compareBy({ it.day }, { it.order }, { it.path.toString() }))
        return when {
            newerThan == 0L -> all.filter { it.modified == all.maxOfOrNull { file -> file.modified } }
            newerThan > 0L -> all.filter { it.modified > newerThan }
            else -> all
        }
    }

    fun read(folder: Path, self: String, includeLatest: Boolean = true, newerThan: Long = -1): CompletableFuture<Result> =
        CompletableFuture.supplyAsync({
            val logs = files(folder, includeLatest, newerThan)
            filesTotal = logs.size; filesDone = 0
            val parser = Parser(self); val failures = mutableListOf<Failure>(); var skippedAccounts = 0
            for (file in logs) {
                try {
                    val json = file.path.toString().endsWith(".json", true)
                    val day = if (dated.matches(file.path.fileName.toString()) || json) file.day else inferredStartDay(file.path, file.modified)
                    val active = runCatching { Files.isSameFile(file.path, defaultLogs().resolve("latest.log")) }.getOrDefault(false)
                    Files.newInputStream(file.path).use { raw ->
                        (if (file.path.toString().endsWith(".gz", true)) GZIPInputStream(raw) else raw).bufferedReader(Charsets.UTF_8).use { reader ->
                            if (json) parser.readJson(reader, day, file.path.toAbsolutePath().normalize().toString())
                            else parser.read(reader, day, closeAtEnd = !active, source = file.path.toAbsolutePath().normalize().toString())
                        }
                    }
                } catch (_: HistoryBackfill.OwnerMismatch) {
                    skippedAccounts++
                } catch (error: Exception) {
                    failures += Failure(file.path.fileName.toString(), error.message ?: error.javaClass.simpleName)
                    logger.warn("Couldn't import {}", file.path.fileName, error)
                } finally { filesDone++ }
            }
            Result(parser.runs, parser.sparklings, logs.size, logs.maxOfOrNull { it.modified } ?: 0L, failures, parser.ticketTimes, skippedAccounts, parser.alphaRuns, parser.alphaSparklings)
        }, executor)

    data class CatchLog(val path: Path, val lines: List<String>, val line: Int)

    /** Locate the original server announcement, never a player-written detection alert or another account's log. */
    fun findCatch(entry: SparklingEntry, self: String, folder: Path = defaultLogs()): CompletableFuture<CatchLog?> =
        CompletableFuture.supplyAsync({
            val day = Instant.ofEpochMilli(entry.at).atZone(ZoneId.systemDefault()).toLocalDate()
            val preferred = runCatching { Path.of(entry.logFile) }.getOrNull()?.takeIf { entry.logFile.isNotBlank() && Files.isRegularFile(it) }
            val nearby = if (Files.isDirectory(folder)) files(folder, true, -1).filter { file ->
                !file.path.toString().endsWith(".json", true) &&
                    (!dated.matches(file.path.fileName.toString()) || kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(day, file.day)) <= 1)
            }.sortedBy { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(day, it.day)) }.map { it.path } else emptyList()
            val candidates = (listOfNotNull(preferred) + nearby).distinct()
            var best: CatchLog? = null; var distance = Long.MAX_VALUE
            for (path in candidates) {
                if (!path.toString().endsWith(".log", true) && !path.toString().endsWith(".log.gz", true)) continue
                runCatching {
                    val match = dated.matchEntire(path.fileName.toString())
                    val start = match?.let { LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
                        ?: inferredStartDay(path, Files.getLastModifiedTime(path).toMillis())
                    val text = Files.newInputStream(path).use { raw ->
                        (if (path.toString().endsWith(".gz", true)) GZIPInputStream(raw) else raw).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    }
                    val events = HistoryBackfill.log(text.reader().buffered(), start, path.toString(), active = false, expectedSelf = self)
                    val event = events.filter { !it.alpha && kotlin.math.abs(it.at - entry.at) <= 10_000 }.filter { event ->
                        net.johnceo.sparklingmutuals.history.RunTally.sparkling.matchEntire(event.text)?.let {
                            it.groupValues[1].equals(entry.player, true) && it.groupValues[2].trim().equals(entry.critter, true)
                        } == true
                    }.minByOrNull { kotlin.math.abs(it.at - entry.at) }
                    if (event != null && kotlin.math.abs(event.at - entry.at) < distance) {
                        distance = kotlin.math.abs(event.at - entry.at)
                        best = CatchLog(path.toAbsolutePath().normalize(), text.lineSequence().toList(), event.ordinal)
                    }
                }
                // Log timestamps have second precision; a match in that second cannot be improved usefully.
                if (distance < 1000) break
            }
            best
        }, executor)

    /** Shared by the command and settings button. Parsing is off-thread; merging is serialized on the client. */
    fun runImport(folder: Path) {
        val client = Minecraft.getInstance()
        if (!Files.isDirectory(folder)) return tell("[SM] No logs folder at $folder")
        if (!busy.compareAndSet(false, true)) return tell("[SM] A history import is already running.")
        filesTotal = 0; filesDone = 0; progressTicks = 0
        val self = client.player?.name?.string.orEmpty()
        val owner = client.player?.uuid
        if (owner == null || self.isBlank()) { busy.set(false); return tell("[SM] Join the account whose history you want to import first.") }
        tell("[SM] Importing run and sparkling history from $folder. Progress is shown above your hotbar.")
        val reference = RunHistory.runs.map { RunRecord(caught = it.caught, lootShares = it.lootShares, party = it.party,
            feathers = it.feathers, sparklings = it.sparklings.toMutableList(), coins = it.coins, essenceCoins = it.essenceCoins) }
        val quotes = net.johnceo.sparklingmutuals.history.BazaarPrices.cachedPrices
        val instaSell = net.johnceo.sparklingmutuals.config.ConfigManager.runCoinsInstaSell
        // Manual import always rescans all eligible files, including logs imported previously.
        var apiTotal: Long? = null
        read(folder, self, includeLatest = true, newerThan = -1).thenApply { result ->
            result.runs.forEach { HistoryEstimates.assignCoins(it, reference, quotes, instaSell) }
            // Optional comparison only: API lag, incomplete logs and profile changes must never drive deletions.
            if (net.johnceo.sparklingmutuals.api.HypixelApi.provider.enabled) apiTotal = runCatching {
                net.johnceo.sparklingmutuals.api.HypixelApi.getSafariProfile(owner.toString()).totalCaptured
            }.getOrNull()
            result
        }.whenComplete { result, error -> client.execute {
            try {
                if (error != null || result == null) {
                    logger.warn("History import failed", error)
                    tell("[SM] History import failed; see latest.log.")
                    client.player?.sendOverlayMessage(Component.literal("§cHistory import failed"))
                } else if (client.player?.uuid != owner) {
                    tell("[SM] Import cancelled because the active account changed. No history was merged.")
                } else {
                    HistoryStore.backup("runs"); HistoryStore.backup("sparklings")
                    val removedRuns = RunHistory.reconcileImport(result.runs, result.alphaRuns)
                    val removedSparks = SparklingLog.reconcileImport(result.sparklings, result.alphaSparklings)
                    val runs = RunHistory.importRuns(result.runs)
                    val sparks = SparklingLog.importEntries(result.sparklings, result.ticketTimes)
                    val earned = Achievements.afterImport()
                    apiTotal?.let { total -> tell("[SM] $self: ${SparklingLog.entries.size} saved party catches; API total $total. " +
                        "Totals are a cross-check only; records without log evidence are kept.") }
                    client.player?.sendOverlayMessage(Component.literal("§aHistory import complete"))
                    tell("[SM] Scanned ${result.files} files: added §e$runs§r runs, updated §e${RunHistory.lastImportUpdated}§r existing runs, and added §e$sparks§r sparklings. " +
                        "Removed $removedRuns Alpha/duplicate runs and $removedSparks Alpha/duplicate catches confirmed by scanned logs. ${result.skippedAccounts} files skipped for a different or unverifiable account." + (if (result.failures.isNotEmpty()) " §c${result.failures.size} files failed; see latest.log." else "") +
                        (if (earned > 0) " §6$earned achievements unlocked!" else ""))
                }
            } catch (failure: Exception) {
                logger.warn("Couldn't merge imported history", failure)
                tell("[SM] Couldn't save imported history; see latest.log. Pre-import backups were kept.")
            } finally { busy.set(false) }
        } }
    }
    fun tick(client: Minecraft) {
        if (!running || client.player == null || ++progressTicks % 4 != 0) return
        val fraction = if (filesTotal == 0) 0.0 else filesDone.toDouble() / filesTotal
        val filled = (fraction * 24).toInt().coerceIn(0, 24)
        client.player?.sendOverlayMessage(Component.literal("§bImporting both histories §a" + "■".repeat(filled) + "§8" + "■".repeat(24 - filled) +
            " §f${(fraction * 100).toInt()}% §7($filesDone/$filesTotal files)"))
    }
    private fun tell(text: String) { Minecraft.getInstance().player?.sendSystemMessage(LocalChat.component(text)) }
}
