package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CapsulePreviewFrameTest {
    private val eye=ShotVector(0.0,2.0,0.0)
    private val aim=ShotVector(0.0,0.0,1.0)
    @Test fun `available frame contains bounded curve without an impact marker`() {
        val frame=CapsulePreviewFrameBuilder.build(CapsulePreviewAvailability.AVAILABLE,eye,aim)
        assertEquals(64,frame.points.size)
        assertEquals(15.0,frame.points.last().z,1e-12)
        assertEquals("Experimental path",frame.status)
    }
    @Test fun `unavailable frame explains restriction without geometry`() {
        val expected=mapOf(CapsulePreviewAvailability.HIDDEN to null,
            CapsulePreviewAvailability.STAND_STILL to "Stand still for capsule path",
            CapsulePreviewAvailability.TESTED_ANGLE_ONLY to "Look within tested angle",
            CapsulePreviewAvailability.VIEW_UNAVAILABLE to "Capsule path unavailable in this view",
            CapsulePreviewAvailability.HIDING_UNAVAILABLE to "Disable capsule hiding for preview")
        for((availability,message) in expected) {
            val frame=CapsulePreviewFrameBuilder.build(availability,eye,aim)
            assertTrue(frame.points.isEmpty());assertEquals(message,frame.status)
        }
    }
    @Test fun `invalid available geometry falls back quietly`() {
        val frame=CapsulePreviewFrameBuilder.build(CapsulePreviewAvailability.AVAILABLE,eye,ShotVector(0.0,0.0,0.0))
        assertTrue(frame.points.isEmpty());assertEquals("Capsule path unavailable",frame.status)
    }
    @Test fun `world reset and current view invalidate a previously available frame`() {
        val s=CapsulePreviewState(true,true,true,true,true,false,false,false,false,false,false,false,false,false,0.0,eye,ShotVector(0.0,0.0,0.0))
        val r=CapsulePreviewRules();val w=Any();r.tick(w,s);r.tick(w,s)
        assertEquals(64,CapsulePreviewFrameBuilder.build(r.evaluate(s),eye,aim).points.size)
        assertTrue(CapsulePreviewFrameBuilder.build(r.evaluate(s.copy(firstPerson=false)),eye,aim).points.isEmpty())
        r.tick(Any(),s)
        assertTrue(CapsulePreviewFrameBuilder.build(r.evaluate(s),eye,aim).points.isEmpty())
    }
}
