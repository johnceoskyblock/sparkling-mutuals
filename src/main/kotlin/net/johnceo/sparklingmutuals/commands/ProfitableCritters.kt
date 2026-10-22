package net.johnceo.sparklingmutuals.commands

import com.google.gson.JsonParser
import java.util.Locale
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.history.BazaarPrices
import net.johnceo.sparklingmutuals.safari.SafariRoster

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

data class ProfitableCritter(val name: String, val shardPrice: Double, val averageSpawns: Double) {
    val shardsPerRun get() = averageSpawns * ProfitableCritters.SHARDS_PER_CRITTER
    val coinsPerRun get() = shardPrice * shardsPerRun
}

object ProfitableCritters {
    const val SHARDS_PER_CRITTER = 1.5025

    /** Rank by the price of one shard, retaining fractional yields until display formatting. */
    fun top(prices: (String) -> Double?, averages: Map<String, Double> = CritterSpawnAverages.fullParty): List<ProfitableCritter> =
        SafariRoster.all.mapNotNull { critter ->
            val average = averages[critter.name]?.takeIf { it.isFinite() && it > 0 } ?: return@mapNotNull null
            val price = prices(critter.name)?.takeIf { it.isFinite() && it > 0 } ?: return@mapNotNull null
            ProfitableCritter(critter.name, price, average)
        }.sortedWith(compareByDescending<ProfitableCritter> { it.shardPrice }.thenBy { it.name }).take(5)

    fun line(rank: Int, entry: ProfitableCritter): String = String.format(Locale.ROOT,
        "[SM] %d. %s: %,.0f coins/shard | %.2f shards/run | ~%,.0f coins/run",
        rank, entry.name, entry.shardPrice, entry.shardsPerRun, entry.coinsPerRun)

    fun show(source: FabricClientCommandSource): Int {
        val instaSell = ConfigManager.runCoinsInstaSell
        val ranked = top({ BazaarPrices.cachedShardPrice(it, instaSell) })
        if (ranked.isEmpty()) {
            source.sendFeedback(LocalChat.component("[SM] No cached Safari shard prices available yet. Try again after the normal Bazaar update."))
            return 1
        }
        source.sendFeedback(LocalChat.component("[SM] Top ${ranked.size} by shard price (${if (instaSell) "insta-sell" else "sell offer"}, cached). Full-party averages, x1.5025 shards."))
        ranked.forEachIndexed { index, entry -> source.sendFeedback(LocalChat.component(line(index + 1, entry))) }
        return 1
    }
}
