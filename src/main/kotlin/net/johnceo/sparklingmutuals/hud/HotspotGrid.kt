package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.PanelStyle
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.sync.SafariPartySync
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

class HotspotGrid(val width: Int,count: Int) {
    private val columns=if(count<=1) 1 else 2
    private val rows=(count.coerceIn(1,4)+columns-1)/columns
    val height=17+rows*50+(rows-1)*8+6
    fun cell(index: Int): HudBounds {
        require(index in 0..3)
        val cellWidth=(width-12-(columns-1)*8)/columns
        return HudBounds(6+(index%columns)*(cellWidth+8),17+(index/columns)*58,cellWidth,50)
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
        if(players.isEmpty()) graphics.text(font,"Waiting for party…",6,20,HudRow.GRAY,true)
        players.take(4).forEachIndexed { i,player ->
            val cell=grid.cell(i)
            graphics.fill(cell.x,cell.y,cell.x+cell.width,cell.y+cell.height,0x22000000)
            val nameColor=rankColors[player.name.lowercase()] ?: HudRow.WHITE
            val nameScale=minOf(1f,(cell.width-6).coerceAtLeast(1).toFloat()/font.width(player.name).coerceAtLeast(1))
            graphics.pose().pushMatrix();graphics.pose().translate((cell.x+3).toFloat(),(cell.y+3).toFloat())
            graphics.pose().scale(nameScale,nameScale)
            graphics.text(font,player.name,0,0,nameColor,true)
            graphics.pose().popMatrix()
            fun biome(label: String,biome: SafariBiome?,line: Int) {
                graphics.text(font,label,cell.x+3,cell.y+line,HudRow.GRAY,true)
                graphics.text(font,biome?.label ?: "?",cell.x+15,cell.y+line,HotspotGrid.biomeColor(biome),true)
            }
            biome("H:",player.actual,15);biome("A:",player.assigned,26)
            graphics.text(font,"Wins",cell.x+3,cell.y+38,HudRow.GRAY,true)
            graphics.text(font,player.wins.toString(),cell.x+28,cell.y+38,HudRow.GOLD,true)
        }
        graphics.pose().popMatrix()
    }
}
