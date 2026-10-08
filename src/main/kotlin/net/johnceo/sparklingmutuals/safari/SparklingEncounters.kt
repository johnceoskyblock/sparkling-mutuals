package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.BlockPos
import java.util.UUID
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

object SparklingAlert {
    fun panel(name: String, where: String, color: Int = HudRow.GOLD) =
        HudPanel("SPARKLING!", color, listOf(HudRow(name), HudRow(where)))
}

class SparklingMemory {
    private val alerted = mutableSetOf<UUID>()
    private val announced = mutableSetOf<UUID>()
    fun alert(id: UUID, enabled: Boolean) = enabled && alerted.add(id)
    fun announce(id: UUID, enabled: Boolean) = enabled && announced.add(id)
    fun reset() { alerted.clear(); announced.clear() }
}

/** CritterMod v0.9.0 SparklingWatch behaviour, with paced announcements and per-run deduplication. */
object SparklingEncounters {
    private val memory = SparklingMemory()
    private val queue = ArrayDeque<String>()
    private var banner: Pair<String, String>? = null
    private var shownAt = 0L
    private var sentAt = 0L

    fun observe(id: UUID, name: String, biome: SafariBiome, pos: BlockPos) {
        val where = "${biome.label} ${pos.x} ${pos.y} ${pos.z}"
        if (memory.alert(id, ConfigManager.sparklingAlert)) {
            banner = name to where; shownAt = System.currentTimeMillis(); playSound(Minecraft.getInstance())
        }
        if (memory.announce(id, ConfigManager.sparklingPartyAnnouncer)) queue.addLast("SPARKLING $name! ($where)")
    }
    fun tick(client: Minecraft) {
        if (!ConfigManager.sparklingPartyAnnouncer || !SafariAssist.inSafari || client.connection == null) { queue.clear(); return }
        val now = System.currentTimeMillis()
        if (queue.isNotEmpty() && now - sentAt >= 1500) { client.connection!!.sendCommand("pc ${queue.removeFirst()}"); sentAt = now }
    }
    fun render(graphics: GuiGraphicsExtractor) {
        if (!ConfigManager.sparklingAlert || !SafariAssist.inSafari) return
        val (name, where) = banner ?: return
        val remaining = 7000 - (System.currentTimeMillis() - shownAt)
        if (remaining <= 0) { banner = null; return }
        val client = Minecraft.getInstance()
        val alpha = (255 * (remaining / 1200.0).coerceAtMost(1.0)).toInt()
        val color = (alpha.coerceAtLeast(4) shl 24) or (SafariEspConfig.rgb(AppearanceConfig.sparklingColor) and 0xFFFFFF)
        val panel = SparklingAlert.panel(name, where, color)
        val width = client.window.guiScaledWidth; val height = client.window.guiScaledHeight
        val style = AppearanceConfig.panels.getValue("alert")
        val tint = SafariEspConfig.rgb(style.backgroundColor).takeIf { it and 0xFFFFFF != 0 } ?: color
        val washAlpha = (alpha * (100 - style.transparency) / 100 * .2).toInt()
        graphics.fill(0, 0, width, height, (washAlpha shl 24) or (tint and 0xFFFFFF))
        val frame = if (style.borderEnabled) (alpha shl 24) or (SafariEspConfig.rgb(style.borderColor) and 0xFFFFFF) else color
        graphics.fill(0, 0, width, 3, frame); graphics.fill(0, height - 3, width, height, frame)
        graphics.fill(0, 0, 3, height, frame); graphics.fill(width - 3, 0, width, height, frame)
        fun centered(text: String, y: Float, scale: Float, textColor: Int) {
            graphics.pose().pushMatrix(); graphics.pose().translate(width / 2f, y); graphics.pose().scale(scale, scale)
            graphics.text(client.font, text, -client.font.width(text) / 2, 0, textColor, true)
            graphics.pose().popMatrix()
        }
        val titleScale = minOf(4f, (width - 16f) / client.font.width(panel.title))
        val subtitleScale = minOf(2f, (width - 16f) / maxOf(client.font.width(name), client.font.width(where)))
        val top = height / 2f - (client.font.lineHeight * titleScale + 22 + client.font.lineHeight * subtitleScale * 2) / 2
        centered(panel.title, top, titleScale, color)
        val white = (alpha shl 24) or 0xFFFFFF
        panel.rows.forEachIndexed { index, row -> centered(row.label,
            top + client.font.lineHeight * titleScale + 14 + index * (client.font.lineHeight * subtitleScale + 4), subtitleScale, white) }
    }
    fun playSound(client: Minecraft) {
        if (ConfigManager.sparklingSoundVolume <= 0) return
        val id = Identifier.tryParse(ConfigManager.sparklingSound) ?: return
        val sound = BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(null) ?: return
        client.soundManager.play(SimpleSoundInstance.forUI(sound, 1f, ConfigManager.sparklingSoundVolume.coerceIn(0, 100) / 100f))
    }
    fun leave() { queue.clear(); banner = null }
    fun reset() { leave(); memory.reset(); sentAt = 0 }
}
