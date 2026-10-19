package net.johnceo.sparklingmutuals.compat

import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

/** Optional queries through Iris's public API; never loads Iris classes when the mod is absent. */
object IrisCapsuleCompat {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/iris")
    private var failed = false
    private val api: IrisGuideApi? by lazy {
        if (!FabricLoader.getInstance().isModLoaded("iris")) null else query<IrisGuideApi?>(null) {
            IrisGuideApi.connect(Class.forName("net.irisshaders.iris.api.v0.IrisApi"))
        }
    }
    private fun <T> query(fallback: T, action: () -> T): T {
        if (failed) return fallback
        return try { action() } catch (error: ReflectiveOperationException) {
            unavailable(error); fallback
        } catch (error: LinkageError) {
            unavailable(error); fallback
        }
    }
    private fun unavailable(error: Throwable) {
        failed = true
        logger.warn("Iris capsule integration unavailable; keeping the vanilla guide renderer", error)
    }
    fun shadersActive(): Boolean = query(false) { api?.shadersActive() == true }
    fun isShadowPass(): Boolean = query(false) { api?.isShadowPass() == true }
}
