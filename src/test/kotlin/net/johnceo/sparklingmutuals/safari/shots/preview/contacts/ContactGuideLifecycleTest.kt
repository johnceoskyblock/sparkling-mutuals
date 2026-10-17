package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import net.johnceo.sparklingmutuals.safari.shots.preview.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class ContactGuideLifecycleTest {
    @Test fun `shared HUD frame cannot survive world or aim change`() {
        val holder=ContactGuideLifecycle();val world=Any();val aim=ShotVector(0.0,0.0,1.0)
        val frame=CapsulePreviewFrame(emptyList(),"Uncalibrated")
        holder.store(world,aim,frame)
        assertSame(frame,holder.current(world,aim))
        assertNull(holder.current(Any(),aim));assertNull(holder.current(world,ShotVector(1.0,0.0,0.0)))
        holder.reset();assertNull(holder.current(world,aim))
    }
}
