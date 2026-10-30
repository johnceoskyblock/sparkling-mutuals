package net.johnceo.sparklingmutuals.safari

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HotspotAssignmentsTest {
    private val ids=mapOf("first" to "00000000-0000-0000-0000-000000000001",
        "second" to "00000000-0000-0000-0000-000000000002",
        "third" to "00000000-0000-0000-0000-000000000003",
        "fourth" to "00000000-0000-0000-0000-000000000004")
    @Test fun `all four player hotspot combinations allocate exactly one biome each and preserve first claimants`() {
        for(a in SafariBiome.entries) for(b in SafariBiome.entries) for(c in SafariBiome.entries) for(d in SafariBiome.entries) {
            val reports=linkedMapOf("first" to a,"second" to b,"third" to c,"fourth" to d)
            val assigned=HotspotAssignments.allocate(reports,ids)
            assertEquals(SafariBiome.entries.toSet(),assigned.values.toSet())
            assertEquals(4,assigned.size)
            reports.entries.groupBy { it.value }.forEach { (biome,claimants) ->
                assertEquals(biome,assigned[claimants.first().key],reports.toString())
            }
            assertEquals(assigned,HotspotAssignments.allocate(reports,ids.mapValues { it.value.replace("-","") }))
        }
    }
    @Test fun `two icy two forest reports rank duplicate reporters by UUID not local random choice`() {
        val reports=linkedMapOf("first" to SafariBiome.ICY,"second" to SafariBiome.FOREST,
            "third" to SafariBiome.ICY,"fourth" to SafariBiome.FOREST)
        val result=HotspotAssignments.allocate(reports,ids)
        assertEquals(setOf(SafariBiome.CAVERN,SafariBiome.HAUNTED),setOf(result["third"],result["fourth"]))
        val swapped=ids+mapOf("third" to ids.getValue("fourth"),"fourth" to ids.getValue("third"))
        val other=HotspotAssignments.allocate(reports,swapped)
        assertEquals(result["third"],other["fourth"])
        assertEquals(result["fourth"],other["third"])
    }
    @Test fun `title waits for reports while nonreporting members cannot block forever and outsiders never claim biomes`() {
        val state=HotspotAssignments()
        state.begin("Third",SafariBiome.ICY,0)
        state.report("Outsider",SafariBiome.HAUNTED,100)
        state.report("first",SafariBiome.ICY,100)
        state.report("second",SafariBiome.FOREST,200)
        state.report("third",SafariBiome.ICY,300)
        state.report("THIRD",SafariBiome.HAUNTED,301) // Duplicate/repeated notices cannot replace the actual report.
        assertNull(state.assignment(ids.keys,3499,ids))
        assertTrue(state.assignment(ids.keys,3500,ids) in setOf(SafariBiome.CAVERN,SafariBiome.HAUNTED))
        state.report("fourth",SafariBiome.FOREST,3600)
        val all=HotspotAssignments.allocate(linkedMapOf("first" to SafariBiome.ICY,"second" to SafariBiome.FOREST,
            "third" to SafariBiome.ICY,"fourth" to SafariBiome.FOREST),ids)
        assertEquals(all["third"],state.assignment(ids.keys,3600,ids))
        assertEquals(all["third"],state.assignment(setOf("third"),60000,ids),"Party changes later in the run do not reassign your biome")
        val solo=HotspotAssignments().apply { begin("Solo",SafariBiome.HAUNTED,0) }
        assertEquals(SafariBiome.HAUNTED,solo.assignment(setOf("Solo"),0))
        val unresolved=HotspotAssignments().apply { begin("Solo",SafariBiome.HAUNTED,0) }
        assertNull(unresolved.assignment(setOf("Solo"),0,expectedSize=4))
        assertEquals(SafariBiome.HAUNTED,unresolved.assignment(setOf("Solo"),3500,expectedSize=4),"Missing cached names must not block the local title forever")
        assertEquals("First" to SafariBiome.ICY,HotspotAssignments.parse("Party > [MVP+] First: [SM] Hotspot: Icy"))
        assertNull(HotspotAssignments.parse("Guild > First: [SM] Hotspot: Icy"))
        assertNull(HotspotAssignments.parse("Party > First: I think [SM] Hotspot: Icy"))
    }
}
