package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.safari.shots.ShotVariant
import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import net.johnceo.sparklingmutuals.safari.shots.preview.contacts.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CapsulePreviewFrameTest {
    @Test fun `unverified launch conditions dash even the old observation window without moving contact`() {
        val hit=ContactCandidate(ContactKind.CRITTER,1.0,ShotVector(0.0,0.0,1.0),null,"p1","client_body_v1")
        val trace=ContactTrace(listOf(ContactNode(0.0,ShotVector(0.0,0.0,0.0),true),
            ContactNode(1.0,ShotVector(0.0,0.0,1.0),true)),hit,ContactStop.CONTACT,emptySet())
        fun length(frame:CapsulePreviewFrame)=frame.mesh!!.core.sumOf{kotlin.math.sqrt(it.from.distanceSquared(it.to))}
        val original=CapsulePreviewFrameBuilder.contacts(trace,ShotVector(0.0,1.0,0.0),3f)
        assertEquals(1.0,length(original),1e-9)
        for(label in listOf("Masterful","Moving","Masterful / Moving")) {
            val frame=CapsulePreviewFrameBuilder.contacts(trace,ShotVector(0.0,1.0,0.0),3f,label)
            assertEquals(.7,length(frame),1e-9)
            assertEquals(original.points,frame.points)
            assertEquals(original.mesh!!.marker,frame.mesh!!.marker)
            assertTrue(frame.status!!.contains("Uncalibrated"))
        }
        assertTrue(trace.nodes.all{it.observedRegion})
    }
    @Test fun `partial target coverage retains diagnostic provenance for both contact shapes`() {
        for(kind in ContactKind.entries) {
            val hit=ContactCandidate(kind,1.0,ShotVector(0.0,0.0,1.0),ShotVector(0.0,1.0,0.0),null,null)
            val trace=ContactTrace(emptyList(),hit,ContactStop.CONTACT,setOf("target_limit"))
            val frame=CapsulePreviewFrameBuilder.contacts(trace,ShotVector(0.0,1.0,0.0),3f)
            assertTrue(frame.mesh!!.marker.isNotEmpty())
            assertTrue(frame.status!!.contains("Provisional"))
            assertTrue(frame.status.contains("Other critters unverified"))
            assertTrue(frame.status.contains("Uncalibrated"))
        }
    }
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
    @Test fun `world reset and screens invalidate a previously available frame`() {
        val s=CapsulePreviewState(true,true,ShotVariant.ORDINARY,true,true,false,false,false,false,false,false,false,false,false,0.0,eye,ShotVector(0.0,0.0,0.0))
        val r=CapsulePreviewRules();val w=Any();r.tick(w,s);r.tick(w,s)
        assertEquals(64,CapsulePreviewFrameBuilder.build(r.evaluate(s),eye,aim).points.size)
        assertTrue(CapsulePreviewFrameBuilder.build(r.evaluate(s.copy(screenOpen=true)),eye,aim).points.isEmpty())
        r.tick(Any(),s)
        assertTrue(CapsulePreviewFrameBuilder.build(r.evaluate(s),eye,aim).points.isEmpty())
    }
}
