package net.johnceo.sparklingmutuals.history.importing

import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import net.johnceo.sparklingmutuals.history.RunTally
import net.johnceo.sparklingmutuals.safari.SafariCatch

internal object HistoryArchives {
    class Input(val path: Path, val digest: String, val archive: HistoryBackfill.Archive)
    data class Selection(
        val sessions: List<List<HistoryBackfill.Event>>, val foreign: List<Input>, val unresolved: List<Input>,
        val unknownServers: Set<String>
    )
    private val Input.day get() = Instant.ofEpochMilli(archive.first).atZone(ZoneId.systemDefault()).toLocalDate()
    private val Input.copyKey get() = digest to archive.first

    /** A named server broadcast and a first-person shiny capture of the same species in the same second. */
    private fun personalNames(events: List<HistoryBackfill.Event>): Set<String> {
        val receipts = events.asSequence().filter { it.text.startsWith("CAPTURE!") }.mapNotNull { event ->
            SafariCatch.parse(event.text)?.takeIf { it.personal && it.sparkling }?.let { Triple(event.at, event.realm, it.critter.name.lowercase(Locale.ROOT)) }
        }.groupingBy { it }.eachCount()
        if (receipts.isEmpty()) return emptySet()
        return events.mapNotNull { event ->
            RunTally.sparkling.matchEntire(event.text)?.let {
                Triple(event.at, event.realm, it.groupValues[2].lowercase(Locale.ROOT)) to it.groupValues[1]
            }
        }.groupBy({ it.first }, { it.second.lowercase(Locale.ROOT) })
            .mapNotNull { (key, names) -> names.distinct().singleOrNull()?.takeIf { receipts[key] == 1 } }.toSet()
    }

    fun select(inputs: List<Input>, self: String, uuid: String?, knownNames: Map<String, Set<String>>, strictRealm: Boolean): Selection {
        val identityNames = mutableMapOf<String, MutableSet<String>>()
        knownNames.forEach { (id, names) -> identityNames.getOrPut(id) { mutableSetOf() }.addAll(names) }
        if (uuid != null) identityNames.getOrPut(uuid) { mutableSetOf() } += self
        for (input in inputs) input.archive.identities.forEach { (id, names) -> identityNames.getOrPut(id) { mutableSetOf() }.addAll(names) }
        val idsByName = mutableMapOf<String, MutableSet<String>>()
        identityNames.forEach { (id, names) -> names.forEach { idsByName.getOrPut(it.lowercase(Locale.ROOT)) { mutableSetOf() } += id } }
        val byCopy = inputs.groupBy { it.copyKey }
        val byEndDay = inputs.filter { it.archive.last > 0 && !it.archive.stopped }
            .groupBy { it.path.parent to Instant.ofEpochMilli(it.archive.last).atZone(ZoneId.systemDefault()).toLocalDate() }
        val predecessor = mutableMapOf<Input, Input>()
        for (input in inputs) {
            if (input.archive.first == 0L || input.archive.started) continue
            val start = Instant.ofEpochMilli(input.archive.first).atZone(ZoneId.systemDefault()).toLocalTime()
            if (start.toSecondOfDay() > 120) continue
            val candidates = byCopy.getValue(input.copyKey).flatMap { copy ->
                byEndDay[copy.path.parent to input.day.minusDays(1)].orEmpty()
                    .filter { input.archive.first - it.archive.last in 1..120_000 }
            }.distinctBy { it.copyKey }
            candidates.singleOrNull()?.takeIf { previous ->
                val left = previous.archive.identities.keys
                val right = input.archive.identities.keys
                left.isEmpty() || right.isEmpty() || left == right
            }?.let { predecessor[input] = it }
        }
        fun root(input: Input): Input = predecessor[input]?.let(::root) ?: input
        val foreign = mutableListOf<Input>(); val unresolved = mutableListOf<Input>()
        val sessions = mutableListOf<List<HistoryBackfill.Event>>()
        val unknownServers = mutableSetOf<String>()
        for (group in inputs.groupBy { root(it).copyKey }.values) {
            val fragments = group.distinctBy { it.copyKey }.sortedBy { it.archive.first }
            val archives = fragments.map { it.archive }
            if (archives.none { archive -> archive.events.any { !it.boundary && !it.eof } }) continue
            val ids = archives.flatMap { it.identities.keys }.toSet()
            val names = archives.flatMap { it.names + it.identities.values.flatten() }
                .ifEmpty { personalNames(archives.flatMap { it.events }).toList() }
                .map { it.lowercase(Locale.ROOT) }.toSet()
            val verdict = when {
                ids.isNotEmpty() && uuid != null -> if (ids.size != 1) null else ids.single() == uuid
                names.isEmpty() -> null
                uuid == null -> names.all { it.equals(self, true) }
                names.all { idsByName[it] == setOf(uuid) } -> true
                names.all { name -> idsByName[name]?.let { it.isNotEmpty() && uuid !in it } == true } -> false
                else -> null
            }
            if (verdict == null) { unresolved += fragments; continue }
            if (!verdict) { foreign += group; continue }
            var carry = HistoryBackfill.Realm.UNKNOWN
            val complete = mutableListOf<HistoryBackfill.Event>()
            for ((index, archive) in archives.withIndex()) {
                var inherited = carry
                for (event in archive.events) {
                    val classified = if (event.realm == HistoryBackfill.Realm.UNKNOWN) event.copy(realm = inherited) else event
                    if (!event.eof || index == archives.lastIndex) complete += classified
                    if (event.boundary || event.realm != HistoryBackfill.Realm.UNKNOWN) inherited = HistoryBackfill.Realm.UNKNOWN
                    if (event.eof) carry = classified.realm
                }
            }
            if (strictRealm) complete.asSequence()
                .filter { it.realm == HistoryBackfill.Realm.UNKNOWN && !it.eof && !it.boundary }
                .mapTo(unknownServers) { it.source }
            val accepted = if (strictRealm) complete.filter { it.realm != HistoryBackfill.Realm.UNKNOWN } else complete
            val legacyName = names.singleOrNull()?.let { name ->
                archives.asSequence().flatMap { (it.names + it.identities.values.flatten()).asSequence() }
                    .firstOrNull { it.equals(name, true) } ?: if (self.equals(name, true)) self else name
            } ?: self
            sessions += accepted.map { event -> event.copy(localName = legacyName) }
        }
        return Selection(sessions, foreign, unresolved, unknownServers)
    }
}
