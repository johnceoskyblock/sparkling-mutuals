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
        val valid = SafariRoster.all.map { it.name }.filter { it !in setOf("Wumpa", "Foxtrot", "Macaw", "Doomspiral", "Hideyho") }.take(9)
        val prices = valid.associateWith { 100.0 } + mapOf("Wumpa" to 1000.0, "Foxtrot" to 900.0,
            "Macaw" to Double.NaN, "Doomspiral" to 0.0, "Hideyho" to -1.0)
        val averages = prices.keys.associateWith { if (it == "Foxtrot") 10.0 else 1.0 }
        val top = ProfitableCritters.top(prices::get, averages)
        assertEquals(10, top.size)
        assertEquals(listOf("Foxtrot", "Wumpa") + valid.sorted().take(8), top.map { it.name })
        assertTrue(top.zipWithNext().all { (a, b) -> a.coinsPerRun >= b.coinsPerRun })
        assertEquals(1.5025, top[1].shardsPerRun, 1e-10)
        assertEquals(1502.5, top[1].coinsPerRun, 1e-10)
        assertEquals(1, ProfitableCritters.top({ if (it == "Wumpa") 100.0 else null }).size)
        assertTrue(ProfitableCritters.top({ null }).isEmpty())
    }
    @Test fun `essence uses rarity and spawn count with each ticket multiplier and can change ranking`() {
        val tickets = net.johnceo.sparklingmutuals.safari.SafariTicketType.entries
        for ((ticket, multiplier) in tickets.zip(listOf(1.0, 1.1, 1.25, 1.5))) {
            val entry = ProfitableCritters.top({ 100.0 }, mapOf("Wumpa" to 2.0), 50.0, ticket).single()
            assertEquals(3.005, entry.shardsPerRun, 1e-10)
            assertEquals(64 * multiplier, entry.essencePerRun, 1e-10)
            assertEquals(300.5 + 3200 * multiplier, entry.coinsPerRun, 1e-8)
        }
        for ((species, essence) in mapOf("Foxtrot" to 2, "Bluebird" to 4, "Troodon" to 8, "Gemzie" to 16, "Macaw" to 32))
            assertEquals(essence, ProfitableCritters.baseEssence(species))
        val prices = mapOf("Foxtrot" to 1000.0, "Wumpa" to 1.0)
        val averages = prices.keys.associateWith { 1.0 }
        assertEquals("Foxtrot", ProfitableCritters.top(prices::get, averages, 50.0, tickets[0]).first().name)
        assertEquals("Wumpa", ProfitableCritters.top(prices::get, averages, 50.0, tickets[3]).first().name)
        assertEquals("Troodon", ProfitableCritters.top({ 100.0 }, mapOf("Troodon" to 3.0), 50.0).single().name)
        for (species in listOf("Scrappy", "Rockmite"))
            assertEquals(species, ProfitableCritters.top({ 100.0 }, mapOf(species to 3.0), 50.0).single().name)
    }
}
