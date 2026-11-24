package net.johnceo.sparklingmutuals.safari.hideyho

import kotlin.math.floor
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.config.BannerConfig
import net.johnceo.sparklingmutuals.hud.BannerKind
import net.johnceo.sparklingmutuals.hud.SafariBanners
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

/** Where Hideyho can hide and the mansion's outline: the pure part of [HideyhoSpotter], so it can be tested. */
object HideyhoHiding {
    data class Spot(val x: Int, val y: Int, val z: Int) {
        override fun toString() = "$x, $y, $z"
    }
    /** How close Hideyho has to be to a listed spot to count as settled there, in blocks. */
    const val SPOT_RADIUS = 3.5

    /** Every spot Hideyho has been seen hiding at in the Safari. */
    val SPOTS = listOf(
        Spot(13, 71, -88), Spot(-4, 68, -64), Spot(19, 70, -24), Spot(-28, 72, -80), Spot(-4, 71, -79),
        Spot(37, 70, -14), Spot(-20, 71, -80), Spot(-6, 87, -63), Spot(13, 79, -50), Spot(27, 71, -53),
        Spot(12, 71, -61), Spot(0, 79, -78), Spot(-13, 79, -84), Spot(-13, 79, -57), Spot(0, 71, -56),
        Spot(-7, 79, -83), Spot(-17, 71, -59), Spot(18, 79, -63), Spot(-21, 79, -78), Spot(-8, 87, -70))

    /**
     * The mansion's footprint, corners (x, z) in walking order. The building isn't a box and its floors sit at
     * different heights, so only the outline decides inside or outside.
     */
    val MANSION = listOf(-28.5 to -46.5, -28.5 to -75.5, -23.5 to -75.5, -24.5 to -85.5, 10.0 to -87.0, 10.0 to -84.0,
        17.0 to -84.0, 17.0 to -76.0, 21.0 to -76.0, 21.0 to -55.0, 17.0 to -55.0, 17.0 to -47.0)

    /** The listed spot an entity at this position is settled on, if any (block positions, as the server sends them). */
    fun nearestSpot(x: Double, y: Double, z: Double, spots: List<Spot> = SPOTS): Spot? {
        val bx = floor(x).toInt(); val by = floor(y).toInt(); val bz = floor(z).toInt()
        return spots.map { it to ((it.x - bx) * (it.x - bx) + (it.y - by) * (it.y - by) + (it.z - bz) * (it.z - bz)).toDouble() }
            .filter { it.second <= SPOT_RADIUS * SPOT_RADIUS }.minByOrNull { it.second }?.first
    }

    /** Ray-casting point-in-polygon on the mansion's footprint. */
    fun insideMansion(x: Double, z: Double, corners: List<Pair<Double, Double>> = MANSION): Boolean {
        if (corners.size < 3) return false
        var inside = false
        var j = corners.size - 1
        for (i in corners.indices) {
            val (xi, zi) = corners[i]
            val (xj, zj) = corners[j]
            if ((zi > z) != (zj > z) && x < (xj - xi) * (z - zi) / (zj - zi) + xi) inside = !inside
            j = i
        }
        return inside
    }
    fun inside(spot: Spot) = insideMansion(spot.x + .5, spot.z + .5)

    /** Hideyho running off to hide, and being found (the quest is over). NPC lines only, never relayed chat. */
    private val accepted = Regex("^\\[MOB] Hideyho: (?:Close your eyes and then come find me!|No peeking!)$")
    private val found = Regex("^\\[MOB] Hideyho: (?:Aah! You found me!|Hehe, you found me!)$")
    fun accepted(text: String) = accepted.matches(text)
    fun found(text: String) = found.matches(text)
}

/**
 * Tracks the initial and hidden locations for the encounter banner. Adapted
 * from Shiny Hunter's HideyhoSpotter (MIT).
 *
 * Accepting the quest arms a watch. Hideyho vanishes and is re-sent at its hiding spot, surfacing from below, so the
 * verdict waits until it is within a few blocks of a known spot. With no quest running, a Hideyho already sitting on
 * a spot is called out once, which covers walking into the Safari with it already hidden.
 */
class HideyhoLocationWatch {
    private var active = false
    private var armedAt: Long? = null
    private var spotBefore: HideyhoHiding.Spot? = null
    private var announced: HideyhoHiding.Spot? = null
    private var hidingAnnounced = false
    val armed get() = armedAt != null
    fun context(enabled: Boolean, biome: SafariBiome?, mode: SafariMode = SafariMode.UNIQUE): Boolean {
        active = enabled && biome == SafariBiome.HAUNTED && mode != SafariMode.SPARKLING
        // Travel pauses observation; disabling the feature or changing mode cancels it.
        if(!enabled || mode == SafariMode.SPARKLING) armedAt = null
        return active
    }
    fun chat(text: String, now: Long, spot: HideyhoHiding.Spot?) {
        if (!active) return
        when {
            HideyhoHiding.accepted(text) && !armed && !hidingAnnounced -> {
                spotBefore = spot ?: announced
                armedAt = now
            }
            // The initial greeting also says "you found me", before the hiding quest starts.
            HideyhoHiding.found(text) && armed -> { armedAt = null; hidingAnnounced = true }
        }
    }
    fun settled(spot: HideyhoHiding.Spot?, now: Long): HideyhoHiding.Spot? {
        if (!active) return null
        if (armedAt?.let { now - it > 90_000L } == true) { armedAt = null; return null }
        if (spot == null) return null
        if (armed) {
            if (spot == spotBefore) return null
            armedAt = null
            hidingAnnounced = true
        } else if (announced != null || hidingAnnounced) return null
        announced = spot
        return spot
    }
    fun reset() { active = false; armedAt = null; spotBefore = null; announced = null; hidingAnnounced = false }
}

object HideyhoSpotter {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/hideyho")
    private const val SCAN_TICKS = 5
    private const val IDLE_TICKS = 20

    private val watch = HideyhoLocationWatch()
    private var ticks = 0
    private var run: Any? = null
    private val kind = BannerKind.HIDEYHO_LOCATION
    private fun enabled() = BannerConfig.enabled && BannerConfig.options.getValue(kind).enabled
    private fun context() = watch.context(enabled(), SafariAssist.biome.takeIf { SafariAssist.inSafari }, SafariFullClear.mode)

    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { chat(SafariRules.strip(message.string).trim()) }
        }
    }

    private fun chat(text: String) {
        if (!context()) return
        watch.chat(text, System.currentTimeMillis(), position())
    }

    fun onClientTick(client: Minecraft) {
        val current = SafariTracking.ledger.current
        if (current !== run) { reset(); run = current }
        if (current == null || client.player == null || current.count("Hideyho") > 0 || !context()) return
        if (++ticks < if (watch.armed) SCAN_TICKS else IDLE_TICKS) return
        ticks = 0
        val spot = position(knownOnly = watch.armed)
        watch.settled(spot, System.currentTimeMillis())?.let { spot ->
            val location = if (HideyhoHiding.inside(spot)) "Inside" else "Outside"
            logger.info("Hideyho location: {} ({})", spot, location)
            SafariBanners.fire(kind, SafariBiome.HAUNTED, location = location)
        }
    }

    private fun position(knownOnly: Boolean = false): HideyhoHiding.Spot? {
        val pos = SafariEsp.hideyhoPosition() ?: return null
        return HideyhoHiding.nearestSpot(pos.x, pos.y, pos.z)
            ?: if (knownOnly) null else HideyhoHiding.Spot(floor(pos.x).toInt(), floor(pos.y).toInt(), floor(pos.z).toInt())
    }

    fun reset() { watch.reset(); ticks = 0; run = null }
}
