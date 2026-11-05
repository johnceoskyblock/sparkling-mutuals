package net.johnceo.sparklingmutuals.safari

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.compat.smHideGui
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters
import net.johnceo.sparklingmutuals.sync.SafariPartySync
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.monster.warden.Warden
import net.minecraft.world.phys.AABB
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** A local reveal of Hypixel's encounter, driven by the same ping clock as the throw timer. */
object DoomspiralLottery {
    private const val RITUAL_X = -5.5
    private const val RITUAL_Y = 46.0
    private const val RITUAL_Z = -23.5
    private const val RADIUS = 15.0
    private const val CAPTURE_TICKS = 98
    private const val REVEAL_TICKS = 5
    private const val JACKPOT = 0b111
    private const val LAST_CANDLE = "You used the Soothing Incense to light the candle! The ground beneath your feet starts to shift..."
    private const val SUMMONED = "Your ritual summoned a Doomspiral into this world. Stay still."
    private val ritualBounds = AABB(RITUAL_X, RITUAL_Y, RITUAL_Z, RITUAL_X, RITUAL_Y, RITUAL_Z).inflate(RADIUS)
    private val icon = Identifier.fromNamespaceAndPath("sparkling-mutuals", "textures/gui/doomspiral_lottery.png")
    private val countdown = DoomspiralCountdown()
    private var run: SafariRun? = null
    private var world: Any? = null
    private var lastPulse = 0L
    private var startAt: Long? = null
    private var summoned = false
    private var settleAt: Long? = null
    private var resultMask = 0

    internal val active get() = startAt?.let { System.nanoTime() >= it } == true

    internal fun inRange(x: Double, y: Double, z: Double): Boolean {
        val dx = x - RITUAL_X
        val dy = y - RITUAL_Y
        val dz = z - RITUAL_Z
        return dy <= 10.0 && dx * dx + dy * dy + dz * dz <= RADIUS * RADIUS
    }

    private fun inRange(client: Minecraft) = client.player?.let { inRange(it.x, it.y, it.z) } == true

    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) {
                val cues = SafariMessages.lines(message.string).filter {
                    it == LAST_CANDLE || it == SUMMONED || it == DOOMSPIRAL_STIRS
                }
                if (cues.isNotEmpty()) {
                    val client = Minecraft.getInstance()
                    client.execute { cues.forEach { cue -> observe(client, cue) } }
                }
            }
        }
    }

    private fun observe(client: Minecraft, cue: String) {
        val now = System.nanoTime()
        if (cue == LAST_CANDLE) {
            if (!ConfigManager.doomspiralLottery || !SafariAssist.inSafari || SafariAssist.departing || !inRange(client)) return
            SafariTracking.ensureRun(client)
            val current = SafariTracking.ledger.current ?: return
            if (current.endedAt != null || current.count("Doomspiral") > 0 || client.level == null) return
            if (run === current && world === client.level) return
            reset()
            run = current
            world = client.level
            // Pick one losing layout per ritual. A positive Sparkling detection upgrades it to the jackpot.
            resultMask = Random.nextInt(JACKPOT)
            startAt = now + 1_000_000_000L
            return
        }
        if (startAt == null || world !== client.level || run !== SafariTracking.ledger.current) return
        if (cue == SUMMONED) summoned = true
        if (cue == DOOMSPIRAL_STIRS && countdown.trigger(cue, true, CAPTURE_TICKS, 0)) {
            lastPulse = now
            settleAt = now
        }
    }

    fun tick(client: Minecraft) {
        if (world == null) return
        if (world !== client.level || run !== SafariTracking.ledger.current || !SafariAssist.inSafari || SafariAssist.departing) {
            reset()
            return
        }
        val start = startAt ?: return
        val now = System.nanoTime()
        val remaining = countdown.remaining
        val timingExpired = if (remaining == null) now - start > 15_000_000_000L
            else remaining == 0 || now - lastPulse > 2_500_000_000L
        if (timingExpired || !ConfigManager.doomspiralLottery || !inRange(client) ||
            client.smScreen != null || client.options.smHideGui || run?.endedAt != null ||
            (run?.count("Doomspiral") ?: 0) > 0) {
            finish()
            return
        }
        if ((!summoned && remaining == null) || now < start || resultMask == JACKPOT) return
        if (run?.sparklingCaptures?.pending("Doomspiral") == true ||
            SafariPartySync.hasOutstandingSharedSparkling("Doomspiral")) {
            resultMask = JACKPOT
            return
        }
        val level = client.level ?: return
        val warden = level.getEntitiesOfClass(Warden::class.java, ritualBounds) {
            !it.isRemoved && it.isAlive
        }.minByOrNull { it.distanceToSqr(RITUAL_X, RITUAL_Y, RITUAL_Z) } ?: return
        val labels = level.getEntities(warden, warden.boundingBox.inflate(2.0))
            .mapNotNull { it.customName?.string } + listOfNotNull(warden.customName?.string)
        if (labels.any { SafariRules.sparklingSpecies(it) == "Doomspiral" }) {
            resultMask = JACKPOT
        }
    }

    fun serverTick(listener: Any, pingId: Int) {
        val client = Minecraft.getInstance()
        if (pingId == 0 || client.connection !== listener || world !== client.level || countdown.remaining == null) return
        countdown.serverTick()
        lastPulse = System.nanoTime()
        if (countdown.remaining == 0) {
            finish()
            if (resultMask == JACKPOT) SparklingEncounters.doomspiralRevealed()
        }
    }

    private fun finish() {
        countdown.clear()
        startAt = null
    }

    fun reset() {
        countdown.reset()
        run = null
        world = null
        startAt = null
        summoned = false
        settleAt = null
        resultMask = 0
    }

    /** Returns true when the lottery takes over the vanilla and mod HUD for this frame. */
    fun render(g: GuiGraphicsExtractor): Boolean {
        val client = Minecraft.getInstance()
        val start = startAt ?: return false
        val now = System.nanoTime()
        if (now < start || !ConfigManager.doomspiralLottery || client.level !== world || client.smScreen != null ||
            client.options.smHideGui || !inRange(client)) return false
        val width = client.window.guiScaledWidth
        val height = client.window.guiScaledHeight
        val elapsed = (now - start) / 1_000_000_000f
        val remaining = countdown.remaining
        val spinTicks = CAPTURE_TICKS - REVEAL_TICKS
        val settleElapsed = settleAt?.let { ((it - start) / 1_000_000_000f).coerceAtLeast(0f) }
        val ticksSinceStirs = if (remaining == null) 0f else CAPTURE_TICKS - remaining +
            ((now - lastPulse) / 50_000_000f).coerceIn(0f, .99f)
        val result = resultMask
        val win = result == JACKPOT
        val revealed = remaining != null && remaining <= REVEAL_TICKS
        val accent = if (revealed && win) 0xFFFFD87A.toInt() else 0xFF67D9DB.toInt()
        g.fill(0, 0, width, height, 0xFF09141B.toInt())
        g.pose().pushMatrix()
        g.pose().translate(width / 2f, height / 2f)
        val scale = minOf(1.6f, width / 224f, height / 148f)
        g.pose().scale(scale, scale)
        g.fill(-105, -58, 105, 62, 0xFF11272E.toInt())
        g.outline(-105, -58, 210, 120, accent)
        fun centered(text: String, y: Int, color: Int) {
            g.text(client.font, text, -client.font.width(text) / 2, y, color, true)
        }
        centered("DOOMSPIRAL LOTTERY", -47, accent)
        repeat(3) { index ->
            val offset = if (settleElapsed == null) spinOffset(elapsed, index) else {
                val from = spinOffset(settleElapsed, index)
                val showsDoom = result and (1 shl index) != 0
                val to = ceil((from + 4) / 6) * 6 + if (showsDoom) 0 else 1
                val spin = (ticksSinceStirs / spinTicks).coerceIn(0f, 1f)
                if (spin == 1f) to else from + (to - from) * (1f - (1f - spin) * (1f - spin))
            }
            reel(g, (index - 1) * 62, offset, elapsed, index)
        }
        if (revealed) centered(if (win) "SPARKLING DOOMSPIRAL!" else "Doomspiral", 37, accent)
        val revealAge = ticksSinceStirs - spinTicks
        if (revealed && win && revealAge < 13f) {
            repeat(12) { index ->
                val angle = index * Math.PI / 6
                val radius = 72 + revealAge * .8
                spark(g, (cos(angle) * radius).toInt(), (sin(angle) * radius * .66).toInt(), accent)
            }
        }
        remaining?.let {
            g.fill(-90, 52, 90, 54, 0xFF283F46.toInt())
            g.fill(-90, 52, -90 + 180 * remaining / CAPTURE_TICKS, 54, accent)
        }
        g.pose().popMatrix()
        return true
    }

    private fun spinOffset(seconds: Float, index: Int) =
        seconds * 7f + 14f * (1f - exp(-seconds / 1.8f)) + index * 2

    // Clipped vertical reels and quadratic easing adapted from SkyOcean's SlotMachineSpinner.
    private fun reel(g: GuiGraphicsExtractor, x: Int, offset: Float, elapsed: Float, index: Int) {
        val base = floor(offset).toInt()
        g.fill(x - 27, -30, x + 27, 29, 0xFF07151B.toInt())
        g.enableScissor(x - 26, -29, x + 26, 28)
        for (row in -1..2) {
            val y = -((offset - base - row) * 56).toInt()
            if ((base + row) % 6 == 0) {
                val phase = elapsed * 3f + index
                val bob = (sin(phase) * 2f).toInt()
                g.blit(RenderPipelines.GUI_TEXTURED, icon, x - 19, y - 25 + bob,
                    0f, 0f, 38, 50, 230, 307, 230, 307, -1)
                repeat(6) { particle ->
                    val angle = phase + particle * Math.PI / 3
                    val alpha = (155 + 100 * sin(angle * 2)).toInt()
                    spark(g, x + (cos(angle) * 21).toInt(), y + (sin(angle) * 20).toInt(),
                        (alpha shl 24) or if (particle % 2 == 0) 0xFFC960 else 0xFFE6A4)
                }
            } else {
                val font = Minecraft.getInstance().font
                g.pose().pushMatrix()
                g.pose().translate(x.toFloat(), y.toFloat())
                g.pose().scale(2.8f, 2.8f)
                g.text(font, "✕", -font.width("✕") / 2, -4, 0xFFFF525D.toInt(), true)
                g.pose().popMatrix()
            }
        }
        g.disableScissor()
        g.outline(x - 27, -30, 54, 59, 0xFF39616B.toInt())
    }

    private fun spark(g: GuiGraphicsExtractor, x: Int, y: Int, color: Int) {
        g.fill(x - 2, y, x + 3, y + 1, color)
        g.fill(x, y - 2, x + 1, y + 3, color)
    }
}
