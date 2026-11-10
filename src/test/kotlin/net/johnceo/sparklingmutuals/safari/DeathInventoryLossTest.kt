package net.johnceo.sparklingmutuals.safari
import net.johnceo.sparklingmutuals.safari.resources.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class DeathInventoryLossTest {
 @Test fun `death waits for stable respawn inventory and counts only actually lost items`() {
  val d=DeathInventoryLoss()
  d.observe(true,listOf("Lime Gem" to 1,"Coin" to 2),0)
  assertTrue(d.observe(false,emptyList(),10).isEmpty())
  assertTrue(d.observe(true,emptyList(),20).isEmpty())
  assertTrue(d.observe(true,listOf("Coin" to 2),100).isEmpty())
  assertEquals(mapOf("Lime Gem" to 1),d.observe(true,listOf("Coin" to 2),1100))
  assertTrue(d.observe(true,listOf("Coin" to 2),1200).isEmpty())
 }
 @Test fun `ordinary consumption is not treated as death loss`() {
  val d=DeathInventoryLoss();d.observe(true,listOf("Lime Gem" to 1),0)
  assertTrue(d.observe(true,emptyList(),2000).isEmpty())
 }
 @Test fun `lost starting items no longer contribute but other discoveries remain`() {
  val r=SafariResources();r.inventory(listOf("Lime Gem" to 1),true)
  r.lostOnDeath(mapOf("Lime Gem" to 1))
  assertEquals(0,r.discoveredCount(FloorDropType.LIME));assertEquals(0,r.heldCount(FloorDropType.LIME))
 }
}
