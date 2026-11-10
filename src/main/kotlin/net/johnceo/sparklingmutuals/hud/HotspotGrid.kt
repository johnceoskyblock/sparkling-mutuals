package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.PanelStyle
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.sync.SafariPartySync
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

class HotspotGrid(val width: Int,count: Int) {
    private val rows=(count.coerceIn(1,4)+1)/2
    val height=28+rows*62+(rows-1)*8+6
    fun cell(index: Int): HudBounds {
        require(index in 0..3)
        val cellWidth=(width-20)/2
        return HudBounds(6+(index%2)*(cellWidth+8),28+(index/2)*70,cellWidth,62)
    }
    companion object {
        fun biomeColor(biome: SafariBiome?): Int=when(biome) {
            SafariBiome.FOREST -> 0xFF55FF55.toInt()
            SafariBiome.CAVERN -> 0xFFFFBB77.toInt()
            SafariBiome.ICY -> 0xFF80DDFF.toInt()
            SafariBiome.HAUNTED -> 0xFFBB66FF.toInt()
            null -> HudRow.GRAY
        }
    }
}

class HotspotGridPanel(private val players: List<SafariPartySync.HotspotRow>,private val rankColors: Map<String,Int>,
    width: Int,private val grid: HotspotGrid=HotspotGrid(width,players.size)):
    HudPanel("Hotspots",rows=emptyList(),titleIcon=null,widthOverride=width,heightOverride=grid.height) {
    override fun draw(graphics: GuiGraphicsExtractor,x: Int,y: Int,scale: Float,style: PanelStyle,borderOverride: Int?) {
        val font=Minecraft.getInstance().font
        graphics.pose().pushMatrix();graphics.pose().translate(x.toFloat(),y.toFloat());graphics.pose().scale(scale,scale)
        style.draw(graphics,grid.width,grid.height,borderOverride)
        graphics.text(font,"Hotspots",6,6,HudRow.GOLD,true)
        graphics.text(font,"H: Hotspot   A: Assigned",6,17,HudRow.GRAY,true)
        if(players.isEmpty()) graphics.text(font,"Waiting for party…",6,31,HudRow.GRAY,true)
        players.take(4).forEachIndexed { i,player ->
            val cell=grid.cell(i)
            graphics.fill(cell.x,cell.y,cell.x+cell.width,cell.y+cell.height,0x22000000)
            val first=font.plainSubstrByWidth(player.name,(cell.width-6).coerceAtLeast(1))
            val second=player.name.substring(first.length)
            val nameColor=rankColors[player.name.lowercase()] ?: HudRow.WHITE
            graphics.text(font,first,cell.x+3,cell.y+3,nameColor,true)
            if(second.isNotEmpty()) graphics.text(font,second,cell.x+3,cell.y+14,nameColor,true)
            fun biome(label: String,biome: SafariBiome?,line: Int) {
                graphics.text(font,label,cell.x+3,cell.y+line,HudRow.GRAY,true)
                graphics.text(font,biome?.label ?: "?",cell.x+15,cell.y+line,HotspotGrid.biomeColor(biome),true)
            }
            biome("H:",player.actual,27);biome("A:",player.assigned,38)
            graphics.text(font,"Wins",cell.x+3,cell.y+50,HudRow.GRAY,true)
            graphics.text(font,player.wins.toString(),cell.x+28,cell.y+50,HudRow.GOLD,true)
        }
        graphics.pose().popMatrix()
    }
}
