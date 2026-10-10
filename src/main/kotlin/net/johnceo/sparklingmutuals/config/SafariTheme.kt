package net.johnceo.sparklingmutuals.config

/** Scope theme changes to this screen; other mods' MoulConfig screens keep their own appearance. */
object SafariTheme {
    const val ACCENT = 0xE3C77E
    const val BACKGROUND = 0x241F18
    private val rendering = ThreadLocal.withInitial { false }
    fun begin() { rendering.set(true) }
    fun end() { rendering.remove() }
    @JvmStatic fun color(original: Int): Int {
        if (!rendering.get()) return original
        val rgb = original and 0xFFFFFF
        val alpha = original and 0xFF000000.toInt()
        val replacement = when (rgb) {
            0x08080E, 0x101016, 0x101010, 0x000000 -> BACKGROUND
            0x202026 -> 0x352D21
            0x28282E, 0x303036 -> 0x625039
            0xFFFFFF -> 0xF5ECD7
            0x444444, 0xAAAAAA -> 0xC5B99E
            else -> rgb
        }
        return alpha or replacement
    }
}
