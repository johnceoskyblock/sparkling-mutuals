package net.johnceo.sparklingmutuals.alerts

import java.util.Properties

/** Saved names only; editing this list never makes a network request. */
object WarpList {
    val critters = listOf("Tepid", "Strongarm", "Polaris", "Shuddersquid", "Troodon", "Mantis Shrimp",
        "Nozzlenose", "Billygoat", "Areita", "Solsnatcher", "Litterbug", "Bloodbat", "Duplico",
        "Hideonwall", "Hideyho", "Foxtrot", "Treefrog", "Woodchucker", "Hideonfloor", "Fluffling",
        "Shyworm", "Flitter", "Cavernfish", "Driftling", "Scrappy", "Chuckwalla")
    private val entries = linkedMapOf<String, List<String>>()
    fun names(critter: String) = entries[critter].orEmpty()
    fun parse(text: String): List<String> = text.split(Regex("[\\s,;]+"))
        .filter { Regex("[A-Za-z0-9_]{1,16}").matches(it) }.distinctBy(String::lowercase)
    fun set(critter: String, text: String) { if (critter in critters) entries[critter] = parse(text) }
    private fun key(critter: String) = "warpList." + critter.replace(' ', '_')
    fun load(properties: Properties) {
        entries.clear()
        critters.forEach { set(it, properties.getProperty(key(it), "")) }
    }
    fun save(properties: Properties) { critters.forEach { properties.setProperty(key(it), names(it).joinToString(" ")) } }
}

/** One invitation at a time, bounded by the existing warp deadline. */
class WarpInvitations {
    private val queue = ArrayDeque<String>()
    private val seen = mutableSetOf<String>()
    private val critters = mutableSetOf<String>()
    private var nextAt = 0L
    var deadline = 0L
        private set
    fun start(deadline: Long) { stop(); this.deadline = deadline }
    fun add(critter: String, names: List<String>) {
        if (deadline == 0L || !critters.add(critter)) return
        names.filter { seen.add(it.lowercase()) }.forEach(queue::addLast)
    }
    fun next(now: Long, members: Set<String>): String? {
        if (now >= deadline) { stop(); return null }
        if (now < nextAt) return null
        val present = members.map(String::lowercase).toSet()
        while (queue.isNotEmpty()) {
            val name = queue.removeFirst()
            if (name.lowercase() in present) continue
            nextAt = now + 2000
            return name
        }
        return null
    }
    fun stop() { queue.clear(); seen.clear(); critters.clear(); deadline = 0; nextAt = 0 }
    fun discardPending() { queue.clear() }
}
