package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariCatchTest {
    @Test fun `exact v090 roster and quotas`() {
        assertEquals(listOf(9, 9, 9, 10), SafariBiome.entries.map { it.critters.size })
        assertEquals(37, SafariRoster.all.size)
        assertEquals(4, SafariRoster.named("Gazer")!!.required(false))
        assertEquals(3, SafariRoster.named("Troodon")!!.required(false))
        assertEquals(1, SafariRoster.named("Billygoat")!!.required(false))
    }
    @Test fun `capture messages count captures not shards`() {
        val run = SafariRun(100)
        repeat(2) { run.record(SafariCatch.parse("§a§lCAPTURE! §7You caught a Honeybug and gained 3x Honeybug Shard!")!!) }
        assertEquals(2, run.count("Honeybug"))
        assertEquals(2, run.count("Honeybug"))
    }
    @Test fun `loot share and Hideyho reward are successful catches`() {
        val run = SafariRun(100)
        run.record(SafariCatch.parse("LOOT SHARE! You received a Rainbow Feather and 4x Mantis Shrimp Shard from TestPlayer catching a SPARKLING Mantis Shrimp!")!!)
        run.record(SafariCatch.parse("CAPTURE! You found the Hideyho, and as a reward it gave you a Hideyho Shard!")!!)
        assertEquals(1, run.count("Mantis Shrimp"))
        assertEquals(1, run.count("Mantis Shrimp"))
        assertEquals(1, run.count("Hideyho"))
        assertNull(SafariCatch.parse("LOOT SHARE! You received a Honeybug Shard!"))
    }
    @Test fun `quoted party messages attempts and inventories are not catches`() {
        listOf("Party > [VIP] Test: CAPTURE! You caught a Honeybug!", "You threw a Critter Capsule at the Honeybug!",
            "You sent 3 Honeybug Shards to your Hunting Box.", "CAPTURE! You caught a Honeybugger!").forEach {
            assertNull(SafariCatch.parse(it), it)
        }
    }
    @Test fun `unique setting changes quota completion without changing raw counts`() {
        val run = SafariRun(100)
        run.record(SafariCatch.parse("CAPTURE! You caught a Gemzie and gained a Gemzie Shard!")!!)
        assertFalse(run.complete(SafariRoster.named("Gemzie")!!, false))
        assertTrue(run.complete(SafariRoster.named("Gemzie")!!, true))
        assertEquals(1, run.count("Gemzie"))
    }
    @Test fun `biome changes retain captures and a new run resets them`() {
        val ledger = SafariLedger()
        ledger.arrive(100)
        ledger.current!!.record(SafariCatch.parse("CAPTURE! You caught a Honeybug!")!!)
        ledger.arrive(200)
        assertEquals(1, ledger.current!!.count("Honeybug"))
        ledger.leave(300)
        assertEquals(1, ledger.last!!.count("Honeybug"))
        ledger.arrive(400)
        assertEquals(0, ledger.current!!.count("Honeybug"))
    }
    @Test fun `show where includes entrance only when selected`() {
        assertFalse(SafariVisibility.visible(0, false, true))
        assertTrue(SafariVisibility.visible(1, false, true))
        assertFalse(SafariVisibility.visible(1, false, false))
        assertTrue(SafariVisibility.visible(2, false, false))
        assertTrue(SafariVisibility.visible(0, true, false))
    }
    @Test fun `explicit new entry resets a run even before departure was observed`() {
        val ledger = SafariLedger()
        ledger.arrive(100)
        ledger.current!!.record(SafariCatch.parse("CAPTURE! You caught a Honeybug!")!!)
        ledger.enter(200)
        assertEquals(0, ledger.current!!.count("Honeybug"))
        assertEquals(1, ledger.last!!.count("Honeybug"))
    }
}
