package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.abs

class ContactBodySurfaceTest {
    private fun v(x:Double,y:Double,z:Double)=ShotVector(x,y,z)
    private fun part(id:String,x:Double,y:Double,z:Double,body:Boolean=false)=ContactTarget(id,1,"Shyworm",
        ShotBounds(v(x-.3,y+1.35,z-.3),v(x+.3,y+1.95,z+.3)),"armor_stand_body_v1",bodyPart=body)
    private fun box(x:Double,y:Double,z:Double,sx:Double,sy:Double,sz:Double)=ContactSurfaceBox(v(x,y,z),v(sx,0.0,0.0),v(0.0,sy,0.0),v(0.0,0.0,sz))

    // Replacing segment support with a head-only selection loses the raised tail captured in snapshot 3.
    @Test fun `raised tail remains eligible when head is lower`() {
        val parts=listOf(part("head",4.280,-4.910,2.265),part("a",4.294,-5.023,1.859,true),
            part("b",4.276,-5.023,1.442,true),part("c",4.290,-4.938,1.015,true),
            part("d",4.273,-4.511,.598,true),part("e",4.271,-3.614,.187,true),
            part("f",4.271,-2.593,-.243,true),part("tail",4.271,-2.207,-.653,true))
        val support=ShywormContactParts.support(parts)
        assertEquals("tail",support["tail"]?.first()?.key)
        assertEquals("head",support["tail"]?.last()?.key)
        assertTrue(support.getValue("tail").size in 2..8)
        val curve=ContactCurve(listOf(ContactNode(0.0,v(4.27,-.557,-2.0),false),ContactNode(1.0,v(4.27,-.557,0.0),false)),ContactStop.RANGE_LIMIT,"test")
        val trace=CapsuleContactSolver.trace(curve,parts.filter{!it.bodyPart || it.key in support},ContactCoverage(true,emptySet())){_,_->ContactSegmentScene(null,null)}
        assertEquals("tail",trace.primary?.targetKey)
        assertEquals(ContactKind.CRITTER,trace.primary?.kind)
    }
    @Test fun `disconnected or ambiguous body parts do not become independent worms`() {
        assertTrue(ShywormContactParts.support(listOf(part("orphan",0.0,0.0,0.0,true))).isEmpty())
        val pieces=listOf(part("h1",0.0,0.0,0.0),part("p",1.0,0.0,0.0,true),part("h2",2.0,0.0,0.0))
        assertTrue(ShywormContactParts.support(pieces).isEmpty())
        assertTrue(ShywormContactParts.support(listOf(part("h",0.0,0.0,0.0),part("far",5.0,0.0,0.0,true))).isEmpty())
    }
    @Test fun `long connected component with two heads is ambiguous even beyond support depth`() {
        val pieces=listOf(part("left",0.0,0.0,0.0),part("right",20.4,0.0,0.0))+
            (1..11).map{part("p$it",it*1.7,0.0,0.0,true)}
        assertTrue(ShywormContactParts.support(pieces).isEmpty())
    }
    @Test fun `surface intersection uses transformed local box rather than its enclosing world box`() {
        val s=ContactSurfaceBox(v(0.0,0.0,0.0),v(1.0,0.0,1.0),v(0.0,1.0,0.0),v(-1.0,0.0,1.0))
        val hit=s.entry(v(0.0,.5,-1.0),v(0.0,.5,3.0))!!
        assertEquals(.25,hit.fraction,1e-9)
        assertNull(s.entry(v(.9,.5,-1.0),v(.9,.5,.5)))
        assertNull(box(0.0,0.0,0.0,0.0,1.0,1.0).entry(v(0.0,0.0,-1.0),v(0.0,0.0,2.0)))
    }
    @Test fun `mound surface produces amber contact before later ground without becoming a critter`() {
        val surface=box(-.25,1.0,1.0,.5,.5,.5)
        val mound=ContactTarget("mound",1,"Rockmite",surface.bounds(),"mound_surface_v1",surface=surface)
        val curve=ContactCurve(listOf(ContactNode(0.0,v(0.0,2.0,0.0),false),ContactNode(1.0,v(0.0,0.0,3.0),false)),ContactStop.RANGE_LIMIT,"test")
        val t=CapsuleContactSolver.trace(curve,listOf(mound),ContactCoverage(true,emptySet())){_,_->ContactSegmentScene(null,null)}
        assertEquals(ContactKind.TERRAIN,t.primary?.kind)
        assertEquals("mound_surface_v1",t.primary?.proxy)
        assertEquals(1.0,t.primary!!.position.z,1e-9)
        val mesh=ContactGuideGeometry.build(t.copy(surfacePatch=listOf(surface)),v(0.0,2.0,0.0),3f)
        assertTrue(mesh.marker.isNotEmpty())
        assertTrue(mesh.marker.all{it.color==0xFFFFB347.toInt()})
    }
    @Test fun `frame marker consumes local surface patch rather than reverting to a flat ring`() {
        val hit=ContactCandidate(ContactKind.TERRAIN,0.0,v(.08,0.0,0.0),v(0.0,1.0,0.0),null,null)
        val trace=ContactTrace(emptyList(),hit,ContactStop.CONTACT,emptySet(),listOf(
            box(-2.0,-1.0,-2.0,4.0,1.0,4.0),box(-1.0,0.0,-2.0,1.0,2.0,4.0)))
        val marker=ContactGuideGeometry.build(trace,v(3.0,3.0,3.0),3f).marker
        assertTrue(marker.any{it.from.y>.05 && abs(it.from.x)<.01})
        assertTrue(ContactGuideGeometry.build(trace.copy(surfacePatch=emptyList()),v(3.0,3.0,3.0),3f).marker.isEmpty())
    }
    @Test fun `floor wall ring has segments on both surfaces without cutting through the corner`() {
        val ring=ContactSurfaceRing.build(v(.08,0.0,0.0),v(0.0,1.0,0.0),listOf(
            box(-2.0,-1.0,-2.0,4.0,1.0,4.0),box(-1.0,0.0,-2.0,1.0,2.0,4.0)),3f)
        assertTrue(ring.isNotEmpty() && ring.size<=24)
        assertTrue(ring.any{it.from.y>.05 && abs(it.from.x)<.01})
        assertTrue(ring.any{it.from.x>.15 && abs(it.from.y)<.01})
        for(line in ring) {
            val m=line.from.lerp(line.to,.5)
            assertTrue(abs(m.y)<.01 || abs(m.x)<.01,"line must stay on a face: $line")
        }
    }
    @Test fun `convex step ring follows both faces and does not bridge a separate surface`() {
        val step=box(-1.0,0.0,-1.0,1.0,1.0,2.0)
        val ring=ContactSurfaceRing.build(v(-.05,1.0,0.0),v(0.0,1.0,0.0),listOf(step),3f)
        assertTrue(ring.any{it.from.x>.001 && it.from.y<.99})
        assertTrue(ring.any{abs(it.from.y-1.0)<.01})
        val disconnected=box(.08,0.0,-1.0,1.0,.8,2.0)
        val withGap=ContactSurfaceRing.build(v(-.05,1.0,0.0),v(0.0,1.0,0.0),listOf(step,disconnected),3f)
        assertEquals(ring,withGap)
        assertTrue(ContactSurfaceRing.build(v(0.0,0.0,0.0),v(0.0,1.0,0.0),emptyList(),3f).isEmpty())
    }
    @Test fun `ring never bridges a slit even when surfaces connect around it`() {
        val ring=ContactSurfaceRing.build(v(0.0,0.0,0.0),v(0.0,1.0,0.0),listOf(
            box(-1.0,-1.0,-1.0,1.11,1.0,2.0),box(.12,-1.0,-1.0,.88,1.0,2.0),
            box(.11,-1.0,-1.0,.01,1.0,1.15)),3f)
        assertTrue(ring.isNotEmpty())
        for(line in ring) for(i in 0..100) {
            val p=line.from.lerp(line.to,i/100.0)
            assertFalse(p.x>.110001 && p.x<.119999 && p.z>.150001,"marker bridged open slit: $line")
        }
    }
}
