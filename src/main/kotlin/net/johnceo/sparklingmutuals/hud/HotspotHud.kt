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
        val entries=if(preview) listOf(
            SafariPartySync.HotspotRow("Bluaza",SafariBiome.ICY,SafariBiome.HAUNTED,2),
            SafariPartySync.HotspotRow("JohnCEO",SafariBiome.CAVERN,SafariBiome.CAVERN,2),
            SafariPartySync.HotspotRow("Pyruuuu",SafariBiome.FOREST,SafariBiome.FOREST,1),
            SafariPartySync.HotspotRow("Dangerdogs1234",SafariBiome.HAUNTED,SafariBiome.ICY,1))
            else if(SafariPartySync.backendCoordination) SafariPartySync.hotspotRows() else HotspotAssignmentAlert.rows()
        return HotspotGridPanel(entries,net.johnceo.sparklingmutuals.alerts.WarpListRanks.colors(client),width)
    }
    fun render(g: GuiGraphicsExtractor) {
        val client=Minecraft.getInstance()
        if(!ConfigManager.hotspotAnnouncer || !SafariIsland.onIsland || client.player==null || client.options.smHideGui || client.smScreen is ContestGui) return
        panel().draw(g,SafariHud.HOTSPOT)
    }
}
