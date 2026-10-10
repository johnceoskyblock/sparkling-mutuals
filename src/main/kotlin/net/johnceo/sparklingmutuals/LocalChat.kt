package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.SafariTheme
import net.minecraft.network.chat.Component

/** All mod-generated local feedback uses one prefix; outbound party chat stays unchanged. */
object LocalChat {
    private val prefix = Regex("^(?:§[0-9a-fk-or])*\\[(?:SM|Sparkling Mutuals)\\]\\s*", RegexOption.IGNORE_CASE)
    fun lines(text: String): List<String> = text.lines().map { "[SM] " + it.replace(prefix, "") }
    fun component(text: String): Component {
        val result = Component.empty()
        lines(text).forEachIndexed { index, line ->
            if (index > 0) result.append("\n")
            result.append(Component.literal("[SM] ").withStyle { it.withColor(SafariTheme.ACCENT) })
            result.append(Component.literal(line.removePrefix("[SM] ")))
        }
        return result
    }
}
