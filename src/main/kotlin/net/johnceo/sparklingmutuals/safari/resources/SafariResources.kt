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

/** Combined local title; text parsing/serialization remains for inbound legacy compatibility. */
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
    val contributions = StartingResourceLedger()
    private val starting = mutableMapOf<FloorDropType, Int>()
    private val pickups = mutableMapOf<FloorDropType, Int>()
    private val held = mutableMapOf<FloorDropType, Int>()
    private val notified = mutableMapOf<FloorDropType, Int>()
    private data class Outstanding(val amount: Int, val population: Int, val events: Int, val credits: Int)
    private val outstanding = linkedMapOf<Pair<String, FloorDropType>, Outstanding>()
    private val resolved = mutableSetOf<Pair<String, FloorDropType>>()
    fun foodContributions(): Map<Pair<String, FloorDropType>, Int> = contributions.food()
    fun coinContributions(): Map<String, Int> = contributions.coins()
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
    fun startingTotal() = starting.values.sum()
    fun plausibleStartingInventory(stacks: List<Pair<String, Int>>): Boolean = FloorDropType.entries
        .filter { it != FloorDropType.GENERIC }.sumOf { type ->
            (stacks.filter { SafariRules.strip(it.first).trim() == type.label }.sumOf { it.second.coerceAtLeast(0) } -
                (pickups[type] ?: 0)).coerceAtLeast(0)
        } <= 2
    fun startingQuantities() = StartingItemReport.order.mapNotNull { type ->
        startingCount(type).takeIf { it > 0 }?.let { type to it }
    }.toMap()
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
    /** Presentation only. A title cannot create starting contributions or spawning obligations. */
    fun noticeSent(player: String, text: String) {
        val reminders = StartingResourceNotice.parse(text, PartySparklingState()) ?: return
        reminders.items.forEach { reminder ->
            notified[reminder.type] = maxOf(notified[reminder.type] ?: 0, reminder.quantity)
        }
    }
    fun partyNotice(raw: String): Boolean {
        val chat = PartyChat.parse(SafariRules.strip(raw)) ?: return false
        return reportedBy(chat.sender, chat.body)
    }
    /** Pure admission boundary: the runtime caller already verified UUID membership and run scope. */
    fun reportedBy(sender: String, body: String): Boolean {
        val early = StartingItemReport.parse(body)
        if (early != null) {
            contributions.report(sender, early.quantities, complete = true)
            return true
        }
        val reminders = StartingResourceNotice.parse(body, PartySparklingState()) ?: return false
        reminders.items.forEach { notice(sender, it.type, it.quantity) }
        return true
    }
    private fun notice(player: String, type: FloorDropType, amount: Int) {
        val biome = destination(type) ?: return
        val key = player.lowercase() to type
        val alreadyKnown = if (type == FloorDropType.COIN) coinContributions()[key.first] ?: 0 else foodContributions()[key] ?: 0
        contributions.report(player, mapOf(type to amount), complete = false)
        if (alreadyKnown >= amount) return
        if (key in resolved || key in outstanding) return
        // Reserve partially observed evidence belonging to earlier notices, even
        // before their full quantities resolve. Later allocation cannot subtract it twice.
        val creditBaseline = maxOf(credits[biome] ?: 0, outstanding.filterKeys { destination(it.second) == biome }
            .values.maxOfOrNull { previous -> previous.credits + maxOf(0,
                (populations[biome] ?: 0) - previous.population, (events[biome] ?: 0) - previous.events) } ?: 0)
        // A first visit must not use the biome's ordinary baseline as the extra spawn.
        val populationBaseline = if (biome == SafariBiome.FOREST) 9 else 3
        outstanding[key] = Outstanding(amount, populationBaseline, events[biome] ?: 0, creditBaseline)
        required[biome] = populationBaseline + if (biome == SafariBiome.FOREST) contributions.foodTotal else contributions.total(FloorDropType.COIN)
    }
    fun pending(biome: SafariBiome) = outstanding.keys.any { destination(it.second) == biome }
    /** The qualified coin owner has accounted for every verified floor and starting opportunity.
     * Its F+S proof supersedes legacy fixed-baseline bookkeeping, without inventing catches. */
    fun verifiedCoinPopulation(requiredPopulation: Int) {
        required[SafariBiome.HAUNTED] = requiredPopulation
        val iterator = outstanding.iterator()
        while (iterator.hasNext()) {
            val (key, value) = iterator.next()
            if (key.second != FloorDropType.COIN) continue
            credits.merge(SafariBiome.HAUNTED, value.amount, Int::plus)
            resolved.add(key)
            iterator.remove()
        }
    }
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
