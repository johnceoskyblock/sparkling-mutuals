package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonParser
import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartySyncBootstrapTest {
    private val a="1".repeat(32)
    private val b="2".repeat(32)
    private fun snapshot(run: String, revision: Int=1) = JsonParser.parseString("""{"version":1,"run":"$run","revision":$revision,"assignments":{"$a":"FOREST"},"fairness":{"$a":2},"leader":"$a","notifications":[],"conflict":false,"initialDone":true}""").asJsonObject
    @Test fun `migration retains original facts and locks but revokes current peer inventory`() {
        val s=PartySyncState(a,setOf(a,b))
        val values=IntArray(39).apply {this[0]=5;this[17]=-1;this[23]=2;this[33]=3;this[38]=4}
        s.receive(b,1,"A",values.joinToString(","));s.receive(b,2,"N","4");s.receive(b,3,"S","1,0,1,0")
        assertTrue(s.hotspots(snapshot("primary")));val sequence=s.nextSequence()
        assertTrue(s.transitionAuthority("backup","generation"));assertFalse(s.inventoriesCurrent);assertTrue(s.inventories.isEmpty())
        assertEquals(SafariBiome.FOREST,s.locks[a]);assertEquals(sequence,s.sequence)
        val facts=s.bootstrapFacts().single {it.get("reporter").asString==b}
        assertEquals(4,facts.get("nests").asInt);assertEquals(2,facts.getAsJsonArray("facts")[23].asInt);assertEquals(0,facts.getAsJsonArray("facts")[0].asInt)
        assertEquals(1,facts.getAsJsonArray("caught")[0].asInt);assertFalse(facts.has("found"))
        assertTrue(s.hotspots(snapshot("backup")))
        assertFalse(s.transitionAuthority("backup","different"));assertFalse(s.hotspots(snapshot("rogue",2)))
    }
    @Test fun `bootstrap never imports another players current holdings`() {
        val s=PartySyncState(a,setOf(a,b));val good=JsonParser.parseString("""{"reporter":"$b","nests":2,"facts":[${List(38){0}.joinToString(",")}],"caught":[1,0]}""").asJsonObject
        assertTrue(s.importBootstrap(good));assertEquals(2,s.nestMask);assertEquals(1,s.sparkling[2]);assertTrue(s.inventories.isEmpty())
        good.getAsJsonArray("facts").set(0,com.google.gson.JsonPrimitive(2));assertFalse(s.importBootstrap(good))
    }
    @Test fun `instruction target survives a backend-specific notification id`() {
        val s=PartySyncState(a,setOf(a,b));s.markInstructed(a,SafariBiome.FOREST)
        assertTrue(s.wasInstructed(a,SafariBiome.FOREST));assertFalse(s.wasInstructed(a,SafariBiome.CAVERN))
        s.transitionAuthority("backup","generation");assertTrue(s.wasInstructed(a,SafariBiome.FOREST))
    }
}
