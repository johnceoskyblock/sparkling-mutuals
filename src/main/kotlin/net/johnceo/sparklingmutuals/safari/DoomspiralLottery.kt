package net.johnceo.sparklingmutuals.safari

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.compat.smHideGui
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters
import net.johnceo.sparklingmutuals.sync.SafariPartySync
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.monster.warden.Warden
import net.minecraft.world.phys.AABB
import kotlin.random.Random

internal const val DOOMSPIRAL_CAPTURE_TICKS = 98
internal const val DOOMSPIRAL_JACKPOT_MASK = 0b111

/** A local reveal of Hypixel's encounter, driven by the same ping clock as the throw timer. */
object DoomspiralLottery {
    private const val RITUAL_X = -5.5
    private const val RITUAL_Y = 46.0
    private const val RITUAL_Z = -23.5
    private const val RADIUS = 15.0
    private const val LAST_CANDLE = "You used the Soothing Incense to light the candle! The ground beneath your feet starts to shift..."
    private const val SUMMONED = "Your ritual summoned a Doomspiral into this world. Stay still."
    private val chimes = setOf("minecraft:block.amethyst_block.chime", "minecraft:block.amethyst_block.resonate",
        "minecraft:block.note_block.chime")
    private val ritualBounds = AABB(RITUAL_X, RITUAL_Y, RITUAL_Z, RITUAL_X, RITUAL_Y, RITUAL_Z).inflate(RADIUS)
    private val countdown = DoomspiralCountdown()
    private var run: SafariRun? = null
    private var world: Any? = null
    private var lastPulse = 0L
    private var startAt: Long? = null
    private var summoned = false
    private var settleAt: Long? = null
    private var resultMask = 0
    private var resultConfirmed = false
    private var deferredChime: SoundInstance? = null

    internal val pendingOrActive get() = startAt != null
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

    private fun begin(client: Minecraft, start: Long) {
        if (!ConfigManager.doomspiralLottery || !SafariAssist.inSafari || SafariAssist.departing || !inRange(client)) return
        val level = client.level ?: return
        SafariTracking.ensureRun(client)
        val current = SafariTracking.ledger.current ?: return
        if (current.endedAt != null || current.count("Doomspiral") > 0 || run === current && world === level) return
        reset()
        run = current
        world = level
        // Pick one losing layout per ritual. A positive Sparkling detection upgrades it to the jackpot.
        resultMask = Random.nextInt(DOOMSPIRAL_JACKPOT_MASK)
        startAt = start
    }

    private fun observe(client: Minecraft, cue: String) {
        val now = System.nanoTime()
        if (cue == LAST_CANDLE) {
            begin(client, now + 1_000_000_000L)
            return
        }
        // Nearby players receive the public spawn cue without the candle placer's lead-in.
        if (cue == DOOMSPIRAL_STIRS && startAt == null) begin(client, now)
        if (startAt == null || world !== client.level || run !== SafariTracking.ledger.current) return
        if (cue == SUMMONED) summoned = true
        if (cue == DOOMSPIRAL_STIRS && countdown.trigger(cue, true, DOOMSPIRAL_CAPTURE_TICKS, 0)) {
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
            finish(client)
            return
        }
        if ((!summoned && remaining == null) || now < start || resultMask == DOOMSPIRAL_JACKPOT_MASK) return
        if (!SafariSettings.standardEdition && (run?.sparklingCaptures?.pending("Doomspiral") == true ||
            SafariPartySync.hasOutstandingSharedSparkling("Doomspiral"))) {
            resultMask = DOOMSPIRAL_JACKPOT_MASK
            return
        }
        when (visibleSparklingResult(client)) {
            true -> {
                resultConfirmed = true
                resultMask = DOOMSPIRAL_JACKPOT_MASK
            }
            false -> resultConfirmed = true
            null -> Unit
        }
    }

    private fun visibleSparklingResult(client: Minecraft): Boolean? {
        val level = client.level ?: return null
        val warden = level.getEntitiesOfClass(Warden::class.java, ritualBounds) {
            !it.isRemoved && it.isAlive
        }.minByOrNull { it.distanceToSqr(RITUAL_X, RITUAL_Y, RITUAL_Z) } ?: return null
        val labels = (level.getEntities(warden, warden.boundingBox.inflate(2.0)) + warden)
            .filter { SafariPreviewVisibility.label(client, it) }
            .flatMap {
                val text = if (it is Display.TextDisplay) it.text.string else it.customName?.string.orEmpty()
                SafariMessages.lines(text)
            }
        if (labels.any { SafariRules.sparklingSpecies(it) == "Doomspiral" }) return true
        return false.takeIf { labels.any { SafariEspRules.labelSpecies(it) == "Doomspiral" } }
    }

    fun serverTick(listener: Any, pingId: Int) {
        val client = Minecraft.getInstance()
        if (pingId == 0 || client.connection !== listener || world !== client.level || countdown.remaining == null) return
        countdown.serverTick()
        lastPulse = System.nanoTime()
        if (countdown.remaining == 0) {
            val jackpot = resultMask == DOOMSPIRAL_JACKPOT_MASK
            finish(client)
            if (jackpot) SparklingEncounters.doomspiralRevealed { visibleSparklingResult(client) == true }
        }
    }

    internal fun isChime(id: String, alertSound: String) = id == alertSound || id in chimes

    internal fun deferChime(sound: SoundInstance): Boolean {
        if (!pendingOrActive || !ConfigManager.doomspiralLottery || !SafariAssist.inSafari || SafariAssist.departing) return false
        val client = Minecraft.getInstance()
        if (client.level !== world || !inRange(client) || !isChime(sound.identifier.toString(), ConfigManager.sparklingSound)) return false
        if (!sound.isRelative && !ritualBounds.contains(sound.x, sound.y, sound.z)) return false
        // Keep the first cue. Repeated chimes during the animation should not all play at once.
        if (deferredChime == null) deferredChime = sound
        return true
    }

    private fun finish(client: Minecraft) {
        countdown.clear()
        startAt = null
        val chime = deferredChime
        deferredChime = null
        if (chime != null) client.soundManager.play(chime)
    }

    fun reset() {
        countdown.reset()
        run = null
        world = null
        startAt = null
        summoned = false
        settleAt = null
        resultMask = 0
        resultConfirmed = false
        deferredChime = null
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
        val settleElapsed = settleAt?.let { ((it - start) / 1_000_000_000f).coerceAtLeast(0f) }
        val ticksSinceStirs = if (remaining == null) 0f else DOOMSPIRAL_CAPTURE_TICKS - remaining +
            ((now - lastPulse) / 50_000_000f).coerceIn(0f, .99f)
        val outcome = resultMask.takeIf { !SafariSettings.standardEdition || resultConfirmed }
        val frame = DoomspiralCabinet.frame(elapsed, settleElapsed, remaining, ticksSinceStirs, outcome)
        DoomspiralCabinetRenderer.render(g, width, height, frame)
        return true
    }
}
