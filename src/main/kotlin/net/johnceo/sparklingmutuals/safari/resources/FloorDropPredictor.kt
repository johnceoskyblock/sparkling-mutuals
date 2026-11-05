package net.johnceo.sparklingmutuals.safari.resources

import kotlin.math.floor
import kotlin.math.round
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.esp.EspDrop
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules

enum class FloorDropType(val label: String) {
    LIME("Lime Gem"), PURPLE("Purple Gem"), ORANGE("Orange Gem"),
    WORM("Wriggleworm"), BERRY("Yogi Berry"), SEEDS("Bag of Seeds"),
    INCENSE("Soothing Incense"), COIN("Shining Coin"), ICEBREAKER("Icebreaker"),
    GENERIC("XP / Essence / Shard")
}

data class FloorDisplay(val id: Int, val x: Double, val y: Double, val z: Double, val description: String)
data class PredictedFloorDrop(val id: Int, val x: Double, val y: Double, val z: Double,
    val biome: SafariBiome?, var type: FloorDropType? = null, var live: Boolean = true,
    var seenAt: Long = 0, var actual: FloorPickup? = null) {
    fun marker() = EspDrop(id, floor(x).toInt(), floor(y).toInt(), floor(z).toInt())
}

/** Order prediction documented in FLOOR_DROPS.md. History anchors survive unloads and pickups.
 * Labels are predictions only; this class never supplies resource or completion evidence. */
class FloorDropPredictor {
    // Layout consensus adapted from SafariUtils FloorDropPattern (MIT).
    private data class Layout(val base: Int, val coins: Int, val ice: Int) {
        fun kind(id: Int): FloorDropType? {
            val delta = id - base
            if (delta < 0) return null
            if (delta >= (20 + coins + ice) * 3) return FloorDropType.GENERIC
            if (delta % 3 != 0) return null
            val slot = delta / 3
            return when {
                slot < 6 -> gems[slot % 3]
                slot < 15 -> food[(slot - 6) / 3]
                slot < 20 -> FloorDropType.INCENSE
                slot < 20 + coins -> FloorDropType.COIN
                else -> FloorDropType.ICEBREAKER
            }
        }
    }

    private companion object {
        val gems = listOf(FloorDropType.LIME, FloorDropType.PURPLE, FloorDropType.ORANGE)
        val food = listOf(FloorDropType.WORM, FloorDropType.BERRY, FloorDropType.SEEDS)
    }

    private val groups = sortedMapOf<Int, PredictedFloorDrop>()
    val knownDrops: List<PredictedFloorDrop> get() = groups.values.toList()
    fun reset() = groups.clear()
    fun scan(displays: List<FloorDisplay>, now: Long = System.currentTimeMillis()): List<PredictedFloorDrop> {
        groups.values.forEach { it.live = false }
        // Spawn packets can arrive before item metadata. Empty/decorative triples must never
        // become persistent anchors: their IDs would shift every later resource prediction.
        val markers = displays.filter { it.description == "minecraft:string" || it.description.startsWith("minecraft:string:") }
        val byId = markers.associateBy { it.id }
        val consumed = mutableSetOf<Int>()
        for (a in markers.sortedBy { it.id }) {
            if (a.id in consumed) continue
            val triple = listOf(a, byId[a.id + 1] ?: continue, byId[a.id + 2] ?: continue)
            if (triple.any { it.id in consumed } || triple.any { p -> triple.any { q ->
                    (p.x-q.x)*(p.x-q.x)+(p.y-q.y)*(p.y-q.y)+(p.z-q.z)*(p.z-q.z) > 2.25
                } }) continue
            if (triple.map { it.description }.distinct().size != 1) continue
            consumed.addAll(triple.map { it.id })
            val x = round(triple.sumOf { it.x } / 3 * 100) / 100
            val y = round(triple.sumOf { it.y } / 3 * 100) / 100
            val z = round(triple.sumOf { it.z } / 3 * 100) / 100
            groups.getOrPut(a.id) { PredictedFloorDrop(a.id, x, y, z, SafariWaypointSites.floorBiome(x, y, z) ?: SafariEspRules.biomeAt(x, z)) }.also {
                it.live = true; it.seenAt = now
            }
        }
        val base = groups.keys.firstOrNull()
        if (base != null && (0 until 20).all { slot ->
                groups[base + slot * 3]?.biome == when {
                    slot < 6 -> SafariBiome.CAVERN
                    slot < 15 -> SafariBiome.FOREST
                    else -> SafariBiome.HAUNTED
                }
            }) {
            val layouts = (3..6).flatMap { coins -> (4..5).map { ice -> Layout(base, coins, ice) } }.filter { layout ->
                groups.values.all { group ->
                    val kind = layout.kind(group.id)
                    kind != null && (kind == FloorDropType.GENERIC || when (kind) {
                        in gems -> group.biome == SafariBiome.CAVERN
                        in food -> group.biome == SafariBiome.FOREST
                        FloorDropType.ICEBREAKER -> group.biome == SafariBiome.ICY
                        else -> group.biome == SafariBiome.HAUNTED
                    })
                }
            }
            // Pickup chat is matched by proximity. A wrong match must never erase a verified ID prefix.
            val corroborated = layouts.filter { layout ->
                groups.values.none { group -> group.actual?.let { it.type != layout.kind(group.id) } == true }
            }.ifEmpty { layouts }
            val preferred = corroborated.filter { it.ice == 5 }.ifEmpty { corroborated }
            val shortest = Layout(base, coins = 3, ice = 4)
            val longest = Layout(base, coins = 6, ice = 5)
            groups.values.forEach { group ->
                val kinds = preferred.map { it.kind(group.id) }.distinct()
                group.type = kinds.singleOrNull() ?: shortest.kind(group.id)?.takeIf { it == longest.kind(group.id) }
            }
            return groups.values.filter { it.live }
        }
        var phase = 0
        var anchor: Int? = null
        var tail = false
        val phases = listOf(SafariBiome.CAVERN, SafariBiome.FOREST, SafariBiome.HAUNTED, SafariBiome.ICY)
        for (group in groups.values) {
            val next = phases.indexOf(group.biome)
            if (next < 0) { group.type = null; continue }
            if (next > phase && !tail) { phase = next; anchor = null }
            if (next != phase) tail = true
            if (anchor == null) anchor = group.id
            val i = (group.id - anchor) / 3
            group.type = if (tail) FloorDropType.GENERIC else when (group.biome) {
                SafariBiome.CAVERN -> listOf(FloorDropType.LIME, FloorDropType.PURPLE, FloorDropType.ORANGE).getOrNull(i % 3)?.takeIf { i < 6 }
                SafariBiome.FOREST -> listOf(FloorDropType.WORM, FloorDropType.BERRY, FloorDropType.SEEDS).getOrNull(i / 3)
                SafariBiome.HAUNTED -> if (i < 5) FloorDropType.INCENSE else FloorDropType.COIN.takeIf { i < 11 }
                SafariBiome.ICY -> FloorDropType.ICEBREAKER.takeIf { i < 5 }
                else -> null
            } ?: FloorDropType.GENERIC.also { tail = true }
        }
        return groups.values.filter { it.live }
    }
    /** Nearby matching is a heuristic for retiring a visual marker, never pickup evidence itself. */
    fun pickup(reward: FloorPickup, x: Double, y: Double, z: Double, now: Long): PredictedFloorDrop? {
        fun distance(drop: PredictedFloorDrop) = (drop.x-x)*(drop.x-x)+(drop.y-y)*(drop.y-y)+(drop.z-z)*(drop.z-z)
        val nearby = groups.values.filter { it.actual == null && now - it.seenAt in 0..3000 && distance(it) <= 25 }
            .sortedBy(::distance)
        val nearest = nearby.firstOrNull() ?: return null
        if (nearby.size > 1 && kotlin.math.abs(distance(nearest) - distance(nearby[1])) < .01) return null
        nearest.actual = reward
        return nearest
    }
}

data class FloorPickup(val type: FloorDropType, val quantity: Int) {
    companion object {
        fun parse(raw: String): FloorPickup? {
            val text = SafariRules.strip(raw)
            if (!text.startsWith("FLOOR DROP!")) return null
            for (type in FloorDropType.entries.filter { it != FloorDropType.GENERIC }) {
                val match = Regex("(?:([0-9]+)x?\\s+)?\\b${Regex.escape(type.label)}\\b(?! Shard)(?:\\s+x([0-9]+))?", RegexOption.IGNORE_CASE).find(text) ?: continue
                return FloorPickup(type, (match.groupValues[1].toIntOrNull() ?: match.groupValues[2].toIntOrNull() ?: 1).coerceAtLeast(1))
            }
            return null
        }
    }
}
