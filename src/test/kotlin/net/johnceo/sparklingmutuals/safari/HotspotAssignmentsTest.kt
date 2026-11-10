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
    @Test fun `complete reports assign immediately and rotates hotspot wins across runs once`() {
        val wins=mutableMapOf<String,Int>()
        fun round(): Map<String,SafariBiome> {
            val states=listOf("a","b").associateWith { name -> HotspotAssignments(wins.toMutableMap()).apply {
                begin(name,SafariBiome.FOREST,0)
                report("a",SafariBiome.FOREST,100)
                report("b",SafariBiome.FOREST,1800)
            } }
            states.values.forEach { assertNotNull(it.assignment(states.keys,1800)) }
            val result=states.mapValues { it.value.assignment(states.keys,3800)!! }
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
        assertNull(state.assignment(members,2999))
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
        assertNull(state.assignment(setOf("me","friend"),30499))
        assertEquals(SafariBiome.FOREST,state.assignment(setOf("me","friend"),30500))
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
    @Test fun `missing report waits thirty seconds and leader instructs only reported manual members`() {
        val state=HotspotAssignments().apply {
            begin("a",SafariBiome.FOREST,0)
            report("a",SafariBiome.FOREST,0,true)
            report("b",SafariBiome.FOREST,100,true)
            report("c",SafariBiome.CAVERN,500)
            report("c",SafariBiome.HAUNTED,1000)
        }
        val members=setOf("a","b","c","d")
        assertNull(state.assignment(members,29999))
        assertNotNull(state.assignment(members,30000))
        assertTrue(state.notifications(false,"a").isEmpty())
        val messages=state.notifications(true,"a")
        assertEquals(setOf("c"),messages.map { it.first }.toSet())
        assertEquals(1,messages.map { it.second }.toSet().size)
        assertTrue(state.notifications(true,"a").isEmpty())
        state.report("d",SafariBiome.ICY,31000,true)
        assertTrue(state.notifications(true,"a").isEmpty())
    }
    @Test fun `manual biome abbreviations and typos work without matching ordinary conversations or assignment replies`() {
        for(body in listOf("haunt","haunted","hvant","my hotspot is haunted","[SU] Haunted Hotspot!"))
            assertEquals(SafariBiome.HAUNTED,HotspotAssignments.parseReport("Party > Friend: $body")?.biome,body)
        assertTrue(HotspotAssignments.parseReport("Party > Friend: [SU] Hotspot: Icy")!!.coordinated)
        assertFalse(HotspotAssignments.parseReport("Party > Friend: haunt")!!.coordinated)
        for(body in listOf("Friend you are assigned Haunted biome!","forest or icy","haunted done","!m cavern","hello"))
            assertNull(HotspotAssignments.parseReport("Party > Friend: $body"),body)
    }
    @Test fun `JohnFounder log replay includes manual Forest claim on every updated client and only leader messages Pyruuuu`() {
        val lines=listOf("Party > JohnFounder: [SM] Hotspot: Haunted",
            "Party > metalhat46: [SM] Hotspot: Cavern","Party > m3fi: [SM] Hotspot: Cavern",
            "Party > Pyruuuu: forest")
        val reports=lines.map { HotspotAssignments.parseReport(it)!! }
        val members=reports.map { it.sender.lowercase() }.toSet()
        // The lower Cavern tally wins it; the other runner must not steal the sole Forest claim.
        val assignments=reports.associate { self ->
            val state=HotspotAssignments(mutableMapOf("metalhat46" to 1)).apply {
                begin(self.sender,self.biome,0)
                reports.forEachIndexed { i,report -> report(report.sender,report.biome,i*100L,report.coordinated) }
            }
            assertNotNull(state.assignment(members,300))
            val biome=state.assignment(members,2200)!!
            if(self.sender=="JohnFounder") assertEquals(listOf("pyruuuu" to SafariBiome.FOREST),state.notifications(true,self.sender))
            else assertTrue(state.notifications(false,self.sender).isEmpty())
            self.sender.lowercase() to biome
        }
        assertEquals(SafariBiome.HAUNTED,assignments["johnfounder"])
        assertEquals(SafariBiome.CAVERN,assignments["m3fi"])
        assertEquals(SafariBiome.FOREST,assignments["pyruuuu"])
        assertEquals(SafariBiome.ICY,assignments["metalhat46"])
        assertEquals(4,assignments.values.toSet().size)
    }
}
