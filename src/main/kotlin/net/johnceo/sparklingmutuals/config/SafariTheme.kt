package net.johnceo.sparklingmutuals.config

/** Scope theme changes to this screen; other mods' MoulConfig screens keep their own appearance. */
object SafariTheme {
    private val rendering = ThreadLocal.withInitial { false }
    fun begin() { rendering.set(true) }
    fun end() { rendering.remove() }
    @JvmStatic fun color(original: Int): Int {
        if (!rendering.get()) return original
        val rgb = original and 0xFFFFFF
        val alpha = original and 0xFF000000.toInt()
        val replacement = when (rgb) {
            0x08080E, 0x101016, 0x101010, 0x000000 -> 0x16211A
            0x202026 -> 0x203329
            0x28282E, 0x303036 -> 0x3E6040
            0xFFFFFF -> 0xF0E1BE
            0x444444, 0xAAAAAA -> 0xB5C4A1
            else -> rgb
        }
        return alpha or replacement
    }
}
