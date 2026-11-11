package net.johnceo.sparklingmutuals.safari
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class HotspotTimingRegressionTest {
 @Test fun `early manual reports accept copied lines abbreviations and typos but reject ambiguous assignments`() {
  val cases=mapOf("fprest" to SafariBiome.FOREST,"for" to SafariBiome.FOREST,"f" to SafariBiome.FOREST,
   "c" to SafariBiome.CAVERN,"cav" to SafariBiome.CAVERN,"i" to SafariBiome.ICY,"icy" to SafariBiome.ICY,
   "ice" to SafariBiome.ICY,"h" to SafariBiome.HAUNTED,"haunt" to SafariBiome.HAUNTED,
   "haunte" to SafariBiome.HAUNTED,"My hotspot today is forest!" to SafariBiome.FOREST,
   "HOTSPOT! Your Hunting Hotspot is the Cavern Biome!" to SafariBiome.CAVERN)
  cases.forEach { (body,biome) ->
   assertEquals(biome,HotspotAssignments.parseReport("Party > Friend: $body",true)?.biome,body)
  }
  assertNull(HotspotAssignments.parseReport("Party > Friend: forest or cavern",true))
  assertNull(HotspotAssignments.parseReport("Party > Leader: Friend you are assigned Icy biome!",true))
  assertNull(HotspotAssignments.parseReport("Guild > Friend: forest",true))
  assertNull(HotspotAssignments.parseReport("Party > Friend: heading to forest",true))
  assertNull(HotspotAssignments.parseReport("Party > Friend: f!",true))
  assertTrue(HotspotAssignments.manualReportWindow(100,30100))
  assertFalse(HotspotAssignments.manualReportWindow(100,30101))
  assertFalse(HotspotAssignments.manualReportWindow(null,100))
 }
 @Test fun `immediate hotspot announcement retries until party submission succeeds`() {
  val a=net.johnceo.sparklingmutuals.safari.resources.StartingResourceAnnouncement(net.johnceo.sparklingmutuals.safari.resources.SafariResources())
  a.observeHotspot("HOTSPOT! Your Hunting Hotspot is the Forest Biome!",0)
  val party=PartySparklingState()
  assertFalse(a.dispatchReliable(0,party,true,false,{ "[SM] Hotspot: $it" }) { false })
  val messages=mutableListOf<String>()
  assertTrue(a.dispatchReliable(1,party,true,false,{ "[SM] Hotspot: $it" }) { messages.add(it);true })
  assertFalse(a.dispatchReliable(2000,party,true,false,{ "[SM] Hotspot: $it" }) { messages.add(it);true })
  assertEquals(listOf("[SM] Hotspot: Forest"),messages)
 }
 @Test fun `four reports assign immediately including delayed manual report`() {
  val a=HotspotAssignments();a.begin("a",SafariBiome.FOREST,0)
  a.report("a",SafariBiome.FOREST,0);a.report("b",SafariBiome.ICY,0);a.report("c",SafariBiome.CAVERN,0)
  assertNull(a.assignment(setOf("a","b","c","d"),29999))
  a.report("d",SafariBiome.HAUNTED,29999)
  assertEquals(SafariBiome.FOREST,a.assignment(setOf("a","b","c","d"),29999))
 }
 @Test fun `missing reports fall back at thirty seconds not five`() {
  val a=HotspotAssignments();a.begin("a",SafariBiome.FOREST,0);a.report("a",SafariBiome.FOREST,0)
  assertNull(a.assignment(setOf("a","b"),29999));assertNotNull(a.assignment(setOf("a","b"),30000))
 }
 @Test fun `manual instruction waits for actual report even after plan locks`() {
  val a=HotspotAssignments();a.begin("a",SafariBiome.FOREST,0);a.report("a",SafariBiome.FOREST,0)
  a.assignment(setOf("a","b"),30000)
  assertEquals(setOf("a"),a.finalPlan!!.keys)
  assertTrue(a.notifications(true,"a").isEmpty());a.report("b",SafariBiome.ICY,45000)
  a.assignment(setOf("a","b"),45249)
  assertTrue(a.notifications(true,"a").isEmpty())
  a.assignment(setOf("a","b"),45250)
  assertEquals(SafariBiome.FOREST,a.finalPlan!!["a"])
  assertEquals(SafariBiome.ICY,a.finalPlan!!["b"])
  assertEquals(1,a.notifications(true,"a").size);assertTrue(a.notifications(true,"a").isEmpty())
 }
}
