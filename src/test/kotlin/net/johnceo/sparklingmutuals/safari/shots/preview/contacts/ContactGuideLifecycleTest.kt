package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import net.johnceo.sparklingmutuals.safari.shots.preview.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class ContactGuideLifecycleTest {
    @Test fun `HUD and world share the same current view without rebuilding or falling back`() {
        val holder=ContactGuideLifecycle();val world=Any();val view=ShotVector(0.0,0.0,1.0);var builds=0
        fun build()=CapsulePreviewFrame(listOf(view),"Uncalibrated · Estimated terrain contact").also{builds++}
        val frame=holder.resolve(world,view,::build)
        repeat(30){assertSame(frame,holder.resolve(world,view,::build))}
        assertEquals(1,builds)
    }
    @Test fun `turning recomputes current contact status rather than returning no HUD`() {
        val holder=ContactGuideLifecycle();val world=Any()
        holder.resolve(world,"old-view"){CapsulePreviewFrame(emptyList(),"Uncalibrated · Estimated terrain contact")}
        val turned=holder.resolve(world,"new-view"){CapsulePreviewFrame(emptyList(),"Uncalibrated · Possible critter contact")}
        assertEquals("Uncalibrated · Possible critter contact",turned.status)
        assertSame(turned,holder.resolve(world,"new-view"){error("same view must reuse its current frame")})
    }
    @Test fun `new render frame world or state cannot reuse old geometry`() {
        val holder=ContactGuideLifecycle();val world=Any();var builds=0
        fun build()=CapsulePreviewFrame(emptyList(),"Uncalibrated").also{builds++}
        holder.resolve(world,"view",::build)
        holder.reset();holder.resolve(world,"view",::build)
        holder.resolve(Any(),"view",::build)
        holder.resolve(world,"changed eligibility or scene",::build)
        assertEquals(4,builds)
    }
}
