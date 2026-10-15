package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.commands.PartyChat

data class StartingResourceReminder(val type: FloorDropType, val quantity: Int, val missing: List<String>) {
    val destination get() = if (type == FloorDropType.COIN) SafariBiome.HAUNTED else SafariBiome.FOREST
    val title get() = if (type == FloorDropType.COIN) "USE YOUR SHINING COIN!" else "USE YOUR BIRD FOOD!"
    val detail get() = "${quantity}x ${type.label} -> ${destination.label}; party needs Sparkling ${missing.joinToString(", ")}."
    companion object {
        private val body = Regex("^\\[SM] Unused starting item: ([0-9]+)x (.+) -> (Forest|Haunted)(?: \\([^)]*\\))?$")
        fun parse(text: String, party: PartySparklingState): StartingResourceReminder? {
            val match = body.matchEntire(text.trim()) ?: return null
            val type = FloorDropType.entries.firstOrNull { it.label == match.groupValues[2] } ?: return null
            val species = when (type) {
                FloorDropType.COIN -> listOf("Gimmiegold")
                FloorDropType.WORM, FloorDropType.BERRY, FloorDropType.SEEDS -> listOf("Bluebird", "Parakeet", "Macaw")
                else -> return null
            }
            val quantity = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..64 } ?: return null
            return StartingResourceReminder(type, quantity, species.filter(party::needs))
                .takeIf { it.destination.label == match.groupValues[3] }
        }
    }
}

/** Grants overlap inventory; pickups add resources. Neither is a catch or a spawn. */
class SafariResources {
    private val starting = mutableMapOf<FloorDropType, Int>()
    private val pickups = mutableMapOf<FloorDropType, Int>()
    private val held = mutableMapOf<FloorDropType, Int>()
    private val notified = mutableSetOf<FloorDropType>()
    private data class Outstanding(val amount: Int, val population: Int, val events: Int, val credits: Int)
    private val outstanding = linkedMapOf<Pair<String, FloorDropType>, Outstanding>()
    private val resolved = mutableSetOf<Pair<String, FloorDropType>>()
    private val populations = mutableMapOf<SafariBiome, Int>()
    private val captured = mutableMapOf<String, Int>()
    private val required = mutableMapOf<SafariBiome, Int>()
    private val credits = mutableMapOf<SafariBiome, Int>()
    private val events = mutableMapOf<SafariBiome, Int>()
    fun requiredPopulation(biome: SafariBiome) = required[biome] ?: 0
    fun capture(species: String) {
        if (species !in setOf("Bluebird", "Parakeet", "Macaw", "Gimmiegold")) return
        captured.merge(species, 1, Int::plus)
        val biome = if (species == "Gimmiegold") SafariBiome.HAUNTED else SafariBiome.FOREST
        val total = if (biome == SafariBiome.HAUNTED) captured[species] ?: 0 else
            (captured["Bluebird"] ?: 0) + (captured["Parakeet"] ?: 0) + (captured["Macaw"] ?: 0) / 2
        populations[biome] = maxOf(populations[biome] ?: 0, total)
        reconcile(biome)
    }
    fun inventory(stacks: List<Pair<String, Int>>, initial: Boolean) {
        FloorDropType.entries.forEach { type ->
            held[type] = stacks.filter { SafariRules.strip(it.first).trim() == type.label }.sumOf { it.second.coerceAtLeast(0) }
            if (initial) starting[type] = maxOf(starting[type] ?: 0, held.getValue(type) - (pickups[type] ?: 0))
        }
    }
    fun pickup(text: String) { FloorPickup.parse(text)?.let { pickups.merge(it.type, it.quantity, Int::plus) } }
    fun startingCount(type: FloorDropType) = starting[type] ?: 0
    fun headStart(text: String) {
        val name = Regex("^HEAD START! You started with a (.+)!$").matchEntire(text)?.groupValues?.get(1) ?: return
        FloorDropType.entries.firstOrNull { it.label == name }?.let { starting[it] = maxOf(starting[it] ?: 0, 1) }
    }
    private fun destination(type: FloorDropType) = when (type) {
        FloorDropType.COIN -> SafariBiome.HAUNTED
        FloorDropType.WORM, FloorDropType.BERRY, FloorDropType.SEEDS -> SafariBiome.FOREST
        else -> null
    }
    fun noticeStillRelevant(text: String, biome: SafariBiome, party: PartySparklingState): Boolean {
        val type = FloorDropType.entries.firstOrNull { text.contains("x ${it.label} ->") } ?: return false
        val area = destination(type) ?: return false
        val needed = if (area == SafariBiome.HAUNTED) party.needs("Gimmiegold")
            else listOf("Bluebird", "Parakeet", "Macaw").any(party::needs)
        return needed && area != biome && (held[type] ?: 0) > 0
    }
    fun notices(player: String, biome: SafariBiome, party: PartySparklingState): List<String> = starting.mapNotNull { (type, amount) ->
        val destination = destination(type) ?: return@mapNotNull null
        val needed = if (destination == SafariBiome.HAUNTED) party.needs("Gimmiegold")
            else listOf("Bluebird", "Parakeet", "Macaw").any(party::needs)
        val remaining = minOf(amount, held[type] ?: 0)
        if (!needed || destination == biome || remaining <= 0 || !notified.add(type)) return@mapNotNull null
        notice(player, type, remaining)
        val discoveries = if (destination == SafariBiome.HAUNTED) listOf("Gimmiegold")
            else listOf("Bluebird", "Parakeet", "Macaw").filter(party::needs)
        "[SM] Unused starting item: ${remaining}x ${type.label} -> ${destination.label} ($player; party needs Sparkling ${discoveries.joinToString(", ")})"
    }
    fun partyNotice(raw: String): Boolean {
        val chat = PartyChat.parse(SafariRules.strip(raw)) ?: return false
        val reminder = StartingResourceReminder.parse(chat.body, PartySparklingState()) ?: return false
        notice(chat.sender, reminder.type, reminder.quantity)
        return true
    }
    private fun notice(player: String, type: FloorDropType, amount: Int) {
        val biome = destination(type) ?: return
        val key = player.lowercase() to type
        if (key in resolved || key in outstanding) return
        outstanding[key] = Outstanding(amount, populations[biome] ?: 0, events[biome] ?: 0, credits[biome] ?: 0)
        required[biome] = maxOf(required[biome] ?: 0, populations[biome] ?: 0) + amount
    }
    fun pending(biome: SafariBiome) = outstanding.keys.any { destination(it.second) == biome }
    fun spawn(biome: SafariBiome) { events.merge(biome, 1, Int::plus); reconcile(biome) }
    fun scan(biome: SafariBiome, population: Int) { populations[biome] = population; reconcile(biome) }
    private fun reconcile(biome: SafariBiome) {
        val iterator = outstanding.iterator()
        while (iterator.hasNext()) {
            val (key, value) = iterator.next()
            if (destination(key.second) != biome) continue
            // Food population is measured in feeding equivalents (a Macaw pair is one).
            val added = maxOf((populations[biome] ?: 0) - value.population, (events[biome] ?: 0) - value.events) - ((credits[biome] ?: 0) - value.credits)
            if (added >= value.amount) { credits.merge(biome, value.amount, Int::plus); resolved.add(key); iterator.remove() }
        }
    }
}
