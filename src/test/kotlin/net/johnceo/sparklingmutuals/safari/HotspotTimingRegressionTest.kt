package net.johnceo.sparklingmutuals.safari
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class HotspotTimingRegressionTest {
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
  assertTrue(a.notifications(true,"a").isEmpty());a.report("b",SafariBiome.ICY,45000)
  assertEquals(1,a.notifications(true,"a").size);assertTrue(a.notifications(true,"a").isEmpty())
 }
}
