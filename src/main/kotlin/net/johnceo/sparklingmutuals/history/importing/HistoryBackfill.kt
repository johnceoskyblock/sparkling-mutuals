package net.johnceo.sparklingmutuals.history.importing

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.time.*
import java.util.Locale
import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.history.RunTally
import net.johnceo.sparklingmutuals.history.SparklingEntry
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariRules

/** Kotlin port of the supplied run/sparkling backfillers; no browser or script execution is involved. */
internal object HistoryBackfill {
    data class Event(val at: Long, val text: String, val source: String, val ordinal: Int, val eof: Boolean = false, val active: Boolean = false, val alpha: Boolean = false, val main: Boolean = false)
    data class Rebuilt(val runs: List<RunRecord>, val sparklings: List<SparklingEntry>, val tickets: List<Long>,
        val alphaRuns: List<RunRecord> = emptyList(), val alphaSparklings: List<SparklingEntry> = emptyList())
    class OwnerMismatch(message: String) : IllegalArgumentException(message)
    private val connectionLine = Regex("^Connecting to ([A-Za-z0-9.-]+), [0-9]+$")
    private val ownerLine = Regex("(?:^|\\s)Setting user: ([A-Za-z0-9_]{1,16})(?:\\s|$)")
    private val ansi = Regex("\u001B\\[[0-9;]*m")
    private val timePrefix = Regex("^\\s*\\[(\\d{1,2}):(\\d{2}):(\\d{2})(?:[.,]\\d+)?]:?")
    private val headers = listOf(timePrefix, Regex("^\\s*\\[[^]\\r\\n]*/(?:INFO|WARN|ERROR|DEBUG|TRACE|FATAL)]:?", RegexOption.IGNORE_CASE),
        Regex("^\\s*\\([^()\\r\\n]{1,100}\\)"), Regex("^\\s*\\[(?:System|CHAT)]:?", RegexOption.IGNORE_CASE), Regex("^\\s*:"))
    private val ticket = Regex("^(?:\\[NPC]\\s*)?Safari Manager:\\s*(?:Looks good to me\\. Have fun out there!|I already saw your ticket, so you['’]re free to go\\.)$", RegexOption.IGNORE_CASE)
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
    fun isTicket(text: String) = ticket.matches(text)
    private fun relevant(text: String): Boolean = isTicket(text) || hotspot.matches(text) || text == SUMMARY || sending.matches(text) ||
        mode(text) != null || split.matches(text) || shortSplit(text) != null || summaryReward.matches(text) || SafariMessages.lines(text).isNotEmpty() &&
        (SafariCatch.parse(text) != null || RunTally.sparkling.matches(text) || text == RunTally.MACAW_BONUS || text == STASH_FEATHER)

    fun log(reader: BufferedReader, day: LocalDate, source: String, active: Boolean, expectedSelf: String? = null): List<Event> {
        val result = mutableListOf<Event>()
        val owners = mutableSetOf<String>()
        var date = day; var previous: LocalTime? = null; var last = 0L; var alpha = false; var main = false
        reader.lineSequence().forEachIndexed { index, raw ->
            if ("[CHAT]" !in raw) ownerLine.find(SafariRules.strip(raw))?.let {
                owners += it.groupValues[1].lowercase(Locale.ROOT)
                if (expectedSelf != null && !it.groupValues[1].equals(expectedSelf, true)) throw OwnerMismatch("Log belongs to another account")
            }
            val time = time(raw) ?: return@forEachIndexed
            if (previous?.let { time.toSecondOfDay() < it.toSecondOfDay() - 3600 } == true) date = date.plusDays(1)
            previous = time
            last = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val text = body(raw)
            // Only client connection metadata or an exact server footer can change server state.
            // A party member mentioning Alpha must never hide legitimate history.
            val host = if ("[CHAT]" !in raw) connectionLine.matchEntire(text)?.groupValues?.get(1) else null
            if (host != null) {
                result += Event(last, "Sending to server connection...", source, index, alpha = alpha, main = main)
                alpha = host.equals("alpha.hypixel.net", true)
                main = host.endsWith(".hypixel.net", true) && !alpha
            }
            else if (text.equals("alpha.hypixel.net", true)) { alpha = true; main = false }
            else if (text.equals("www.hypixel.net", true)) { alpha = false; main = true }
            if (relevant(text)) result += Event(last, text, source, index, alpha = alpha, main = main)
        }
        checkOwner(owners, expectedSelf)
        if (last > 0) result += Event(last, "", source, Int.MAX_VALUE, eof = true, active = active, alpha = alpha, main = main)
        return result
    }
    private fun checkOwner(owners: Set<String>, expectedSelf: String?) {
        if (expectedSelf == null) return
        if (expectedSelf.isBlank() || owners.isEmpty()) throw OwnerMismatch("Log account could not be verified")
        if (owners.any { !it.equals(expectedSelf, true) }) throw OwnerMismatch("Log belongs to another account")
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
    fun json(reader: BufferedReader, day: LocalDate, source: String, expectedSelf: String? = null): List<Event> {
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
        var alpha = rootServer.equals("alpha.hypixel.net", true)
        var main = rootServer?.endsWith(".hypixel.net", true) == true && !alpha
        items.forEachIndexed { index, item ->
            val obj = item.takeIf { it.isJsonObject }?.asJsonObject
            obj?.get("server")?.takeIf { it.isJsonPrimitive }?.asString?.let { alpha = it.equals("alpha.hypixel.net", true); main = it.endsWith(".hypixel.net", true) && !alpha }
            val at = obj?.let { value -> listOf("at", "timestamp", "time", "epochMillis").firstNotNullOfOrNull { epoch(value.get(it)) } }
            val direct = obj?.takeIf { at != null && it.get("player")?.let { field -> field.isJsonPrimitive && field.asJsonPrimitive.isString } == true &&
                it.get("critter")?.let { field -> field.isJsonPrimitive && field.asJsonPrimitive.isString } == true }
            if (direct != null) {
                val player = direct.get("player").asString; val critter = direct.get("critter").asString
                val text = "SPARKLING! $player caught a SPARKLING $critter!"
                if (RunTally.sparkling.matches(text)) result += Event(at!!, text, source, index, alpha = alpha, main = main)
            } else {
                val raw = if (item.isJsonPrimitive && item.asJsonPrimitive.isString) item.asString else
                    obj?.let { value -> listOf("message", "text", "formattedMessage", "content", "line").firstNotNullOfOrNull {
                        value.get(it)?.takeIf { field -> field.isJsonPrimitive && field.asJsonPrimitive.isString }?.asString
                    } } ?: return@forEachIndexed
                val timestamp = at ?: time(raw)?.let { day.atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() } ?: return@forEachIndexed
                ownerLine.find(SafariRules.strip(raw))?.let { owners += it.groupValues[1] }
                val text = body(raw)
                val host = if ("[CHAT]" !in raw) connectionLine.matchEntire(text)?.groupValues?.get(1) else null
                if (host != null) { alpha = host.equals("alpha.hypixel.net", true); main = host.endsWith(".hypixel.net", true) && !alpha }
                else if (text.equals("alpha.hypixel.net", true)) { alpha = true; main = false }
                else if (text.equals("www.hypixel.net", true)) { alpha = false; main = true }
                if (relevant(text)) result += Event(timestamp, text, source, index, alpha = alpha, main = main)
            }
        }
        checkOwner(owners, expectedSelf)
        return result
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
        val main = rebuildEvents(raw.filter { !it.alpha && (it.main || (it.at to it.text) !in alphaIdentities) }, self)
        val alpha = rebuildEvents(raw.filter { it.alpha }, self)
        return main.copy(alphaRuns = alpha.runs, alphaSparklings = alpha.sparklings)
    }
    private fun rebuildEvents(raw: List<Event>, self: String): Rebuilt {
        val events = dedupe(raw)
        val tickets = events.filter { !it.eof && isTicket(it.text) }
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
                logFile = event.source, logLine = event.ordinal + 1)
                .also { previousTicketIndex = ticketIndex }
        }
        data class Group(val start: Event, var ticket: Event? = null)
        val groups = events.filter { hotspot.matches(it.text) }.map { Group(it) }.toMutableList()
        val taken = mutableSetOf<Event>()
        for (group in groups) {
            val nearby = tickets.subList(lowerBound(tickets, group.start.at - 180000), lowerBound(tickets, group.start.at + 5001))
            val match = nearby.filter { it !in taken }.minByOrNull { kotlin.math.abs(group.start.at - it.at) }
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
                        if (self.isNotBlank()) players += self.lowercase(Locale.ROOT)
                        catch.catcherName?.let { players += it.lowercase(Locale.ROOT) }
                    }
                    if (event.text == STASH_FEATHER) {
                        if (inlineFeathers > 0) inlineFeathers-- else tally.feathers++
                    } else {
                        val before = tally.feathers
                        tally.line(event.text, self, event.at)
                        inlineFeathers += tally.feathers - before
                    }
                }
            }
            if (summary != null) within.filter { !it.eof && it.at >= summary.at && it.at <= summary.at + 15000 &&
                (departure == null || it.at <= departure.at) }.forEach { tally.summaryLine(it.text) }
            RunRecord(start = group.start.at, length = end?.let { (it.at - group.start.at).coerceAtLeast(0) } ?: 0,
                mode = observedMode ?: "SPARKLING", party = tally.party.size, you = tally.catches.size, caught = tally.caught,
                lootShares = tally.lootShares, partySize = players.size, tickets = if (group.ticket != null) 1 else 0,
                shards = LinkedHashMap(tally.shards), catches = LinkedHashMap(tally.catches), feathers = tally.feathers,
                sparklings = tally.sparklings.toMutableList(), ownSparklings = tally.ownSparklings, macaws = tally.macaws,
                splits = splits, imported = true, coins = 0, ticketAt = group.ticket?.at ?: 0, coinsEstimated = true, modeEstimated = observedMode == null,
                safariEssence = tally.safariEssence, summaryShards = tally.summaryShards, huntingXp = tally.huntingXp, allTypesMillis = tally.allTypesAt?.let { (it - group.start.at).coerceAtLeast(0) } ?: -1,
                hotspot = Regex("^HOTSPOT! Your Hunting Hotspot is the (.+) Biome!$").matchEntire(group.start.text)?.groupValues?.get(1).orEmpty())
        }
        return Rebuilt(runs, sparklings, tickets.map { it.at })
    }
}
