package net.johnceo.sparklingmutuals.config

import java.awt.Color

/** Preserves existing speed:alpha:red:green:blue strings, including animated colors. */
object SettingsColor {
    private const val CACHE_LIMIT = 128
    private val parsed = ThreadLocal.withInitial {
        object : LinkedHashMap<String, ParsedColor>(16, .75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ParsedColor>?) = size > CACHE_LIMIT
        }
    }

    private class ParsedColor(private val base: Int, private val hsv: FloatArray? = null, private val period: Float = 1f) {
        fun argb(now: Long): Int {
            val components = hsv ?: return base
            val rotation = (now.toDouble() / 1000.0 / period % 1).toFloat()
            return base or (Color.HSBtoRGB((components[0] + rotation) % 1f, components[1], components[2]) and 0xFFFFFF)
        }
    }

    // Only the input is cached. Animated colors still resolve against the caller's timestamp.
    fun argb(value: String, now: Long = System.currentTimeMillis()): Int =
        parsed.get().getOrPut(value) { parse(value) }.argb(now)

    private fun parse(value: String): ParsedColor {
        val parts = value.split(':')
        if (parts.size != 5) return ParsedColor(-1)
        val speed = parts[0].toIntOrNull() ?: 0
        val alpha = (parts[1].toIntOrNull() ?: 0) shl 24
        val red = parts[2].toIntOrNull() ?: 0
        val green = parts[3].toIntOrNull() ?: 0
        val blue = parts[4].toIntOrNull() ?: 0
        return if (speed == 0) ParsedColor(alpha or (red shl 16) or (green shl 8) or blue)
        else ParsedColor(alpha, Color.RGBtoHSB(red, green, blue, null), (255 - speed) / 254f * 59 + 1)
    }

    fun hex(value: String): String {
        val p=value.split(':').drop(2).map { it.toIntOrNull() ?: 0 }
        return if(p.size==3) "#%02X%02X%02X".format(p[0],p[1],p[2]) else "#FFFFFF"
    }
    fun contrast(value: String,background: Int): Int {
        val color=argb(value);val alpha=(color ushr 24)/255f
        fun channel(shift: Int)=((color shr shift and 255)*alpha+(background shr shift and 255)*(1-alpha))
        val light=channel(16)*.299+channel(8)*.587+channel(0)*.114
        return if(light>150) 0xFF101014.toInt() else -1
    }
    fun fromHex(input: String, original: String): String? {
        val text=input.trim().removePrefix("#")
        if (!text.matches(Regex("[0-9a-fA-F]{6}"))) return null
        val rgb=text.toInt(16);val old=original.split(':')
        return "${old.getOrElse(0){"0"}}:${old.getOrElse(1){"255"}}:${rgb shr 16 and 255}:${rgb shr 8 and 255}:${rgb and 255}"
    }
}
