package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariTheme
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.SmallAlerts
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariSparklingMode
import net.johnceo.sparklingmutuals.safari.resources.StartingResourceReminder
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

object SafariCompletion {
    private var owner: SafariRun? = null
    private val queue = ArrayDeque<Pair<SafariBiome, String>>()
    private val resourceQueue = ArrayDeque<String>()
    private var sentAt = 0L
    fun tick(client: Minecraft, run: SafariRun, biome: SafariBiome) {
        if (owner !== run) { owner = run; queue.clear(); resourceQueue.clear(); sentAt = 0 }
        val mode = SafariFullClear.mode
        val party = SafariSparklingMode.state
        SafariBiome.entries.forEach { area ->
            run.completion.announcement(run, area, mode, party)?.let { queue.addLast(area to it) }
        }
        // Revalidate queued completions after contradictory local evidence or a mode change.
        queue.removeAll { (area, _) -> run.sparklingCaptures.pending(area) || area != biome || mode == SafariMode.SPARKLING && !party.ready || run.completed(mode,
            area.critters.filter { mode != SafariMode.SPARKLING || party.needs(it.name) }, party, localOnly = true) !=
            area.critters.count { mode != SafariMode.SPARKLING || party.needs(it.name) } }
        val now = System.currentTimeMillis()
        // Sample the actual inventory at completion, rather than a prior HUD poll.
        client.player?.inventory?.let { inventory ->
            run.inventoryEvidence((0 until inventory.containerSize).map { inventory.getItem(it) }
                .filterNot { it.isEmpty }.map { it.hoverName.string to it.count }, now)
        }
        resourceQueue.removeAll { !run.resources.noticeStillRelevant(it, biome, party) }
        if (client.connection != null && resourceQueue.isNotEmpty() && now - sentAt >= 3000) {
            val notice = resourceQueue.removeFirst()
            StartingResourceReminder.parse(notice, party)?.let { reminder ->
                client.gui.setTimes(5, 50, 15)
                client.gui.setTitle(net.minecraft.network.chat.Component.literal(reminder.title)
                    .withStyle { it.withColor(SafariTheme.ACCENT).withBold(true) })
                client.gui.setSubtitle(net.minecraft.network.chat.Component.literal(reminder.detail))
                client.player?.sendSystemMessage(LocalChat.component(notice))
                client.connection!!.sendCommand("pc $notice")
                sentAt = now
            }
        }
        if (client.connection != null && queue.isNotEmpty() && now - sentAt >= 3000) {
            val (area, word) = queue.removeFirst()
            client.connection!!.sendCommand("pc $word")
            run.completion.sent(area); sentAt = now
            run.resources.notices(client.player?.name?.string ?: "Player", biome, party).forEach { notice ->
                resourceQueue.addLast(notice)
            }
        }
        if (run.completion.alert(run, mode, party, ConfigManager.runCompleteAlert)) {
            SmallAlerts.show(SafariHud.RUN_COMPLETE)
            playSound(client)
        }
    }
    fun playSound(client: Minecraft) {
        if (ConfigManager.runCompleteVolume <= 0) return
        val id = Identifier.tryParse(ConfigManager.runCompleteSound) ?: return
        val sound = BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(null) ?: return
        client.soundManager.play(SimpleSoundInstance.forUI(sound, 1f, ConfigManager.runCompleteVolume.coerceIn(0, 100) / 100f))
    }
}
