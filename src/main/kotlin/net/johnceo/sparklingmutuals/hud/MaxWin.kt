package net.johnceo.sparklingmutuals.hud

import java.awt.Color
import kotlin.math.sqrt
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.SafariTicketType
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource

internal class MaxWinState {
    private var run: Any? = null
    private var celebrated = 0
    var shownAt: Long? = null
        private set
    var coins = 0
        private set
    var musicAt: Long? = null
        private set

    fun poll(key: Any?, held: Int, ownStarting: Int, floorCoins: Int, remaining: Int,
             firstClass: Boolean?, now: Long, presentImmediately: Boolean = true): Boolean {
        if (key !== run) { reset(); run = key }
        if (key == null) return false
        if (held == 0) musicAt = null
        // Source counts deliberately exclude all teammate contributions.
        val maximum = ownStarting + floorCoins
        val target = when {
            maximum == 8 && firstClass != false -> 8
            maximum == 7 -> 7
            else -> return false
        }
        if (remaining != 0 || floorCoins <= 0 || held != target || target <= celebrated) return false
        celebrated = target; coins = target
        if (musicAt == null) musicAt = now
        if (presentImmediately) present(now)
        return true
    }
    fun visible(now: Long) = shownAt?.let { now - it in 0 until 5000 } == true
    fun gain(distance: Double): Float = if(musicAt==null) 0f else (distance/64.0).coerceIn(0.0,1.0).toFloat()
    fun present(now: Long) { shownAt = now }
    fun finishPresentation() { shownAt = null }
    fun hide() { shownAt = null; musicAt = null }
    fun reset() { run = null; celebrated = 0; shownAt = null; coins = 0; musicAt = null }
}

/** Local-only surprise. No commands, settings, party messages or external downloads. */
internal object MaxWin {
    private val state = MaxWinState()
    private val soundId = Identifier.fromNamespaceAndPath("sparkling-mutuals", "hidden_max_win")
    private val image = Identifier.fromNamespaceAndPath("sparkling-mutuals", "textures/hidden_max_win.png")
    private var song: Song? = null
    private var songStartedAt=0L

    private class Song : AbstractTickableSoundInstance(SoundEvent.createVariableRangeEvent(soundId), SoundSource.MASTER, RandomSource.create()) {
        init { looping = true; relative = true; attenuation = SoundInstance.Attenuation.NONE; volume = 1f }
        override fun canStartSilent() = true
        override fun tick() {
            if (state.musicAt == null) stop()
            else {
                val player=Minecraft.getInstance().player ?: return
                volume=state.gain(sqrt((player.x+3)*(player.x+3)+(player.y-61)*(player.y-61)+(player.z+57)*(player.z+57)))
            }
        }
    }
    fun tick(client: Minecraft) {
        val player = client.player
        val run = SafariTracking.ledger.current
        if (!SafariAssist.inSafari || player == null || run == null || run.endedAt != null) {
            stop(client); state.hide()
            if (run == null) state.reset()
            return
        }
        val held = (0 until player.inventory.containerSize).sumOf { slot ->
            val stack = player.inventory.getItem(slot)
            if (!stack.isEmpty && SafariRules.strip(stack.hoverName.string).trim() == "Shining Coin") stack.count else 0
        }
        val now = System.currentTimeMillis()
        val firstClass = run.entry.ticket?.let { it == SafariTicketType.FIRST_CLASS }
        if (state.poll(run, held, run.resources.startingCount(FloorDropType.COIN),
            run.gimmiegoldCheck.floorMinimum, run.gimmiegoldCheck.remaining, firstClass, now, presentImmediately = false)) {
            val coins = state.coins
            SafariAlertSequence.queue.submit(this, 5_000, coins,
                valid = { SafariAssist.inSafari && SafariTracking.ledger.current === run },
                finish = state::finishPresentation) {
                shownCoins = coins
                state.present(System.currentTimeMillis())
            }
        }
        if (state.musicAt == null) stop(client)
        else if (song==null || now-songStartedAt>1000 && !client.soundManager.isActive(song!!)) {
            songStartedAt=now
            song = Song().also { client.soundManager.play(it) }
        }
    }
    private fun stop(client: Minecraft) { song?.let { client.soundManager.stop(it) }; song = null }
    fun reset() { SafariAlertSequence.queue.clear(this); stop(Minecraft.getInstance()); state.reset() }

    private var shownCoins = 0
    fun render(g: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        val now = System.currentTimeMillis()
        if (!SafariAssist.inSafari || client.player == null || client.options.smHideGui || !state.visible(now)) return
        val width = client.window.guiScaledWidth; val height = client.window.guiScaledHeight
        val elapsed = now - (state.shownAt ?: return)
        val fade = minOf(1.0, (5000 - elapsed) / 800.0).coerceAtLeast(0.0)
        if (shownCoins == 8) {
            // Fill the viewport while preserving the supplied image's aspect ratio.
            val scale = maxOf(width / 498f, height / 410f)
            val w = (498 * scale).toInt(); val h = (410 * scale).toInt()
            g.blit(RenderPipelines.GUI_TEXTURED, image, (width - w) / 2, (height - h) / 2,
                0f, 0f, w, h, 498, 410, 498, 410, ((255 * .35 * fade).toInt() shl 24) or 0xFFFFFF)
        }
        fun rgb(x: Int) = Color.HSBtoRGB(((now % 5000) / 5000f + x.toFloat() / width.coerceAtLeast(1)) % 1f, .75f, 1f) and 0xFFFFFF
        val edgeAlpha = (255 * fade).toInt().coerceAtLeast(4) shl 24
        val washAlpha = (24 * fade).toInt() shl 24
        for (x in 0 until width step 4) {
            val right = minOf(width, x + 4); val color = rgb(x)
            g.fill(x, 0, right, height, washAlpha or color)
            g.fill(x, 0, right, 3, edgeAlpha or color)
            g.fill(x, height - 3, right, height, edgeAlpha or color)
        }
        g.fill(0, 0, 3, height, edgeAlpha or rgb(0))
        g.fill(width - 3, 0, width, height, edgeAlpha or rgb(width - 1))
        val title = "MAX WIN"
        val scale = minOf(4f, (width - 16f) / client.font.width(title))
        val y = height / 2f - 30
        g.pose().pushMatrix(); g.pose().translate(width / 2f, y); g.pose().scale(scale, scale)
        var x = -client.font.width(title) / 2
        title.forEachIndexed { i, c ->
            val text = c.toString()
            g.text(client.font, text, x, 0, edgeAlpha or rgb(i * width / title.length), true)
            x += client.font.width(text)
        }
        g.pose().popMatrix()
        val subtitle = "$shownCoins Shining Coins in inventory"
        val subScale = minOf(2f, (width - 16f) / client.font.width(subtitle))
        g.pose().pushMatrix(); g.pose().translate(width / 2f, y + client.font.lineHeight * scale + 14); g.pose().scale(subScale, subScale)
        g.text(client.font, subtitle, -client.font.width(subtitle) / 2, 0, edgeAlpha or 0xFFFFFF, true)
        g.pose().popMatrix()
    }
}
