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
    @Test fun `only the thirty second biome owner answers even after moving elsewhere`() {
        val command = PartyCommand(PartyCommandKind.MISSING_RUN, biome = SafariBiome.ICY)
        assertTrue(command.answerable(inSafari = true, responderBiome = SafariBiome.ICY))
        assertFalse(command.answerable(inSafari = true, responderBiome = SafariBiome.CAVERN))
        val run = net.johnceo.sparklingmutuals.safari.SafariRun(0)
        run.sampleBiome(30000,SafariBiome.ICY)
        assertTrue(SafariMissingReply.holds(command, run, run, true, currentBiome = SafariBiome.ICY))
        assertTrue(SafariMissingReply.holds(command, run, run, true, currentBiome = SafariBiome.CAVERN))
        assertFalse(command.answerable(inSafari = false))
        assertTrue(PartyCommand(PartyCommandKind.MUTUALS).answerable(inSafari = false))
    }
    private fun party(vararg needed: String) = net.johnceo.sparklingmutuals.safari.PartySparklingState().apply {
        select(setOf("local"))
        accept(setOf("local"), mapOf("local" to net.johnceo.sparklingmutuals.safari.SafariRoster.all.map { it.name }.toSet() - needed.toSet()))
    }
    @Test fun `full clear reply uses minimum or known populations and rendered source counts`() {
        var now = 0L
        val run = net.johnceo.sparklingmutuals.safari.SafariRun(0) { now }
        run.record(net.johnceo.sparklingmutuals.safari.SafariCatch.parse("CAPTURE! You caught a Nozzlenose!")!!, now)
        fun left(biome: SafariBiome) = SafariMissingReply.requirements(run, biome,
            net.johnceo.sparklingmutuals.safari.SafariMode.FULL_CLEAR, party(), 2, "3")
        assertTrue("Nozzlenose: 1" in left(SafariBiome.ICY))
        now = 3000 // A genuinely additional population, after capture models have settled.
        run.observePopulation(listOf("Nozzlenose" to java.util.UUID.randomUUID(), "Nozzlenose" to java.util.UUID.randomUUID()))
        assertTrue("Nozzlenose: 2" in left(SafariBiome.ICY))
        run.updateNestEvidence(false, 1, found = 5, unknown = 0)
        run.record(net.johnceo.sparklingmutuals.safari.SafariCatch.parse("CAPTURE! You caught a Honeybug!")!!)
        assertTrue("Honeybug: 4" in left(SafariBiome.FOREST))
        assertTrue(left(SafariBiome.CAVERN).containsAll(listOf("Mounds: 2", "Snoozle Walls: 3")))
    }
    @Test fun `unique list and only known needed source counters appear in replies`() {
        val run = net.johnceo.sparklingmutuals.safari.SafariRun(0)
        run.record(net.johnceo.sparklingmutuals.safari.SafariCatch.parse("CAPTURE! You caught a Honeybug!")!!)
        val unique = net.johnceo.sparklingmutuals.safari.SafariMode.UNIQUE
        val forest=SafariMissingReply.requirements(run,SafariBiome.FOREST,unique,party("Honeybug"),0,"?",3)
        assertTrue(forest.first().startsWith("Uniques left to capture: "))
        assertFalse(forest.first().contains("Honeybug"))
        assertTrue("Bee nests: 3" in forest)
        assertFalse(SafariMissingReply.requirements(run,SafariBiome.FOREST,unique,party(),0,"0",3).any { it.contains("Bee nests") })
        val shiny = net.johnceo.sparklingmutuals.safari.SafariMode.SPARKLING
        assertEquals(listOf("Snoozle Walls: 2"),SafariMissingReply.requirements(run,SafariBiome.CAVERN,shiny,party("Snoozle"),4,"2"))
        assertTrue(SafariMissingReply.requirements(run,SafariBiome.ICY,shiny,party("Wumpa"),0,"0").isEmpty())
        assertEquals(listOf("Gemzie: chamber unopened"),SafariMissingReply.requirements(run,SafariBiome.CAVERN,shiny,party("Gemzie"),0,"?"))
        run.sparklingChecks.chat("A rumbling sound can be heard, and the door at the back of the chamber opens...")
        assertTrue(SafariMissingReply.requirements(run,SafariBiome.CAVERN,shiny,party("Gemzie"),0,"?").isEmpty())
        assertEquals(listOf("Gazers: 4 left to check"),SafariMissingReply.requirements(run,SafariBiome.HAUNTED,shiny,party("Gazer","Gimmiegold"),0,"?"))
        assertTrue(SafariMissingReply.requirements(run,SafariBiome.CAVERN,shiny,net.johnceo.sparklingmutuals.safari.PartySparklingState(),0,"?").isEmpty())
    }
    @Test fun `a delayed reply is dropped once its run ends or another run starts`() {
        val command = PartyCommand(PartyCommandKind.MISSING_RUN)
        val ledger = SafariLedger().apply { enter(1_000) }
        ledger.current!!.sampleBiome(31000,SafariBiome.FOREST)
        val asked = ledger.current
        // While the election and lookup wait, the same run inside the Safari still gets its answer.
        assertTrue(SafariMissingReply.holds(command, asked, ledger.current, inSafari = true))
        assertFalse(SafariMissingReply.holds(command, asked, ledger.current, inSafari = false))
        ledger.leave(32_000)
        assertFalse(SafariMissingReply.holds(command, asked, ledger.current, inSafari = true))
        ledger.enter(33_000)
        assertFalse(SafariMissingReply.holds(command, asked, ledger.current, inSafari = true))
        // Asked outside any run: there is nothing to answer for.
        assertFalse(SafariMissingReply.holds(command, null, null, inSafari = true))
        // Commands that are not about the run are never bound to one.
        assertTrue(SafariMissingReply.holds(PartyCommand(PartyCommandKind.MUTUALS), asked, null, inSafari = false))
    }
    @Test fun `replies list what is left and mark finished biomes done`() {
        val left = mapOf(SafariBiome.CAVERN to listOf("Snoozle", "Rockmite"), SafariBiome.HAUNTED to listOf("Gazer"))
        val missing = { b: SafariBiome -> left[b].orEmpty() }
        assertEquals("Missing Cavern: Snoozle; Rockmite", SafariMissingReply.format(SafariBiome.CAVERN, missing))
        assertEquals("Missing Icy: None", SafariMissingReply.format(SafariBiome.ICY, missing))
        assertEquals("Missing: Forest: None | Cavern: Snoozle; Rockmite | Icy: None | Haunted: Gazer",
            SafariMissingReply.format(null, missing))
    }
    @Test fun `a world transition drops a reply before the stale run ledger updates`() {
        val command = PartyCommand(PartyCommandKind.MISSING_RUN)
        val ledger = SafariLedger().apply { enter(1_000) }
        ledger.current!!.sampleBiome(31000,SafariBiome.FOREST)
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
        assertFalse(command.matchesResponse("Missing Icy: None"))
        assertTrue(PartyCommand(PartyCommandKind.MISSING_RUN).matchesResponse("Missing: Forest: Done | Cavern: Done | Icy: Done | Haunted: Done"))
    }
    @Test fun `assignment freezes the biome crossing thirty seconds and does not guess missed samples`() {
        val run=net.johnceo.sparklingmutuals.safari.SafariRun(1000)
        run.sampleBiome(30950,SafariBiome.FOREST)
        assertNull(run.biomeAtThirtySeconds)
        run.sampleBiome(31050,SafariBiome.CAVERN)
        assertEquals(SafariBiome.FOREST,run.biomeAtThirtySeconds)
        run.sampleBiome(32000,SafariBiome.HAUNTED)
        assertEquals(SafariBiome.FOREST,run.biomeAtThirtySeconds)
        val unknown=net.johnceo.sparklingmutuals.safari.SafariRun(0)
        unknown.sampleBiome(35000,SafariBiome.ICY)
        assertNull(unknown.biomeAtThirtySeconds)
        assertFalse(SafariMissingReply.holds(PartyCommand(PartyCommandKind.MISSING_RUN),unknown,unknown,true))
    }
}
