package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class ContactGuideGeometryTest {
    private val p=ShotVector(100.0,10.0,100.0)
    private fun trace(kind:ContactKind?)=ContactTrace(listOf(ContactNode(0.0,p,false),ContactNode(1.0,ShotVector(100.0,10.0,101.0),false)),
        kind?.let{ContactCandidate(it,1.0,p,ShotVector(0.0,1.0,0.0),null,null)},ContactStop.CONTACT,emptySet())
    @Test fun `unknown and range limited results have no endpoint`() {
        for(stop in listOf(ContactStop.UNLOADED,ContactStop.FLUID,ContactStop.TARGET_COVERAGE,ContactStop.RANGE_LIMIT))
            assertTrue(ContactGuideGeometry.build(trace(null).copy(stop=stop),p,3f).marker.isEmpty())
        val empty=ContactGuideGeometry.build(ContactTrace(emptyList(),null,ContactStop.RANGE_LIMIT,emptySet()),p,3f)
        assertTrue(empty.core.isEmpty());assertTrue(empty.marker.isEmpty())
    }
    @Test fun `ring and diamond are distinct bounded world space shapes`() {
        val ring=ContactGuideGeometry.build(trace(ContactKind.TERRAIN),p,3f)
        val diamond=ContactGuideGeometry.build(trace(ContactKind.CRITTER),p,3f)
        assertEquals(16,ring.marker.size);assertEquals(6,diamond.marker.size)
        assertTrue(ring.marker.all{it.width==3f && kotlin.math.abs(it.from.y-10)<1e-8})
        assertTrue(ring.outline.all{it.width==5f})
        assertTrue(ring.marker.all{it.from.x>99.0}) // geometry has not subtracted camera
        assertTrue(diamond.marker.all{it.from.x.isFinite() && it.from.y.isFinite()})
    }
    @Test fun `extrapolated segments are dashed while observed region is solid`() {
        val raw=trace(null)
        val dashed=ContactGuideGeometry.build(raw,p,Float.NaN)
        val solid=ContactGuideGeometry.build(raw.copy(nodes=raw.nodes.map{it.copy(observedRegion=true)}),p,100f)
        assertTrue(dashed.core.size>1);assertEquals(1,solid.core.size)
        assertTrue(dashed.core.all{it.width==3f});assertTrue(solid.core.all{it.width==5f})
        assertTrue(dashed.core.sumOf{kotlin.math.sqrt(it.from.distanceSquared(it.to))}<1.0)
    }
}
