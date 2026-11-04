package net.johnceo.sparklingmutuals.safari.sparkling

import net.johnceo.sparklingmutuals.party.PartyManager
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
    fun message() = "SPARKLING $name!"
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
    private var useTicket = false
    private var queuedParticle: SparklingNotice? = null
    private var sound: net.minecraft.client.resources.sounds.SoundInstance? = null

    internal fun ticketReminder(show: Boolean) { useTicket = show }

    internal fun shared(name: String) {
        requestAlert(SparklingNotice(UUID.randomUUID(), name, "Party discovery"))
    }

    fun observeUnknown(id: UUID, biome: SafariBiome, pos: BlockPos) =
        observe(id, SparklingAlert.UNKNOWN, biome, pos, unknown = true)
    fun identify(ids: Collection<UUID>): Boolean {
        queue.identify(ids)
        return banner?.let { it.unknown && it.id in ids } == true ||
            queuedParticle?.let { it.unknown && it.id in ids } == true
    }
    fun captured(name: String, ids: Collection<UUID>) {
        queue.captured(name, ids)
        val particle = queuedParticle ?: banner
        val clearParticle = particle?.let { it.name == name || it.unknown && it.id in ids } == true
        net.johnceo.sparklingmutuals.hud.SafariAlertSequence.queue.clear(this) {
            it == name || it == SparklingAlert.UNKNOWN && clearParticle
        }
        if (clearParticle) queuedParticle = null
    }
    fun observe(id: UUID, name: String, biome: SafariBiome, pos: BlockPos, unknown: Boolean = false,
                alreadyAlertedByParticle: Boolean = false) {
        val previouslyShared = !unknown && net.johnceo.sparklingmutuals.sync.SafariPartySync.found(name)
        if (!unknown && !previouslyShared) net.johnceo.sparklingmutuals.alerts.SparkleAutoParty.detected(name)
        val where = "${biome.label} ${pos.x} ${pos.y} ${pos.z}"
        val notice = SparklingNotice(id, name, where, unknown)
        if (!previouslyShared) requestAlert(notice, alreadyAlertedByParticle)
        if (!unknown && !previouslyShared && memory.announce(id, ConfigManager.sparklingPartyAnnouncer, name)) queue.add(notice)
    }
    private fun requestAlert(notice: SparklingNotice, alreadyAlertedByParticle: Boolean = false) {
        if (memory.alert(notice.id, ConfigManager.sparklingAlert, notice.name)) {
            if (!alreadyAlertedByParticle) enqueue(notice)
            else if (queuedParticle != null) queuedParticle = notice
            else banner = notice
        }
    }
    private fun enqueue(notice: SparklingNotice) {
        val world = Minecraft.getInstance().level
        val alerts = net.johnceo.sparklingmutuals.hud.SafariAlertSequence.queue
        if (notice.unknown) queuedParticle = notice
        alerts.submit(this, 7_000, notice.name, valid = {
            ConfigManager.sparklingAlert && Minecraft.getInstance().level === world &&
                (SafariAssist.inSafari || net.johnceo.sparklingmutuals.safari.SafariIsland.onIsland)
        }, finish = { banner = null }) {
            banner = if (notice.unknown) queuedParticle ?: notice else notice
            if (notice.unknown) queuedParticle = null
            shownAt = System.currentTimeMillis()
            sound = playSound(Minecraft.getInstance())
        }
    }
    fun tick(client: Minecraft) {
        if (!ConfigManager.sparklingPartyAnnouncer || !SafariAssist.inSafari || client.connection == null) { queue.clear(); return }
        if (!PartyManager.hasTeammates(client)) {
            if (PartyManager.hasInfo) queue.clear()
            return
        }
        val now = System.currentTimeMillis()
        if (now - sentAt >= 1500) queue.next()?.let { PartyManager.sendPartyChat(client, "${it.message()}"); sentAt = now }
    }
    fun render(graphics: GuiGraphicsExtractor) {
        if (!ConfigManager.sparklingAlert) return
        val client = Minecraft.getInstance()
        if (useTicket && !net.johnceo.sparklingmutuals.hud.SafariAlertSequence.queue.busy) {
            val text = "Party sparkling found. Use a Safari ticket to join."
            graphics.text(client.font, text, (client.window.guiScaledWidth - client.font.width(text)) / 2,
                client.window.guiScaledHeight / 4, HudRow.GOLD, true)
        }
        if (!SafariAssist.inSafari && !net.johnceo.sparklingmutuals.safari.SafariIsland.onIsland) return
        val notice = banner ?: return
        val name = notice.name
        val where = notice.where
        val remaining = 7000 - (System.currentTimeMillis() - shownAt)
        if (remaining <= 0) { banner = null; return }
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
    fun playSound(client: Minecraft): SimpleSoundInstance? {
        if (ConfigManager.sparklingSoundVolume <= 0) return null
        val id = Identifier.tryParse(ConfigManager.sparklingSound) ?: return null
        val sound = BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(null) ?: return null
        return SimpleSoundInstance.forUI(sound, 1f, ConfigManager.sparklingSoundVolume.coerceIn(0, 100) / 100f)
            .also { client.soundManager.play(it) }
    }
    fun leave() {
        queue.clear()
        net.johnceo.sparklingmutuals.hud.SafariAlertSequence.queue.clear(this)
        banner = null; queuedParticle = null; useTicket = false
        sound?.let { Minecraft.getInstance().soundManager.stop(it) }
        sound = null
    }
    internal fun audioPlaying() = sound?.let { Minecraft.getInstance().soundManager.isActive(it) } == true

    fun reset() {
        leave()
        memory.reset()
        sentAt = 0
    }
}
