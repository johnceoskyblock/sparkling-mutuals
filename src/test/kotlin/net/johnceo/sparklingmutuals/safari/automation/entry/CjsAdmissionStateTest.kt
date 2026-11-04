package net.johnceo.sparklingmutuals.safari.automation.entry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CjsAdmissionStateTest {
    @Test fun `local source removed between end scan and next input cannot move`() {
        val session=EntryDryRunSession();val frame=EntryDryRunFrame(EntryFixtures.context(),true,true,false,0)
        session.scan(frame,listOf(EntryDryRunObservation(EntryFixtures.self,"Bluebird")),0)
        assertNotNull(session.fresh(frame,10,setOf(EntryFixtures.self)))
        assertNull(session.fresh(frame,20,emptySet()))
    }
    @Test fun `Critter Safari arrival remains query eligible until personal ticket acceptance`() {
        val state = CjsAdmissionState(); state.world(Any())
        state.chat("ᛜ Self joined the Critter Safari.")
        state.chat("Party > Leader: HOTSPOT! Your Hunting Hotspot is the Forest Biome!")
        state.chat("SPARKLING! Leader caught a SPARKLING Flitter!")
        assertFalse(state.admitted)
        val guest = EntryFixtures.context()
        val frame = state.frame(guest, true, 0, true, 1000)
        assertTrue(frame.atEntry); assertFalse(frame.activeRun)
        val session = EntryDryRunSession()
        val q = session.tick(frame, 1340).filterIsInstance<EntryDryRunOutput.Party>().single().message as EntryMessage.Query
        session.receive(frame, EntryPeer(EntryFixtures.leader, "Leader"), EntryFixtures.live(q.nonce), 1400)
        assertNotNull(session.fresh(frame, 1401, emptySet()))
        state.chat("[NPC] Safari Manager: §fLooks good to me. Have fun out there!")
        val admitted = state.frame(guest, true, 0, true, 1000)
        assertTrue(admitted.activeRun); assertFalse(admitted.atEntry)
        assertNull(session.fresh(admitted, 1402, emptySet()))
    }
    @Test fun `admission is bound to world and exact personal evidence`() {
        val s = CjsAdmissionState(); val world = Any(); s.world(world)
        s.chat("HOTSPOT! Your Hunting Hotspot is the Cavern Biome!")
        assertTrue(s.admitted); s.world(world); assertTrue(s.admitted)
        s.world(Any()); assertFalse(s.admitted)
        s.chat("Party > Friend: [NPC] Safari Manager: Looks good to me. Have fun out there!")
        s.chat("[MVP++] Leader entered Critter Safari!"); assertFalse(s.admitted)
        s.chat("[NPC] Safari Manager: I already saw your ticket, so you're free to go.")
        assertTrue(s.admitted)
        s.world(null); s.chat("HOTSPOT! Your Hunting Hotspot is the Forest Biome!"); assertFalse(s.admitted)
    }
    @Test fun `leader can serve sightings while general island presence cannot admit a guest`() {
        val s = CjsAdmissionState(); s.world(Any())
        assertTrue(s.frame(EntryFixtures.host(), true, 2, true, 0).activeRun)
        assertFalse(s.frame(EntryFixtures.context(), true, 2, true, 0).activeRun)
        assertFalse(s.frame(null, true, 0, true, 0).atEntry)
    }
}
