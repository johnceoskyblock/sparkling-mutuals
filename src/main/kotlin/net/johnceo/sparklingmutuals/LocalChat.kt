package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.SafariTheme
import net.minecraft.network.chat.Component

/** All mod-generated local feedback uses one prefix; outbound party chat stays unchanged. */
object LocalChat {
    private val prefix = Regex("^(?:§[0-9a-fk-or])*\\[(?:SM|Sparkling Mutuals)\\]\\s*", RegexOption.IGNORE_CASE)
    fun lines(text: String): List<String> = text.lines().map { "[SM] " + it.replace(prefix, "") }
    private fun prefix() = Component.literal("[SM] ").withStyle { it.withColor(SafariTheme.CHAT_ACCENT) }
    fun component(text: String): Component {
        val result = Component.empty()
        lines(text).forEachIndexed { index, line ->
            if (index > 0) result.append("\n")
            result.append(prefix())
            result.append(Component.literal(line.removePrefix("[SM] ")).withStyle { it.withColor(SafariTheme.CHAT_TEXT) })
        }
        return result
    }
    /** An already-styled line behind the usual prefix. */
    fun component(body: Component): Component = Component.empty().withStyle { it.withColor(SafariTheme.CHAT_TEXT) }
        .append(prefix()).append(body)

    /** Light local feedback, with a gold inside verdict. */
    fun hideyho(inside: Boolean, bold: Boolean = false): Component = Component.literal("Hideyho ")
        .withStyle { it.withColor(SafariTheme.CHAT_TEXT).withBold(bold) }
        .append(Component.literal(if (inside) "inside!" else "outside!")
            .withStyle { it.withColor(if (inside) SafariTheme.CHAT_ACCENT else SafariTheme.CHAT_TEXT).withBold(true) })
}
