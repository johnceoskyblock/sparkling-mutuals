package net.johnceo.sparklingmutuals.sync
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class HotspotPreferencesTest {
 @Test fun `order must contain every biome exactly once`() {
  assertEquals(listOf("ICY","FOREST","HAUNTED","CAVERN"),HotspotPreferences.parse("icy, forest, haunted, cavern"))
  assertNull(HotspotPreferences.parse(""));assertNull(HotspotPreferences.parse("ICY,ICY,HAUNTED,CAVERN"))
  assertNull(HotspotPreferences.parse("ICY,FOREST,HAUNTED"))
 }
 @Test fun `initial grants retain known zero and exclude floor pickups and death losses`() {
  val r=net.johnceo.sparklingmutuals.safari.resources.SafariResources()
  val coin=net.johnceo.sparklingmutuals.safari.resources.FloorDropType.COIN
  assertNull(r.initialGrant(coin)); r.inventory(emptyList(),true);assertEquals(0,r.initialGrant(coin))
  r.inventory(listOf(coin.label to 2),true);r.lostOnDeath(mapOf(coin.label to 1));assertEquals(2,r.initialGrant(coin))
  r.inventory(listOf(coin.label to 9),false);assertEquals(2,r.initialGrant(coin))
 }
}
