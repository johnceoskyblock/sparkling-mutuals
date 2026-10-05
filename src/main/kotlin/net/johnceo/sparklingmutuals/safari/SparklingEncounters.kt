package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.BlockPos
import java.util.UUID

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
        if (memory.alert(id, ConfigManager.sparklingAlert)) { banner = name to where; shownAt = System.currentTimeMillis() }
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
        val y = client.window.guiScaledHeight / 3
        graphics.pose().pushMatrix(); graphics.pose().translate(0f, (y - 8).toFloat())
        AppearanceConfig.panels.getValue("alert").draw(graphics, client.window.guiScaledWidth, 56)
        graphics.pose().popMatrix()
        listOf("SPARKLING!", name, where).forEachIndexed { index, line ->
            graphics.centeredText(client.font, net.minecraft.network.chat.Component.literal(line),
                client.window.guiScaledWidth / 2, y + index * 14,
                (alpha shl 24) or (SafariEspConfig.rgb(AppearanceConfig.sparklingColor) and 0xFFFFFF))
        }
    }
    fun leave() { queue.clear(); banner = null }
    fun reset() { leave(); memory.reset(); sentAt = 0 }
}
