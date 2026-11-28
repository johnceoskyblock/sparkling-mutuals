package net.johnceo.sparklingmutuals.history.importing

import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.*
import java.util.HexFormat
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.compat.smSetScreen
import net.johnceo.sparklingmutuals.history.Achievements
import net.johnceo.sparklingmutuals.history.DyeDropProofs
import net.johnceo.sparklingmutuals.history.EmeraldDyeDrop
import net.johnceo.sparklingmutuals.history.HistoryStore
import net.johnceo.sparklingmutuals.history.HistoryAccountId
import net.johnceo.sparklingmutuals.history.SafariEncounter
import net.johnceo.sparklingmutuals.history.SafariEncounterHistory
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
    // Windows may insert the copy suffix before .log.gz or before just its final .gz extension.
    private val dated = Regex("^(\\d{4})-(\\d{2})-(\\d{2})-(\\d+)(?: \\(\\d+\\)| - Copy(?: \\(\\d+\\))?)?\\.(?:log(?: \\(\\d+\\))?(?:\\.gz)?|json)$", RegexOption.IGNORE_CASE)
    private val compressedLog = Regex("\\.log(?: \\(\\d+\\))?\\.gz$", RegexOption.IGNORE_CASE)
    private fun isLog(name: String) = name.endsWith(".log", true) || compressedLog.containsMatchIn(name)
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "Sparkling Mutuals history import").apply { isDaemon = true } }
    private val busy = AtomicBoolean()
    val running get() = busy.get()
    @Volatile private var filesTotal = 0
    @Volatile private var filesDone = 0
    private var progressTicks = 0

    data class Failure(val file: String, val reason: String)
    internal class Result(val runs: List<RunRecord>, val sparklings: List<SparklingEntry>, val files: Int, val newest: Long,
        val failures: List<Failure> = emptyList(), val ticketTimes: List<Long> = emptyList(), val skippedAccounts: Int = 0,
        val alphaRuns: List<RunRecord> = emptyList(), val alphaSparklings: List<SparklingEntry> = emptyList(),
        val skippedFiles: List<Failure> = emptyList(), val unresolvedAccounts: Int = 0, val unknownServer: Int = 0,
        val foreignRuns: List<RunRecord> = emptyList(), val foreignSparklings: List<SparklingEntry> = emptyList(),
        val encounters: List<SafariEncounterHistory.Check> = emptyList(), val lastEmeraldDyeAt: Long? = null)
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

    private fun loadArchive(path: Path, day: LocalDate, active: Boolean): HistoryArchives.Input {
        val digest = MessageDigest.getInstance("SHA-256")
        return openLog(path) { stream ->
            DigestInputStream(stream, digest).bufferedReader(Charsets.UTF_8).use { reader ->
                val archive = if (path.toString().endsWith(".json", true)) HistoryBackfill.jsonArchive(reader, day, path.toString())
                    else HistoryBackfill.archive(reader, day, path.toString(), active)
                HistoryArchives.Input(path, HexFormat.of().formatHex(digest.digest()), archive)
            }
        }
    }

    private inline fun <T> openLog(path: Path, read: (java.io.InputStream) -> T): T =
        Files.newInputStream(path).use { raw ->
            if (path.toString().endsWith(".gz", true)) GZIPInputStream(raw).use(read) else read(raw)
        }

    private fun files(folder: Path, includeLatest: Boolean, newerThan: Long): List<LogFile> {
        // Copied instance logs are often left in a subfolder. Do not follow directory symlinks.
        val all = Files.walk(folder).use { paths -> paths.filter { !Files.isSymbolicLink(it) && Files.isRegularFile(it) }.toList() }.mapNotNull { path ->
            val name = path.fileName.toString()
            if (!isLog(name) && !name.endsWith(".json", true)) return@mapNotNull null
            if (!includeLatest && name.equals("latest.log", true)) return@mapNotNull null
            val modified = Files.getLastModifiedTime(path).toMillis()
            val match = dated.matchEntire(name)
            val day = match?.let { runCatching { LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }.getOrNull() }
                ?: Instant.ofEpochMilli(modified).atZone(ZoneId.systemDefault()).toLocalDate()
            LogFile(path, day, match?.groupValues?.get(4)?.toIntOrNull() ?: Int.MAX_VALUE, modified)
        }.sortedWith(compareBy({ it.day }, { it.order }, { it.path.toString() }))
        return when {
            newerThan == 0L -> all.filter { it.modified == all.maxOfOrNull { file -> file.modified } }
            newerThan > 0L -> all.filter { it.modified > newerThan }
            else -> all
        }
    }

    internal fun read(folder: Path, self: String, includeLatest: Boolean = true, newerThan: Long = -1,
             ownerUuid: String? = null, knownNames: Map<String, Set<String>> = emptyMap()): CompletableFuture<Result> =
        CompletableFuture.supplyAsync({
            val logs = files(folder, includeLatest, newerThan)
            filesTotal = logs.size; filesDone = 0
            val inputs = mutableListOf<HistoryArchives.Input>(); val failures = mutableListOf<Failure>()
            for (file in logs) {
                try {
                    val json = file.path.toString().endsWith(".json", true)
                    val day = if (dated.matches(file.path.fileName.toString()) || json) file.day else inferredStartDay(file.path, file.modified)
                    val active = runCatching { Files.isSameFile(file.path, defaultLogs().resolve("latest.log")) }.getOrDefault(false)
                    val path = file.path.toAbsolutePath().normalize()
                    inputs += loadArchive(path, day, active)
                } catch (error: Exception) {
                    failures += Failure(file.path.fileName.toString(), error.message ?: error.javaClass.simpleName)
                    logger.warn("Couldn't import {}", file.path.fileName, error)
                } finally { filesDone++ }
            }
            val selected = HistoryArchives.select(inputs, self, ownerUuid, knownNames, strictRealm = ownerUuid != null)
            val rebuilt = HistoryBackfill.rebuildSessions(selected.sessions, self)
            val foreign = HistoryBackfill.rebuildSessions(selected.foreign.map { it.archive.events }, "")
            val encounters = selected.sessions.asSequence().flatten().filter { it.main }.mapNotNull { event ->
                SafariEncounter.parse(event.text)?.let { SafariEncounterHistory.Check(it, event.at) }
            }.distinct().toList()
            val dyeProofs = DyeDropProofs()
            val lastDye = selected.sessions.asSequence().flatten().filter {
                it.main && EmeraldDyeDrop.owned(it.text, it.localName ?: self)
            }.distinctBy { it.at to it.text }.sortedBy { it.at }
                .lastOrNull { dyeProofs.accept(it.text, it.at) }?.at
            (selected.foreign + selected.unresolved).forEach { input ->
                logger.info("History log {}: {} ({})", input.path,
                    if (input in selected.foreign) "different account" else "ownership unresolved",
                    (input.archive.names + input.archive.identities.values.flatten()).joinToString())
            }
            selected.unknownServers.forEach { logger.info("History log {}: server unresolved for part of this log", it) }
            val skipped = selected.foreign.map { Failure(it.path.fileName.toString(),
                "Different account: ${(it.archive.names + it.archive.identities.values.flatten()).joinToString()}") }
            logger.info("History scan for {}: {} files, {} other-account files, {} unresolved accounts, {} unknown servers, {} failures, {} runs, {} sparkling catches",
                self, logs.size, skipped.size, selected.unresolved.size, selected.unknownServers.size, failures.size, rebuilt.runs.size, rebuilt.sparklings.size)
            rebuilt.runs.forEach { it.ownerUuid = ownerUuid }
            rebuilt.sparklings.forEach { it.ownerUuid = ownerUuid }
            Result(rebuilt.runs, rebuilt.sparklings, logs.size, logs.maxOfOrNull { it.modified } ?: 0L, failures, rebuilt.tickets,
                skipped.size, rebuilt.alphaRuns, rebuilt.alphaSparklings, skipped, selected.unresolved.size, selected.unknownServers.size,
                foreign.runs + foreign.alphaRuns, foreign.sparklings + foreign.alphaSparklings, encounters, lastDye)
        }, executor)

    data class CatchLog(val path: Path, val lines: List<String>, val line: Int)

    /** Locate the original server announcement, never a player-written detection alert or another account's log. */
    fun findCatch(entry: SparklingEntry, self: String, folder: Path = defaultLogs()): CompletableFuture<CatchLog?> {
        val knownNames = RunHistory.knownAccountNames()
        return CompletableFuture.supplyAsync({
            val day = Instant.ofEpochMilli(entry.at).atZone(ZoneId.systemDefault()).toLocalDate()
            val preferred = runCatching { Path.of(entry.logFile) }.getOrNull()?.takeIf { entry.logFile.isNotBlank() && Files.isRegularFile(it) }
            val nearby = if (Files.isDirectory(folder)) files(folder, true, -1).filter { file ->
                !file.path.toString().endsWith(".json", true) &&
                    (!dated.matches(file.path.fileName.toString()) || kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(day, file.day)) <= 1)
            }.sortedBy { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(day, it.day)) }.map { it.path } else emptyList()
            val candidates = (listOfNotNull(preferred) + nearby).map { it.toAbsolutePath().normalize() }.distinct()
            val inputs = candidates.mapNotNull { path ->
                if (!isLog(path.toString())) return@mapNotNull null
                runCatching {
                    val match = dated.matchEntire(path.fileName.toString())
                    val start = match?.let { LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
                        ?: inferredStartDay(path, Files.getLastModifiedTime(path).toMillis())
                    loadArchive(path, start, active = false)
                }.getOrNull()
            }
            val owned = HistoryArchives.select(inputs, self, entry.ownerUuid, knownNames, strictRealm = true)
            val event = owned.sessions.asSequence().flatten().filter { it.main && kotlin.math.abs(it.at - entry.at) <= 10_000 }
                .filter { event ->
                    val broadcast = net.johnceo.sparklingmutuals.history.RunTally.sparkling.matchEntire(event.text)?.let {
                        it.groupValues[1].equals(entry.player, true) && it.groupValues[2].trim().equals(entry.critter, true)
                    }
                    broadcast ?: (entry.personal == true && entry.player.equals(event.localName ?: self, true) &&
                        net.johnceo.sparklingmutuals.safari.SafariCatch.parse(event.text)?.let {
                            it.personal && it.sparkling && it.critter.name.equals(entry.critter, true)
                        } == true)
                }.minByOrNull { kotlin.math.abs(it.at - entry.at) } ?: return@supplyAsync null
            val path = Path.of(event.source)
            runCatching { openLog(path) { raw ->
                CatchLog(path, raw.bufferedReader(Charsets.UTF_8).use { it.readLines() }, event.ordinal)
            } }.getOrNull()
        }, executor)
    }

    internal fun runImport(folder: String): Int {
        val path = try { Path.of(folder.trim().removeSurrounding("\"")) } catch (_: InvalidPathException) {
            return rejected("[SM] Enter a valid logs folder.")
        }
        return runImport(path)
    }

    /** Shared by the command and settings button. Parsing is off-thread and merging runs on the client. */
    fun runImport(folder: Path = defaultLogs()): Int {
        val client = Minecraft.getInstance()
        if (!Files.isDirectory(folder)) return rejected("[SM] No logs folder at $folder")
        if (!busy.compareAndSet(false, true)) return rejected("[SM] A history import is already running.")
        filesTotal = 0; filesDone = 0; progressTicks = 0
        try {
            client.smSetScreen(null)
            HistoryAccountId.tick(client)
        } catch (error: Exception) {
            busy.set(false)
            logger.warn("Could not start history import", error)
            return rejected("[SM] Could not start history import. See latest.log.")
        }
        val self = client.player?.gameProfile?.name.orEmpty()
        val owner = HistoryAccountId.selected(client)
        if (owner == null || self.isBlank()) {
            busy.set(false)
            return rejected("[SM] Join the account selected under History UUID before importing.")
        }
        val knownNames = RunHistory.knownAccountNames()
        tell("[SM] Importing run and sparkling history from $folder. Progress is shown above your hotbar.")
        val reference = RunHistory.runs.map { RunRecord(caught = it.caught, lootShares = it.lootShares, party = it.party,
            feathers = it.feathers, sparklings = it.sparklings.toMutableList(), coins = it.coins, essenceCoins = it.essenceCoins) }
        val quotes = net.johnceo.sparklingmutuals.history.BazaarPrices.cachedPrices
        val instaSell = net.johnceo.sparklingmutuals.config.ConfigManager.runCoinsInstaSell
        // Manual import always rescans all eligible files, including logs imported previously.
        var apiTotal: Long? = null
        read(folder, self, includeLatest = true, newerThan = -1, ownerUuid = owner, knownNames = knownNames).thenApply { result ->
            result.runs.forEach { HistoryEstimates.assignCoins(it, reference, quotes, instaSell) }
            // Optional comparison only: API lag, incomplete logs and profile changes must never drive deletions.
            if (net.johnceo.sparklingmutuals.api.HypixelApi.provider.enabled) apiTotal = runCatching {
                net.johnceo.sparklingmutuals.api.HypixelApi.getSafariProfile(owner).totalCaptured
            }.getOrNull()
            result
        }.whenComplete { result, error -> client.execute {
            try {
                if (error != null || result == null) {
                    logger.warn("History import failed", error)
                    tell("[SM] History import failed; see latest.log.")
                    client.player?.sendOverlayMessage(Component.literal("§cHistory import failed"))
                } else if (HistoryAccountId.selected(client) != owner) {
                    tell("[SM] Import cancelled because the active account changed. No history was merged.")
                } else {
                    HistoryStore.backup("runs"); HistoryStore.backup("sparklings")
                    val removedRuns = RunHistory.reconcileImport(result.runs, result.alphaRuns, result.foreignRuns)
                    val removedSparks = SparklingLog.reconcileImport(result.sparklings, result.alphaSparklings, result.foreignSparklings)
                    val runs = RunHistory.importRuns(result.runs)
                    val sparks = SparklingLog.importEntries(result.sparklings, result.ticketTimes)
                    val checks = SafariEncounterHistory.import(owner, result.encounters)
                    Achievements.importLastEmeraldDye(owner, result.lastEmeraldDyeAt)
                    val earned = Achievements.afterImport()
                    apiTotal?.let { total ->
                        val personal = SparklingLog.entries.count { SparklingLog.isPersonal(it, self) }
                        tell("[SM] $self has $personal personal sparkling catches in history and $total on the API.")
                    }
                    client.player?.sendOverlayMessage(Component.literal("§aHistory import complete"))
                    tell("[SM] Scanned ${result.files} logs. Added §e$runs§r runs and §e$sparks§r sparklings. Recovered §e$checks§r checks from encounter announcements. Updated ${RunHistory.lastImportUpdated} imported runs. " +
                        "Removed $removedRuns invalid or duplicate imported runs and $removedSparks invalid or duplicate catches." +
                        (if (result.skippedAccounts > 0) " Excluded ${result.skippedAccounts} logs from other accounts." else "") +
                        (if (result.unresolvedAccounts + result.unknownServer > 0) " Still unresolved: ${result.unresolvedAccounts} account, ${result.unknownServer} server. Add their companion logs and import again." else "") +
                        (if (result.failures.isNotEmpty()) " §c${result.failures.size} logs could not be read. See latest.log." else "") +
                        (if (earned > 0) " §6$earned achievements unlocked!" else ""))
                }
            } catch (failure: Exception) {
                logger.warn("Couldn't merge imported history", failure)
                tell("[SM] Couldn't save imported history; see latest.log. Pre-import backups were kept.")
            } finally { busy.set(false) }
        } }
        return 1
    }
    fun tick(client: Minecraft) {
        if (!running || client.player == null || ++progressTicks % 4 != 0) return
        val fraction = if (filesTotal == 0) 0.0 else filesDone.toDouble() / filesTotal
        val filled = (fraction * 24).toInt().coerceIn(0, 24)
        client.player?.sendOverlayMessage(Component.literal("§bImporting Safari history §a" + "■".repeat(filled) + "§8" + "■".repeat(24 - filled) +
            " §f${(fraction * 100).toInt()}% §7($filesDone/$filesTotal files)"))
    }
    private fun rejected(message: String): Int { tell(message); return 0 }
    private fun tell(text: String) { Minecraft.getInstance().player?.sendSystemMessage(LocalChat.component(text)) }
}
