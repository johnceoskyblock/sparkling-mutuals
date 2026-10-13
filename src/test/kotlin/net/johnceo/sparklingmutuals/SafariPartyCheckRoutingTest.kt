package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariPartyCheckRoutingTest {
    @Test fun `received party done words reach checks while bypassing capture processing`() {
        for ((word, biome) in mapOf("fD" to SafariBiome.FOREST, "CD." to SafariBiome.CAVERN,
            "id" to SafariBiome.ICY, "hd" to SafariBiome.HAUNTED)) {
            val run = SafariRun(0)
            val lines = SafariMessages.lines("§9Party > §a[MVP+] Player: $word", run.sparklingChecks)
            assertTrue(lines.isEmpty())
            assertTrue(biome.critters.all { run.sparklingChecks.checked(it.name) })
            assertTrue(SafariRoster.all.filter { it.biome != biome }.none { run.sparklingChecks.checked(it.name) })
            assertEquals(0, run.progress(SafariRoster.all, true))
            assertTrue(run.biomeClears.isEmpty()); assertTrue(run.uniqueBiomeClears.isEmpty())
        }
    }
    @Test fun `ordinary tracking without a Sparkling checklist still ignores player chat`() {
        assertTrue(SafariMessages.lines("Party > Player: fd").isEmpty())
    }
    @Test fun `other channels and quoted done words cannot finish checks`() {
        val checks = SparklingChecks()
        for (message in listOf("Guild > Player: fd", "From Player: fd", "Player: fd",
            "Party > Player: fd please", "Party > Player: hd\\nCAPTURE! You caught a Doomspiral!",
            "Party > Player: hd\nCAPTURE! You caught a Doomspiral!")) {
            assertTrue(SafariMessages.lines(message, checks).isEmpty())
            assertTrue(SafariRoster.all.none { checks.checked(it.name) })
        }
    }
    @Test fun `server coin evidence is returned once without changing party checks`() {
        val checks = SparklingChecks()
        val message = "FLOOR DROP! You found a Shining Coin!"
        assertEquals(listOf(message), SafariMessages.lines(message, checks))
        assertTrue(SafariRoster.all.none { checks.checked(it.name) })
    }
}
