package net.johnceo.sparklingmutuals.safari

/** Nearest-node area lookup adapted from CritterMod v0.9.0; table originates from SkyHanni's Safari graph. */
object SafariAreaMap {
    private data class Node(val x: Double, val y: Double, val z: Double, val area: Int)
    private val nodes: List<Node> by lazy {
        javaClass.getResourceAsStream("/assets/sparkling-mutuals/safari_areas.txt")?.bufferedReader()?.useLines { lines ->
            lines.filter(String::isNotBlank).map { line ->
                val parts = line.trim().split(Regex("\\s+"))
                Node(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toInt())
            }.toList()
        }.orEmpty()
    }

    /** The western Forest canopy extends over Cavern near the Woodchucker trees. */
    internal fun isForestCanopy(x: Double, y: Double, z: Double) =
        x in -58.0..-50.0 && y in 89.0..112.0 && z in 36.0..56.0

    fun areaAt(x: Double, y: Double, z: Double): Int? {
        if (isForestCanopy(x, y, z)) return 1
        var best = 1600.0
        var area: Int? = null
        nodes.forEach { node ->
            val distance = (x - node.x) * (x - node.x) + (y - node.y) * (y - node.y) + (z - node.z) * (z - node.z)
            if (distance <= best) { best = distance; area = node.area.takeIf { it in 0..4 } }
        }
        return area
    }
    fun biomeAt(x: Double, y: Double, z: Double) = areaAt(x, y, z)?.takeIf { it in 1..4 }
}
