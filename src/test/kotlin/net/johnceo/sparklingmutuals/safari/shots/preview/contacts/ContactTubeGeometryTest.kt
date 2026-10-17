package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.*

class ContactTubeGeometryTest {
    @Test fun `guide supplies a closed volume with fixed world thickness rather than a flat stroke`() {
        val trace=ContactTrace(listOf(ContactNode(0.0,ShotVector(0.0,0.0,0.0),true),
            ContactNode(1.0,ShotVector(0.0,0.0,2.0),true)),null,ContactStop.RANGE_LIMIT,emptySet())
        val mesh=ContactGuideGeometry.build(trace,ShotVector(0.0,1.0,-3.0),3f)
        val faces=mesh.tube.core
        assertTrue(faces.isNotEmpty(),"A line alone has no volume")
        val points=faces.flatMap(::points)
        assertEquals(-.03,points.minOf { it.x },1e-9)
        assertEquals(.03,points.maxOf { it.x },1e-9)
        assertEquals(0.0,points.minOf { it.z },1e-9)
        assertEquals(2.0,points.maxOf { it.z },1e-9)
        assertTrue(points.maxOf { it.y }-points.minOf { it.y }>.05)
        assertEquals(18,faces.size) // Six sides and six triangles on each end.
        val otherView=ContactGuideGeometry.build(trace,ShotVector(100.0,-50.0,30.0),3f)
        assertEquals(faces,otherView.tube.core,"Camera movement cannot rotate or resize the tube")
    }
    private val zero=ShotVector(0.0,0.0,0.0)
    private fun line(a:ShotVector=zero,b:ShotVector=ShotVector(0.0,0.0,2.0),width:Float=3f)=ContactLine(a,b,0xFFFFB347.toInt(),width)
    private fun points(q:ContactQuad)=listOf(q.a,q.b,q.c,q.d)
    private fun cross(a:ShotVector,b:ShotVector)=ShotVector(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x)
    private fun dot(a:ShotVector,b:ShotVector)=a.x*b.x+a.y*b.y+a.z*b.z
    @Test fun `connected bends share their ring while dash gaps get separate closed ends`() {
        val bend=ShotVector(0.0,0.0,1.0)
        val joined=ContactTubeGeometry.build(listOf(line(b= bend),line(bend,ShotVector(1.0,0.0,2.0))))
        assertEquals(24,joined.core.size)
        val firstRing=joined.core.take(6).flatMap { listOf(it.c,it.d) }.toSet()
        val nextRing=joined.core.drop(6).take(6).flatMap { listOf(it.a,it.b) }.toSet()
        assertEquals(firstRing,nextRing)
        val dashed=ContactTubeGeometry.build(listOf(line(b=ShotVector(0.0,0.0,.35)),line(ShotVector(0.0,0.0,.55),ShotVector(0.0,0.0,.9))))
        assertEquals(36,dashed.core.size)
        assertTrue(dashed.core.all { q -> points(q).all { it.z<=.35000001 } || points(q).all { it.z>=.54999999 } })
    }
    @Test fun `faces and end caps wind outward for culling and retain amber alpha`() {
        val faces=ContactTubeGeometry.build(listOf(line())).core
        for(q in faces) {
            val n=cross(q.b-q.a,q.c-q.a)
            if(abs(n.z)>1e-10) assertTrue(if(q.a.z==0.0) n.z<0 else n.z>0)
            else assertTrue(dot(n,ShotVector(q.a.x+q.b.x,q.a.y+q.b.y,0.0))>0)
            assertEquals(255,q.color ushr 24)
        }
        assertTrue(faces.map { it.color }.distinct().size>1,"Face shading makes the volume readable")
    }
    @Test fun `width scales world radius while dark border remains outside it`() {
        for((width,radius) in listOf(1f to .01,3f to .03,5f to .05,100f to .05,Float.NaN to .03)) {
            val mesh=ContactTubeGeometry.build(listOf(line(width=width)))
            assertEquals(radius,mesh.core.flatMap(::points).maxOf { hypot(it.x,it.y) },1e-8)
            assertEquals(radius+.01,mesh.outline.flatMap(::points).maxOf { hypot(it.x,it.y) },1e-8)
        }
    }
    @Test fun `invalid and zero length inputs cannot bridge good segments or produce nonfinite faces`() {
        val good=line(b=ShotVector(0.0,0.0,1.0))
        val invalid=line(ShotVector(Double.NaN,0.0,1.0),ShotVector(0.0,0.0,2.0))
        val mesh=ContactTubeGeometry.build(listOf(good,invalid,line(ShotVector(0.0,0.0,2.0),ShotVector(0.0,0.0,3.0))))
        assertEquals(36,mesh.core.size)
        assertTrue(mesh.core.flatMap(::points).all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
        assertTrue(ContactTubeGeometry.build(listOf(line(zero,zero),invalid,line(width=0f))).core.isEmpty())
        val vertical=ContactTubeGeometry.build(listOf(line(zero,ShotVector(0.0,2.0,0.0)),line(ShotVector(0.0,2.0,0.0),zero)))
        assertEquals(36,vertical.core.size)
        assertTrue(vertical.core.flatMap(::points).all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
    }
    @Test fun `world translation preserves thickness and geometry work stays bounded`() {
        val origin=ShotVector(30_000_000.0,1000.0,-30_000_000.0)
        val mesh=ContactTubeGeometry.build(listOf(line(origin,ShotVector(origin.x,origin.y,origin.z+2))))
        assertEquals(.03,mesh.core.flatMap(::points).maxOf { hypot(it.x-origin.x,it.y-origin.y) },1e-8)
        val many=List(1000){i->line(ShotVector(0.0,0.0,i*2.0),ShotVector(0.0,0.0,i*2.0+1))}
        val bounded=ContactTubeGeometry.build(many)
        assertEquals(512*18,bounded.core.size)
        assertTrue(bounded.core.flatMap(::points).maxOf { it.z }<=1023.0)
    }
    @Test fun `nearly touching endpoints are not merged into a zero length join`() {
        val mesh=ContactTubeGeometry.build(listOf(line(b=ShotVector(0.0,0.0,1.0)),
            line(ShotVector(0.0,0.0,1.0+2e-10),ShotVector(0.0,0.0,1.0))))
        assertEquals(36,mesh.core.size)
    }
    @Test fun `render submission subtracts the camera once before float conversion and sorts without changing thickness`() {
        val origin=ShotVector(30_000_000.0,1000.0,-30_000_000.0)
        val world=ContactTubeGeometry.build(listOf(line(origin,ShotVector(origin.x,origin.y,origin.z+2)))).core
        for(distance in listOf(2.0,20.0)) {
            val relative=ContactTubeGeometry.cameraRelative(world,ShotVector(origin.x,origin.y,origin.z-distance))
            assertEquals(world.size,relative.size)
            val p=relative.flatMap(::points)
            assertEquals(.03,p.maxOf { hypot(it.x,it.y) },1e-7)
            assertEquals(distance,p.minOf { it.z },1e-7)
            assertEquals(distance+2,p.maxOf { it.z },1e-7)
        }
        assertTrue(ContactTubeGeometry.cameraRelative(world,ShotVector(Double.NaN,0.0,0.0)).isEmpty())
        val invalid=ContactQuad(zero,zero,zero,ShotVector(Double.MAX_VALUE,0.0,0.0),-1)
        assertTrue(ContactTubeGeometry.cameraRelative(listOf(invalid),zero).isEmpty())
    }
}
