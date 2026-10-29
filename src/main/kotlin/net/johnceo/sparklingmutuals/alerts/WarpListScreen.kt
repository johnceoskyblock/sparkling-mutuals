package net.johnceo.sparklingmutuals.alerts

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariTheme
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.nio.file.Files
import java.nio.file.Path

class WarpListScreen(private val parent: Screen?) : Screen(Component.literal("Warp List")) {
    private val drafts = WarpList.critters.associateWith { WarpList.names(it).joinToString(" ") }.toMutableMap()
    private var page = 0
    private var pageSize = 5
    private var status = "IGNs separated by spaces, in invitation order"
    private var importing = false
    private val rows get() = WarpList.critters.drop(page * pageSize).take(pageSize)
    override fun init() {
        pageSize = ((height - 125) / 42).coerceIn(1, 8)
        val pages = (WarpList.critters.size + pageSize - 1) / pageSize
        page = page.coerceIn(0, pages - 1)
        val fieldWidth = (width - 30).coerceIn(80, 460)
        rows.forEachIndexed { i, critter ->
            addRenderableWidget(EditBox(font, (width - fieldWidth) / 2, 63 + i * 42, fieldWidth, 20,
                Component.literal("$critter IGNs")).also {
                it.setMaxLength(2048)
                it.value = drafts.getValue(critter)
                it.setResponder { value -> drafts[critter] = value }
            })
        }
        addRenderableWidget(Button.builder(Component.literal("Import CSV")) { importCsv() }
            .bounds(width / 2 - 125, height - 53, 250, 20).build().also { it.active = !importing })
        addRenderableWidget(Button.builder(Component.literal("Previous")) { page--; rebuildWidgets() }
            .bounds(width / 2 - 125, height - 28, 80, 20).build().also { it.active = page > 0 })
        addRenderableWidget(Button.builder(Component.literal("Save & Back")) { onClose() }
            .bounds(width / 2 - 42, height - 28, 84, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Next")) { page++; rebuildWidgets() }
            .bounds(width / 2 + 45, height - 28, 80, 20).build().also { it.active = page + 1 < pages })
    }
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0xF0000000.toInt() or SafariTheme.BACKGROUND)
        graphics.centeredText(font, title, width / 2, 12, 0xFF000000.toInt() or SafariTheme.ACCENT)
        graphics.centeredText(font, Component.literal(status), width / 2, 30,
            0xFF000000.toInt() or SafariTheme.TEXT)
        val x = (width - (width - 30).coerceIn(80, 460)) / 2
        rows.forEachIndexed { i, critter -> graphics.text(font, critter, x, 51 + i * 42,
            0xFF000000.toInt() or SafariTheme.ACCENT, false) }
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
    private fun importCsv() {
        if (importing) return
        importing = true
        status = "Choose CSV: replaces list with rows 2-11."
        rebuildWidgets()
        val client = minecraft
        NativeCsvDialog.choose().thenApplyAsync { selected ->
            if (selected == null) return@thenApplyAsync null
            Files.newBufferedReader(Path.of(selected)).use(WarpListCsv::read)
        }.whenComplete { imported, error -> client.execute {
            importing = false
            if (client.smScreen !== this) return@execute
            status = when {
                error != null -> "Import failed. Current list unchanged."
                imported == null -> "Import cancelled. Current list unchanged."
                else -> {
                    drafts.clear()
                    imported.forEach { (critter, names) -> drafts[critter] = names.joinToString(" ") }
                    drafts.forEach { (critter, text) -> WarpList.set(critter, text) }
                    ConfigManager.save()
                    "Imported ${imported.values.sumOf { it.size }} names. Replaced list."
                }
            }
            rebuildWidgets()
        } }
    }
    override fun onClose() {
        drafts.forEach { (name, text) -> WarpList.set(name, text) }
        ConfigManager.save()
        minecraft.smSetScreen(parent)
    }
    override fun isPauseScreen() = false
}
