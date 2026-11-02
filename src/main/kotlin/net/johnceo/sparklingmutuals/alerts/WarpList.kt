package net.johnceo.sparklingmutuals.alerts

import java.util.Properties

/** Saved names only; editing this list never makes a network request. */
object WarpList {
    val critters = listOf("Tepid", "Strongarm", "Polaris", "Shuddersquid", "Troodon", "Mantis Shrimp",
        "Nozzlenose", "Billygoat", "Areita", "Solsnatcher", "Litterbug", "Bloodbat", "Duplico",
        "Hideonwall", "Hideyho", "Foxtrot", "Treefrog", "Woodchucker", "Hideonfloor", "Fluffling",
        "Shyworm", "Flitter", "Cavernfish", "Driftling", "Scrappy", "Chuckwalla")
    private val players = mutableListOf<String>()
    private val entries = linkedMapOf<String, List<String>>()
    fun names(critter: String) = entries[critter].orEmpty()
    fun parse(text: String): List<String> = text.split(Regex("[\\s,;]+"))
        .filter { Regex("[A-Za-z0-9_]{1,16}").matches(it) }.distinctBy(String::lowercase)
    fun set(critter: String, text: String) {
        if(critter !in critters) return
        entries[critter]=parse(text)
        entries.getValue(critter).forEach(::addPlayer)
    }
    fun players(): List<String> = players.toList()
    fun addPlayer(name: String): Boolean {
        if(!Regex("[A-Za-z0-9_]{1,16}").matches(name) || players.any { it.equals(name,true) }) return false
        players.add(name);return true
    }
    fun selected(name: String,critter: String)=names(critter).any { it.equals(name,true) }
    fun select(name: String,critter: String,on: Boolean) {
        if(critter !in critters) return
        addPlayer(name)
        val canonical=players.firstOrNull { it.equals(name,true) }?:return
        val current=names(critter).filterNot { it.equals(canonical,true) }
        entries[critter]=if(on) current+canonical else current
    }
    fun removePlayer(name: String) {
        players.removeAll { it.equals(name,true) }
        critters.forEach { entries[it]=names(it).filterNot { player -> player.equals(name,true) } }
    }
    fun replace(values: Map<String,List<String>>) {
        entries.clear();players.clear()
        critters.forEach { set(it,values[it].orEmpty().joinToString(" ")) }
    }
    private fun key(critter: String) = "warpList." + critter.replace(' ', '_')
    fun load(properties: Properties) {
        entries.clear();players.clear()
        parse(properties.getProperty("warpList.players", "")).forEach(::addPlayer)
        critters.forEach { set(it, properties.getProperty(key(it), "")) }
    }
    fun save(properties: Properties) { properties.setProperty("warpList.players",players.joinToString(" "));critters.forEach { properties.setProperty(key(it), names(it).joinToString(" ")) } }
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
