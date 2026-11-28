package net.johnceo.sparklingmutuals.history.importing

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.time.*
import java.util.Locale
import net.johnceo.sparklingmutuals.history.HistoryAccountId
import net.johnceo.sparklingmutuals.history.CaptureProgress
import net.johnceo.sparklingmutuals.history.EmeraldDyeDrop
import net.johnceo.sparklingmutuals.history.SafariEncounter
import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.history.RunTally
import net.johnceo.sparklingmutuals.history.SparklingEntry
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRules

/** Kotlin port of the supplied run/sparkling backfillers; no browser or script execution is involved. */
internal object HistoryBackfill {
    enum class Realm { UNKNOWN, MAIN, ALPHA, OTHER;
        companion object {
            fun fromHost(host: String?) = when (host?.lowercase(Locale.ROOT)?.substringBefore(':')) {
                null, "" -> UNKNOWN
                "alpha.hypixel.net" -> ALPHA
                "hypixel.net", "mc.hypixel.net", "proxy.hypixel.net", "www.hypixel.net" -> MAIN
                else -> OTHER
            }
        }
    }
    data class Event(val at: Long, val text: String, val source: String, val ordinal: Int, val eof: Boolean = false,
        val active: Boolean = false, val realm: Realm = Realm.UNKNOWN, val boundary: Boolean = false, val localName: String? = null) {
        val alpha get() = realm == Realm.ALPHA
        val main get() = realm == Realm.MAIN
    }
    data class Archive(val events: List<Event>, val names: Set<String>, val identities: Map<String, Set<String>>,
        val first: Long, val last: Long, val started: Boolean = false, val stopped: Boolean = false)
    data class Rebuilt(val runs: List<RunRecord>, val sparklings: List<SparklingEntry>, val tickets: List<Long>,
        val alphaRuns: List<RunRecord> = emptyList(), val alphaSparklings: List<SparklingEntry> = emptyList())
    class OwnerMismatch(message: String) : IllegalArgumentException(message)
    private val connectionLine = Regex("^Connecting to ([A-Za-z0-9.-]+), [0-9]+$")
    private val ownerLine = Regex("^Setting user: ([A-Za-z0-9_]{1,16})$")
    private val identityLine = Regex("^\\[SM History] Account ([0-9a-fA-F-]{32,36}) ([A-Za-z0-9_]{1,16})$")
    private val authenticationLine = Regex("^Authenticating to Mojang as ([A-Za-z0-9_]{1,16}) \\(([0-9a-fA-F-]{32,36})\\)$")
    private val ansi = Regex("\u001B\\[[0-9;]*m")
    private val timePrefix = Regex("^\\s*\\[(\\d{1,2}):(\\d{2}):(\\d{2})(?:[.,]\\d+)?]:?")
    private val headers = listOf(timePrefix, Regex("^\\s*\\[[^]\\r\\n]*/(?:INFO|WARN|ERROR|DEBUG|TRACE|FATAL)]:?", RegexOption.IGNORE_CASE),
        Regex("^\\s*\\([^()\\r\\n]{1,100}\\)"), Regex("^\\s*\\[(?:System|CHAT)]:?", RegexOption.IGNORE_CASE), Regex("^\\s*:"))
    private val admission = Regex("^(?:\\[NPC]\\s*)?Safari Manager:\\s*(Looks good to me\\. Have fun out there!|I already saw your ticket, so you['’]re free to go\\.)$", RegexOption.IGNORE_CASE)
    private val hotspot = Regex("^HOTSPOT! Your Hunting Hotspot is the (?:Cavern|Haunted|Icy|Forest) Biome!$")
    private val sending = Regex("^Sending to server \\S+\\.\\.\\.$")
    private val split = Regex("^Party > [^:]+: (forest|cavern|icy|ice|haunted) (?:(?:clear|done)!?|completed in (?:(\\d+)m )?(\\d+(?:\\.\\d+)?)s!?(?: \\(\\d/4\\))?)$", RegexOption.IGNORE_CASE)
    private val summaryReward = Regex("^\\+[0-9,]+ (?:Shards|Safari Essence|Hunting Exp)$")
    private fun shortSplit(text: String) = when (net.johnceo.sparklingmutuals.commands.PartyChat.parse(text)?.body?.lowercase(Locale.ROOT)) {
        "fd" -> "forest"; "cd" -> "cavern"; "id" -> "icy"; "hd" -> "haunted"; else -> null
    }
    private fun mode(text: String) = when (text) {
        "[SM] Unique run mode on." -> "UNIQUE"
        "[SM] Full clear mode on." -> "FULL_CLEAR"
        "[SM] Sparkling mode on." -> "SPARKLING"
        else -> null
    }
    private const val SUMMARY = "SAFARI REWARD SUMMARY"
    private const val STASH_FEATHER = "Since you're in the Critter Safari, the Rainbow Feather was sent to your stash!"

    fun body(raw: String): String {
        var result = SafariRules.strip(ansi.replace(raw, "")).removePrefix("\uFEFF").trim()
        while (true) {
            val head = headers.firstNotNullOfOrNull { it.find(result)?.takeIf { match -> match.range.first == 0 } } ?: return result.trim()
            result = result.substring(head.value.length).trimStart()
        }
    }
    fun time(raw: String): LocalTime? = timePrefix.find(ansi.replace(raw, ""))?.let {
        runCatching { LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }.getOrNull()
    }
    fun isTicket(text: String) = admission.matchEntire(text)?.groupValues?.get(1)?.startsWith("Looks good", true) == true
    private fun relevant(text: String): Boolean = when {
        text.startsWith("CAPTURE!") || text.startsWith("LOOT SHARE!") -> SafariCatch.parse(text) != null
        text.startsWith("SPARKLING!") -> RunTally.sparkling.matches(text)
        text.startsWith("Party") -> split.matches(text) || shortSplit(text) != null
        text.startsWith("[SM]") -> mode(text) != null
        text.startsWith("HOTSPOT!") -> hotspot.matches(text)
        text.startsWith("Sending to server ") -> sending.matches(text)
        text.startsWith("+") -> summaryReward.matches(text)
        else -> text == SUMMARY || text == RunTally.MACAW_BONUS || text == STASH_FEATHER ||
            admission.matches(text) || SafariEncounter.parse(text) != null || EmeraldDyeDrop.candidate(text)
    }

    fun log(reader: BufferedReader, day: LocalDate, source: String, active: Boolean, expectedSelf: String? = null): List<Event> =
        archive(reader, day, source, active).also { checkOwner(it.names, expectedSelf) }.events

    fun archive(reader: BufferedReader, day: LocalDate, source: String, active: Boolean): Archive {
        val result = mutableListOf<Event>()
        val owners = mutableSetOf<String>(); val identities = mutableMapOf<String, MutableSet<String>>()
        val authenticated = mutableSetOf<Pair<String, String>>()
        var date = day; var previous: LocalTime? = null; var first = 0L; var last = 0L
        var realm = Realm.UNKNOWN; var started = false; var stopped = false
        reader.lineSequence().forEachIndexed { index, raw ->
            val time = time(raw) ?: return@forEachIndexed
            if (previous?.let { time.toSecondOfDay() < it.toSecondOfDay() - 3600 } == true) date = date.plusDays(1)
            previous = time
            last = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (first == 0L) first = last
            val text = body(raw)
            if (!raw.contains("[CHAT]", ignoreCase = true)) {
                ownerLine.matchEntire(text)?.groupValues?.get(1)?.let {
                    owners += it; started = true
                }
                identityLine.matchEntire(text)?.let {
                    HistoryAccountId.normalize(it.groupValues[1])?.let { uuid ->
                        identities.getOrPut(uuid) { mutableSetOf() } += it.groupValues[2]
                    }
                }
                authenticationLine.matchEntire(text)?.destructured?.let { (name, id) ->
                    HistoryAccountId.normalize(id)?.let { authenticated += it to name }
                }
                val host = connectionLine.matchEntire(text)?.groupValues?.get(1)
                if (host != null || text == "Stopping!") {
                    result += Event(last, "Sending to server connection...", source, index, realm = realm, boundary = true)
                    realm = Realm.fromHost(host)
                    if (host == null) stopped = true
                }
            }
            if (text.equals("alpha.hypixel.net", true)) realm = Realm.ALPHA
            else if (text.equals("www.hypixel.net", true)) realm = Realm.MAIN
            if (relevant(text)) result += Event(last, text, source, index, realm = realm)
        }
        // A login from an account switcher must agree with the client startup name when present.
        for ((id, name) in authenticated) {
            if (owners.isEmpty() || owners.any { it.equals(name, true) }) identities.getOrPut(id) { mutableSetOf() } += name
        }
        if (last > 0) result += Event(last, "", source, Int.MAX_VALUE, eof = true, active = active, realm = realm)
        return Archive(result, owners, identities, first, last, started, stopped)
    }
    private fun checkOwner(owners: Set<String>, expectedSelf: String?) {
        if (expectedSelf == null) return
        if (expectedSelf.isBlank() || owners.isEmpty()) throw OwnerMismatch("Log account could not be verified")
        if (owners.any { !it.equals(expectedSelf, true) }) throw OwnerMismatch("Log belongs to ${owners.joinToString()}, not $expectedSelf")
    }
    private fun epoch(value: JsonElement?): Long? {
        if (value == null || !value.isJsonPrimitive) return null
        return runCatching {
            if (value.asJsonPrimitive.isNumber) value.asDouble.let { if (it < 1e12) (it * 1000).toLong() else it.toLong() }
            else runCatching { Instant.parse(value.asString).toEpochMilli() }.getOrElse {
                OffsetDateTime.parse(value.asString).toInstant().toEpochMilli()
            }
        }.getOrNull()?.takeIf { it > 0 }
    }
    fun json(reader: BufferedReader, day: LocalDate, source: String, expectedSelf: String? = null): List<Event> =
        jsonArchive(reader, day, source).also { checkOwner(it.names, expectedSelf) }.events

    fun jsonArchive(reader: BufferedReader, day: LocalDate, source: String): Archive {
        val root = JsonParser.parseReader(reader)
        val owners = mutableSetOf<String>()
        if (root.isJsonObject) listOf("owner", "self", "username").forEach { field ->
            root.asJsonObject.get(field)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.let { owners += it.asString }
        }
        val items = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject && root.asJsonObject.get("entries")?.isJsonArray == true -> root.asJsonObject.getAsJsonArray("entries")
            else -> error("Expected a chat JSON array or an object containing entries")
        }
        val result = mutableListOf<Event>()
        val rootServer = root.takeIf { it.isJsonObject }?.asJsonObject?.get("server")?.takeIf { it.isJsonPrimitive }?.asString
        val uuid = root.takeIf { it.isJsonObject }?.asJsonObject?.get("uuid")?.takeIf { it.isJsonPrimitive }?.asString?.let(HistoryAccountId::normalize)
        var realm = Realm.fromHost(rootServer)
        items.forEachIndexed { index, item ->
            val obj = item.takeIf { it.isJsonObject }?.asJsonObject
            obj?.get("server")?.takeIf { it.isJsonPrimitive }?.asString?.let { realm = Realm.fromHost(it) }
            val at = obj?.let { value -> listOf("at", "timestamp", "time", "epochMillis").firstNotNullOfOrNull { epoch(value.get(it)) } }
            val direct = obj?.takeIf { at != null && it.get("player")?.let { field -> field.isJsonPrimitive && field.asJsonPrimitive.isString } == true &&
                it.get("critter")?.let { field -> field.isJsonPrimitive && field.asJsonPrimitive.isString } == true }
            if (direct != null) {
                val player = direct.get("player").asString; val critter = direct.get("critter").asString
                val text = "SPARKLING! $player caught a SPARKLING $critter!"
                if (RunTally.sparkling.matches(text)) result += Event(at!!, text, source, index, realm = realm)
            } else {
                val raw = if (item.isJsonPrimitive && item.asJsonPrimitive.isString) item.asString else
                    obj?.let { value -> listOf("message", "text", "formattedMessage", "content", "line").firstNotNullOfOrNull {
                        value.get(it)?.takeIf { field -> field.isJsonPrimitive && field.asJsonPrimitive.isString }?.asString
                    } } ?: return@forEachIndexed
                val timestamp = at ?: time(raw)?.let { day.atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() } ?: return@forEachIndexed
                val text = body(raw)
                if ("[CHAT]" !in raw) ownerLine.matchEntire(text)?.let { owners += it.groupValues[1] }
                val host = if ("[CHAT]" !in raw) connectionLine.matchEntire(text)?.groupValues?.get(1) else null
                if (host != null) { result += Event(timestamp, "Sending to server connection...", source, index, realm = realm, boundary = true); realm = Realm.fromHost(host) }
                else if (text.equals("alpha.hypixel.net", true)) realm = Realm.ALPHA
                else if (text.equals("www.hypixel.net", true)) realm = Realm.MAIN
                if (relevant(text)) result += Event(timestamp, text, source, index, realm = realm)
            }
        }
        return Archive(result, owners, uuid?.let { mapOf(it to owners) }.orEmpty(), 0, 0)
    }

    /** Keep repeated captures within one file; overlapping file copies contribute only their maximum multiplicity. */
    fun dedupe(raw: List<Event>): List<Event> {
        val occurrences = mutableMapOf<Pair<String, String>, Int>()
        val seen = mutableSetOf<Pair<String, Int>>()
        val lastSpark = mutableMapOf<String, Long>()
        return raw.sortedWith(compareBy<Event> { it.at }.thenBy { it.eof }).filter { event ->
            if (event.eof) return@filter true
            val identity = "${event.at}|${event.text}"
            val sourceKey = event.source to identity
            val occurrence = (occurrences[sourceKey] ?: 0) + 1
            occurrences[sourceKey] = occurrence
            if (!seen.add(identity to occurrence)) return@filter false
            val spark = RunTally.sparkling.matchEntire(event.text)
            if (spark != null) {
                val key = "${spark.groupValues[1]}:${spark.groupValues[2]}".lowercase(Locale.ROOT)
                val previous = lastSpark[key]
                if (previous != null && event.at - previous <= 1500) return@filter false
                lastSpark[key] = event.at
            }
            true
        }
    }
    fun rebuild(raw: List<Event>, self: String): Rebuilt {
        val alphaIdentities = raw.filter { it.alpha }.map { it.at to it.text }.toSet()
        // An unlabelled JSON/chat copy cannot reintroduce a catch explicitly recorded on Alpha.
        val main = rebuildEvents(raw.filter { it.main || it.realm == Realm.UNKNOWN && (it.at to it.text) !in alphaIdentities }, self)
        val alpha = rebuildEvents(raw.filter { it.alpha }, self)
        return main.copy(alphaRuns = alpha.runs, alphaSparklings = alpha.sparklings)
    }
    fun rebuildSessions(sessions: List<List<Event>>, self: String): Rebuilt {
        val main = mutableListOf<Rebuilt>(); val alpha = mutableListOf<Rebuilt>()
        for (events in sessions) {
            val segment = mutableListOf<Event>()
            fun flush() {
                if (segment.isEmpty()) return
                val realm = segment.first().realm
                if (realm != Realm.OTHER) {
                    val target = if (realm == Realm.ALPHA) alpha else main
                    target += rebuildEvents(segment, self)
                }
                segment.clear()
            }
            for (event in events) {
                if (segment.isNotEmpty() && segment.last().realm != event.realm) flush()
                segment += event
                if (event.boundary) flush()
            }
            flush()
        }
        val tickets = main.flatMap { it.tickets }.distinct().sorted()
        val mainCatches = main.flatMap { it.sparklings }.distinctBy(::catchKey).sortedBy { it.at }
        var used = 0
        mainCatches.forEach { entry ->
            val before = used
            while (used < tickets.size && tickets[used] <= entry.at) used++
            entry.tickets = used - before
        }
        return Rebuilt(main.flatMap { it.runs }.distinctBy { it.start to it.ticketAt }.sortedBy { it.start },
            mainCatches, tickets, alpha.flatMap { it.runs }, alpha.flatMap { it.sparklings })
    }
    private fun catchKey(entry: SparklingEntry) =
        Triple(entry.at, entry.player.lowercase(Locale.ROOT), entry.critter.lowercase(Locale.ROOT))

    private fun includePersonalReceipts(raw: List<Event>, self: String): List<Event> {
        val broadcasts = raw.mapNotNull { event ->
            RunTally.sparkling.matchEntire(event.text)?.let { match ->
                (match.groupValues[1].lowercase(Locale.ROOT) to match.groupValues[2].lowercase(Locale.ROOT)) to event.at
            }
        }.groupBy({ it.first }, { it.second })
        val receipts = raw.mapNotNull { event ->
            val local = event.localName ?: self
            if (local.isBlank() || !event.text.startsWith("CAPTURE!")) return@mapNotNull null
            val found = SafariCatch.parse(event.text)?.takeIf { it.personal && it.sparkling } ?: return@mapNotNull null
            val species = found.critter.name
            if (broadcasts[local.lowercase(Locale.ROOT) to species.lowercase(Locale.ROOT)].orEmpty()
                    .any { kotlin.math.abs(event.at - it) <= 2_000 }) return@mapNotNull null
            event.copy(text = "SPARKLING! $local caught a SPARKLING $species!")
        }
        return raw + receipts
    }

    private fun rebuildEvents(raw: List<Event>, self: String): Rebuilt {
        val events = dedupe(includePersonalReceipts(raw, self))
        val tickets = events.filter { !it.eof && isTicket(it.text) }
        val admissions = events.filter { !it.eof && admission.matches(it.text) }
        fun lowerBound(list: List<Event>, at: Long): Int {
            var lo = 0; var hi = list.size
            while (lo < hi) { val mid = (lo + hi) ushr 1; if (list[mid].at < at) lo = mid + 1 else hi = mid }
            return lo
        }
        var ticketIndex = 0; var previousTicketIndex = 0
        val sparklings = events.mapNotNull { event ->
            val match = RunTally.sparkling.matchEntire(event.text) ?: return@mapNotNull null
            while (ticketIndex < tickets.size && tickets[ticketIndex].at <= event.at) ticketIndex++
            SparklingEntry(event.at, match.groupValues[1], match.groupValues[2].trim(), tickets = ticketIndex - previousTicketIndex,
                logFile = event.source, logLine = event.ordinal + 1,
                personal = match.groupValues[1].equals(event.localName ?: self, true))
                .also { previousTicketIndex = ticketIndex }
        }
        data class Group(val start: Event, var ticket: Event? = null)
        val groups = events.filter { hotspot.matches(it.text) }.map { Group(it) }.toMutableList()
        val taken = mutableSetOf<Event>()
        for ((index, group) in groups.withIndex()) {
            val nearby = admissions.subList(lowerBound(admissions, group.start.at - 180000), lowerBound(admissions, group.start.at + 5001))
            val match = nearby.filter { it !in taken && it.at > (groups.getOrNull(index - 1)?.start?.at ?: 0) }
                .minByOrNull { kotlin.math.abs(group.start.at - it.at) }
            if (match != null) { group.ticket = match; taken += match }
        }
        val hotspotStarts = groups.map { it.start }
        tickets.filter { it !in taken && lowerBound(hotspotStarts, it.at - 14999) == lowerBound(hotspotStarts, it.at + 15000) }
            .forEach { groups += Group(it, it) }
        groups.sortBy { it.start.at }
        var modeCursor = 0; var observedMode: String? = null
        val runs = groups.mapIndexedNotNull { index, group ->
            val boundary = groups.getOrNull(index + 1)?.start?.at ?: Long.MAX_VALUE
            val within = events.subList(lowerBound(events, group.start.at), lowerBound(events, boundary))
            val summary = within.firstOrNull { it.text == SUMMARY }
            val departure = within.firstOrNull { sending.matches(it.text) }
            val end = summary ?: departure
            // latest.log belongs to the live recorder until an explicit end or another run arrives.
            if (end == null && boundary == Long.MAX_VALUE && within.lastOrNull { it.eof }?.active == true) return@mapIndexedNotNull null
            val limit = end?.at ?: boundary
            val tally = RunTally(); val players = mutableSetOf<String>(); val splits = linkedMapOf<String, Long>()
            while (modeCursor < events.size && events[modeCursor].at <= limit) {
                mode(events[modeCursor++].text)?.let { observedMode = it }
            }
            var inlineFeathers = 0
            within.filter { !it.eof && it.at <= limit }.forEach { event ->
                val splitMatch = split.matchEntire(event.text)
                val short = shortSplit(event.text)
                if (splitMatch != null || short != null) {
                    val word = short ?: splitMatch!!.groupValues[1].lowercase(Locale.ROOT)
                    val reported = splitMatch?.groupValues?.get(3)?.toDoubleOrNull()?.let { seconds ->
                        ((splitMatch.groupValues[2].toLongOrNull() ?: 0) * 60000 + seconds * 1000).toLong()
                    }
                    splits.putIfAbsent(if (word == "ice") "Icy" else word.replaceFirstChar(Char::uppercaseChar), reported ?: event.at - group.start.at)
                } else {
                    val catch = SafariCatch.parse(event.text)
                    if (catch != null) {
                        val local = event.localName ?: self
                        if (local.isNotBlank()) players += local.lowercase(Locale.ROOT)
                        catch.catcherName?.let { players += it.lowercase(Locale.ROOT) }
                    }
                    if (event.text == STASH_FEATHER) {
                        if (inlineFeathers > 0) inlineFeathers-- else tally.feathers++
                    } else {
                        val before = tally.feathers
                        tally.line(event.text, event.localName ?: self, event.at)
                        inlineFeathers += tally.feathers - before
                    }
                }
            }
            if (summary != null) within.filter { !it.eof && it.at >= summary.at && it.at <= summary.at + 15000 &&
                (departure == null || it.at <= departure.at) }.forEach { tally.summaryLine(it.text) }
            RunRecord(start = group.start.at, length = end?.let { (it.at - group.start.at).coerceAtLeast(0) } ?: 0,
                mode = observedMode ?: "SPARKLING", party = tally.party.size, you = tally.catches.size, caught = tally.caught,
                lootShares = tally.lootShares, partySize = players.size, tickets = if (group.ticket?.let { isTicket(it.text) } == true) 1 else 0,
                shards = LinkedHashMap(tally.shards), catches = LinkedHashMap(tally.catches), feathers = tally.feathers,
                sparklings = tally.sparklings.toMutableList(), ownSparklings = tally.ownSparklings, macaws = tally.macaws,
                splits = splits, imported = true, coins = 0, ticketAt = group.ticket?.at ?: 0, coinsEstimated = true, modeEstimated = observedMode == null,
                safariEssence = tally.safariEssence, summaryShards = tally.summaryShards, huntingXp = tally.huntingXp, allTypesMillis = tally.allTypesAt?.let { (it - group.start.at).coerceAtLeast(0) } ?: -1,
                hotspot = Regex("^HOTSPOT! Your Hunting Hotspot is the (.+) Biome!$").matchEntire(group.start.text)?.groupValues?.get(1).orEmpty(),
                importSource = group.start.source, rewardSummarySeen = tally.rewardSummarySeen,
                captureOffsetsMillis = CaptureProgress.offsets(tally.captureTimes, group.start.at))
        }
        return Rebuilt(runs, sparklings, tickets.map { it.at })
    }
}
