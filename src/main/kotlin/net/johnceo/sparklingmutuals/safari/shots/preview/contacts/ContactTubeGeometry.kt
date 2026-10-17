package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import kotlin.math.*

data class ContactQuad(val a:ShotVector,val b:ShotVector,val c:ShotVector,val d:ShotVector,val color:Int)
data class ContactTubeMesh(val core:List<ContactQuad>,val outline:List<ContactQuad>)

/** Closed six-sided runs in world coordinates. Camera position never defines their thickness or orientation. */
object ContactTubeGeometry {
    private const val SIDES=6
    private const val MAX_LINES=512
    private const val BORDER=.01
    private const val DARK=0xFF151515.toInt()
    private fun finite(v:ShotVector)=v.x.isFinite() && v.y.isFinite() && v.z.isFinite()
    private fun add(a:ShotVector,b:ShotVector,s:Double=1.0)=ShotVector(a.x+b.x*s,a.y+b.y*s,a.z+b.z*s)
    private fun dot(a:ShotVector,b:ShotVector)=a.x*b.x+a.y*b.y+a.z*b.z
    private fun cross(a:ShotVector,b:ShotVector)=ShotVector(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x)
    private fun unit(v:ShotVector):ShotVector? {
        val length=hypot(hypot(v.x,v.y),v.z)
        return if(length.isFinite() && length>1e-10) ShotVector(v.x/length,v.y/length,v.z/length) else null
    }
    private fun radius(line:ContactLine)=.01*(if(line.width.isFinite())line.width.coerceIn(1f,5f) else 3f)
    private fun perpendicular(t:ShotVector)=unit(cross(t,if(abs(t.y)<.9)ShotVector(0.0,1.0,0.0) else ShotVector(1.0,0.0,0.0)))!!
    private fun shaded(color:Int,normal:ShotVector):Int {
        val n=unit(normal) ?: return color
        val brightness=.55+.45*max(0.0,dot(n,ShotVector(-.303045763,.808122036,-.505076272)))
        fun channel(shift:Int)=(((color ushr shift) and 255)*brightness).roundToInt().coerceIn(0,255)
        return (color and 0xFF000000.toInt()) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
    fun build(lines:List<ContactLine>):ContactTubeMesh {
        val runs=ArrayList<MutableList<ContactLine>>()
        var run:MutableList<ContactLine>?=null
        var direction:ShotVector?=null
        for(line in lines.take(MAX_LINES)) {
            val next=if(finite(line.from) && finite(line.to) && (line.width>0 || line.width.isNaN())) unit(line.to-line.from) else null
            if(next==null) { run=null;direction=null;continue }
            val previous=run?.lastOrNull()
            if(previous==null || previous.to!=line.from || previous.color!=line.color ||
                radius(previous)!=radius(line) || dot(direction!!,next)<-.95) {
                run=ArrayList<ContactLine>().also(runs::add)
            }
            run.add(line);direction=next
        }
        val core=ArrayList<ContactQuad>();val outline=ArrayList<ContactQuad>()
        for(part in runs) {
            val centers=listOf(part.first().from)+part.map { it.to }
            val directions=centers.zipWithNext().map { (a,b)->unit(b-a)!! }
            val tangents=centers.indices.map { i->when(i) {
                0->directions.first()
                centers.lastIndex->directions.last()
                else->unit(add(directions[i-1],directions[i])) ?: directions[i]
            } }
            var previousU:ShotVector?=null
            val bases=tangents.map { t->
                val u=previousU?.let { unit(add(it,t,-dot(it,t))) } ?: perpendicular(t)
                previousU=u
                u to cross(t,u)
            }
            fun emit(r:Double,destination:MutableList<ContactQuad>,border:Boolean) {
                val rings=centers.indices.map { i->
                    val (u,v)=bases[i]
                    List(SIDES) { side->val angle=2*PI*side/SIDES;add(add(centers[i],u,r*cos(angle)),v,r*sin(angle)) }
                }
                // An unrepresentable cross-section is omitted rather than emitting collapsed/nonfinite faces.
                if(rings.any { ring->ring.any { !finite(it) } || ring.toSet().size!=SIDES }) return
                fun quad(a:ShotVector,b:ShotVector,c:ShotVector,d:ShotVector) {
                    destination.add(ContactQuad(a,b,c,d,if(border) DARK else shaded(part.first().color,cross(b-a,c-a))))
                }
                for(i in 0 until centers.lastIndex) repeat(SIDES) { side->
                    val next=(side+1)%SIDES
                    quad(rings[i][side],rings[i][next],rings[i+1][next],rings[i+1][side])
                }
                repeat(SIDES) { side->
                    val next=(side+1)%SIDES
                    quad(centers.first(),rings.first()[next],rings.first()[side],rings.first()[side])
                    quad(centers.last(),rings.last()[side],rings.last()[next],rings.last()[next])
                }
            }
            val r=radius(part.first())
            emit(r,core,false);emit(r+BORDER,outline,true)
        }
        return ContactTubeMesh(core.toList(),outline.toList())
    }
    /** Snapshot before deferred drawing; painter order provides self-occlusion without writing world depth. */
    fun cameraRelative(quads:List<ContactQuad>,camera:ShotVector):List<ContactQuad> {
        if(!finite(camera)) return emptyList()
        fun relative(v:ShotVector):ShotVector? {
            val x=(v.x-camera.x).toFloat();val y=(v.y-camera.y).toFloat();val z=(v.z-camera.z).toFloat()
            return if(x.isFinite() && y.isFinite() && z.isFinite()) ShotVector(x.toDouble(),y.toDouble(),z.toDouble()) else null
        }
        return quads.take(MAX_LINES*SIDES*3).mapNotNull { q->
            val a=relative(q.a) ?: return@mapNotNull null;val b=relative(q.b) ?: return@mapNotNull null
            val c=relative(q.c) ?: return@mapNotNull null;val d=relative(q.d) ?: return@mapNotNull null
            ContactQuad(a,b,c,d,q.color)
        }.sortedByDescending { q->
            val center=ShotVector((q.a.x+q.b.x+q.c.x+q.d.x)*.25,(q.a.y+q.b.y+q.c.y+q.d.y)*.25,(q.a.z+q.b.z+q.c.z+q.d.z)*.25)
            dot(center,center)
        }
    }
}
