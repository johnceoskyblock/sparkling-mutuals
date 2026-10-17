package net.johnceo.sparklingmutuals.compat

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IrisGuideApiTest {
    class Api {
        var active = false
        var shadow = false
        fun isShaderPackInUse() = active
        fun isRenderingShadowPass() = shadow
        companion object {
            var current = Api()
            @JvmStatic fun getInstance() = current
        }
    }
    @Test fun `shader activation is queried live across pack toggles and reloads`() {
        Api.current = Api()
        val bridge = IrisGuideApi.connect(Api::class.java)
        assertFalse(bridge.shadersActive())
        Api.current.active = true
        assertTrue(bridge.shadersActive())
        Api.current.active = false
        assertFalse(bridge.shadersActive())
    }
    @Test fun `shadow pass is queried each time rather than cached`() {
        Api.current = Api()
        val bridge = IrisGuideApi.connect(Api::class.java)
        assertFalse(bridge.isShadowPass())
        Api.current.shadow = true
        assertTrue(bridge.isShadowPass())
        Api.current.shadow = false
        assertFalse(bridge.isShadowPass())
    }
    @Test fun `unsupported API cannot be mistaken for a working integration`() {
        assertThrows(ReflectiveOperationException::class.java) { IrisGuideApi.connect(String::class.java) }
    }
}
