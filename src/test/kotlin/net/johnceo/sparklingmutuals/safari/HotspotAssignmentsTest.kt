package net.johnceo.sparklingmutuals.safari

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HotspotAssignmentsTest {
    @Test fun `allocation is independent of report order and gives each member a different biome`() {
        for(a in SafariBiome.entries) for(b in SafariBiome.entries) for(c in SafariBiome.entries) for(d in SafariBiome.entries) {
            val reports=linkedMapOf("a" to a,"b" to b,"c" to c,"d" to d)
            val assigned=HotspotAssignments.allocate(reports)
            assertEquals(SafariBiome.entries.toSet(),assigned.values.toSet())
            assertEquals(assigned,HotspotAssignments.allocate(reports.entries.reversed().associate { it.toPair() }))
            reports.entries.groupBy { it.value }.filterValues { it.size==1 }.forEach { (biome,members) ->
                assertEquals(biome,assigned[members.single().key])
            }
        }
    }
    @Test fun `two second window collects late claims and rotates hotspot wins across runs once`() {
        val wins=mutableMapOf<String,Int>()
        fun round(): Map<String,SafariBiome> {
            val states=listOf("a","b").associateWith { name -> HotspotAssignments(wins.toMutableMap()).apply {
                begin(name,SafariBiome.FOREST,0)
                report("a",SafariBiome.FOREST,100)
                report("b",SafariBiome.FOREST,1800)
            } }
            states.values.forEach { assertNull(it.assignment(states.keys,2099)) }
            val result=states.mapValues { it.value.assignment(states.keys,2100)!! }
            states.forEach { (name,state) -> assertEquals(result[name],state.assignment(states.keys,9000)) }
            result.filterValues { it==SafariBiome.FOREST }.keys.forEach { wins[it]=(wins[it] ?: 0)+1 }
            return result
        }
        val first=round();val second=round()
        assertNotEquals(first["a"],second["a"])
        assertEquals(setOf(1),wins.values.toSet())
    }
    @Test fun `CritterMod reports from JohnFounder log participate before assignment freezes`() {
        val members=setOf("johnfounder","m3fi","_cj","giannaaa")
        val state=HotspotAssignments().apply { begin("JohnFounder",SafariBiome.FOREST,0) }
        val lines=listOf(
            "§9Party §8> §a[VIP§6+§a] JohnFounder§f: [SM] Hotspot: Forest",
            "Party > [MVP++] _cj: [SM] Hotspot: Icy",
            "Party > [MVP+] m3fi: [SM] Hotspot: Forest",
            "§9Party §8> §6[MVP§0++§6] giannaaa§f: My hotspot: Cavern")
        lines.take(3).forEach { line -> val report=HotspotAssignments.parse(line)!!;state.report(report.first,report.second,1000) }
        assertNull(state.assignment(members,3000),"Do not freeze before the delayed CritterMod report")
        val report=HotspotAssignments.parse(lines.last())!!
        assertEquals("giannaaa" to SafariBiome.CAVERN,report)
        state.report(report.first,report.second,4000)
        val assigned=state.assignment(members,4000)
        assertTrue(assigned in setOf(SafariBiome.FOREST,SafariBiome.HAUNTED))
        assertEquals(assigned,state.assignment(members,20000))
    }
    @Test fun `missing announcements have a bounded grace period and common variants remain anchored`() {
        val state=HotspotAssignments().apply {
            begin("Me",SafariBiome.FOREST,0);report("Me",SafariBiome.FOREST,500)
        }
        assertNull(state.assignment(setOf("me","friend"),5499))
        assertEquals(SafariBiome.FOREST,state.assignment(setOf("me","friend"),5500))
        for(body in listOf("My hotspot: Icy","my hotspot: ice!","[CritterMod] My hotspot: Icy Biome","[SafariUtils] Hotspot: Icy"))
            assertEquals("Friend" to SafariBiome.ICY,HotspotAssignments.parse("Party > Friend: $body"),body)
        for(line in listOf("Guild > Friend: My hotspot: Cavern","From Friend: My hotspot: Cavern",
            "Party > Friend: she said My hotspot: Cavern","Party > Friend: My hotspot: Cavern later",
            "Party > Friend: My hotspot: Desert")) assertNull(HotspotAssignments.parse(line),line)
    }
    @Test fun `unique claim beats tally and parsing only accepts actual party reports`() {
        assertEquals(SafariBiome.ICY,HotspotAssignments.allocate(mapOf("a" to SafariBiome.ICY,"b" to SafariBiome.FOREST),wins=mapOf("a" to 100))["a"])
        assertEquals("First" to SafariBiome.ICY,HotspotAssignments.parse("Party > [MVP+] First: [SM] Hotspot: Icy"))
        assertNull(HotspotAssignments.parse("Guild > First: [SM] Hotspot: Icy"))
        assertNull(HotspotAssignments.parse("Party > First: I think [SM] Hotspot: Icy"))
    }
}
