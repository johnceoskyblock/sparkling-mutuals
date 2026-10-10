package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariSessionTest {
    @Test fun `quoted multiline player chat cannot create catches entries or personal bests`() {
        for (prefix in listOf("Party > [VIP] Test:", "Guild > Test:", "Officer > Test:", "Co-op > Test:",
            "From [MVP+] Test:", "To Test:", "[MVP+] Test [GUILD]:", "Test:", "<Test>")) {
            for (separator in listOf("\\n", "\n", "\r\n")) {
                val raw = "$prefix ${separator}Player entered Critter Safari!${separator}CAPTURE! You caught a Doomspiral!"
                assertTrue(SafariMessages.lines(raw).isEmpty(), raw)
            }
        }
    }
    @Test fun `boat and center are inside even without a biome`() {
        val area = SafariAreaMap.areaAt(-52.0, 69.0, 21.0)
        assertEquals(0, area)
        assertNull(SafariAreaMap.biomeAt(-52.0, 69.0, 21.0))
        assertEquals(SafariLocation.INSIDE, SafariLocation.resolve(listOf("Area: Critter Safari"), area, false))
        assertEquals(SafariLocation.OUTSIDE, SafariLocation.resolve(listOf("Area: Critter Safari"), null, false))
        assertEquals(SafariLocation.OUTSIDE, SafariLocation.resolve(listOf("Area: Torrhus Canyon"), area, true))
        assertEquals(SafariLocation.OUTSIDE, SafariLocation.resolve(listOf("Area: Hub"), area, true))
    }
    @Test fun `timer starts on boat and stays running through center and loading sidebar`() {
        val ledger = SafariLedger()
        ledger.update(SafariLocation.INSIDE, null, 1000)
        val run = ledger.current!!
        ledger.update(SafariLocation.INSIDE, SafariBiome.FOREST, 2000)
        run.record(SafariCatch.parse("CAPTURE! You caught a Honeybug!")!!)
        ledger.update(SafariLocation.INSIDE, null, 4000)
        ledger.update(SafariLocation.UNKNOWN, null, 6000)
        assertSame(run, ledger.current)
        assertEquals(5000L, run.elapsed(6000))
        assertEquals(SafariBiome.FOREST, run.lastBiome)
        ledger.update(SafariLocation.ENTRANCE, null, 7000)
        assertNull(ledger.current)
        assertSame(run, ledger.displayed)
        assertEquals(6000L, run.elapsed(12000))
        assertEquals(1, ledger.displayed!!.count("Honeybug"))
    }
    @Test fun `toggles never erase counts and last run remains until new arrival`() {
        val ledger = SafariLedger()
        ledger.update(SafariLocation.INSIDE, SafariBiome.HAUNTED, 10)
        ledger.current!!.record(SafariCatch.parse("CAPTURE! You found the Hideyho, and it gave you a Hideyho Shard!")!!)
        val original = ConfigManager.catchCountPanel
        try {
            ConfigManager.catchCountPanel = false
            ledger.update(SafariLocation.INSIDE, null, 20)
            ConfigManager.catchCountPanel = true
            assertEquals(1, ledger.displayed!!.count("Hideyho"))
            ledger.leave(30)
            ledger.update(SafariLocation.ENTRANCE, null, 40)
            assertEquals(SafariBiome.HAUNTED, ledger.displayed!!.lastBiome)
            assertEquals(1, ledger.displayed!!.count("Hideyho"))
            ledger.update(SafariLocation.INSIDE, null, 50)
            assertEquals(0, ledger.displayed!!.count("Hideyho"))
            assertNull(ledger.displayed!!.lastBiome)
        } finally { ConfigManager.catchCountPanel = original }
    }
    @Test fun `delayed entry banner never restarts an already detected run`() {
        val ledger = SafariLedger()
        ledger.arrive(1000)
        ledger.current!!.record(SafariCatch.parse("CAPTURE! You caught a Honeybug!")!!)
        ledger.confirmEntry(1500)
        assertEquals(1000L, ledger.current!!.startedAt)
        assertEquals(1, ledger.current!!.count("Honeybug"))
        ledger.confirmEntry(200000)
        assertEquals(1000L, ledger.current!!.startedAt)
        assertEquals(1, ledger.current!!.count("Honeybug"))
    }
    @Test fun `delayed Safari evidence uses world arrival until destination is resolved`() {
        val ledger = SafariLedger()
        ledger.worldChanged(1000)
        ledger.update(SafariLocation.UNKNOWN, null, 20000)
        ledger.update(SafariLocation.INSIDE, null, 40000)
        assertEquals(1000L, ledger.current!!.startedAt)
        ledger.leave(50000)
        ledger.worldChanged(60000)
        ledger.update(SafariLocation.ENTRANCE, null, 70000)
        ledger.update(SafariLocation.INSIDE, null, 80000)
        assertEquals(80000L, ledger.current!!.startedAt)
        ledger.worldChanged(90000)
        ledger.confirmEntry(120000)
        assertEquals(90000L, ledger.current!!.startedAt)
    }
    @Test fun `actual and escaped multiline server banners and captures process separately`() {
        for (separator in listOf("\n", "\r\n", "\\n")) {
            val lines = SafariMessages.lines("§aWelcome$separator[MVP+] Player entered Critter Safari!${separator}CAPTURE! You caught a Wumpa!")
            val ledger = SafariLedger()
            ledger.worldChanged(1000)
            val bests = SafariPersonalBests()
            lines.forEach { line ->
                if (SafariMessages.enteredBy(line, "player")) ledger.confirmEntry(2000)
                SafariCatch.parse(line)?.let {
                    ledger.arrive(63000); ledger.current!!.record(it)
                    bests.record(line, ledger.current!!, 63000)
                }
            }
            assertEquals(1000L, ledger.current!!.startedAt)
            assertEquals(1, ledger.current!!.count("Wumpa"))
            assertEquals(62000L, bests.time("Wumpa", duo = true))
            assertFalse(SafariMessages.enteredBy("Party > Test: Player entered Critter Safari!", "Player"))
        }
    }
    @Test fun `own and shared captures are one combined species count`() {
        val run = SafariRun(100)
        run.record(SafariCatch.parse("CAPTURE! You caught a Honeybug and gained 3x Honeybug Shard!")!!)
        run.record(SafariCatch.parse("LOOT SHARE! You received 2x Honeybug Shard from TestPlayer catching a Honeybug!")!!)
        assertEquals(2, run.count("Honeybug"))
        assertEquals(1, run.progress(SafariBiome.FOREST.critters, false))
    }
}
