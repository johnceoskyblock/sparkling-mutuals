package net.johnceo.sparklingmutuals.contest

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ContestConfig
import net.johnceo.sparklingmutuals.hud.*
import net.johnceo.sparklingmutuals.safari.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/** All HUDs have previews even when disabled or outside Safari, following v0.9.0's editor. */
class ContestGui(private val parent: Screen? = null) : Screen(Component.literal("Sparkling Mutuals HUDs")) {
    private data class Preview(val label: String, val layout: HudLayout, val width: Int, val height: Int,
        val draw: (GuiGraphicsExtractor, Int, Int) -> Unit)
    private val previews = mutableListOf<Preview>()
    private val miria = HudLayout(0f, 0f)
    private var dragging: Preview? = null
    private var offsetX = 0.0
    private var offsetY = 0.0
    override fun init() {
        miria.x = ContestConfig.hudX.toFloat() / width.coerceAtLeast(1)
        miria.y = ContestConfig.hudY.toFloat() / height.coerceAtLeast(1)
        miria.scale = ContestConfig.hudScale
        previews.clear()
        previews.add(Preview("Miria contest", miria, ContestHud.WIDTH, ContestHud.HEIGHT) { g, x, y ->
            ContestConfig.hudScale = miria.scale; ContestHud.renderPreview(g, x, y)
        })
        val run = SafariPanels.previewRun()
        val panels = listOf(SafariPanels.progress(run, ConfigManager.countUniqueOnly, 49000),
            SafariPanels.missing(run, SafariBiome.FOREST, ConfigManager.countUniqueOnly, 3),
            SafariPanels.captures(run, SafariBiome.FOREST), SafariAssist.nearbyPanel(preview = true)!!)
        SafariHud.entries.zip(panels).forEach { (hud, panel) ->
            previews.add(Preview(hud.label, hud.layout, panel.width(font::width), panel.height) { g, x, y -> panel.draw(g, x, y, hud.layout.scale) })
        }
        addRenderableWidget(Button.builder(Component.literal("Reset positions")) {
            SafariHud.entries.forEach { it.layout.reset() }
            miria.x = 10f / width.coerceAtLeast(1); miria.y = 10f / height.coerceAtLeast(1); miria.scale = 1f
            save()
        }.bounds(width / 2 - 104, height - 24, 120, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Done")) { onClose() }.bounds(width / 2 + 20, height - 24, 84, 20).build())
    }
    private fun bounds(preview: Preview) = preview.layout.bounds(width, height, preview.width, preview.height)
    private fun hovered(x: Double, y: Double) = previews.asReversed().firstOrNull { bounds(it).contains(x, y) }
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0x88000000.toInt())
        val hover = hovered(mouseX.toDouble(), mouseY.toDouble())
        previews.forEach { preview ->
            val box = bounds(preview)
            preview.draw(graphics, box.x, box.y)
            if (preview == hover || preview == dragging) {
                val gold = HudRow.GOLD
                graphics.fill(box.x - 1, box.y - 1, box.x + box.width + 1, box.y, gold)
                graphics.fill(box.x - 1, box.y + box.height, box.x + box.width + 1, box.y + box.height + 1, gold)
                graphics.fill(box.x - 1, box.y, box.x, box.y + box.height, gold)
                graphics.fill(box.x + box.width, box.y, box.x + box.width + 1, box.y + box.height, gold)
                graphics.text(font, "${preview.label} · ${(preview.layout.scale * 100).toInt()}%", box.x, (box.y - 11).coerceAtLeast(0), gold, true)
            }
        }
        graphics.centeredText(font, Component.literal("Drag any HUD · Scroll over it to resize · ESC to save"), width / 2, height - 38, -1)
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event, doubleClick)) return true
        if (event.button() != 0) return false
        dragging = hovered(event.x(), event.y()) ?: return false
        val box = bounds(dragging!!)
        offsetX = event.x() - box.x; offsetY = event.y() - box.y
        return true
    }
    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        val target = dragging ?: return super.mouseDragged(event, dx, dy)
        if (event.button() != 0) return false
        target.layout.move(event.x() - offsetX, event.y() - offsetY, width, height, target.width, target.height)
        return true
    }
    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0 && dragging != null) { dragging = null; save(); return true }
        return super.mouseReleased(event)
    }
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val target = hovered(mouseX, mouseY) ?: return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        if (verticalAmount == 0.0) return false
        target.layout.scale += if (verticalAmount > 0) .1f else -.1f
        save(); return true
    }
    private fun save() {
        val box = bounds(previews.first())
        ContestConfig.hudX = box.x; ContestConfig.hudY = box.y; ContestConfig.hudScale = miria.scale
        ContestConfig.save(); ConfigManager.save()
    }
    override fun onClose() { save(); minecraft.setScreen(parent) }
    override fun isPauseScreen() = false
}
