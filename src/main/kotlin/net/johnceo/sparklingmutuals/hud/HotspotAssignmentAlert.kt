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
    private val pending=ArrayDeque<Pair<HotspotAssignments.Companion.Report,Long>>()
    private val pendingWins=ArrayDeque<HotspotProtocol.Wins>()
    private val pendingAssignments=ArrayDeque<HotspotProtocol.Assignment>()
    private val instructions=ArrayDeque<Pair<String,SafariBiome>>()
    private var leaderAnnounced=false
    private var published=false
    private var lastInstructionAt=0L
    private var shown: SafariBiome?=null
    private var shownAt=0L
    private val wins=mutableMapOf<String,Int>()
    private var sessionRoster: Set<String>?=null
    private data class Members(val ids: Map<String,String>,val names: Map<String,String>,val expected: Int,val leader: String?)
    private fun members(client: Minecraft): Members? {
        val player=client.player ?: return null
        val roster=PartyManager.withLocal(player.uuid.toString()) ?: return null
        if(sessionRoster!=roster) { sessionRoster=roster.toSet();wins.clear() }
        val names=roster.mapNotNull { id ->
            val name=if(id==player.uuid.toString()) player.name.string else
                client.connection?.onlinePlayers?.firstOrNull { it.profile.id.toString()==id }?.profile?.name ?: SafariPartyInput.nameOf(id)
            name?.let { it.lowercase() to it }
        }.toMap()
        val ids=roster.mapNotNull { id ->
            val name=if(id==player.uuid.toString()) player.name.string else
                client.connection?.onlinePlayers?.firstOrNull { it.profile.id.toString()==id }?.profile?.name ?: SafariPartyInput.nameOf(id)
            name?.lowercase()?.let { it to id }
        }.toMap()
        return Members(ids,names,roster.size,ids.entries.firstOrNull { PartyManager.isLeader(it.value) }?.key)
    }
    /** Called by the existing immediate hotspot announcer; no extra profile requests. */
    fun announcement(label: String): String? {
        sync() ?: return null
        val client=Minecraft.getInstance()
        val player=client.player ?: return null
        val members=members(client) ?: return null
        if(members.leader==null) return null
        if(!PartyManager.isLeader(player.uuid.toString())) return "[SM] Hotspot: $label"
        if(members.ids.size!=members.expected) return null
        val actual=SafariBiome.entries.firstOrNull { it.label==label } ?: return null
        val message=HotspotProtocol.winsMessage(actual,assignments.snapshot(members.ids.keys),members.names)
        leaderAnnounced=true
        return message
    }
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
        if (!enabled() || !SafariAssist.inSafari || sync() == null) return
        HotspotProtocol.parseAssignment(raw)?.let {
            if(pendingAssignments.size<8) pendingAssignments.addLast(it)
            return
        }
        HotspotProtocol.parseWins(raw)?.let { if(pendingWins.size<8) pendingWins.addLast(it) }
        val report=HotspotAssignments.parseReport(raw) ?: return
        if (pending.size < 32) pending.addLast(report to System.currentTimeMillis())
    }
    fun tick(client: Minecraft) {
        val current=sync() ?: return
        if (!SafariAssist.inSafari || current.endedAt != null) return
        val player=client.player ?: return
        val actual=current.entry.hotspot ?: return
        val now=System.currentTimeMillis()
        assignments.begin(player.name.string,actual,now)
        if (!enabled()) { shown=null;return }
        val members=members(client) ?: return
        val identities=members.ids
        val verifiedNames=identities.keys
        // Keep pending reports until membership/names are known, without performing profile requests.
        val remaining=ArrayDeque<Pair<HotspotAssignments.Companion.Report,Long>>()
        while (pending.isNotEmpty()) {
            val (report,receivedAt)=pending.removeFirst()
            if (report.sender.lowercase() in verifiedNames) assignments.report(report.sender,report.biome,receivedAt,report.coordinated)
            else if (verifiedNames.size < members.expected) remaining.addLast(report to receivedAt)
        }
        pending.addAll(remaining)
        val leader=PartyManager.isLeader(player.uuid.toString())
        if(verifiedNames.size==members.expected && members.leader!=null) {
            while(pendingWins.isNotEmpty()) {
                val snapshot=pendingWins.removeFirst()
                if(!leader && assignments.acceptWins(snapshot,members.leader,verifiedNames,now)) { shown=null;shownAt=0 }
            }
            while(pendingAssignments.isNotEmpty()) {
                val result=pendingAssignments.removeFirst()
                if(!leader) assignments.acceptAssignment(result,members.leader,verifiedNames,now)
            }
        }
        if(leader && members.expected>1 && !leaderAnnounced) return
        assignments.assignment(verifiedNames,now,identities,members.expected)?.let { target ->
            if (shown != target) { shown=target;shownAt=now }
        }
        if(leader && members.expected>1 && !published && client.connection!=null) assignments.finalPlan?.let { plan ->
            client.connection!!.sendCommand("pc ${HotspotProtocol.assignmentMessage(plan,members.names)}")
            published=true
            lastInstructionAt=now
        }
        instructions.addAll(assignments.notifications(leader,player.name.string))
        if(!leader) instructions.clear()
        if(leader && instructions.isNotEmpty() && now-lastInstructionAt>=1500 && client.connection!=null) {
            val (name,biome)=instructions.removeFirst()
            val id=identities[name]
            if(id!=null) {
                val display=client.connection!!.onlinePlayers.firstOrNull { it.profile.id.toString()==id }?.profile?.name
                    ?: SafariPartyInput.nameOf(id) ?: name
                client.connection!!.sendCommand("pc $display you are assigned ${biome.label} biome!")
                lastInstructionAt=now
            }
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
    private fun resetRun() {
        run=null;assignments=HotspotAssignments(wins);pending.clear();pendingWins.clear();pendingAssignments.clear()
        instructions.clear();leaderAnnounced=false;published=false;lastInstructionAt=0;shown=null;shownAt=0
    }
    fun reset() { resetRun();wins.clear();sessionRoster=null }
}
