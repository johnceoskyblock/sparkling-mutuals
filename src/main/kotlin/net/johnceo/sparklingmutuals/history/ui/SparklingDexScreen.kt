package net.johnceo.sparklingmutuals.history.ui

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ThemedButton
import net.johnceo.sparklingmutuals.history.AchievementRules
import net.johnceo.sparklingmutuals.history.Achievements
import net.johnceo.sparklingmutuals.history.SparklingDexType
import net.johnceo.sparklingmutuals.hud.HudFonts
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.ui.CritterNames
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

internal class SparklingDexScreen(
    private var tier: SparklingDexType,
    private val returnToSparklings: (SparklingDexType) -> Unit,
) : Screen(Minecraft.getInstance(), HudFonts.font, Component.literal("Sparkling Critterdex")) {
    private val panelWidth get() = minOf(728, (width - 16).coerceAtLeast(1))
    private val panelHeight get() = minOf(268, (height - 16).coerceAtLeast(1))
    private val left get() = (width - panelWidth) / 2
    private val top get() = (height - panelHeight) / 2
    private val contentWidth get() = (panelWidth - 24).coerceAtLeast(1)
    private val footerTop get() = top + panelHeight - 28
    private var gridOffset = 0
    private var gridTrack: HistoryScrollTrack? = null

    override fun init() {
        SparklingDexType.entries.forEachIndexed { i, type ->
            val start = contentWidth * i / SparklingDexType.entries.size
            val end = contentWidth * (i + 1) / SparklingDexType.entries.size
            val label = if (end - start < 26) type.required.toString() else "${type.required}×"
            control(label, "${type.label} • ${type.required} per species", left + 12 + start, top + 43, end - start - 3, tier == type) {
                if (tier != type) {
                    tier = type
                    rebuildWidgets()
                }
            }
        }
        val closeWidth = minOf(64, contentWidth)
        control("Close", "Back to Sparklings", left + 12 + contentWidth - closeWidth, footerTop,
            closeWidth, primary = true, action = ::onClose)
    }

    private fun control(label: String, description: String, x: Int, y: Int, w: Int, primary: Boolean, action: () -> Unit) {
        val button = ThemedButton.builder(Component.literal(label)) { action() }.bounds(x, y, w.coerceAtLeast(1), 17)
        if (primary) button.primary()
        addRenderableWidget(button.build().apply { setTooltip(Tooltip.create(Component.literal(description))) })
    }

    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        g.fill(0, 0, width, height, 0x960A070B.toInt())
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xF0141210.toInt())
        g.outline(left, top, panelWidth, panelHeight, 0xFF93634C.toInt())
        g.fill(left + 1, top + 1, left + panelWidth - 1, top + 3, PINK)

        val counts = Achievements.sparklingCounts()
        val completed = AchievementRules.dex(counts, tier.required)
        val progress = "${completed.size}/${SafariRoster.all.size}"
        val duplicates = "+" + "%,d".format(counts.values.sumOf { (it - 1).coerceAtLeast(0) })
        val right = left + panelWidth - 12
        val duplicateX = right - font.width(duplicates)
        val progressX = duplicateX - 10 - font.width(progress)
        g.text(font, font.plainSubstrByWidth(title.string, (progressX - left - 20).coerceAtLeast(1)), left + 12, top + 12, PINK)
        g.text(font, progress, progressX, top + 12, if (completed.size == SafariRoster.all.size) 0xFF7DE898.toInt() else WHITE)
        g.text(font, duplicates, duplicateX, top + 12, PINK)
        g.text(font, "${tier.label}  ·  ${tier.required} per species", left + 12, top + 29, MUTED)
        if (mx in progressX until right && my in top + 9 until top + 23) {
            tooltip(g, mx, my, "$progress meet the selected tier", "$duplicates credited catches beyond the first of each species")
        }
        drawBiomes(g, mx, my, counts, completed)
        g.fill(left + 12, footerTop - 6, right, footerTop - 5, LINE)
        super.extractRenderState(g, mx, my, delta)
    }

    private fun drawBiomes(g: GuiGraphicsExtractor, mx: Int, my: Int, counts: Map<String, Int>, completed: Set<String>) {
        val y = top + 66
        val viewportHeight = (footerTop - 12 - y).coerceAtLeast(1)
        val gridWidth = (contentWidth - 6).coerceAtLeast(1)
        val columns = if (gridWidth >= 390) 4 else 2
        val rows = BIOMES.size / columns
        val gridHeight = rows * TILE_HEIGHT + (rows - 1) * GAP
        val fitWidth = minOf(1.15f, gridWidth / (columns * 124f + (columns - 1) * GAP))
        val scale = minOf(fitWidth, (viewportHeight.toFloat() / gridHeight).coerceAtLeast(.8f))
        val track = HistoryScrollTrack(y, y + viewportHeight, viewportHeight,
            kotlin.math.ceil(gridHeight * scale).toInt())
        gridTrack = track
        gridOffset = gridOffset.coerceIn(0, track.maxOffset)
        val columnWidth = ((gridWidth / scale).toInt() - (columns - 1) * GAP) / columns
        val gridX = left + 12
        g.enableScissor(gridX, y, gridX + gridWidth, track.bottom)
        val pose = g.pose()
        pose.pushMatrix(); pose.translate(gridX.toFloat(), (y - gridOffset).toFloat()); pose.scale(scale, scale)
        BIOMES.forEachIndexed { index, biome ->
            val bx = (index % columns) * (columnWidth + GAP)
            val by = (index / columns) * (TILE_HEIGHT + GAP)
            val have = biome.critters.count { it.name in completed }
            val biomeStatus = "$have/${biome.critters.size}"
            g.fill(bx, by, bx + columnWidth, by + TILE_HEIGHT, 0x90342A26.toInt())
            g.text(font, biome.label, bx + 7, by + 6, biome.color)
            g.text(font, biomeStatus, bx + columnWidth - 7 - font.width(biomeStatus), by + 6, WHITE)
            g.fill(bx + 7, by + 18, bx + columnWidth - 7, by + 19, LINE)
            biome.critters.forEachIndexed { row, critter ->
                val rowY = by + 22 + ROW_HEIGHT * row
                val count = counts[critter.name] ?: 0
                val name = CritterNames.display(critter.name)
                val rarity = when (critter.color) {
                    0xFF55FF55.toInt() -> "Uncommon"
                    0xFF5555FF.toInt() -> "Rare"
                    0xFFAA00AA.toInt() -> "Epic"
                    0xFFFFAA00.toInt() -> "Legendary"
                    else -> "Common"
                }
                val countLabel = if (count == 0) "—" else "×" + "%,d".format(count)
                val amountX = bx + columnWidth - 7 - font.width(countLabel)
                val acquired = critter.name in completed
                val nameColor = if (count > 0) critter.color else MUTED
                val hovered = mx >= gridX + (bx + 3) * scale && mx < gridX + (bx + columnWidth - 3) * scale &&
                    my in y until track.bottom && my >= y - gridOffset + rowY * scale && my < y - gridOffset + (rowY + ROW_HEIGHT) * scale
                if (hovered) g.fill(bx + 3, rowY, bx + columnWidth - 3, rowY + ROW_HEIGHT, 0x22FFFFFF)
                g.text(font, rarity.take(1), bx + 7, rowY + 2, nameColor)
                g.text(font, font.plainSubstrByWidth(name, (amountX - bx - 24).coerceAtLeast(1)), bx + 19, rowY + 2, nameColor)
                g.text(font, countLabel, amountX, rowY + 2, if (acquired) WHITE else MUTED)
                if (hovered) tooltip(g, mx, my, "$name · $rarity", "$count credited sparkling catches", "${tier.required} required for ${tier.label}")
            }
        }
        pose.popMatrix()
        g.disableScissor()
        if (track.scrollable) {
            val x = gridX + gridWidth + 2
            val thumb = track.thumbTop(gridOffset)
            g.fill(x, y, x + 3, track.bottom, LINE)
            g.fill(x, thumb, x + 3, thumb + track.thumbHeight, PINK)
        }
    }

    private fun tooltip(g: GuiGraphicsExtractor, mx: Int, my: Int, vararg lines: String) {
        g.setComponentTooltipForNextFrame(font, lines.map(Component::literal), mx, my)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val track = gridTrack
        if (scrollY == 0.0 || track == null || !track.scrollable || mouseY < track.top || mouseY >= track.bottom)
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        gridOffset = (gridOffset + if (scrollY > 0) -28 else 28).coerceIn(0, track.maxOffset)
        return true
    }

    override fun isPauseScreen() = false
    override fun onClose() { returnToSparklings(tier) }

    private companion object {
        val BIOMES = listOf(SafariBiome.CAVERN, SafariBiome.FOREST, SafariBiome.HAUNTED, SafariBiome.ICY)
        const val ROW_HEIGHT = 13
        const val TILE_HEIGHT = 155
        const val GAP = 8
        const val PINK = 0xFFFF77FF.toInt()
        const val WHITE = 0xFFF5EDEF.toInt()
        const val MUTED = 0xFFABA2A9.toInt()
        const val LINE = 0xFF5E4D45.toInt()
    }
}
