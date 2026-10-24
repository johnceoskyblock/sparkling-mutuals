package net.johnceo.sparklingmutuals.config

/** Scope theme changes to this screen; other mods' MoulConfig screens keep their own appearance. */
object SafariTheme {
    const val ACCENT = 0xC9A75D
    const val BACKGROUND = 0x0D0D10
    const val SURFACE = 0x19191E
    const val HOVER = 0x292930
    const val BORDER = 0x605238
    /** Body text and muted text, as the settings screen draws them. */
    const val TEXT = 0xE6E6EB
    const val MUTED = 0xA6A6B0
    const val CHAT_TEXT = 0xF1F1F5
    const val CHAT_ACCENT = 0xE6C878
    private val rendering = ThreadLocal.withInitial { false }
    fun begin() { rendering.set(true) }
    fun end() { rendering.remove() }
    @JvmStatic fun color(original: Int): Int {
        if (!rendering.get()) return original
        val rgb = original and 0xFFFFFF
        val alpha = original and 0xFF000000.toInt()
        val replacement = when (rgb) {
            0x08080E, 0x101016, 0x101010, 0x000000 -> BACKGROUND
            0x202026 -> SURFACE
            0x28282E, 0x303036 -> HOVER
            0xFFFFFF -> TEXT
            0x444444, 0xAAAAAA -> MUTED
            else -> rgb
        }
        return alpha or replacement
    }
}
