package net.johnceo.sparklingmutuals.hud

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

object HotspotAssignmentAlert {
    private var run: SafariRun?=null
    private var assignments=HotspotAssignments()
    private val pending=ArrayDeque<Pair<String,SafariBiome>>()
    private var shown: SafariBiome?=null
    private var shownAt=0L
    private val wins=mutableMapOf<String,Int>()
    private var sessionRoster: Set<String>?=null
    fun enabled() = ConfigManager.hotspotAnnouncer
    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { receive(message.string) }
        }
        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ ->
            Minecraft.getInstance().execute { receive(message.string) }
        }
    }
    private fun sync(): SafariRun? {
        val current=SafariTracking.ledger.current
        if (current !== run) { resetRun();run=current }
        return current
    }
    private fun receive(raw: String) {
        if (!SafariAssist.inSafari || sync() == null) return
        val report=HotspotAssignments.parse(raw) ?: return
        if (pending.size < 32) pending.addLast(report)
    }
    fun tick(client: Minecraft) {
        val current=sync() ?: return
        if (!SafariAssist.inSafari || current.endedAt != null) return
        val player=client.player ?: return
        val actual=current.entry.hotspot ?: return
        val now=System.currentTimeMillis()
        assignments.begin(player.name.string,actual,now)
        if (!enabled()) { shown=null;return }
        val roster=PartyManager.withLocal(player.uuid.toString()) ?: return
        if (sessionRoster != roster.toSet()) {
            sessionRoster=roster.toSet();wins.clear()
        }
        val identities=roster.mapNotNull { id ->
            val name=if (id == player.uuid.toString()) player.name.string else
                client.connection?.onlinePlayers?.firstOrNull { it.profile.id.toString() == id }?.profile?.name ?: SafariPartyInput.nameOf(id)
            name?.lowercase()?.let { it to id }
        }.toMap()
        val verifiedNames=identities.keys
        // Keep pending reports until membership/names are known, without performing profile requests.
        val remaining=ArrayDeque<Pair<String,SafariBiome>>()
        while (pending.isNotEmpty()) {
            val report=pending.removeFirst()
            if (report.first.lowercase() in verifiedNames) assignments.report(report.first,report.second,now)
            else if (verifiedNames.size < roster.size) remaining.addLast(report)
        }
        pending.addAll(remaining)
        assignments.assignment(verifiedNames,now,identities,roster.size)?.let { target ->
            if (shown != target) { shown=target;shownAt=now }
        }
    }
    fun render(graphics: GuiGraphicsExtractor) {
        val client=Minecraft.getInstance()
        if (!enabled() || !SafariAssist.inSafari || SafariTracking.ledger.current !== run || client.player == null ||
            client.options.smHideGui || client.smScreen is ContestGui) return
        val biome=shown ?: return
        val remaining=5000-(System.currentTimeMillis()-shownAt)
        if (remaining <= 0) return
        val alpha=(255*(remaining/1000.0).coerceAtMost(1.0)).toInt()
        val rgb=when(biome) {
            SafariBiome.FOREST -> 0x55FF55
            SafariBiome.CAVERN -> 0xFFBB77
            SafariBiome.ICY -> 0x80DDFF
            SafariBiome.HAUNTED -> 0xBB66FF
        }
        val color=(alpha shl 24) or rgb
        val width=client.window.guiScaledWidth;val height=client.window.guiScaledHeight
        graphics.fill(0,0,width,height,((alpha/12) shl 24) or rgb)
        graphics.fill(0,0,width,3,color);graphics.fill(0,height-3,width,height,color)
        graphics.fill(0,0,3,height,color);graphics.fill(width-3,0,width,height,color)
        fun centered(text: String,y: Float,scale: Float,tint: Int) {
            graphics.pose().pushMatrix();graphics.pose().translate(width/2f,y);graphics.pose().scale(scale,scale)
            graphics.text(client.font,text,-client.font.width(text)/2,0,tint,true)
            graphics.pose().popMatrix()
        }
        val title="${biome.label} Biome"
        val scale=minOf(4f,(width-20f)/client.font.width(title))
        centered(title,height/2f-client.font.lineHeight*scale,scale,color)
        centered("Assigned biome · Hotspot: ${run?.entry?.hotspot?.label}",height/2f+12, minOf(1.5f,(width-20f)/client.font.width("Assigned biome · Hotspot: Haunted")),(alpha shl 24) or 0xFFFFFF)
    }
    private fun resetRun() { run=null;assignments=HotspotAssignments(wins);pending.clear();shown=null;shownAt=0 }
    fun reset() { resetRun();wins.clear();sessionRoster=null }
}
