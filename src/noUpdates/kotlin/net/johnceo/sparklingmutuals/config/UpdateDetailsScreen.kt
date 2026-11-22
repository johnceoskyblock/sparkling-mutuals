package net.johnceo.sparklingmutuals.config

import com.mojang.blaze3d.platform.InputConstants
import net.johnceo.sparklingmutuals.compat.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.johnceo.sparklingmutuals.config.ThemedButton as Button
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.screens.ConfirmLinkScreen
import net.minecraft.util.FormattedCharSequence

/** Shares a scrollable notes layout between the update checker and installed release highlights. */
internal class UpdateDetailsScreen(private val parent: Screen, private val highlights: ReleaseHighlights.Update? = null,
    private val acknowledge: Boolean = true) :
    Screen(Component.literal(if (highlights == null) "Sparkling Mutuals updates" else "What's New")) {
    private data class Row(val kind: ReleaseNotes.Kind, val lines: List<FormattedCharSequence>, val height: Int, val indent: Int)
    private data class FeatureText(val title: List<FormattedCharSequence>, val details: List<FormattedCharSequence>)
    private data class HighlightsPage(val layout: ReleaseHighlightsLayout, val content: ReleaseHighlightsLayout.Content,
        val range: String, val versions: List<Component>, val features: List<List<FeatureText>>, val lineHeight: Int)
    private var highlightsPage: HighlightsPage? = null
    private var wrappedBlocks: List<ReleaseNotes.Block>? = null
    private var wrappedWidth = 0
    private var rows: List<Row> = emptyList()
    private var offset=0
    private var maximum=0
    private var notesTop=0
    private var thumbY=0
    private var thumbHeight=0
    private var dragging=false
    private var grab=0.0
    private val left get()=(width-(width-32).coerceAtMost(740))/2
    private val right get()=width-left
    private val notesBottom get() = highlightsPage?.layout?.contentBottom ?: height-52
    private val trackX get() = highlightsPage?.layout?.trackX ?: right-7
    override fun init() {
        dragging=false
        wrappedBlocks = null
        if (highlights != null) {
            val page = makeHighlightsPage(highlights)
            highlightsPage = page
            notesTop = page.layout.contentTop
            addRenderableWidget(Button.builder(Component.literal(if (acknowledge) "Open Settings" else "Back")) { onClose() }.primary()
                .bounds(page.layout.buttonRight - 124, page.layout.buttonY, 124, 22).build())
            return
        }
        ModUpdates.check(force=true)
        addRenderableWidget(Button.builder(Component.literal("GitHub releases")) {
            ConfirmLinkScreen.confirmLinkNow(this,java.net.URI.create(ModUpdates.REPOSITORY+"/releases"))
        }.bounds(left,height-34,130,22).build())
        addRenderableWidget(Button.builder(Component.literal("Back")) { onClose() }
            .bounds(right-60,height-34,60,22).build())
    }
    override fun onClose() {
        if (acknowledge) highlights?.let { ConfigManager.acknowledgeReleaseHighlights(it.installed) }
        minecraft.smSetScreen(parent)
    }
    override fun tick() { if (highlights == null) ModUpdates.check() }
    override fun isPauseScreen()=false
    override fun extractRenderState(g: GuiGraphicsExtractor,mx: Int,my: Int,delta: Float) {
        g.fill(0,0,width,height,SettingsPalette.background or 0xFF000000.toInt())
        highlightsPage?.let { page ->
            drawHighlights(g, page)
            super.extractRenderState(g,mx,my,delta)
            return
        }
        g.fill(left,12,right,height-44,SettingsPalette.surface or 0xFF000000.toInt())
        g.fill(left,12,left+3,70,SettingsPalette.accent)
        val x=left+14;val textWidth=right-x-20
        g.text(font,Component.literal("Sparkling Mutuals").withStyle { it.withBold(true) },x,23,SettingsPalette.text,false)
        val status=ModUpdates.label
        g.text(font,status,x,40,if(status=="Up to Date") SettingsPalette.green else SettingsPalette.accent,false)
        val installed=font.split(Component.literal("Installed ${ModUpdates.installed}  ·  Minecraft ${ModUpdates.minecraftVersion}"),textWidth)
        installed.forEachIndexed { i,line -> g.text(font,line,x,56+i*11,SettingsPalette.muted,false) }
        var y=56+installed.size*11+15
        val found=ModUpdates.release
        val releaseTitle=found?.name?.ifBlank { found.tag } ?: "Release notes"
        val heading=font.split(Component.literal(releaseTitle).withStyle { it.withBold(true) },textWidth)
        heading.forEachIndexed { i,line -> g.text(font,line,x,y+i*11,SettingsPalette.gold,false) }
        y+=heading.size*11+10
        val blocks = ReleaseNotes.parse(found?.notes ?: "Release status is unavailable. View GitHub releases to check manually.")
        g.fill(x,y-3,right-14,y-2,SettingsPalette.border)
        notesTop=y+7
        if (wrappedBlocks != blocks || wrappedWidth != textWidth) {
            wrappedBlocks = blocks
            wrappedWidth = textWidth
            rows = blocks.map { block ->
                val heading = block.kind == ReleaseNotes.Kind.HEADING
                val bullet = block.kind == ReleaseNotes.Kind.BULLET
                val indent = if (bullet) 12 else 0
                val component=Component.literal(block.text).withStyle { it.withBold(heading) }
                val lines=font.split(component,(textWidth-indent).coerceAtLeast(1))
                val spacing = if (heading) 8 else 7
                Row(block.kind, lines, if(block.kind==ReleaseNotes.Kind.SPACE) 8 else lines.size*12+spacing, indent)
            }
        }
        updateScroll(rows.sumOf { it.height })
        if (notesBottom <= notesTop) { super.extractRenderState(g,mx,my,delta); return }
        g.enableScissor(x,notesTop,right-14,notesBottom)
        y=notesTop-offset
        rows.forEach { row ->
            if(row.kind!=ReleaseNotes.Kind.SPACE && y+row.height>notesTop && y<notesBottom) {
                if(row.kind==ReleaseNotes.Kind.BULLET) g.text(font,"•",x,y,SettingsPalette.accent,false)
                val color = if (row.kind == ReleaseNotes.Kind.HEADING) SettingsPalette.gold else SettingsPalette.text
                row.lines.forEachIndexed { i,line -> g.text(font,line,x+row.indent,y+i*12,color,false) }
            }
            y+=row.height
        }
        g.disableScissor()
        drawScrollbar(g)
        super.extractRenderState(g,mx,my,delta)
    }

    private fun makeHighlightsPage(update: ReleaseHighlights.Update): HighlightsPage {
        val range = update.through?.let { "v$it  →  v${update.installed}" } ?: "v${update.installed}"
        val layout = ReleaseHighlightsLayout(width, height, font.width(title) * 2 + font.width(range) + 128 <= minOf(width - 24, 580))
        val lineHeight = font.lineHeight + 3
        val features = update.releases.map { release -> release.features.map { feature ->
            FeatureText(font.split(bold(feature.title), layout.textWidth), font.split(Component.literal(feature.details), layout.textWidth))
        } }
        val heights = features.map { section -> section.map { (it.title.size + it.details.size) * lineHeight + 23 } }
        return HighlightsPage(layout, layout.content(heights, lineHeight), range,
            update.releases.map { bold("v${it.version}") }, features, lineHeight)
    }

    private fun drawHighlights(g: GuiGraphicsExtractor, page: HighlightsPage) {
        val layout = page.layout
        val palette = SettingsPalette
        val accent = palette.accent
        g.fill(layout.left, layout.top, layout.right, layout.bottom, palette.surface or 0xFF000000.toInt())
        g.fillGradient(layout.left + 1, layout.top + 2, layout.right - 1, layout.headerBottom,
            (accent and 0xFFFFFF) or 0x29000000, (accent and 0xFFFFFF) or 0x03000000)
        frame(g, layout.left, layout.top, layout.right, layout.bottom, palette.border)
        g.fill(layout.left + 1, layout.top + 1, layout.right - 1, layout.top + 3, accent)
        g.fill(layout.left + 1, layout.footerTop, layout.right - 1, layout.footerTop + 1, palette.border)

        val badgeX = layout.contentLeft
        val badgeY = layout.top + if (layout.compact) 9 else 13
        val badgeSize = if (layout.compact) 29 else 34
        g.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, palette.card)
        frame(g, badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, palette.border)
        sparkle(g, badgeX + badgeSize / 2, badgeY + badgeSize / 2, 6, accent)
        sparkle(g, badgeX + badgeSize - 7, badgeY + 8, 3, palette.gold)
        sparkle(g, badgeX + 8, badgeY + badgeSize - 8, 2, palette.gold)

        g.pose().pushMatrix()
        val titleX = badgeX + badgeSize + 12
        g.pose().translate(titleX.toFloat(), (badgeY + 2).toFloat())
        g.pose().scale(2f, 2f)
        g.text(font,bold("What's New"),0,0,palette.text,false)
        g.pose().popMatrix()
        g.text(font,"Sparkling Mutuals",titleX,badgeY + 24,palette.muted,false)

        val range = font.plainSubstrByWidth(page.range, (layout.contentRight - layout.contentLeft - 20).coerceAtLeast(1))
        val rangeWidth = font.width(range) + 16
        val rangeX = if (layout.rangeInline) layout.right - 16 - rangeWidth else layout.contentLeft
        val rangeY = layout.top + when {
            layout.compact -> if (layout.rangeInline) 14 else 42
            layout.rangeInline -> 20
            else -> 50
        }
        g.fill(rangeX, rangeY, rangeX + rangeWidth, rangeY + 18, palette.card)
        frame(g, rangeX, rangeY, rangeX + rangeWidth, rangeY + 18, palette.border)
        g.text(font,range,rangeX + 8,rangeY + 5,palette.gold,false)
        g.fill(layout.left + 1, layout.headerBottom, layout.right - 1, layout.headerBottom + 1, palette.border)

        updateScroll(page.content.height)
        if (notesBottom <= notesTop) return
        g.enableScissor(layout.contentLeft, notesTop, layout.contentRight, notesBottom)
        val start = notesTop - offset
        if (page.content.sections.isEmpty()) {
            val empty = font.split(Component.literal("No highlights were published for this update."),
                (layout.contentRight - layout.contentLeft).coerceAtLeast(1))
            empty.forEachIndexed { index, line ->
                g.text(font, line, layout.contentLeft, start + index * page.lineHeight, palette.muted, false)
            }
        }
        page.content.sections.forEachIndexed { sectionIndex, section ->
            val labelY = start + section.labelTop
            if (labelY + page.lineHeight > notesTop && labelY < notesBottom) {
                val version = page.versions[sectionIndex]
                g.fill(layout.contentLeft, labelY, layout.contentLeft + 3, labelY + page.lineHeight - 2, accent)
                g.text(font,version,layout.contentLeft + 9,labelY,palette.gold,false)
                g.fill(layout.contentLeft + 19 + font.width(version), labelY + 4, layout.contentRight, labelY + 5, palette.border)
            }
            section.cards.forEach { card ->
                val y = start + card.top
                if (y + card.height <= notesTop || y >= notesBottom) return@forEach
                val feature = page.features[sectionIndex][card.index]
                g.fill(card.left, y, card.right, y + card.height, palette.card)
                frame(g, card.left, y, card.right, y + card.height, palette.border)
                sparkle(g, card.left + 10, y + 13, 3, accent)
                val textX = card.left + 20
                feature.title.forEachIndexed { i, line -> g.text(font,line,textX,y + 9 + i * page.lineHeight,palette.text,false) }
                val detailY = y + 12 + feature.title.size * page.lineHeight
                feature.details.forEachIndexed { i, line -> g.text(font,line,textX,detailY + i * page.lineHeight,palette.muted,false) }
            }
        }
        g.disableScissor()
        drawScrollbar(g)
    }

    private fun bold(text: String) = Component.literal(text).withStyle { it.withBold(true) }

    private fun sparkle(g: GuiGraphicsExtractor, x: Int, y: Int, size: Int, color: Int) {
        g.fill(x, y - size, x + 1, y + size + 1, color)
        g.fill(x - size, y, x + size + 1, y + 1, color)
        g.fill(x - 1, y - 1, x + 2, y + 2, color)
    }

    private fun frame(g: GuiGraphicsExtractor, left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        g.fill(left,top,right,top+1,color)
        g.fill(left,bottom-1,right,bottom,color)
        g.fill(left,top+1,left+1,bottom-1,color)
        g.fill(right-1,top+1,right,bottom-1,color)
    }

    private fun updateScroll(contentHeight: Int) {
        maximum = (contentHeight - (notesBottom - notesTop).coerceAtLeast(1)).coerceAtLeast(0)
        offset = offset.coerceIn(0, maximum)
    }

    private fun drawScrollbar(g: GuiGraphicsExtractor) {
        val viewport = notesBottom - notesTop
        if(maximum>0 && viewport>0) {
            thumbHeight=(viewport.toLong()*viewport/(viewport+maximum)).toInt().coerceIn(minOf(18,viewport),viewport)
            thumbY=notesTop+((viewport-thumbHeight).toLong()*offset/maximum).toInt()
            g.fill(trackX,notesTop,trackX+3,notesBottom,SettingsPalette.border)
            g.fill(trackX,thumbY,trackX+3,thumbY+thumbHeight,SettingsPalette.accent)
        }
    }
    override fun mouseScrolled(mouseX: Double,mouseY: Double,scrollX: Double,scrollY: Double): Boolean {
        offset=(offset-(scrollY*30).toInt()).coerceIn(0,maximum);return true
    }
    override fun keyPressed(event: KeyEvent): Boolean {
        val page = (notesBottom - notesTop - 12).coerceAtLeast(12)
        offset = when (event.key()) {
            InputConstants.KEY_PAGEUP -> (offset-page).coerceAtLeast(0)
            InputConstants.KEY_PAGEDOWN -> (offset+page).coerceAtMost(maximum)
            InputConstants.KEY_HOME -> 0
            InputConstants.KEY_END -> maximum
            else -> return super.keyPressed(event)
        }
        return true
    }
    override fun mouseClicked(event: MouseButtonEvent,doubleClick: Boolean): Boolean {
        if(event.button()==InputConstants.MOUSE_BUTTON_LEFT && maximum>0 && event.x()>=trackX-4 && event.x()<trackX+7 && event.y()>=notesTop && event.y()<notesBottom) {
            dragging=true;grab=if(event.y()>=thumbY && event.y()<thumbY+thumbHeight) event.y()-thumbY else thumbHeight/2.0
            drag(event.y());return true
        }
        return super.mouseClicked(event,doubleClick)
    }
    private fun drag(y: Double) { offset=(((y-notesTop-grab)/(notesBottom-notesTop-thumbHeight).coerceAtLeast(1))*maximum).toInt().coerceIn(0,maximum) }
    override fun mouseDragged(event: MouseButtonEvent,dx: Double,dy: Double): Boolean {
        if(dragging) { drag(event.y());return true };return super.mouseDragged(event,dx,dy)
    }
    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if(dragging && event.button()==InputConstants.MOUSE_BUTTON_LEFT) { dragging=false;return true };return super.mouseReleased(event)
    }
}
