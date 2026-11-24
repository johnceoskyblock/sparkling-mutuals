package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import java.util.Properties
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

/** Opt in for scenarios exercising Unique mode's extra sparkling checks. */
@Target(AnnotationTarget.FUNCTION)
@ExtendWith(OptionalSparklingChecksExtension::class)
internal annotation class WithOptionalSparklingChecks

internal class OptionalSparklingChecksExtension : BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private val namespace = ExtensionContext.Namespace.create(OptionalSparklingChecksExtension::class.java)

    override fun beforeTestExecution(context: ExtensionContext) {
        val previous = Properties().also(BiomeObjectiveConfig::save)
        context.getStore(namespace).put("config", previous)
        enableOptionalSparklingChecks()
    }

    override fun afterTestExecution(context: ExtensionContext) {
        context.getStore(namespace).remove("config", Properties::class.java)?.let(BiomeObjectiveConfig::load)
    }
}

internal fun initWithOptionalSparklingChecks(dir: Path) {
    ConfigManager.init(dir)
    enableOptionalSparklingChecks()
}

private fun enableOptionalSparklingChecks() {
    val enabled = Properties().also(BiomeObjectiveConfig::save)
    BiomeObjectiveConfig.optionalSpecies.forEach {
        enabled.setProperty("objectives.checks.${it.lowercase()}", "true")
    }
    BiomeObjectiveConfig.load(enabled)
}
