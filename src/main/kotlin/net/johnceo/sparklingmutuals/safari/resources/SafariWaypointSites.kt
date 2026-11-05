package net.johnceo.sparklingmutuals.safari.resources

import com.google.gson.JsonParser
import kotlin.math.floor
import net.johnceo.sparklingmutuals.safari.SafariBiome

/** Candidate sites from SafariUtils (MIT). A site alone never proves a spawn. */
internal object SafariWaypointSites {
    private val catalog = SafariWaypointSites::class.java.getResourceAsStream("/safari-waypoint-sites.json")!!
        .bufferedReader().use { JsonParser.parseReader(it).asJsonObject }

    private fun block(raw: String): Triple<Int, Int, Int> {
        val (x, y, z) = raw.split(',').map { floor(it.toDouble()).toInt() }
        return Triple(x, y, z)
    }

    private val floorBiomes = catalog.getAsJsonObject("floorDrops").entrySet().flatMap { (name, sites) ->
        sites.asJsonArray.map { block(it.asString) to SafariBiome.valueOf(name) }
    }.toMap()

    val mounds = catalog.getAsJsonArray("mounds").mapTo(linkedSetOf()) { block(it.asString) }

    fun floorBiome(x: Double, y: Double, z: Double): SafariBiome? =
        floorBiomes[Triple(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())]
}
