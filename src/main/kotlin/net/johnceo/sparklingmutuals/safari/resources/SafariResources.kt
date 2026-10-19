package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRules

data class StartingResourceReminder(val type: FloorDropType, val quantity: Int, val missing: List<String>) {
    val destination get() = if (type == FloorDropType.COIN) SafariBiome.HAUNTED else SafariBiome.FOREST
    val title get() = if (type == FloorDropType.COIN) "USE YOUR SHINING COIN!" else "USE YOUR BIRD FOOD!"
    val detail get() = "${quantity}x ${type.label} -> ${destination.label}; party needs Sparkling ${missing.joinToString(", ")}."
    companion object {
        private val body = Regex("^\\[SM] Unused starting item: ([0-9]+)x (.+) -> (Forest|Haunted)(?: \\([^)]*\\))?$")
        private val item = Regex("^([0-9]+)x (.+)$")
        fun parse(text: String, party: PartySparklingState): StartingResourceReminder? {
            val compact = text.trim().startsWith("[SM] Use: ")
            val match = if (compact) item.matchEntire(text.trim().removePrefix("[SM] Use: "))
                else body.matchEntire(text.trim())
            if (match == null) return null
            val type = FloorDropType.entries.firstOrNull { it.label == match.groupValues[2] } ?: return null
            val species = when (type) {
                FloorDropType.COIN -> listOf("Gimmiegold")
                FloorDropType.WORM, FloorDropType.BERRY, FloorDropType.SEEDS -> listOf("Bluebird", "Parakeet", "Macaw")
                else -> return null
            }
            val quantity = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..64 } ?: return null
            return StartingResourceReminder(type, quantity, species.filter(party::needs))
                .takeIf { compact || it.destination.label == match.groupValues[3] }
        }
    }
}

/** One party message and one local title for every eligible item in the current inventory. */
data class StartingResourceNotice(val items: List<StartingResourceReminder>) {
    val text get() = "[SM] Unused floor drops: " + items.groupBy { it.destination }.values.joinToString("; ") { group ->
        val drops = group.joinToString(" + ") { "${it.quantity}x ${it.type.label}" }
        val missing = group.flatMap { it.missing }.distinct().joinToString(", ")
        "$drops (missing Sparkling $missing)"
    }
    val title get() = if (items.map { it.destination }.distinct().size == 1) items.first().title else "USE YOUR STARTING ITEMS!"
    val detail get() = items.joinToString("; ") { "${it.quantity}x ${it.type.label} -> ${it.destination.label}" }
    companion object {
        fun parse(text: String, party: PartySparklingState): StartingResourceNotice? {
            val trimmed = text.trim()
            val parts = when {
                trimmed.startsWith("[SM] Unused floor drops: ") -> {
                    trimmed.removePrefix("[SM] Unused floor drops: ").split("; ").flatMap { group ->
                        val match = Regex("""^(.+) \(missing Sparkling ([^()]+)\)$""").matchEntire(group) ?: return null
                        val missing = match.groupValues[2].split(", ")
                        val drops = match.groupValues[1].split(" + ")
                        // Displayed discoveries do not replace the receiver's own party requirements.
                        drops.map { drop ->
                            val reminder = StartingResourceReminder.parse("[SM] Use: $drop", party) ?: return null
                            val allowed = if (reminder.type == FloorDropType.COIN) setOf("Gimmiegold")
                                else setOf("Bluebird", "Parakeet", "Macaw")
                            if (missing.any { it !in allowed }) return null
                            "[SM] Use: $drop"
                        }
                    }
                }
                trimmed.startsWith("[SM] Use: ") -> trimmed.removePrefix("[SM] Use: ").split(" + ").map { "[SM] Use: $it" }
                else -> listOf(trimmed)
            }
            val items = parts.map { StartingResourceReminder.parse(it, party) ?: return null }
            if (items.map { it.type }.distinct().size != items.size) return null
            return StartingResourceNotice(items)
        }
    }
}

/** Grants overlap inventory; pickups add resources. Neither is a catch or a spawn. */
class SafariResources {
    private val starting = mutableMapOf<FloorDropType, Int>()
    private val pickups = mutableMapOf<FloorDropType, Int>()
    private val held = mutableMapOf<FloorDropType, Int>()
    private val notified = mutableMapOf<FloorDropType, Int>()
    private data class Outstanding(val amount: Int, val population: Int, val events: Int, val credits: Int)
    private val outstanding = linkedMapOf<Pair<String, FloorDropType>, Outstanding>()
    private val resolved = mutableSetOf<Pair<String, FloorDropType>>()
    private val reportedCoins = mutableMapOf<String, Int>()
    private val reportedFood = mutableMapOf<Pair<String, FloorDropType>, Int>()
    fun foodContributions(): Map<Pair<String, FloorDropType>, Int> = reportedFood.toMap()
    fun coinContributions(): Map<String, Int> = reportedCoins.toMap()
    private val populations = mutableMapOf<SafariBiome, Int>()
    private val captured = mutableMapOf<String, Int>()
    private val required = mutableMapOf<SafariBiome, Int>()
    private val credits = mutableMapOf<SafariBiome, Int>()
    private val events = mutableMapOf<SafariBiome, Int>()
    fun requiredPopulation(biome: SafariBiome) = required[biome] ?: 0
    fun capture(species: String) {
        // Bird evidence is supplied by the source scan combining shared catches and current birds.
        if (species != "Gimmiegold") return
        captured.merge(species, 1, Int::plus)
        val biome = SafariBiome.HAUNTED
        val total = captured[species] ?: 0
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
        val notice = StartingResourceNotice.parse(text, party) ?: return false
        return notice.items.any { it.missing.isNotEmpty() && it.destination != biome && (held[it.type] ?: 0) > 0 }
    }
    fun notices(player: String, biome: SafariBiome, party: PartySparklingState): List<String> = starting.mapNotNull { (type, amount) ->
        val destination = destination(type) ?: return@mapNotNull null
        val needed = if (destination == SafariBiome.HAUNTED) party.needs("Gimmiegold")
            else listOf("Bluebird", "Parakeet", "Macaw").any(party::needs)
        val remaining = minOf(amount, held[type] ?: 0)
        if (!needed || destination == biome || remaining <= (notified[type] ?: 0)) return@mapNotNull null
        "[SM] Use: ${remaining}x ${type.label}"
    }
    /** Called only after local feedback and party chat were actually dispatched. */
    fun noticeSent(player: String, text: String) {
        val reminders = StartingResourceNotice.parse(text, PartySparklingState()) ?: return
        reminders.items.forEach { reminder ->
            notified[reminder.type] = maxOf(notified[reminder.type] ?: 0, reminder.quantity)
            notice(player, reminder.type, reminder.quantity)
        }
    }
    fun partyNotice(raw: String): Boolean {
        val chat = PartyChat.parse(SafariRules.strip(raw)) ?: return false
        val reminders = StartingResourceNotice.parse(chat.body, PartySparklingState()) ?: return false
        reminders.items.forEach { notice(chat.sender, it.type, it.quantity) }
        return true
    }
    private fun notice(player: String, type: FloorDropType, amount: Int) {
        val biome = destination(type) ?: return
        val key = player.lowercase() to type
        if (type == FloorDropType.COIN) reportedCoins[key.first] = maxOf(reportedCoins[key.first] ?: 0, amount)
        else reportedFood[key] = maxOf(reportedFood[key] ?: 0, amount)
        if (key in resolved || key in outstanding) return
        // Reserve partially observed evidence belonging to earlier notices, even
        // before their full quantities resolve. Later allocation cannot subtract it twice.
        val creditBaseline = maxOf(credits[biome] ?: 0, outstanding.filterKeys { destination(it.second) == biome }
            .values.maxOfOrNull { previous -> previous.credits + maxOf(0,
                (populations[biome] ?: 0) - previous.population, (events[biome] ?: 0) - previous.events) } ?: 0)
        // A first visit must not use the biome's ordinary baseline as the extra spawn.
        val populationBaseline = maxOf(populations[biome] ?: 0, if (biome == SafariBiome.FOREST) 9 else 3)
        outstanding[key] = Outstanding(amount, populationBaseline, events[biome] ?: 0, creditBaseline)
        required[biome] = maxOf(required[biome] ?: 0, populationBaseline) + amount
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
            val added = maxOf((populations[biome] ?: 0) - value.population, (events[biome] ?: 0) - value.events) - ((credits[biome] ?: 0) - value.credits).coerceAtLeast(0)
            if (added >= value.amount) { credits.merge(biome, value.amount, Int::plus); resolved.add(key); iterator.remove() }
        }
    }
}
