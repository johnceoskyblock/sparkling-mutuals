package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartySyncProtocolTest {
    @Test fun `retained inventory keeps source facts without stale holdings or feeder occupancy`() {
        val state=PartySyncState(self,setOf(self,peer))
        val fresh=aggregate().apply { this[10]=2;this[17]=0;this[18]=2;this[23]=1 }
        assertTrue(state.receiveOrdered(peer,"A",fresh.joinToString(","),true))
        state.invalidateInventory()
        assertTrue(state.receiveOrdered(peer,"A",fresh.joinToString(","),false))
        assertTrue(state.inventories.isEmpty())
        assertEquals(1,state.facts[23])
        assertEquals(0,state.usage!![5])
        assertTrue(state.receiveOrdered(peer,"A",fresh.joinToString(","),true))
        assertEquals(2,state.inventories.getValue(peer)[10])
        assertEquals(2,state.usage!![5])
    }
    @Test fun `newer death loss snapshot replaces owned inventory and cannot be undone by stale packets`() {
        val state=PartySyncState(self,setOf(self,peer))
        val before=aggregate().apply { this[0]=1;this[19]=1 }
        val after=aggregate()
        assertTrue(state.receive(peer,1,"A",before.joinToString(",")))
        assertTrue(state.receive(peer,2,"A",after.joinToString(",")))
        assertEquals(0,state.inventories[peer]!![0]);assertEquals(0,state.inventories[peer]!![19])
        assertFalse(state.receive(peer,1,"A",before.joinToString(",")))
        assertEquals(0,state.inventories[peer]!![19])
    }
    @Test fun `Hypixel mini lobby IDs match SafariUtils room IDs`() {
        assertEquals("m147ah", PartySyncProtocol.canonicalLobby("mini147AH"))
        assertEquals("m147ah", PartySyncProtocol.canonicalLobby("m147AH"))
        assertEquals("m147ah", PartySyncProtocol.canonicalLobby("MINI147ah"))
        assertEquals("mini-test", PartySyncProtocol.canonicalLobby("mini-test"))
        assertNull(PartySyncProtocol.canonicalLobby("mini147AH\n"))
    }
    private val self="0".repeat(31)+"1"
    private val peer="0".repeat(31)+"2"
    private val third="0".repeat(31)+"3"
    private fun objectOf(text: String)=JsonParser.parseString(text).asJsonObject
    private fun aggregate()=IntArray(38).apply { this[17]=-1;this[33]=2 }
    private fun snapshot(revision: Int=1,run: String="visit",assignments: String="\"$self\":\"FOREST\",\"$peer\":\"CAVERN\"")=objectOf(
        """{"version":1,"run":"$run","revision":$revision,"assignments":{$assignments},"fairness":{"$self":2},"leader":"$self","notifications":[],"initialDone":true,"conflict":false}""")
    @Test fun `welcome follows submitted roster order and rejects wrong identity or duplicate tags`() {
        val roster=listOf(peer,self)
        assertEquals(mapOf("b" to peer,"a" to self),PartySyncProtocol.welcome(objectOf("""{"self":"a","roster":["b","a"]}"""),self,roster))
        assertNull(PartySyncProtocol.welcome(objectOf("""{"self":"b","roster":["b","a"]}"""),self,roster))
        assertNull(PartySyncProtocol.welcome(objectOf("""{"self":"a","roster":["a","a"]}"""),self,roster))
    }
    @Test fun `all numeric fields have exact sizes integer types ranges and versions`() {
        assertNotNull(PartySyncProtocol.numbers("A",aggregate().joinToString(",")))
        assertNull(PartySyncProtocol.numbers("A",aggregate().apply { this[33]=1 }.joinToString(",")))
        assertNull(PartySyncProtocol.numbers("A",aggregate().apply { this[22]=65 }.joinToString(",")))
        assertNull(PartySyncProtocol.numbers("N","8192"))
        assertNull(PartySyncProtocol.numbers("S","0,128,0,0"))
        assertNull(PartySyncProtocol.numbers("U","0,0,0,0,-2,0"))
        assertNull(PartySyncProtocol.numbers("N","1.5"))
        assertNull(PartySyncProtocol.integer(JsonParser.parseString("1.5")))
        assertNull(PartySyncProtocol.integer(JsonParser.parseString("\"1\"")))
    }
    @Test fun `held coins replace prior counts and legacy aggregates clear the known value`() {
        val state = PartySyncState(self, setOf(self, peer))
        fun payload(coins: Int) = aggregate().copyOf(39).apply {
            this[4] = 8
            this[33] = 3
            this[38] = coins
        }.joinToString(",")
        listOf(3, 1, 0).forEachIndexed { index, coins ->
            assertTrue(state.receive(peer, index + 1L, "A", payload(coins)))
            assertEquals(coins, state.inventories.getValue(peer)[38])
        }
        assertFalse(state.receive(peer, 2, "A", payload(1)))
        assertTrue(state.receive(peer, 4, "A", aggregate().joinToString(",")))
        assertNull(state.inventories.getValue(peer).getOrNull(38))
        assertNull(PartySyncProtocol.numbers("A", payload(65)))
        assertNull(PartySyncProtocol.numbers("A", payload(-1)))
        assertNull(PartySyncProtocol.numbers("A", payload(3).substringBeforeLast(',')))
        assertNull(PartySyncProtocol.numbers("A", aggregate().joinToString(",") + ",0"))
    }
    @Test fun `out of order kinds retain monotonic facts but cannot replace newer inventory`() {
        val state=PartySyncState(self,setOf(self,peer))
        val newer=aggregate().apply { this[10]=3;this[23]=1 }
        val older=aggregate().apply { this[10]=7;this[23]=2 }
        assertTrue(state.receive(peer,5,"A",newer.joinToString(",")))
        assertTrue(state.receive(peer,3,"N","4"))
        assertTrue(state.receive(peer,4,"A",older.joinToString(",")))
        assertEquals(3,state.inventories[peer]!![10]);assertEquals(3,state.facts[23]);assertEquals(4,state.nestMask)
        assertFalse(state.receive(peer,3,"N","4"))
        assertFalse(state.receive(third,6,"N","1"))
        assertFalse(state.receive(self,6,"N","1"))
        assertEquals(1,state.inventories.size) // No dummy inventory for peers that only sent masks.
    }
    @Test fun `feeder generations cannot resurrect caught birds or regress to old usage`() {
        val state=PartySyncState(self,setOf(self,peer))
        assertTrue(state.receive(peer,1,"U","4,2,1,0,0,2"))
        assertTrue(state.receive(peer,2,"U","4,0,1,0,0,2"))
        assertTrue(state.receive(peer,3,"U","3,9,9,9,1,4"))
        assertEquals(listOf(4,0,1,0,0,2),state.usage!!.toList())
        assertTrue(state.receive(peer,4,"U","5,2,0,0,-1,0"))
        assertEquals(listOf(5,2,0,0,-1,0),state.usage!!.toList())
    }
    @Test fun `unobserved empty feeder does not erase another reporter's fill`() {
        val state = PartySyncState(self, setOf(self, peer, third))
        state.receive(peer, 1, "U", "4,0,0,0,0,2")
        state.receive(third, 1, "A", aggregate().apply { this[13] = 4 }.joinToString(","))
        assertEquals(2, state.usage!![5])
        state.receive(peer, 2, "A", aggregate().apply { this[13] = 4 }.joinToString(","))
        assertEquals(0, state.usage!![5])
        state.receive(peer, 3, "U", "4,0,0,0,0,1")
        state.receive(third, 2, "U", "5,0,0,0,-1,0")
        state.receive(peer, 4, "U", "4,0,0,0,0,1")
        assertEquals(listOf(5,0,0,0,-1,0), state.usage!!.toList())
    }
    @Test fun `atomic room catchup masks caught species and stale updates cannot revive them`() {
        val state = PartySyncState(self, setOf(self, peer))
        fun activeLow() = state.sparkling[0] and state.sparkling[2].inv()
        assertTrue(state.receiveRoomSparklings("3,0,1,0"))
        assertEquals(2, activeLow())
        assertTrue(state.receiveRoomSparklings("3,0,0,0"))
        assertEquals(2, activeLow())
        assertFalse(state.receiveRoomSparklings("3,128,3,0"))
        assertEquals(2, activeLow())
        assertTrue(state.receiveRoomSparklings("3,0,3,0"))
        assertEquals(0, activeLow())
        assertEquals(0L, state.sequence)
    }
    @Test fun `snapshots preserve omitted locks and refuse reshuffles and unexpected visit generation`() {
        val state=PartySyncState(self,setOf(self,peer))
        assertTrue(state.hotspots(snapshot()))
        assertTrue(state.authority)
        assertFalse(state.hotspots(snapshot()))
        assertTrue(state.hotspots(snapshot(2,assignments="\"$peer\":\"CAVERN\"")))
        assertEquals(SafariBiome.FOREST,state.locks[self])
        assertFalse(state.hotspots(snapshot(3,assignments="\"$peer\":\"FOREST\"")))
        assertTrue(state.conflict)
        assertFalse(state.hotspots(snapshot(4,"other")))
        assertEquals("visit",state.serverRun)
    }
    @Test fun `leader can derive non-mod instructions without backend notifications`() {
        val state=PartySyncState(self,setOf(self,peer,third))
        assertTrue(state.hotspots(snapshot(assignments="\"$self\":\"FOREST\",\"$peer\":\"CAVERN\",\"$third\":\"ICY\"")))
        assertTrue(state.notifications.isEmpty())
        assertTrue(state.assignmentInstructions(1000).isEmpty())
        assertTrue(state.assignmentInstructions(2999).isEmpty())
        // A valid relay identifies a shared client even without an SM/SU chat announcement.
        assertTrue(state.receive(peer,1,"N","0"))
        assertEquals(listOf(third),state.assignmentInstructions(3000).map { it.player })
        state.markInstructed(third,SafariBiome.ICY)
        assertTrue(state.assignmentInstructions(4000).isEmpty())
        state.invalidateInventory() // A reconnect must not make a known client look non-modded.
        assertTrue(state.assignmentInstructions(5000).isEmpty())
    }
    @Test fun `mod chat evidence and conflict prevent non-mod instructions`() {
        val state=PartySyncState(self,setOf(self,peer))
        state.assignmentClient(peer)
        assertTrue(state.hotspots(snapshot()))
        assertTrue(state.assignmentInstructions(0).isEmpty())
        assertTrue(state.assignmentInstructions(2000).isEmpty())
        val nonMod=PartySyncState(self,setOf(self,peer))
        nonMod.hotspots(snapshot());nonMod.assignmentInstructions(0)
        assertEquals(1,nonMod.assignmentInstructions(2000).size)
        nonMod.importLocks(mapOf(peer to SafariBiome.FOREST))
        assertTrue(nonMod.assignmentInstructions(3000).isEmpty())
    }
    @Test fun `notification grace refresh at same revision is accepted without changing assignments`() {
        val state=PartySyncState(self,setOf(self,peer))
        val initial=snapshot(assignments="\"$self\":\"FOREST\",\"$peer\":\"CAVERN\"")
        assertTrue(state.hotspots(initial))
        val refreshed=initial.deepCopy().apply {
            add("notifications",JsonParser.parseString("""[{"id":"visit:$peer","player":"$peer","biome":"CAVERN"}]"""))
        }
        assertTrue(state.hotspots(refreshed))
        assertEquals(peer,state.notifications.single().player)
        assertFalse(state.hotspots(refreshed))
        assertFalse(state.hotspots(refreshed.deepCopy().apply { addProperty("revision",0) }))
        assertFalse(state.hotspots(initial.deepCopy().apply { getAsJsonObject("fairness").addProperty(self,3) }))
        assertEquals(2,state.hotspotWins[self])
        assertFalse(state.hotspots(initial.deepCopy().apply { addProperty("leader",peer) }))
        assertEquals(self,state.leader)
        assertTrue(state.hotspots(initial)) // Acknowledged/withdrawn notifications can also refresh.
        assertTrue(state.notifications.isEmpty())
    }
    @Test fun `fallback locks recover without changing displayed choices and invalid payloads are atomic`() {
        val state=PartySyncState(self,setOf(self,peer))
        assertTrue(state.importLocks(mapOf(self to SafariBiome.FOREST)))
        val invalid=snapshot().apply { addProperty("revision",1.5) }
        assertFalse(state.hotspots(invalid));assertFalse(state.authority)
        assertEquals(mapOf(self to SafariBiome.FOREST),state.locks)
        assertTrue(state.hotspots(snapshot()))
        val n=snapshot(2).apply { add("notifications",JsonParser.parseString("""[{"id":"wrong","player":"$peer","biome":"CAVERN"}]""")) }
        assertFalse(state.hotspots(n));assertTrue(state.notifications.isEmpty())
    }
    @Test fun `visit sequence survives roster expansion and retained reports are independent of a run ledger`() {
        val state=PartySyncState(self,setOf(self,peer))
        assertEquals(1L,state.nextSequence());state.expand(setOf(self,peer,third));assertEquals(2L,state.nextSequence())
        assertTrue(state.receive(peer,9,"B","fd"));assertEquals(setOf(peer to "fd"),state.biomeReports)
        assertTrue(state.receive(self,1,"B","fd"))
        assertEquals(peer to "fd", state.biomeReports.first())
        assertFalse(state.receive(self,1,"B","fd"))
        assertFalse(state.receive(peer,10,"B","my fd"));assertFalse(state.receive(peer,11,"B","FD"))
        assertTrue(PartySyncState(self,setOf(self,peer)).biomeReports.isEmpty())
    }
    @Test fun `catchup hints are aggregate only and sparkling masks never mutate local identities`() {
        val envelope=objectOf(PartySyncProtocol.envelope(1,"A",aggregate().joinToString(","),true))
        assertTrue(envelope.get("catchUpOnly").asBoolean)
        assertFalse(objectOf(PartySyncProtocol.envelope(2,"N","0",true)).has("catchUpOnly"))
        val state=PartySyncState(self,setOf(self,peer))
        assertTrue(state.receive(peer,4,"S","1,0,0,0"))
        assertTrue(state.receive(peer,5,"S","0,0,1,0"))
        assertEquals(listOf(1,0,1,0),state.sparkling.toList())
        val local=net.johnceo.sparklingmutuals.safari.sparkling.SparklingCaptureState()
        local.observe(java.util.UUID.randomUUID(),"Cavernfish")
        assertTrue(local.pending("Cavernfish"));assertFalse(local.captured("Cavernfish"))
    }
}
