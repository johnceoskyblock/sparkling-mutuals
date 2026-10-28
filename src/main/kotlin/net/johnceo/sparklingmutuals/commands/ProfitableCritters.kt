package net.johnceo.sparklingmutuals.commands

import com.google.gson.JsonParser
import java.util.Locale
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.history.BazaarPrices
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariTicketType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

/** Spreadsheet spawn averages, independent of mode capture minima and observed live populations. */
object CritterSpawnAverages {
    private val data by lazy {
        javaClass.getResourceAsStream("/assets/sparkling-mutuals/data/average_critter_spawns.json")!!.bufferedReader().use {
            JsonParser.parseReader(it).asJsonObject
        }
    }
    private fun averages(field: String) = data.getAsJsonObject(field).entrySet()
        .associate { entry -> entry.key to entry.value.asDouble }
    val fullParty: Map<String, Double> by lazy { averages("averages") }
    val lazyParty: Map<String, Double> by lazy { averages("lazyAverages") }

}

data class ProfitableCritter(val name: String, val shardPrice: Double, val averageSpawns: Double,
    val essencePrice: Double = 0.0, val essencePerSpawn: Double = 0.0) {
    val shardsPerRun get() = averageSpawns * ProfitableCritters.SHARDS_PER_CRITTER
    val essencePerRun get() = averageSpawns * essencePerSpawn
    val shardCoinsPerRun get() = shardPrice * shardsPerRun
    val essenceCoinsPerRun get() = essencePrice * essencePerRun
    val coinsPerRun get() = shardCoinsPerRun + essenceCoinsPerRun
}

object ProfitableCritters {
    const val SHARDS_PER_CRITTER = 1.5025
    private var rankedPrices: Map<String, DoubleArray>? = null
    private var rankedInstaSell: Boolean? = null
    private var rankedTicket: SafariTicketType? = null
    private var ranked = emptyList<ProfitableCritter>()
    private var species = emptySet<String>()
    val ticket get() = SafariTicketType.entries.getOrElse(ConfigManager.profitsTicketMode) { SafariTicketType.BASIC }
    fun essenceMultiplier(ticket: SafariTicketType) = when (ticket) {
        SafariTicketType.BASIC -> 1.0
        SafariTicketType.ECONOMY -> 1.1
        SafariTicketType.PREMIUM -> 1.25
        SafariTicketType.FIRST_CLASS -> 1.5
    }
    /** Roster colors are the shard rarities: Common, Uncommon, Rare, Epic, Legendary. */
    fun baseEssence(species: String) = when (SafariRoster.color(species)) {
        0xFF55FF55.toInt() -> 4
        0xFF5555FF.toInt() -> 8
        0xFFAA00AA.toInt() -> 16
        0xFFFFAA00.toInt() -> 32
        else -> 2
    }

    /** Shared by the command and ESP; re-rank only for new cached quotes or a price preference change. */
    fun cachedTop(): List<ProfitableCritter> {
        val prices = BazaarPrices.cachedPrices
        val instaSell = ConfigManager.runCoinsInstaSell
        if (prices !== rankedPrices || instaSell != rankedInstaSell || ticket != rankedTicket) {
            val quote = if (instaSell) 0 else 1
            ranked = top({ prices[BazaarPrices.shardId(it)]?.getOrNull(quote) },
                essencePrice = prices[BazaarPrices.SAFARI_ESSENCE]?.getOrNull(quote) ?: 0.0, ticket = ticket)
            species = ranked.map { it.name }.toSet()
            rankedPrices = prices
            rankedInstaSell = instaSell
            rankedTicket = ticket
        }
        return ranked
    }
    fun cachedSpecies(): Set<String> { cachedTop(); return species }

    /** Rank by estimated gross coins per run, retaining fractional yields until display formatting. */
    fun top(prices: (String) -> Double?, averages: Map<String, Double> = CritterSpawnAverages.fullParty,
        essencePrice: Double = 0.0, ticket: SafariTicketType = SafariTicketType.BASIC): List<ProfitableCritter> =
        SafariRoster.all.mapNotNull { critter ->
            val average = averages[critter.name]?.takeIf { it.isFinite() && it > 0 } ?: return@mapNotNull null
            val price = prices(critter.name)?.takeIf { it.isFinite() && it > 0 } ?: return@mapNotNull null
            ProfitableCritter(critter.name, price, average,
                essencePrice.takeIf { it.isFinite() && it > 0 } ?: 0.0, baseEssence(critter.name) * essenceMultiplier(ticket))
        }.sortedWith(compareByDescending<ProfitableCritter> { it.coinsPerRun }.thenBy { it.name }).take(10)

    fun line(rank: Int, entry: ProfitableCritter): Component {
        val hover = String.format(Locale.ROOT,
            "%.2f shards × %,.0f coins = %,.0f\n%.2f essence × %,.0f coins = %,.0f\nTotal: %,.0f coins/run",
            entry.shardsPerRun, entry.shardPrice, entry.shardCoinsPerRun,
            entry.essencePerRun, entry.essencePrice, entry.essenceCoinsPerRun, entry.coinsPerRun)
        return Component.empty()
            .append(Component.literal("$rank. ").withStyle { it.withColor(0xAAAAAA) })
            .append(Component.literal(entry.name).withStyle { it.withColor(SafariRoster.color(entry.name) and 0xFFFFFF) })
            .append(Component.literal(" │ ").withStyle { it.withColor(0x555555) })
            .append(Component.literal("~${BazaarPrices.format(Math.round(entry.coinsPerRun))}").withStyle { it.withColor(0xFFD700) })
            .withStyle { it.withHoverEvent(HoverEvent.ShowText(Component.literal(hover))) }
    }

    fun show(source: FabricClientCommandSource, selectedTicket: SafariTicketType? = null): Int {
        if (selectedTicket != null && ConfigManager.profitsTicketMode != selectedTicket.ordinal) {
            ConfigManager.profitsTicketMode = selectedTicket.ordinal
            ConfigManager.save()
        }
        val instaSell = ConfigManager.runCoinsInstaSell
        val ranked = cachedTop()
        if (ranked.isEmpty()) {
            source.sendFeedback(LocalChat.component("[SM] No cached Safari shard prices available yet. Try again after the normal Bazaar update."))
            return 1
        }
        val heading = Component.literal("Top ${ranked.size} profits").withStyle { it.withColor(0xFFD700).withBold(true) }
            .append(Component.literal(" · ${ticket.label} · coins/run").withStyle { it.withColor(0xDDDDDD).withBold(false) })
        val output = Component.empty().append(LocalChat.component(heading))
            .append(Component.literal("\n${if (instaSell) "Insta-sell" else "Sell offer"} · Hover for breakdown").withStyle { it.withColor(0xAAAAAA) })
        if (ranked.all { it.essencePrice == 0.0 }) output.append(Component.literal(
            "\nEssence price pending; shards only.").withStyle { it.withColor(0xFFAA00) })
        ranked.forEachIndexed { index, entry ->
            if (index > 0) output.append(Component.literal("\n────────────────────────────").withStyle { it.withColor(0x444444) })
            output.append("\n").append(line(index + 1, entry))
        }
        source.sendFeedback(output)
        return 1
    }
}
