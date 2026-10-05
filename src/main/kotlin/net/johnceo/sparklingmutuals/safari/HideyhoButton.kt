package net.johnceo.sparklingmutuals.safari

import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

/** ShinyHunter's shortest matching button, including child text and inherited styles. */
object HideyhoButton {
    fun find(message: Component): ClickEvent? {
        var best: ClickEvent? = null
        var length = Int.MAX_VALUE
        fun visit(node: Component, inherited: Style) {
            val style = node.style.applyTo(inherited)
            val event = style.clickEvent
            val text = SafariRules.strip(node.string)
            if (Regex("(?i)\\bSure\\b").containsMatchIn(text) && text.length < length &&
                (event is ClickEvent.RunCommand || event is ClickEvent.Custom)) {
                best = event
                length = text.length
            }
            node.siblings.forEach { visit(it, style) }
        }
        visit(message, Style.EMPTY)
        return best
    }
}
