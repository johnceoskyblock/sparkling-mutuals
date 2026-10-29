package net.johnceo.sparklingmutuals.safari.sparkling

import java.util.UUID
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

object SparklingAlert {
    const val UNKNOWN = "Critter Unknown"
    fun panel(name: String, where: String, color: Int = HudRow.GOLD) =
        HudPanel("SPARKLING!", color, listOf(HudRow(name), HudRow(where)))
}

data class SparklingNotice(val id: UUID, val name: String, val where: String, val unknown: Boolean = false) {
    fun message() = "SPARKLING $name! ($where)"
}

class SparklingNotices {
    private val queue = ArrayDeque<SparklingNotice>()
    fun add(notice: SparklingNotice) {
        if (!notice.unknown && notice.name != SparklingAlert.UNKNOWN) queue.addLast(notice)
    }
    fun identify(ids: Collection<UUID>) { queue.removeAll { it.unknown && it.id in ids } }
    fun captured(name: String, ids: Collection<UUID>) { queue.removeAll { it.name == name || it.id in ids } }
    fun next() = queue.removeFirstOrNull()
    fun clear() = queue.clear()
}

class SparklingMemory {
    private val alerted = mutableSetOf<String>()
    private val announced = mutableSetOf<String>()
    private fun key(id: UUID, name: String?) = when (name) {
        // Moving particles can create new location UUIDs; they share one notification per run.
        SparklingAlert.UNKNOWN -> "particle:unknown"
        else -> name?.let(net.johnceo.sparklingmutuals.safari.SafariRoster::named)?.name ?: id.toString()
    }
    fun alert(id: UUID, enabled: Boolean, name: String? = null) = enabled && alerted.add(key(id, name))
    fun announce(id: UUID, enabled: Boolean, name: String? = null) = enabled && name != SparklingAlert.UNKNOWN && announced.add(key(id, name))
    fun reset() { alerted.clear(); announced.clear() }
}

/** CritterMod v0.9.0 SparklingWatch behaviour, with paced announcements and per-run deduplication. */
object SparklingEncounters {
    private val memory = SparklingMemory()
    private val queue = SparklingNotices()
    private var banner: SparklingNotice? = null
    private var shownAt = 0L
    private var sentAt = 0L

    fun observeUnknown(id: UUID, biome: SafariBiome, pos: BlockPos) =
        observe(id, SparklingAlert.UNKNOWN, biome, pos, unknown = true)
    fun identify(ids: Collection<UUID>) {
        queue.identify(ids)
        if (banner?.let { it.unknown && it.id in ids } == true) banner = null
    }
    fun captured(name: String, ids: Collection<UUID>) {
        queue.captured(name, ids)
        if (banner?.let { it.name == name || it.unknown && it.id in ids } == true) banner = null
    }
    fun observe(id: UUID, name: String, biome: SafariBiome, pos: BlockPos, unknown: Boolean = false) {
        if (!unknown) net.johnceo.sparklingmutuals.alerts.SparkleAutoParty.detected(name)
        val where = "${biome.label} ${pos.x} ${pos.y} ${pos.z}"
        val notice = SparklingNotice(id, name, where, unknown)
        if (memory.alert(id, ConfigManager.sparklingAlert, name)) {
            banner = notice; shownAt = System.currentTimeMillis(); playSound(Minecraft.getInstance())
        }
        if (!unknown && memory.announce(id, ConfigManager.sparklingPartyAnnouncer, name)) queue.add(notice)
    }
    fun tick(client: Minecraft) {
        if (!ConfigManager.sparklingPartyAnnouncer || !SafariAssist.inSafari || client.connection == null) { queue.clear(); return }
        val now = System.currentTimeMillis()
        if (now - sentAt >= 1500) queue.next()?.let { client.connection!!.sendCommand("pc ${it.message()}"); sentAt = now }
    }
    fun render(graphics: GuiGraphicsExtractor) {
        if (!ConfigManager.sparklingAlert || !SafariAssist.inSafari) return
        val notice = banner ?: return
        val name = notice.name
        val where = notice.where
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
