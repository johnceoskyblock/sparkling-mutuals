package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariLedger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MissingRunCommandTest {
    private fun parse(line: String) = PartyCommand.fromChat(line)?.command

    @Test fun `bare m asks for every biome and a letter or name asks for one`() {
        assertEquals(PartyCommand(PartyCommandKind.MISSING_RUN), parse("Party > [MVP+] Farmer: !m"))
        mapOf("c" to SafariBiome.CAVERN, "F" to SafariBiome.FOREST, "ice" to SafariBiome.ICY, "Haunted" to SafariBiome.HAUNTED)
            .forEach { (word, biome) -> assertEquals(biome, parse("Party > Farmer: !m $word")?.biome, word) }
        assertEquals("!m icy", parse("Party > Farmer: !M I")?.text)
    }
    @Test fun `other words and player lookups are not run queries`() {
        assertNull(parse("Party > Farmer: !m everything"))
        assertNull(parse("Party > Farmer: !m h extra"))
        assertNull(parse("Guild > Farmer: !m h"))
        assertEquals(PartyCommandKind.MISSING, parse("Party > Farmer: !missing JohnCEO")?.kind)
    }
    @Test fun `only clients inside the Safari answer`() {
        val command = PartyCommand(PartyCommandKind.MISSING_RUN, biome = SafariBiome.ICY)
        assertTrue(command.answerable(inSafari = true, currentBiome = SafariBiome.ICY))
        assertFalse(command.answerable(inSafari = true, currentBiome = SafariBiome.CAVERN))
        val run = net.johnceo.sparklingmutuals.safari.SafariRun(0)
        assertTrue(SafariMissingReply.holds(command, run, run, true, currentBiome = SafariBiome.ICY))
        assertFalse(SafariMissingReply.holds(command, run, run, true, currentBiome = SafariBiome.CAVERN))
        assertFalse(command.answerable(inSafari = false))
        assertTrue(PartyCommand(PartyCommandKind.MUTUALS).answerable(inSafari = false))
    }
    private fun party(vararg needed: String) = net.johnceo.sparklingmutuals.safari.PartySparklingState().apply {
        select(setOf("local"))
        accept(setOf("local"), mapOf("local" to net.johnceo.sparklingmutuals.safari.SafariRoster.all.map { it.name }.toSet() - needed.toSet()))
    }
    @Test fun `full clear reply uses minimum or known populations and rendered source counts`() {
        val run = net.johnceo.sparklingmutuals.safari.SafariRun(0)
        run.record(net.johnceo.sparklingmutuals.safari.SafariCatch.parse("CAPTURE! You caught a Nozzlenose!")!!)
        fun left(biome: SafariBiome) = SafariMissingReply.requirements(run, biome,
            net.johnceo.sparklingmutuals.safari.SafariMode.FULL_CLEAR, party(), 2, "3")
        assertTrue("Nozzlenose: 1" in left(SafariBiome.ICY))
        run.observePopulation(listOf("Nozzlenose" to java.util.UUID.randomUUID(), "Nozzlenose" to java.util.UUID.randomUUID()))
        assertTrue("Nozzlenose: 2" in left(SafariBiome.ICY))
        run.updateNestEvidence(false, 1, found = 5, unknown = 0)
        run.record(net.johnceo.sparklingmutuals.safari.SafariCatch.parse("CAPTURE! You caught a Honeybug!")!!)
        assertTrue("Honeybug: 4" in left(SafariBiome.FOREST))
        assertTrue(left(SafariBiome.CAVERN).containsAll(listOf("Mounds: 2", "Snoozle Walls: 3")))
    }
    @Test fun `unique and shiny requirements use party needs and retain bee source reminder after capture`() {
        val run = net.johnceo.sparklingmutuals.safari.SafariRun(0)
        run.record(net.johnceo.sparklingmutuals.safari.SafariCatch.parse("CAPTURE! You caught a Honeybug!")!!)
        val unique = net.johnceo.sparklingmutuals.safari.SafariMode.UNIQUE
        assertTrue("Make sure to clear all nests" in SafariMissingReply.requirements(run, SafariBiome.FOREST, unique, party("Honeybug"), 0, "0"))
        assertFalse("Honeybug" in SafariMissingReply.requirements(run, SafariBiome.FOREST, unique, party("Honeybug"), 0, "0"))
        assertFalse("Make sure to clear all nests" in SafariMissingReply.requirements(run, SafariBiome.FOREST, unique, party(), 0, "0"))
        val shiny = net.johnceo.sparklingmutuals.safari.SafariMode.SPARKLING
        assertEquals(listOf("Snoozle Walls: 2"), SafariMissingReply.requirements(run, SafariBiome.CAVERN, shiny, party("Snoozle"), 4, "2"))
        assertEquals(listOf("Wumpa: capture"), SafariMissingReply.requirements(run, SafariBiome.ICY, shiny, party("Wumpa"), 0, "0"))
    }
    @Test fun `a delayed reply is dropped once its run ends or another run starts`() {
        val command = PartyCommand(PartyCommandKind.MISSING_RUN)
        val ledger = SafariLedger().apply { enter(1_000) }
        val asked = ledger.current
        // While the election and lookup wait, the same run inside the Safari still gets its answer.
        assertTrue(SafariMissingReply.holds(command, asked, ledger.current, inSafari = true))
        assertFalse(SafariMissingReply.holds(command, asked, ledger.current, inSafari = false))
        ledger.leave(2_000)
        assertFalse(SafariMissingReply.holds(command, asked, ledger.current, inSafari = true))
        ledger.enter(3_000)
        assertFalse(SafariMissingReply.holds(command, asked, ledger.current, inSafari = true))
        // Asked outside any run: there is nothing to answer for.
        assertFalse(SafariMissingReply.holds(command, null, null, inSafari = true))
        // Commands that are not about the run are never bound to one.
        assertTrue(SafariMissingReply.holds(PartyCommand(PartyCommandKind.MUTUALS), asked, null, inSafari = false))
    }
    @Test fun `replies list what is left and mark finished biomes done`() {
        val left = mapOf(SafariBiome.CAVERN to listOf("Snoozle", "Rockmite"), SafariBiome.HAUNTED to listOf("Gazer"))
        val missing = { b: SafariBiome -> left[b].orEmpty() }
        assertEquals("Missing Cavern: Snoozle, Rockmite", SafariMissingReply.format(SafariBiome.CAVERN, missing))
        assertEquals("Missing Icy: Done", SafariMissingReply.format(SafariBiome.ICY, missing))
        assertEquals("Missing: Forest: Done | Cavern: Snoozle, Rockmite | Icy: Done | Haunted: Gazer",
            SafariMissingReply.format(null, missing))
    }
    @Test fun `a world transition drops a reply before the stale run ledger updates`() {
        val command = PartyCommand(PartyCommandKind.MISSING_RUN)
        val ledger = SafariLedger().apply { enter(1_000) }
        val asked = ledger.current
        val askedWorld = Any()
        assertTrue(SafariMissingReply.holds(command, asked, ledger.current, true, askedWorld, askedWorld))
        // The command tick runs before the Safari tick has replaced this run or cleared inSafari.
        assertFalse(SafariMissingReply.holds(command, asked, ledger.current, true, askedWorld, Any()))
        assertFalse(SafariMissingReply.holds(command, asked, ledger.current, true, askedWorld, null))
        assertTrue(SafariMissingReply.holds(PartyCommand(PartyCommandKind.MUTUALS), asked,
            ledger.current, true, askedWorld, Any()))
    }
    @Test fun `replies are recognised so other clients do not repeat them`() {
        val command = PartyCommand(PartyCommandKind.MISSING_RUN, biome = SafariBiome.HAUNTED)
        assertTrue(command.matchesResponse("Missing Haunted: Gazer"))
        assertFalse(command.matchesResponse("Missing Icy: Done"))
        assertTrue(PartyCommand(PartyCommandKind.MISSING_RUN).matchesResponse("Missing: Forest: Done | Cavern: Done | Icy: Done | Haunted: Done"))
    }
}
