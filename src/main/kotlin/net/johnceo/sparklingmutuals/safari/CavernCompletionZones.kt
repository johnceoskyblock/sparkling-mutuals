package net.johnceo.sparklingmutuals.safari

object CavernCompletionZones {
    data class Zone(val x: Double, val z: Double, val fullClearRadius: Double, val sparklingRadius: Double) {
        fun contains(x: Double, z: Double, sparkling: Boolean): Boolean {
            val radius = if (sparkling) sparklingRadius else fullClearRadius
            return x.isFinite() && z.isFinite() && (x-this.x)*(x-this.x)+(z-this.z)*(z-this.z) <= radius*radius
        }
    }
    val zones = mapOf(
        "Cavernfish" to Zone(-85.0,81.0,30.0,40.0), "Flitter" to Zone(-84.0,62.0,30.0,40.0),
        "Shyworm" to Zone(-119.0,43.0,30.0,40.0), "Driftling" to Zone(-120.0,55.0,40.0,60.0),
        "Chuckwalla" to Zone(-100.0,45.0,40.0,60.0), "Gemzie" to Zone(-141.0,51.0,30.0,40.0))
}
