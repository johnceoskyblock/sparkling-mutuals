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
    data class InviteSettings(val priority: Int = 10, val alwaysInvite: Boolean = false)
    private val settings = mutableMapOf<String, InviteSettings>()
    private val critterPriorities = mutableMapOf<Pair<String, String>, Int>()
    fun settings(name: String) = settings[name.lowercase()] ?: InviteSettings()
    fun priority(name: String, critter: String) = critterPriorities[critter to name.lowercase()] ?: settings(name).priority
    fun setPriority(name: String, critter: String, priority: Int) {
        if (critter in critters && players.any { it.equals(name, true) })
            critterPriorities[critter to name.lowercase()] = priority.coerceIn(1, 10)
    }
    fun configure(name: String, value: InviteSettings) {
        if (players.any { it.equals(name, true) }) settings[name.lowercase()] = value.copy(priority = value.priority.coerceIn(1, 10))
    }
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
        settings.remove(name.lowercase())
        critterPriorities.keys.removeIf { it.second == name.lowercase() }
        players.removeAll { it.equals(name,true) }
        critters.forEach { entries[it]=names(it).filterNot { player -> player.equals(name,true) } }
    }
    fun append(values: Map<String, List<WarpListCsv.Entry>>) {
        critters.forEach { critter ->
            values[critter].orEmpty().forEach { (name, priority) ->
                if (!selected(name, critter)) select(name, critter, true)
                if (selected(name, critter)) critterPriorities[critter to name.lowercase()] = priority.coerceIn(1, 10)
            }
        }
    }
    private fun key(critter: String) = "warpList." + critter.replace(' ', '_')
    fun load(properties: Properties) {
        entries.clear();players.clear();settings.clear();critterPriorities.clear()
        parse(properties.getProperty("warpList.players", "")).forEach(::addPlayer)
        critters.forEach { set(it, properties.getProperty(key(it), "")) }
        critters.forEach { critter ->
            players.forEach { name ->
                val normalized = name.lowercase()
                properties.getProperty("${key(critter)}.priority.$normalized")?.toIntOrNull()?.takeIf { it in 1..10 }
                    ?.let { critterPriorities[critter to normalized] = it }
            }
        }
        players.forEach { name ->
            val prefix = "warpList.player.${name.lowercase()}"
            configure(name, InviteSettings(properties.getProperty("$prefix.priority")?.toIntOrNull() ?: 10,
                properties.getProperty("$prefix.alwaysInvite").toBoolean()))
        }
    }
    fun save(properties: Properties) {
        properties.setProperty("warpList.players", players.joinToString(" "))
        critters.forEach { properties.setProperty(key(it), names(it).joinToString(" ")) }
        critterPriorities.forEach { (entry, priority) ->
            properties.setProperty("${key(entry.first)}.priority.${entry.second}", priority.toString())
        }
        settings.forEach { (name, value) ->
            properties.setProperty("warpList.player.$name.priority", value.priority.toString())
            properties.setProperty("warpList.player.$name.alwaysInvite", value.alwaysInvite.toString())
        }
    }
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
    fun next(now: Long, members: Set<String>, eligible: (String) -> Boolean = { true }): String? {
        if (now >= deadline) { stop(); return null }
        if (now < nextAt) return null
        val present = members.map(String::lowercase).toSet()
        val candidates = queue.iterator()
        while (candidates.hasNext()) {
            val name = candidates.next()
            if (name.lowercase() in present) {
                candidates.remove()
                continue
            }
            if (!eligible(name)) continue
            candidates.remove()
            nextAt = now + 2000
            return name
        }
        return null
    }
    fun rejectedOffline(now: Long) {
        if (deadline != 0L && now < deadline && now < nextAt) nextAt = now
    }
    fun stop() { queue.clear(); seen.clear(); critters.clear(); deadline = 0; nextAt = 0 }
    fun discardPending() { queue.clear() }
}
