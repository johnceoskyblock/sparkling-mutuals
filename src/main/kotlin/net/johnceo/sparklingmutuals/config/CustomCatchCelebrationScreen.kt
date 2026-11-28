package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.hud.HudFonts
import net.johnceo.sparklingmutuals.ui.SparklingAlertStyle
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

internal class CustomCatchCelebrationScreen(private val parent: SafariConfigScreen) :
    Screen(Component.literal("Build a Sparkling celebration")) {
    private enum class Tab(val label: String) { EFFECTS("Effects"), LOOK("Appearance"), SOUND("Audio"), PRESETS("Presets") }
    private data class Control(val label: String, val low: Int, val high: Int, val step: Int,
        val get: () -> Int, val set: (Int) -> Unit)
    private var tab = Tab.EFFECTS
    private var page = 0
    private var draft = if (ConfigManager.specialCatchStyle == SparklingAlertStyle.CUSTOM_INDEX) CustomCatchCelebrations.active
        else CelebrationRecipe.fromPreset(ConfigManager.specialCatchStyle)
    private val presets = CustomCatchCelebrations.presets.toMutableList()
    private var selectedName: String? = null
    private var presetName = ""
    private var status = ""
    private val labels = mutableListOf<Triple<Int, Int, () -> String>>()
    private val w get() = (width - 24).coerceIn(20, 550)
    private val x get() = (width - w) / 2
    private val top get() = 12
    private val bottom get() = height - 12
    private val rows get() = ((bottom - top - 54 - when (tab) {
        Tab.PRESETS -> 126
        Tab.LOOK -> 113
        else -> 78
    }) / 22).coerceAtLeast(1)

    override fun init() {
        labels.clear()
        val tabWidth = (w - 26) / Tab.entries.size
        Tab.entries.forEachIndexed { index, section ->
            button(section.label, x + 10 + index * (tabWidth + 2), top + 24, tabWidth, section == tab) {
                tab = section
                page = 0
                rebuildWidgets()
            }
        }
        when (tab) {
            Tab.PRESETS -> buildPresets()
            else -> buildControls()
        }
        val footerWidth = (w - 32) / 3
        button("Cancel", x + 10, bottom - 28, footerWidth) { onClose() }
        button("Preview", x + 16 + footerWidth, bottom - 28, footerWidth) {
            minecraft.smSetScreen(CatchCelebrationPreviewScreen(this, draft.validated()))
        }
        button("Save & use", x + 22 + footerWidth * 2, bottom - 28, footerWidth, true) {
            runCatching { CustomCatchCelebrations.apply(draft, presets) }
                .onSuccess { minecraft.smSetScreen(parent.freshForReturn()) }
                .onFailure { status = "Could not save the celebration." }
        }
    }

    private fun buildControls() {
        val controls = when (tab) {
            Tab.EFFECTS -> EFFECTS.mapIndexed { index, name ->
                setting(name, 0..6, { draft.effects[index] }) { value ->
                    draft = draft.copy(effects = draft.effects.toMutableList().apply { this[index] = value })
                }
            }
            Tab.LOOK -> listOf(
                setting("Screen pulse", 0..6, { draft.pulse }) { draft = draft.copy(pulse = it) },
                setting("Border thickness", 0..6, { draft.frame }) { draft = draft.copy(frame = it) },
                setting("Callouts", 0..2, { draft.callout }) { draft = draft.copy(callout = it) },
                setting("Text shake", 0..6, { draft.shake }) { draft = draft.copy(shake = it) },
                setting("Rainbow speed", 0..6, { draft.gradient }) { draft = draft.copy(gradient = it) },
                setting("Particle detail", 0..2, { draft.detail }) { draft = draft.copy(detail = it) },
            )
            Tab.SOUND -> listOf(
                setting("Song", 0..10, { draft.song }) { draft = draft.copy(song = it) },
                setting("Instruments", 0..12, { draft.instruments }) { draft = draft.copy(instruments = it) },
                setting("Volume (%)", 0..250, { draft.volume }, 10) { draft = draft.copy(volume = it) },
                setting("Duration (seconds)", 1..30, { draft.seconds }) { draft = draft.copy(seconds = it) },
            )
            Tab.PRESETS -> emptyList()
        }
        val columns = if (w >= 350) 2 else 1
        val perPage = rows * columns
        val columnWidth = (w - 18) / columns
        val shown = pageItems(controls, perPage)
        for ((index, control) in shown.withIndex()) {
            val columnX = x + 10 + (index / rows) * columnWidth
            val y = top + 52 + (index % rows) * 22
            val valueX = columnX + columnWidth - 82
            labels.add(Triple(columnX, y + 5) {
                HudFonts.font.plainSubstrByWidth(control.label, (valueX - columnX - 4).coerceAtLeast(1))
            })
            labels.add(Triple(valueX + 23, y + 5) { control.get().toString() })
            button("−", valueX, y, 18) { control.set((control.get() - control.step).coerceAtLeast(control.low)) }
            button("+", valueX + 50, y, 18) { control.set((control.get() + control.step).coerceAtMost(control.high)) }
        }
        if (tab == Tab.LOOK) {
            val fieldWidth = (w - 30).coerceAtLeast(1)
            val y = bottom - 77
            labels.add(Triple(x + 12, y - 10) { "Callout text" })
            editBox("Callout text", draft.caption, x + 10, y, fieldWidth, 64) { draft = draft.copy(caption = it) }
        }
    }

    private fun buildPresets() {
        val ordered = presets.sortedWith(compareByDescending<NamedCelebration> { it.favorite }.thenBy { it.name.lowercase() })
        val visible = pageItems(ordered, rows)
        for ((index, preset) in visible.withIndex()) {
            val text = (if (preset.favorite) "★ " else "") + preset.name
            button(text, x + 10, top + 50 + index * 22, w - 20, selectedName == preset.name) {
                selectedName = preset.name
                presetName = preset.name
                draft = preset.recipe
                status = "Loaded ${preset.name}."
                rebuildWidgets()
            }
        }
        val y = bottom - 100
        editBox("Preset name", presetName, x + 10, y, (w - 84).coerceAtLeast(1), 32) { presetName = it }
        button("Save preset", x + w - 68, y, 58) {
            val name = presetName.trim().filterNot(Char::isISOControl)
            if (name.isBlank()) {
                status = "Enter a preset name."
                return@button
            }
            val index = presets.indexOfFirst { it.name.equals(name, true) }
            if (index < 0 && presets.size >= CustomCatchCelebrations.MAX_PRESETS) {
                status = "Delete a preset before adding another."
                return@button
            }
            val preset = NamedCelebration(name, draft.validated(), presets.getOrNull(index)?.favorite == true)
            if (index < 0) presets.add(preset) else presets[index] = preset
            selectedName = preset.name
            status = "Saved ${preset.name} in this draft."
            rebuildWidgets()
        }
        val size = (w - 29) / 5
        button("Favorite", x + 10, y + 23, size) {
            val index = presets.indexOfFirst { it.name == selectedName }
            if (index >= 0) {
                presets[index] = presets[index].copy(favorite = !presets[index].favorite)
                rebuildWidgets()
            }
        }
        button("Delete", x + 12 + size, y + 23, size) {
            if (presets.removeIf { it.name == selectedName }) {
                selectedName = null
                presetName = ""
                rebuildWidgets()
            }
        }
        button("Duplicate", x + 14 + size * 2, y + 23, size) {
            val preset = presets.firstOrNull { it.name == selectedName } ?: return@button
            if (presets.size >= CustomCatchCelebrations.MAX_PRESETS) {
                status = "Delete a preset before adding another."
                return@button
            }
            val name = (1..CustomCatchCelebrations.MAX_PRESETS).firstNotNullOf { number ->
                val suffix = if (number == 1) " copy" else " copy $number"
                (preset.name.take(32 - suffix.length) + suffix).takeUnless { candidate ->
                    presets.any { it.name.equals(candidate, true) }
                }
            }
            presets.add(preset.copy(name = name))
            selectedName = name
            presetName = name
            status = "Duplicated ${preset.name}."
            rebuildWidgets()
        }
        button("Copy recipe", x + 16 + size * 3, y + 23, size) {
            minecraft.keyboardHandler.clipboard = CustomCatchCelebrations.encode(draft)
            status = "Celebration recipe copied."
        }
        button("Paste recipe", x + 18 + size * 4, y + 23, size) {
            runCatching { CustomCatchCelebrations.decode(minecraft.keyboardHandler.clipboard) }
                .onSuccess {
                    draft = it
                    selectedName = null
                    presetName = ""
                    status = "Recipe loaded. Preview it or save it as a preset."
                    rebuildWidgets()
                }.onFailure { status = "Clipboard doesn't contain a celebration recipe." }
        }
    }

    private fun <T> pageItems(items: List<T>, perPage: Int): List<T> {
        val pages = ((items.size + perPage - 1) / perPage).coerceAtLeast(1)
        page = page.coerceIn(0, pages - 1)
        if (pages > 1) {
            val y = bottom - if (tab == Tab.PRESETS) 124 else if (tab == Tab.LOOK) 111 else 76
            button("‹", x + w - 105, y, 22) { if (page > 0) { page--; rebuildWidgets() } }
            labels.add(Triple(x + w - 77, y + 5) { "${page + 1} / $pages" })
            button("›", x + w - 33, y, 22) { if (page + 1 < pages) { page++; rebuildWidgets() } }
        }
        return items.drop(page * perPage).take(perPage)
    }

    private fun setting(label: String, range: IntRange, get: () -> Int, step: Int = 1, set: (Int) -> Unit) =
        Control(label, range.first, range.last, step, get, set)

    private fun editBox(label: String, current: String, left: Int, y: Int, w: Int, limit: Int, change: (String) -> Unit) {
        addRenderableWidget(EditBox(minecraft.font, left, y, w, 19, Component.literal(label)).apply {
            setMaxLength(limit)
            value = current
            setResponder(change)
        })
    }

    private fun button(label: String, left: Int, y: Int, w: Int, activeTab: Boolean = false, action: () -> Unit) {
        val builder = ThemedButton.builder(Component.literal(label)) { action() }.bounds(left, y, w.coerceAtLeast(1), 19)
        if (activeTab) builder.primary()
        addRenderableWidget(builder.build())
    }

    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        g.fill(0, 0, width, height, SettingsPalette.background)
        g.fill(x, top, x + w, bottom, SettingsPalette.surface)
        g.outline(x, top, w, bottom - top, SettingsPalette.border)
        g.text(HudFonts.font, title, x + 10, top + 8, SettingsPalette.accent, false)
        labels.forEach { (left, y, label) -> g.text(HudFonts.font, label(), left, y, SettingsPalette.text, false) }
        val message = status.ifBlank { "Save & use applies the draft and its presets." }
        g.text(HudFonts.font, HudFonts.font.plainSubstrByWidth(message, (w - 20).coerceAtLeast(1)),
            x + 10, bottom - 42, SettingsPalette.muted, false)
        super.extractRenderState(g, mx, my, delta)
    }

    override fun onClose() = minecraft.smSetScreen(parent)
    override fun isPauseScreen() = false

    private companion object {
        val EFFECTS = listOf("Stars", "Comets", "Bursts", "Orbits", "Confetti", "Ribbons", "Corner bursts", "Light rays",
            "Shockwaves", "Glitter waves", "Prism columns", "Fireworks", "Halos", "Nebulae", "Spirals", "Curtains",
            "Chromatic rain", "Diamonds", "Horizon flares", "Constellations", "Lattice", "Embers", "Kaleidoscope",
            "Eclipses", "Runes", "Crown rays", "Hearts", "Mirror comets", "Sweeps", "Twin helix", "Lightning",
            "Prism fans", "Satellites", "Hourglass", "Glyph rain", "Nova crosses", "Portal gates", "Crystal shards",
            "Infinity trails", "Gravity wells", "Spectrum steps", "Starfall columns", "Magic circle",
            "Constellation reveal", "Prismatic wings", "Northern lights", "Celestial clock", "Crystal bloom",
            "Stardust vortex", "Comet impact", "Arcane seal", "Light pillars", "Dimensional tear", "Celestial wings",
            "Meteor shower", "Crown formation", "Reality ripple")
    }
}
