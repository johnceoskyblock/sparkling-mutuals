package net.johnceo.sparklingmutuals.history.ui

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.history.SparklingEntry
import net.johnceo.sparklingmutuals.history.SparklingLog
import net.johnceo.sparklingmutuals.history.importing.LogImporter
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/** Read-only viewer: compressed archives are opened in memory, never extracted or changed on disk. */
class HistoryLogScreen(private val parent: Screen, private val entry: SparklingEntry) : Screen(Component.literal("Catch log")) {
    private var source: LogImporter.CatchLog? = null
    private var lines: List<String> = emptyList()
    private var status = "Finding the original catch in your logs…"
    private var started = false
    private var offset = 0
    private var horizontal = 0
    private var longestLine = 0
    private var dragging = false
    private var grab = 0.0
    private val top get() = 47
    private val bottom get() = maxOf(top + 11, height - 43)
    private val rows get() = maxOf(1, (bottom - top) / 11)
    private val track get() = HistoryScrollTrack(top, bottom, rows, lines.size)

    override fun init() {
        addRenderableWidget(Button.builder(Component.literal("Back")) { onClose() }
            .bounds(width - 78, height - 26, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Jump to catch")) { jump() }
            .bounds(10, height - 26, 104, 20).build())
        if (!started) {
            started = true
            val owner = minecraft.player?.uuid
            val self = minecraft.player?.name?.string.orEmpty()
            LogImporter.findCatch(entry, self).whenComplete { found, error -> minecraft.execute {
                if (minecraft.player?.uuid != owner) { status = "Account changed. Return to history and try again."; return@execute }
                if (error != null || found == null) {
                    status = "Original log unavailable. It may have been moved or deleted."
                } else {
                    source = found
                    lines = found.lines.map { SafariRules.strip(it) }
                    longestLine = lines.maxOfOrNull { it.length } ?: 0
                    entry.logFile = found.path.toString(); entry.logLine = found.line + 1
                    SparklingLog.save()
                    jump()
                }
            } }
        }
        offset = offset.coerceIn(0, track.maxOffset)
    }
    private fun jump() {
        source?.let { offset = (it.line - rows / 2).coerceIn(0, track.maxOffset); horizontal = 0 }
    }
    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        g.fill(0, 0, width, height, 0xF0101014.toInt())
        g.text(font, "${entry.critter} · ${entry.player}", 10, 10, 0xFFFFC94A.toInt())
        val found = source
        val heading = found?.let { "${it.path.fileName} · line ${it.line + 1}" } ?: "Catch log"
        g.text(font, font.plainSubstrByWidth(heading, (width - 20).coerceAtLeast(1)), 10, 26, 0xFFBBBBBB.toInt())
        if (found == null) {
            val message = font.split(Component.literal(status), (width - 32).coerceAtLeast(1))
            message.forEachIndexed { i, text -> g.text(font, text, 16, top + 12 + i * 11, 0xFFDDDDDD.toInt()) }
        } else {
            offset = offset.coerceIn(0, track.maxOffset)
            val numberWidth = font.width(lines.size.toString()) + 12
            val available = (width - numberWidth - 34).coerceAtLeast(1)
            for (row in 0 until rows) {
                val index = offset + row
                val line = lines.getOrNull(index) ?: break
                val y = top + row * 11
                val selected = index == found.line
                if (selected) g.fill(8, y - 1, width - 19, y + 10, 0x604C3C10)
                g.text(font, "${index + 1}", 10, y, if (selected) 0xFFFFC94A.toInt() else 0xFF777777.toInt())
                g.text(font, font.plainSubstrByWidth(line.drop(horizontal), available), 10 + numberWidth, y,
                    if (selected) 0xFFFFE7A0.toInt() else 0xFFDDDDDD.toInt())
            }
            if (track.scrollable) {
                val thumb = track.thumbTop(offset)
                g.fill(width - 14, top, width - 8, bottom, 0x40555555)
                g.fill(width - 14, thumb, width - 8, thumb + track.thumbHeight, 0xFFE8A33A.toInt())
            }
        }
        g.text(font, "Scroll to browse · Shift + scroll sideways", 10, height - 38, 0xFF999999.toInt())
        super.extractRenderState(g, mx, my, delta)
    }
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (shiftDown(minecraft) || scrollX != 0.0) {
            val amount = if (scrollX != 0.0) scrollX else scrollY
            horizontal = (horizontal + if (amount > 0) -12 else 12).coerceIn(0, longestLine)
        } else if (scrollY != 0.0) offset = (offset + if (scrollY > 0) -3 else 3).coerceIn(0, track.maxOffset)
        return true
    }
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == 0 && track.scrollable && event.x() >= width - 18 && event.x() < width - 4 && event.y() >= top && event.y() < bottom) {
            val thumb = track.thumbTop(offset)
            grab = if (event.y() >= thumb && event.y() < thumb + track.thumbHeight) event.y() - thumb else track.thumbHeight / 2.0
            dragging = true; offset = track.offsetAt(event.y(), grab)
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }
    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (event.button() == 0 && dragging) { offset = track.offsetAt(event.y(), grab); return true }
        return super.mouseDragged(event, dx, dy)
    }
    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0 && dragging) { dragging = false; return true }
        return super.mouseReleased(event)
    }
    override fun isPauseScreen() = false
    override fun onClose() { minecraft.smSetScreen(parent) }
}
