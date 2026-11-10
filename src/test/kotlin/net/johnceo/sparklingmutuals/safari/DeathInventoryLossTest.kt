package net.johnceo.sparklingmutuals.safari
import net.johnceo.sparklingmutuals.safari.resources.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class DeathInventoryLossTest {
 @Test fun `death food loss cancels false feeding waits without inventing spawns`() {
  val check=BirdFoodSparklingCheck();check.floor(0)
  check.inventory(listOf("Bag of Seeds" to 9),0);check.inventory(emptyList(),100)
  assertEquals(9,check.pendingSpawns)
  check.lostOnDeath(9)
  assertEquals(0,check.pendingSpawns)
  assertTrue(check.complete)
  val birds=SafariBirdLedger();birds.lostOnDeath(3)
  assertEquals(6,birds.feedingTarget)
 }
 @Test fun `lost floor coins are not required to spawn a critter`() {
  val check=net.johnceo.sparklingmutuals.safari.completion.GimmiegoldSparklingCheck()
  check.floor(1,false,true);check.inventory(0);check.lostFloorCoins(1)
  assertEquals(0,check.required)
 }
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
