package net.johnceo.sparklingmutuals.hud
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import net.johnceo.sparklingmutuals.sync.SafariPartySync
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

object HotspotHud {
    fun panel(preview: Boolean=false): HudPanel {
        val client=Minecraft.getInstance()
        val width=SafariPanels.objectivesPreview().width(client.font::width)
        val entries=if(preview) listOf(SafariPartySync.HotspotRow("Player",SafariBiome.FOREST,SafariBiome.ICY,2))
            else if(SafariPartySync.backendCoordination) SafariPartySync.hotspotRows() else HotspotAssignmentAlert.rows()
        fun fit(text: String)=client.font.plainSubstrByWidth(text,(width-12).coerceAtLeast(1))
        val rows=entries.flatMap { row -> listOf(HudRow(fit("${row.name} · Wins: ${row.wins}"),icon=null),
            HudRow(fit("Hotspot: ${row.actual?.label ?: "Pending"}"),icon=null),
            HudRow(fit("Assigned: ${row.assigned?.label ?: "Pending"}"),icon=null)) }
            .ifEmpty { listOf(HudRow("Waiting for party…",icon=null)) }
        return HudPanel("Hotspots",rows=rows,titleIcon=null,widthOverride=width)
    }
    fun render(g: GuiGraphicsExtractor) {
        val client=Minecraft.getInstance()
        if(!ConfigManager.hotspotAnnouncer || !SafariIsland.onIsland || client.player==null || client.options.smHideGui || client.smScreen is ContestGui) return
        panel().draw(g,SafariHud.HOTSPOT)
    }
}
