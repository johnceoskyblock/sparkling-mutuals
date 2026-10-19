package net.johnceo.sparklingmutuals.safari.esp

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShywormBodySelectionTest {
    private val eye=ShotVector(0.0,0.0,0.0)
    private fun part(id:Int,x:Double,type:String="block_display",head:Boolean=false,vehicle:Int?=null,passengers:List<Int> = emptyList()) =
        ShywormBodyEntity(id,type,ShotVector(x,0.0,0.0),head,vehicle,passengers)
    @Test fun `nearby unrecognized display pieces remain candidates beside a recognized head`() {
        val head=part(1,2.0,"armor_stand",true)
        val selection=ShywormBodySelector.select(listOf(head,part(2,3.0),part(3,5.0,"interaction"),part(4,20.0)),eye)
        assertEquals(listOf(1),selection.heads)
        assertEquals(listOf(1,2,3),selection.candidates.map{it.id})
        assertEquals(0,selection.candidatesOmitted)
    }
    @Test fun `linked carrier beyond proximity radius is retained but players and items are excluded`() {
        val entities=listOf(part(1,1.0,"armor_stand",true,vehicle=4,passengers=listOf(5)),
            part(2,1.1,"player"),part(3,1.2,"item"),part(4,15.0,"silverfish"),part(5,16.0,"armor_stand"),part(6,1.3,"fox"))
        val s=ShywormBodySelector.select(entities,eye)
        assertEquals(setOf(1,4,5),s.candidates.map{it.id}.toSet())
    }
    @Test fun `candidate cap preserves heads then explicit links before nearby clutter`() {
        val s=ShywormBodySelector.select(listOf(part(1,1.0,"armor_stand",true,vehicle=900),part(900,15.0,"silverfish"))+
            (2..100).map{part(it,1.01)},eye)
        assertEquals(64,s.candidates.size)
        assertEquals(listOf(1,900),s.candidates.take(2).map{it.id})
        assertEquals(37,s.candidatesOmitted)
    }
    @Test fun `nearest two heads are retained with omitted counts and finite range checks`() {
        val entities=listOf(part(4,6.0,"armor_stand",true),part(3,4.0,"armor_stand",true),
            part(2,2.0,"armor_stand",true),part(5,25.0,"armor_stand",true),part(6,Double.NaN,"armor_stand",true))
        val s=ShywormBodySelector.select(entities,eye)
        assertEquals(listOf(2,3),s.heads)
        assertEquals(1,s.headsOmitted)
        assertTrue(s.candidates.all{it.position.x.isFinite()})
        assertTrue(ShywormBodySelector.select(entities,ShotVector(Double.NaN,0.0,0.0)).candidates.isEmpty())
    }
    @Test fun `no recognized head produces no body guesses`() {
        assertTrue(ShywormBodySelector.select(listOf(part(1,0.0),part(2,1.0,"armor_stand")),eye).candidates.isEmpty())
    }
    @Test fun `scan bound cannot be mistaken for complete body coverage`() {
        val entities=(1..4096).map{part(it,30.0)}+part(5000,1.0,"armor_stand",true)
        val s=ShywormBodySelector.select(entities,eye)
        assertTrue(s.scanTruncated)
        assertTrue(s.heads.isEmpty())
    }
}
