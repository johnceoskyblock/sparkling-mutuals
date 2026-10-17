package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CapsuleContactSolverTest {
    private fun v(z:Double)=ShotVector(0.0,0.0,z)
    private fun box(z:Double)=ShotBounds(ShotVector(-1.0,-1.0,z),ShotVector(1.0,1.0,z+.1))
    private fun target(z:Double)=ContactTarget("local",1,"Foxtrot",box(z),"client_body_v1")
    private val coverage=ContactCoverage(true,emptySet())
    private val curve=ContactCurveSampler.sample(v(0.0),v(1.0),0.0)
    private fun wall(z:Double):(ContactNode,ContactNode)->ContactSegmentScene = {a,b->
        val f=ContactIntersections.entry(a.position,b.position,box(z))
        val hit=f?.let{ ContactCandidate(ContactKind.TERRAIN,a.parameter+(b.parameter-a.parameter)*it,
            ShotVector(0.0,a.position.y+(b.position.y-a.position.y)*it,z),v(-1.0),null,null) }
        ContactSegmentScene(hit,null)
    }
    @Test fun prefixWallPrecedesCritter() {
        val t=CapsuleContactSolver.trace(curve,listOf(target(2.0)),coverage,wall(.5))
        assertEquals(ContactKind.TERRAIN,t.primary!!.kind)
        assertEquals(.5,t.primary.position.z,1e-12)
        assertTrue(t.primary.parameter<2)
        assertEquals(t.primary.position,t.nodes.last().position)
    }
    @Test fun `target in front of wall wins and inside origin is immediate`() {
        val t=CapsuleContactSolver.trace(curve,listOf(target(.5)),coverage,wall(2.0))
        assertEquals(ContactKind.CRITTER,t.primary!!.kind)
        val inside=ContactTarget("inside",1,"Foxtrot",ShotBounds(ShotVector(-1.0,-1.0,-1.0),ShotVector(1.0,1.0,1.0)),"client_body_v1")
        assertEquals(0.0,CapsuleContactSolver.trace(curve,listOf(inside),coverage){_,_->ContactSegmentScene(null,null)}.primary!!.parameter)
    }
    @Test fun `terrain wins near equal target contact with ambiguity warning`() {
        val t=CapsuleContactSolver.trace(curve,listOf(target(.5)),coverage,wall(.5))
        assertEquals(ContactKind.TERRAIN,t.primary!!.kind)
        assertTrue(t.warnings.contains("contact_tie"))
    }
    @Test fun `unknown boundary before or at contact suppresses farther marker`() {
        for(stop in listOf(ContactStop.UNLOADED,ContactStop.FLUID)) {
            val t=CapsuleContactSolver.trace(curve,listOf(target(2.0)),coverage){a,b->
                ContactSegmentScene(null,if(a.position.z<=.3 && b.position.z>=.3) ContactBoundary((.3-a.position.z)/(b.position.z-a.position.z),stop) else null)
            }
            assertNull(t.primary); assertEquals(stop,t.stop)
            assertEquals(.3,t.nodes.last().position.z,1e-10)
        }
    }
    @Test fun `incomplete targets never establish nearest contact or leak arbitrary reasons`() {
        val t=CapsuleContactSolver.trace(curve,listOf(target(2.0)),ContactCoverage(false,setOf("private arbitrary string")),wall(.5))
        assertNull(t.primary); assertEquals(ContactStop.TARGET_COVERAGE,t.stop)
        assertFalse(t.warnings.contains("private arbitrary string"))
    }
    @Test fun `caps do not become endpoints`() {
        val t=CapsuleContactSolver.trace(curve,emptyList(),coverage){_,_->ContactSegmentScene(null,null)}
        assertNull(t.primary); assertEquals(curve.stop,t.stop)
    }
}
