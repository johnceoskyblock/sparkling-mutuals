package net.johnceo.sparklingmutuals.safari.integration

import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.esp.EspCaptureMemory
import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class ShotRecordingIsolationTest {
    @Test fun `recorded hit escape share and manual miss preserve capture ledger and existing ESP memory`() {
        val existing=SafariTracking.ledger
        val before=SafariTracking.run
        val memory=EspCaptureMemory();val id=UUID.randomUUID();memory.aimed(id,"Gemzie",0)
        val lines=mutableListOf<String>();val budget=ShotByteBudget()
        val sink=object:ShotSink {
            override fun offer(line:ByteArray):Boolean { lines.add(line.toString(Charsets.UTF_8));budget.release(line.size);return true }
            override fun finish(footer:ByteArray) { lines.add(footer.toString(Charsets.UTF_8));budget.release(65536) }
            override fun queuedBytes()=0
        }
        val s=ShotSession(ShotMetadata("isolation","test","test","test","minecraft:overworld",true,true),ShotLimits(),sink,budget)
        val pose=ShotPose(ShotVector(0.0,0.0,0.0),ShotVector(0.0,0.0,0.0),ShotVector(0.0,0.0,0.0),0f,0f,ShotVector(0.0,0.0,1.0),ShotVector(0.0,0.0,0.0))
        for((index,hand) in ShotHand.entries.withIndex()) s.accept(ShotStamp(0,0),ShotObservation.Input("s$index",hand,ShotVariant.entries[index],ShotUseRoute.AIR,pose))
        listOf("You threw a Critter Capsule at the Gemzie!","The Gemzie escaped your Critter Capsule!",
            "LOOT SHARE! You received loot from PrivateName catching a Gemzie!","The Shyworm fled because it was startled!")
            .forEachIndexed { index,text -> s.accept(ShotStamp(1,0),ShotObservation.Feedback(ShotFeedback.parse(text)!!,index.toLong())) }
        s.accept(ShotStamp(2,0),ShotObservation.Annotation("s0",ShotManualOutcome.MISS));s.stop(ShotStamp(3,0),ShotStopReason.USER_STOP)
        assertSame(existing,SafariTracking.ledger);assertSame(before,SafariTracking.run)
        assertFalse(memory.hidden(id));assertEquals(id,memory.escaped("Gemzie",4))
        val encoded=lines.joinToString("")
        assertFalse(encoded.contains("PrivateName"));assertFalse(encoded.contains(id.toString()));assertFalse(encoded.contains(".devAuth"))
        assertTrue(encoded.contains("\"manual\"").not());assertTrue(encoded.contains("\"annotation\""))
    }
}
