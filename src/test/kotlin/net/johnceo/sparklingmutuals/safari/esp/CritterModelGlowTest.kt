package net.johnceo.sparklingmutuals.safari.esp

import net.minecraft.client.renderer.entity.ArmorStandRenderer
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState
import net.minecraft.client.renderer.entity.state.EntityRenderState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CritterModelGlowTest {
    @Test fun `Duplico silhouette is a closed six-sided box with no interior silverfish shape`() {
        val vertices = mutableListOf<List<Float>>()
        DuplicoGlow.faces(-.5f,0f,-.5f,.5f,1f,.5f) { x,y,z -> vertices.add(listOf(x,y,z)) }
        assertEquals(24,vertices.size)
        assertEquals(8,vertices.distinct().size)
        val faces = vertices.chunked(4)
        assertTrue(faces.all { face -> face.distinct().size == 4 })
        val planes = faces.map { face ->
            (0..2).single { axis -> face.map { it[axis] }.distinct().size == 1 }.let { it to face[0][it] }
        }.toSet()
        assertEquals(setOf(0 to -.5f,0 to .5f,1 to 0f,1 to 1f,2 to -.5f,2 to .5f),planes)
    }

    @Test fun `model-only outline hooks exist in the target Minecraft renderer`() {
        assertTrue(ArmorStandRenderer::class.java.declaredMethods.any {
            it.name == "getRenderType" && it.parameterTypes.toList() ==
                listOf(ArmorStandRenderState::class.java,Boolean::class.javaPrimitiveType,Boolean::class.javaPrimitiveType,Boolean::class.javaPrimitiveType)
        })
        assertTrue(EntityRenderer::class.java.declaredMethods.any {
            it.name == "submit" && it.parameterTypes.firstOrNull() == EntityRenderState::class.java
        })
    }
}
