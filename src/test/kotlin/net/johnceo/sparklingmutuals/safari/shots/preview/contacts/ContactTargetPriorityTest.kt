package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContactTargetPriorityTest {
    private fun target(x:Double,z:Double)=ContactTarget("local",1,"Foxtrot",
        ShotBounds(ShotVector(x-.25,-.25,z-.25),ShotVector(x+.25,.25,z+.25)),"client_body_v1")
    private fun curve(vararg p:ShotVector)=ContactCurve(p.mapIndexed{i,v->ContactNode(i.toDouble(),v,false)},ContactStop.RANGE_LIMIT,"fixture")
    @Test fun `ranking uses current curve distance and changes after an aim turn`() {
        val forward=ContactTargetPriority(curve(ShotVector(0.0,0.0,0.0),ShotVector(0.0,0.0,10.0)))
        assertEquals(0.0,forward.score(target(0.0,5.0)),1e-12)
        assertEquals(9.0,forward.score(target(3.0,5.0)),1e-12)
        val turned=ContactTargetPriority(curve(ShotVector(0.0,0.0,0.0),ShotVector(10.0,0.0,0.0)))
        assertTrue(turned.score(target(5.0,0.0))<turned.score(target(0.0,5.0)))
    }
    @Test fun `empty or invalid curve cannot assign a preferred finite score`() {
        assertEquals(Double.POSITIVE_INFINITY,ContactTargetPriority(curve()).score(target(0.0,5.0)))
        assertEquals(Double.POSITIVE_INFINITY,ContactTargetPriority(curve(ShotVector(Double.NaN,0.0,0.0))).score(target(0.0,5.0)))
    }
}
