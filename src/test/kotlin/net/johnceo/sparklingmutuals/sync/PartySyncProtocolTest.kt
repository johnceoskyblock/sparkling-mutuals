package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartySyncProtocolTest {
    @Test fun `Hypixel mini lobby IDs match SafariUtils room IDs`() {
        assertEquals("m147AH", PartySyncProtocol.canonicalLobby("mini147AH"))
        assertEquals("m147AH", PartySyncProtocol.canonicalLobby("m147AH"))
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
