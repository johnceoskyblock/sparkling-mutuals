package net.johnceo.sparklingmutuals.contest

import com.mojang.blaze3d.platform.InputConstants
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.HudConfigLinks
import net.johnceo.sparklingmutuals.config.SettingsTarget
import net.johnceo.sparklingmutuals.config.SafariConfigScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ContestConfig
import net.johnceo.sparklingmutuals.config.SafariTheme
import net.johnceo.sparklingmutuals.hud.HudLayout
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.hud.HudSnapping
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.SmallAlerts
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.johnceo.sparklingmutuals.config.ThemedButton as Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component

/** All HUDs have previews even when disabled or outside Safari, following v0.9.0's editor. */
class ContestGui(private val parent: Screen? = null) : Screen(Component.literal("Sparkling Mutuals HUDs")) {
    private data class Preview(val label: String, val layout: HudLayout, val width: Int, val height: Int,
        val target: SettingsTarget, val draw: (GuiGraphicsExtractor, Int, Int) -> Unit)
    private val previews = mutableListOf<Preview>()
    private val miria = ContestConfig.layout
    private var dragging: Preview? = null
    private var selected: Preview? = null
    private var guideX: Int? = null
    private var guideY: Int? = null
    private var offsetX = 0.0
    private var offsetY = 0.0
    override fun init() {
        ContestConfig.editLayout(width, height)
        previews.clear();dragging=null;selected=null;guideX=null;guideY=null
        previews.add(Preview("Miria contest", miria, ContestHud.WIDTH, ContestHud.previewHeight, HudConfigLinks.contest) { g, x, y ->
            ContestConfig.hudScale = miria.scale; ContestHud.renderPreview(g, x, y)
        })
        val run = SafariPanels.previewRun()
        val panels = mapOf(
            SafariHud.HOTSPOT to net.johnceo.sparklingmutuals.hud.HotspotHud.panel(true),
            SafariHud.ENTRY_COOLDOWN to net.johnceo.sparklingmutuals.safari.SafariEntryCooldown.preview(),
            SafariHud.DEVELOPER to net.johnceo.sparklingmutuals.debug.SafariDeveloper.preview(),
            SafariHud.DOOM_THROW to net.johnceo.sparklingmutuals.safari.DoomspiralThrowTimer.preview(),
            SafariHud.PROGRESS to SafariPanels.progress(run, ConfigManager.countUniqueOnly, 49000),
            SafariHud.MISSING to SafariPanels.missing(run, SafariBiome.FOREST, ConfigManager.countUniqueOnly, 3),
            SafariHud.PROFITABLE to net.johnceo.sparklingmutuals.safari.ui.ProfitableChecklist.preview(),
            SafariHud.LAKE_CLEARED to net.johnceo.sparklingmutuals.safari.LakeClearAlert.preview(),
            SafariHud.OBJECTIVES to SafariPanels.objectivesPreview(),
            SafariHud.CAPTURES to SafariPanels.captures(run, SafariBiome.FOREST),
            SafariHud.SPARKLINGS to SafariAssist.nearbyPanel(preview = true)!!) + SmallAlerts.previews() + net.johnceo.sparklingmutuals.skyblock.market.HuntingBoxHuds.previews()
        SafariHud.available.filter { it !in SmallAlerts.titles.keys }.forEach { hud ->
            val panel = panels.getValue(hud)
            previews.add(Preview(hud.label, hud.layout, panel.width(font::width), panel.height, HudConfigLinks.target(hud)) { g, x, y ->
                if (hud == SafariHud.BANNER) net.johnceo.sparklingmutuals.hud.SafariBanners.renderPreview(g,x,y,
                    hud.layout.effectiveScale(width,height,panel.width(font::width),panel.height))
                else panel.draw(g, x, y, hud.layout.effectiveScale(width, height, panel.width(font::width), panel.height),
                    AppearanceConfig.panels.getValue(hud.name.lowercase())) })
        }
        val gap=6
        val buttonWidth=minOf(120,(width-16-gap*2).coerceAtLeast(3)/3)
        val start=(width-(buttonWidth*3+gap*2))/2
        addRenderableWidget(Button.builder(Component.literal(if(buttonWidth>=100) "Reset positions" else "Reset")) {
            SafariHud.resetDefaults()
            ContestConfig.resetLayout()
            save()
        }.bounds(start, height - 24, buttonWidth, 20).build())
        addRenderableWidget(Button.builder(Component.literal(snapLabel())) { button ->
            ConfigManager.hudSnapping=!ConfigManager.hudSnapping
            button.message=Component.literal(snapLabel())
            guideX=null;guideY=null
            save()
        }.bounds(start+buttonWidth+gap, height-24, buttonWidth,20).build())
        addRenderableWidget(Button.builder(Component.literal("Done")) { onClose() }
            .bounds(start+(buttonWidth+gap)*2, height - 24, buttonWidth, 20).build())
    }
    private fun snapLabel()="Snap: "+if(ConfigManager.hudSnapping) "On" else "Off"
    private fun bounds(preview: Preview) = preview.layout.bounds(width, height, preview.width, preview.height)
    private fun hovered(x: Double, y: Double) = previews.asReversed().firstOrNull { bounds(it).contains(x, y) }
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, (0x88000000.toInt() or SafariTheme.BACKGROUND))
        val hover = hovered(mouseX.toDouble(), mouseY.toDouble())
        if(hover!=null) selected=hover
        previews.forEach { preview ->
            val box = bounds(preview)
            preview.draw(graphics, box.x, box.y)
            if (preview == hover || preview == dragging) {
                val gold = HudRow.GOLD
                graphics.fill(box.x - 1, box.y - 1, box.x + box.width + 1, box.y, gold)
                graphics.fill(box.x - 1, box.y + box.height, box.x + box.width + 1, box.y + box.height + 1, gold)
                graphics.fill(box.x - 1, box.y, box.x, box.y + box.height, gold)
                graphics.fill(box.x + box.width, box.y, box.x + box.width + 1, box.y + box.height, gold)
                val label=font.split(Component.literal("${preview.label} · ${(preview.layout.scale * 100).toInt()}%"),(width-16).coerceAtLeast(1))
                label.forEachIndexed { i,line ->
                    val x=box.x.coerceIn(8,maxOf(8,width-8-font.width(line)))
                    graphics.text(font,line,x,(box.y-label.size*10-1).coerceAtLeast(0)+i*10,gold,true)
                }
            }
        }
        if(dragging!=null && ConfigManager.hudSnapping) {
            val guide=0xAA000000.toInt() or (HudRow.GOLD and 0xFFFFFF)
            guideX?.let { graphics.fill(it, 0, it + 1, (height - 52).coerceAtLeast(0), guide) }
            guideY?.let { graphics.fill(0, it, width, it + 1, guide) }
        }
        val instructions=font.split(Component.literal("Drag to move · Arrows to nudge · Scroll to resize · Right-click for settings · Esc to save"),(width-16).coerceAtLeast(1))
        instructions.forEachIndexed { i,line -> graphics.text(font,line,(width-font.width(line))/2,height-30-instructions.size*10+i*10,-1,true) }
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event, doubleClick)) return true
        if(event.button()==InputConstants.MOUSE_BUTTON_RIGHT) {
            val target=hovered(event.x(),event.y()) ?: return false
            dragging=null;save()
            minecraft.smSetScreen(SafariConfigScreen(initialTarget=target.target,returnTo=this))
            return true
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false
        dragging = hovered(event.x(), event.y()) ?: return false
        selected=dragging
        val box = bounds(dragging!!)
        offsetX = event.x() - box.x; offsetY = event.y() - box.y
        return true
    }
    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        val target = dragging ?: return super.mouseDragged(event, dx, dy)
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false
        val box=bounds(target)
        val x=event.x()-offsetX
        val y=event.y()-offsetY
        val snapped=if(ConfigManager.hudSnapping) HudSnapping.position(x,y,width,height,box.width,box.height,
            previews.filter { it !== target }.map(::bounds)) else null
        guideX = snapped?.guideX ?: (width / 2).takeIf { snapped?.centeredX == true }
        guideY = snapped?.guideY ?: (height / 2).takeIf { snapped?.centeredY == true }
        target.layout.move(snapped?.x?.toDouble() ?: x,snapped?.y?.toDouble() ?: y,width,height,target.width,target.height)
        return true
    }
    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && dragging != null) { dragging = null; guideX=null;guideY=null;save(); return true }
        return super.mouseReleased(event)
    }
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val target = hovered(mouseX, mouseY) ?: return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        if (verticalAmount == 0.0) return false
        target.layout.markPositioned()
        target.layout.scale += if (verticalAmount > 0) .1f else -.1f
        save(); return true
    }
    override fun keyPressed(event: KeyEvent): Boolean {
        val target=selected ?: return super.keyPressed(event)
        val (dx,dy)=when(event.key()) {
            InputConstants.KEY_LEFT -> -1 to 0
            InputConstants.KEY_RIGHT -> 1 to 0
            InputConstants.KEY_UP -> 0 to -1
            InputConstants.KEY_DOWN -> 0 to 1
            else -> return super.keyPressed(event)
        }
        val box=bounds(target)
        target.layout.move((box.x+dx).toDouble(),(box.y+dy).toDouble(),width,height,target.width,target.height)
        guideX=null;guideY=null
        save()
        return true
    }
    private fun save() {
        val box = bounds(previews.first())
        ContestConfig.hudX = box.x; ContestConfig.hudY = box.y; ContestConfig.hudScale = miria.scale
        ContestConfig.save(); ConfigManager.save()
    }
    override fun onClose() { save(); minecraft.smSetScreen(if(parent is SafariConfigScreen) parent.freshForReturn() else parent) }
    override fun isPauseScreen() = false
}
