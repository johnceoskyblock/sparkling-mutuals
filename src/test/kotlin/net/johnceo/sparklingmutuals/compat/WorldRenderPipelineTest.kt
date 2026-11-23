package net.johnceo.sparklingmutuals.compat

//? if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.CompareOp
*///? } else {
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.platform.CompareOp
//? }
import net.johnceo.sparklingmutuals.config.SafariSettings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorldRenderPipelineTest {
    @Test fun `line and filled guides each declare one blended color attachment`() {
        for (filled in listOf(false, true)) {
            val pipeline = WorldRenderCompat.renderType("test_color_$filled", filled = filled).pipeline()
            //? if >=26.3 {
            /*assertEquals(1, pipeline.colorTargetStates.size)
            val color = pipeline.colorTargetStates.single()!!
            *///? } else {
            val color = pipeline.colorTargetState!!
            //? }
            assertEquals(BlendFunction.TRANSLUCENT, color.blendFunction().orElse(null))
        }
    }

    @Test fun `occluded guides use the target depth convention without writing depth`() {
        for (filled in listOf(false, true)) {
            val pipeline = WorldRenderCompat.renderType("test_depth_$filled", filled = filled, depth = WorldDepthMode.OCCLUDED).pipeline()
            assertEquals(DepthStencilState.DEFAULT.depthTest(), pipeline.depthStencilState!!.depthTest())
            assertFalse(pipeline.depthStencilState!!.writeDepth())
        }
    }

    @Test fun `capsule paths can render through terrain while Standard world markers still use depth`() {
        val projection = WorldRenderCompat.capsulePathType("test_capsule_path", WorldDepthMode.THROUGH_TERRAIN)
        assertEquals(CompareOp.ALWAYS_PASS, projection.pipeline().depthStencilState!!.depthTest())
        val marker = WorldRenderCompat.renderType("test_floor_tile", filled = true, depth = WorldDepthMode.THROUGH_TERRAIN)
        val expected = if (SafariSettings.standardEdition) DepthStencilState.DEFAULT.depthTest() else CompareOp.ALWAYS_PASS
        assertEquals(expected, marker.pipeline().depthStencilState!!.depthTest())
    }

    @Test fun `known Torrhus hive waypoints pass the depth test in both editions`() {
        val depth = WorldRenderCompat.honeyHiveWaypointType().pipeline().depthStencilState!!
        assertEquals(CompareOp.ALWAYS_PASS, depth.depthTest())
        assertFalse(depth.writeDepth())
    }
}
