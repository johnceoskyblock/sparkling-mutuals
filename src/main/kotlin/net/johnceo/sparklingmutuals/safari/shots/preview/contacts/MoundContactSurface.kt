package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.mixin.DisplayStateAccessor
import net.johnceo.sparklingmutuals.safari.shots.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.world.entity.Display
import org.joml.Matrix4f
import org.joml.Vector3f

/** Renderer-derived, unpadded model hull. It is a visual estimate, not a server collision claim. */
object MoundContactSurface {
    fun inspect(client:Minecraft,entity:Display.ItemDisplay):ContactSurfaceBox? {
        if(entity.isInvisible || entity.billboardConstraints!=Display.BillboardConstraints.FIXED ||
            (entity as DisplayStateAccessor).sparklingRenderStatePending()) return null
        val state=entity.renderState() ?: return null
        val transform=state.transformation().get(1f)
        val item=ItemStackRenderState()
        client.itemModelResolver.updateForNonLiving(item,entity.itemStack,entity.itemTransform,entity)
        if(item.isEmpty) return null
        val b=item.modelBoundingBox
        if(!listOf(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ).all{it.isFinite()} || b.xsize<=0 || b.ysize<=0 || b.zsize<=0) return null
        // Same order as DisplayRenderer + ItemDisplayRenderer; item extents already include item context.
        val matrix=Matrix4f().rotationYXZ(-entity.yRot*(Math.PI/180).toFloat(),entity.xRot*(Math.PI/180).toFloat(),0f)
            .mul(transform.matrix).rotateY(Math.PI.toFloat())
        fun point(x:Double,y:Double,z:Double):ShotVector {
            val p=matrix.transformPosition(Vector3f(x.toFloat(),y.toFloat(),z.toFloat()))
            return ShotVector(entity.x+p.x,entity.y+p.y,entity.z+p.z)
        }
        val origin=point(b.minX,b.minY,b.minZ)
        val surface=ContactSurfaceBox(origin,point(b.maxX,b.minY,b.minZ)-origin,point(b.minX,b.maxY,b.minZ)-origin,point(b.minX,b.minY,b.maxZ)-origin)
        return surface.takeIf{it.valid() && it.bounds().let{box->maxOf(box.max.x-box.min.x,box.max.y-box.min.y,box.max.z-box.min.z)<=8.0}}
    }
}
