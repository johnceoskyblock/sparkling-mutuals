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

    @Test fun `top five use per shard price rather than run revenue and skip unavailable quotes`() {
        val prices = mapOf("Wumpa" to 1000.0, "Foxtrot" to 900.0, "Bluebird" to 800.0,
            "Gemzie" to 700.0, "Gazer" to 600.0, "Tepid" to 500.0,
            "Macaw" to Double.NaN, "Doomspiral" to 0.0, "Hideyho" to -1.0)
        val top = ProfitableCritters.top(prices::get)
        assertEquals(listOf("Wumpa", "Foxtrot", "Bluebird", "Gemzie", "Gazer"), top.map { it.name })
        assertTrue(top[1].coinsPerRun > top[0].coinsPerRun, "spawn frequency must not change the requested price ranking")
        assertEquals(1.5025, top[0].shardsPerRun, 1e-10)
        assertEquals(1502.5, top[0].coinsPerRun, 1e-10)
        assertTrue(ProfitableCritters.top({ null }).isEmpty())
    }
}
