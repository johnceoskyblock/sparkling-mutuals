package net.johnceo.sparklingmutuals.compat

import java.lang.reflect.Method

/** Public Iris v0 API, resolved only when Iris is installed. No mandatory Iris dependency. */
internal class IrisGuideApi private constructor(
    private val api: Any,
    private val shaderPackInUse: Method,
    private val shadowPass: Method,
) {
    fun shadersActive(): Boolean = shaderPackInUse.invoke(api) as Boolean
    fun isShadowPass(): Boolean = shadowPass.invoke(api) as Boolean

    companion object {
        fun connect(apiType: Class<*>): IrisGuideApi = IrisGuideApi(
            apiType.getMethod("getInstance").invoke(null),
            apiType.getMethod("isShaderPackInUse"),
            apiType.getMethod("isRenderingShadowPass"),
        )
    }
}
