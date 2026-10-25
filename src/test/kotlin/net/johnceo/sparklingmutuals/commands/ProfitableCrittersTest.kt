package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.safari.SafariRoster
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProfitableCrittersTest {
    @Test fun `bundled full party averages cover the roster and preserve fractional spawn estimates`() {
        val averages = CritterSpawnAverages.fullParty
        assertEquals(SafariRoster.all.map { it.name }.toSet(), averages.keys)
        assertTrue(averages.values.all { it.isFinite() && it > 0 })
        assertEquals(7.232949734, averages.getValue("Bluebird"), 1e-10)
        val result = ProfitableCritters.top({ if (it == "Bluebird") 100.0 else null }).single()
        assertEquals(7.232949734 * 1.5025, result.shardsPerRun, 1e-10)
        assertEquals(7.232949734 * 1.5025 * 100.0, result.coinsPerRun, 1e-8)
    }

    @Test fun `top ten use run revenue rather than shard price and skip unavailable quotes`() {
        val excluded = setOf("Scrappy", "Rockmite", "Troodon")
        val valid = SafariRoster.all.map { it.name }.filter { it !in excluded + setOf("Wumpa", "Foxtrot", "Macaw", "Doomspiral", "Hideyho") }.take(9)
        val prices = valid.associateWith { 100.0 } + mapOf("Wumpa" to 1000.0, "Foxtrot" to 900.0,
            "Macaw" to Double.NaN, "Doomspiral" to 0.0, "Hideyho" to -1.0) + excluded.associateWith { 1_000_000.0 }
        val averages = prices.keys.associateWith { if (it == "Foxtrot") 10.0 else 1.0 }
        val top = ProfitableCritters.top(prices::get, averages)
        assertEquals(10, top.size)
        assertTrue(top.none { it.name in excluded }, "Excluded species must not displace eligible results, even at high prices")
        assertEquals(listOf("Foxtrot", "Wumpa") + valid.sorted().take(8), top.map { it.name })
        assertTrue(top.zipWithNext().all { (a, b) -> a.coinsPerRun >= b.coinsPerRun })
        assertEquals(1.5025, top[1].shardsPerRun, 1e-10)
        assertEquals(1502.5, top[1].coinsPerRun, 1e-10)
        assertEquals(1, ProfitableCritters.top({ if (it == "Wumpa") 100.0 else null }).size)
        assertTrue(ProfitableCritters.top({ null }).isEmpty())
    }
}
