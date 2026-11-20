package net.johnceo.sparklingmutuals.honeyhives

import kotlin.math.abs
import net.johnceo.sparklingmutuals.safari.SafariRules

data class HiveLabel(val x: Double, val y: Double, val z: Double, val text: String)

object HoneyHiveLabels {
    fun statusLines(raw: String): List<String> = SafariRules.strip(raw).replace('\u00a0', ' ').lines()
        .map(String::trim).filter { it.equals("Click to loot!", true) || HoneyHiveTracker.countdownMillis(it) != null }
    fun byHive(hives: Collection<Hive>, labels: List<HiveLabel>): Map<String, String> {
        if (labels.isEmpty() || hives.isEmpty()) return emptyMap()
        val matches = HashMap<String, String>()
        val withCountdown = HashSet<String>()
        for (label in labels) {
            val hive = nearestHive(hives, label) ?: continue
            val withinBounds = abs(label.x - hive.x - .5) <= 1.5 && abs(label.z - hive.z - .5) <= 1.5 &&
                label.y in (hive.y - 1.0)..hive.y + 4.0
            if (!withinBounds) continue
            val key = hive.key
            if (key in withCountdown) continue
            if (HoneyHiveTracker.countdownMillis(label.text) != null) {
                withCountdown.add(key)
                matches[key] = label.text
            } else {
                matches.putIfAbsent(key, label.text)
            }
        }
        return matches
    }

    private fun nearestHive(hives: Collection<Hive>, label: HiveLabel): Hive? {
        var nearest: Hive? = null
        var firstDistance = Double.NaN
        var secondDistance = Double.NaN
        for (candidate in hives) {
            val dx = candidate.x + .5 - label.x
            val dy = candidate.y + 1.5 - label.y
            val dz = candidate.z + .5 - label.z
            val distance = dx * dx + dy * dy + dz * dz
            if (nearest == null || distance.compareTo(firstDistance) < 0) {
                nearest = candidate
                secondDistance = firstDistance
                firstDistance = distance
            } else if (distance.compareTo(secondDistance) < 0) {
                secondDistance = distance
            }
        }
        return nearest.takeIf { hives.size == 1 || abs(firstDistance - secondDistance) > .01 }
    }
}
