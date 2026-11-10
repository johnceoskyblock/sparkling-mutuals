package net.johnceo.sparklingmutuals.safari

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HotspotProtocolTest {
    private val reports=linkedMapOf("johnfounder" to SafariBiome.HAUNTED,"metalhat46" to SafariBiome.CAVERN,
        "m3fi" to SafariBiome.CAVERN,"pyruuuu" to SafariBiome.FOREST)
    private fun state(self: String,wins: MutableMap<String,Int>,missForest: Boolean=false)=HotspotAssignments(wins).apply {
        begin(self,reports.getValue(self),0)
        reports.entries.filter { !missForest || it.key!="pyruuuu" }.forEachIndexed { i,(name,biome) ->
            report(name,biome,i*100L,name!="pyruuuu")
        }
    }

    @Test fun `leader messages round trip on both prefixes without becoming manual hotspot or assignment instructions`() {
        val counts=reports.keys.associateWith { 0 }
        val message=HotspotProtocol.winsMessage(SafariBiome.HAUNTED,counts,mapOf("johnfounder" to "JohnFounder"))
        for(prefix in listOf("SM","SU")) {
            val raw="Party > [VIP+] JohnFounder: "+message.replace("[SM]","[$prefix]")
            assertEquals(counts,HotspotProtocol.parseWins(raw)!!.counts)
            assertEquals("JohnFounder" to SafariBiome.HAUNTED,HotspotAssignments.parse(raw))
            assertTrue(HotspotAssignments.parseReport(raw)!!.coordinated)
            val final="Party > JohnFounder: "+HotspotProtocol.assignmentMessage(reports).replace("[SM]","[$prefix]")
            // Duplicate Cavern assignments are deliberately invalid.
            assertNull(HotspotProtocol.parseAssignment(final))
        }
        val maxNames=(1..4).associate { "PlayerNameMax000$it" to 1_000_000 }
        assertTrue(HotspotProtocol.winsMessage(SafariBiome.HAUNTED,maxNames).length<256)
        for(body in listOf("[SM] Hotspot: Forest | Hotspot wins: a=-1","[SM] Hotspot: Forest | Hotspot wins: a=2, A=3",
            "[SM] Hotspot: Forest | Hotspot wins: a=1000001","[SM] Biomes: a=Forest, b=Forest",
            "[SM] Biomes: a=Forest, A=Icy","[SM] Biomes: a=Desert","[SM] Biomes: a=Forest later")) {
            assertNull(HotspotProtocol.parseWins("Party > Leader: $body"))
            assertNull(HotspotProtocol.parseAssignment("Party > Leader: $body"))
        }
        assertNull(HotspotProtocol.parseWins("Guild > Leader: $message"))
    }

    @Test fun `leader snapshot is pre run and all clients follow final map despite missed reports and different histories`() {
        val leaderWins=mutableMapOf("metalhat46" to 1)
        val leader=state("johnfounder",leaderWins)
        val before=leader.snapshot(reports.keys)
        assertEquals(1,before["metalhat46"])
        assertEquals(0,before["m3fi"])
        assertEquals(SafariBiome.HAUNTED,leader.assignment(reports.keys,2200))
        assertEquals(1,leaderWins["m3fi"])
        assertEquals(before,leader.snapshot(reports.keys))
        val snapshot=HotspotProtocol.parseWins("Party > JohnFounder: ${HotspotProtocol.winsMessage(SafariBiome.HAUNTED,before)}")!!
        val plan=HotspotProtocol.parseAssignment("Party > JohnFounder: ${HotspotProtocol.assignmentMessage(leader.finalPlan!!)}")!!
        assertEquals(SafariBiome.FOREST,plan.biomes["pyruuuu"])
        assertEquals(SafariBiome.ICY,plan.biomes["metalhat46"])
        for(self in reports.keys) {
            val wins=mutableMapOf("m3fi" to 10)
            val follower=state(self,wins,missForest=true)
            follower.assignment(reports.keys,2200)
            assertTrue(follower.acceptWins(snapshot,"johnfounder",reports.keys,2300))
            assertEquals(before,wins)
            assertNull(follower.assignment(reports.keys,2400),"Wait for the leader rather than publishing a different local result")
            assertTrue(follower.acceptAssignment(plan,"johnfounder",reports.keys,2500))
            assertEquals(plan.biomes[self],follower.assignment(reports.keys,2600))
            assertEquals(plan.biomes,follower.finalPlan)
            val after=wins.toMap()
            assertFalse(follower.acceptAssignment(plan,"johnfounder",reports.keys,2700))
            assertFalse(follower.acceptWins(snapshot,"johnfounder",reports.keys,2700))
            assertEquals(after,wins,"Repeated server echoes must not credit wins twice")
        }
    }

    @Test fun `only current leader full fresh roster messages can change assignments and final works without snapshot`() {
        val state=state("metalhat46",mutableMapOf())
        val plan=HotspotProtocol.Assignment("JohnFounder",mapOf("johnfounder" to SafariBiome.HAUNTED,
            "metalhat46" to SafariBiome.ICY,"m3fi" to SafariBiome.CAVERN,"pyruuuu" to SafariBiome.FOREST))
        val snapshot=HotspotProtocol.Wins("JohnFounder",SafariBiome.HAUNTED,reports.keys.associateWith { 1 })
        assertFalse(state.acceptWins(snapshot.copy(sender="m3fi"),"johnfounder",reports.keys,1000))
        assertFalse(state.acceptWins(snapshot.copy(counts=snapshot.counts-"m3fi"),"johnfounder",reports.keys,1000))
        assertFalse(state.acceptWins(snapshot,"johnfounder",reports.keys,60001))
        assertFalse(state.acceptAssignment(plan.copy(sender="m3fi"),"johnfounder",reports.keys,2300))
        assertFalse(state.acceptAssignment(plan.copy(biomes=plan.biomes-"pyruuuu"),"johnfounder",reports.keys,2300))
        assertFalse(state.acceptAssignment(plan.copy(biomes=plan.biomes+("pyruuuu" to SafariBiome.ICY)),"johnfounder",reports.keys,2300))
        assertFalse(state.acceptAssignment(plan,"johnfounder",reports.keys,60001))
        assertFalse(state.acceptAssignment(plan,null,reports.keys,2300))
        assertTrue(state.acceptAssignment(plan,"johnfounder",reports.keys,2300))
        assertEquals(SafariBiome.ICY,state.assignment(reports.keys,2400))
    }
}
