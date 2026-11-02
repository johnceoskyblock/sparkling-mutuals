package net.johnceo.sparklingmutuals.hud

/** Readable type colors inside the one shared Biome Objectives panel. */
object ObjectivePalette {
    fun color(label: String): Int {
        val text=label.lowercase()
        val rgb=when {
            "nest" in text -> 0xFFD878
            "bag of seeds" in text -> 0xFFE294
            "wriggleworm" in text -> 0xF2C7A5
            "yogi berry" in text -> 0x75DCEB
            "bird food" in text -> 0xA8E6AD
            "feed in feeder" in text -> 0xD5C0F4
            "birds spawned" in text -> 0xA3CDFF
            "wall" in text -> 0xACC9DE
            "mound" in text -> 0xD8BEA6
            "gem" in text -> 0xDAC3FF
            "coin" in text -> 0xFFE078
            "gazer" in text -> 0xCDB0FF
            "incense" in text -> 0xE3B0EE
            else -> 0xDDDDDD
        }
        return 0xFF000000.toInt() or rgb
    }
}
