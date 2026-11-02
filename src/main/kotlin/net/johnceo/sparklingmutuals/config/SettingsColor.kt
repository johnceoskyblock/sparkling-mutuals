package net.johnceo.sparklingmutuals.config

import java.awt.Color

/** Preserves existing speed:alpha:red:green:blue strings, including animated colors. */
object SettingsColor {
    fun argb(value: String, now: Long = System.currentTimeMillis()): Int {
        val p=value.split(':').map { it.toIntOrNull() ?: 0 }
        if (p.size != 5) return -1
        val rgb=if(p[0] == 0) (p[2] shl 16) or (p[3] shl 8) or p[4] else {
            val hsv=Color.RGBtoHSB(p[2],p[3],p[4],null)
            Color.HSBtoRGB((hsv[0]+((now.toDouble()/1000.0/((255-p[0])/254f*59+1))%1).toFloat())%1f,hsv[1],hsv[2]) and 0xFFFFFF
        }
        return (p[1] shl 24) or rgb
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
