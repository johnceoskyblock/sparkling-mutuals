package net.johnceo.sparklingmutuals.alerts

import com.mojang.blaze3d.platform.InputConstants
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SettingsPalette
import net.johnceo.sparklingmutuals.hud.UiIcons
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.nio.file.Files
import java.nio.file.Path

/** Player cards and per-biome sparkling flags adapted from SafariUtils Ticket Trading (MIT). */
class WarpListScreen(private val parent: Screen?,private val autoPartyChanged: (Boolean)->Unit = {}) : Screen(Component.literal("Sparkle Warping · Warp List")) {
    private data class Hit(val x: Int,val y: Int,val w: Int,val h: Int,val action: ()->Unit) {
        fun contains(mx: Double,my: Double)=mx>=x && mx<x+w && my>=y && my<y+h
    }
    private val hits=mutableListOf<Hit>()
    private var selected=WarpList.players().firstOrNull()
    private var biome=SafariBiome.FOREST
    private var playerScroll=0
    private var flagScroll=0
    private var status="Select a player, then choose which sparklings trigger an invite."
    private var importing=false
    private lateinit var name: EditBox
    private lateinit var filter: EditBox
    private val sidebar get()=(width/3).coerceIn(120,190)
    private val contentLeft get()=sidebar+24
    private val right get()=width-16
    private val inputTop get()=maxOf(70,45+font.split(Component.literal(selected.orEmpty()),(right-contentLeft-108).coerceAtLeast(40)).size*10+10)
    private val actionsTop get()=inputTop+32
    private val rowsTop get()=inputTop+62
    private fun statusLines()=font.split(Component.literal(status),(width-112).coerceAtLeast(40))
    private val footerTop get()=height-18-statusLines().size*10
    private val rowsBottom get()=footerTop-34
    private val playerTop get()=rowsTop
    private val playerRows get()=((rowsBottom-playerTop)/32).coerceAtLeast(1)
    override fun init() {
        val draft=if(::name.isInitialized) name.value else ""
        val query=if(::filter.isInitialized) filter.value else ""
        name=EditBox(font,24,inputTop+7,sidebar-20,18,Component.literal("Player IGN"))
        name.setMaxLength(16);name.setBordered(false);name.value=draft;name.setHint(Component.empty());addRenderableWidget(name)
        filter=EditBox(font,contentLeft+8,inputTop+7,(right-contentLeft-16).coerceAtLeast(30),18,Component.literal("Filter critters"))
        filter.value=query;filter.setBordered(false);filter.setMaxLength(40);filter.setHint(Component.empty());filter.setResponder { flagScroll=0 };addRenderableWidget(filter)
    }
    private fun save() { ConfigManager.save() }
    private fun text(g: GuiGraphicsExtractor,s: String,x: Int,y: Int,c: Int=SettingsPalette.text) { g.text(font,s,x,y,c,false) }
    private fun card(g: GuiGraphicsExtractor,x: Int,y: Int,w: Int,h: Int,selected: Boolean=false,hover: Boolean=false) {
        g.fill(x,y,x+w,y+h,if(hover || selected) SettingsPalette.hover else SettingsPalette.card)
        val border=if(selected) SettingsPalette.accent else SettingsPalette.border
        g.fill(x,y,x+w,y+1,border);g.fill(x,y+h-1,x+w,y+h,border);g.fill(x,y,x+1,y+h,border);g.fill(x+w-1,y,x+w,y+h,border)
    }
    private fun button(g: GuiGraphicsExtractor,label: String,x: Int,y: Int,w: Int,mx: Int,my: Int,action: ()->Unit) {
        val over=mx>=x && mx<x+w && my>=y && my<y+20
        card(g,x,y,w,20,hover=over);text(g,font.plainSubstrByWidth(label,(w-10).coerceAtLeast(1)),x+5,y+6)
        hits.add(Hit(x,y,w,20,action))
    }
    override fun extractRenderState(g: GuiGraphicsExtractor,mx: Int,my: Int,delta: Float) {
        hits.clear();g.fill(0,0,width,height,SettingsPalette.background)
        g.fill(8,8,width-8,height-8,SettingsPalette.surface)
        text(g,"Sparkle Warping · Warp List",16,18,SettingsPalette.accent)
        button(g,if(ConfigManager.autoParty) "Auto-party: On" else "Auto-party: Off",right-106,12,106,mx,my) {
            val enabled=!ConfigManager.autoParty;ConfigManager.autoParty=enabled;autoPartyChanged(enabled);save()
        }
        text(g,"Players",16,47,SettingsPalette.muted)
        name.setY(inputTop+7);filter.setY(inputTop+7)
        card(g,16,inputTop,sidebar-4,24)
        card(g,contentLeft,inputTop,right-contentLeft,24)
        if(name.value.isEmpty()) text(g,"Player IGN…",24,inputTop+7,SettingsPalette.muted)
        if(filter.value.isEmpty()) text(g,"Filter critters…",contentLeft+8,inputTop+7,SettingsPalette.muted)
        val people=WarpList.players()
        val rows=people.drop(playerScroll).take(playerRows)
        g.enableScissor(16,playerTop,sidebar+12,rowsBottom)
        rows.forEachIndexed { index,player ->
            val y=playerTop+index*32
            val over=mx in 16..sidebar+10 && my in y until y+29
            card(g,16,y,sidebar-4,29,selected==player,over)
            text(g,font.plainSubstrByWidth(player,sidebar-18),22,y+5)
            val count=WarpList.critters.count { WarpList.selected(player,it) }
            text(g,"$count/${WarpList.critters.size} selected",22,y+17,SettingsPalette.muted)
            hits.add(Hit(16,y,sidebar-4,29) { selected=player;flagScroll=0 })
        }
        g.disableScissor()
        if(people.size>playerRows) {
            val track=rowsBottom-playerTop;val h=(track*playerRows/people.size).coerceAtLeast(12)
            val y=playerTop+(track-h)*playerScroll/(people.size-playerRows).coerceAtLeast(1)
            g.fill(sidebar+6,y,sidebar+9,y+h,SettingsPalette.accent)
        }
        button(g,"Add",16,actionsTop,(sidebar/2-7).coerceAtLeast(35),mx,my) { addPlayer() }
        button(g,if(importing) "Importing…" else "Import CSV",16+sidebar/2,actionsTop,(sidebar/2-6).coerceAtLeast(42),mx,my) { importCsv() }
        if(people.isEmpty()) {
            val lines=font.split(Component.literal("No players yet. Add an IGN or import a CSV."),sidebar-20)
            lines.forEachIndexed { i,line -> g.text(font,line,22,playerTop+12+i*11,SettingsPalette.muted,false) }
        }
        val current=selected?.takeIf { it in people }
        if(current!=null) {
            val nameLines=font.split(Component.literal(current),(right-contentLeft-108).coerceAtLeast(40))
            nameLines.forEachIndexed { i,line -> g.text(font,line,contentLeft,45+i*10,SettingsPalette.gold,false) }
            button(g,"All",right-98,42,46,mx,my) { WarpList.critters.forEach { WarpList.select(current,it,true) };save() }
            button(g,"None",right-46,42,46,mx,my) { WarpList.critters.forEach { WarpList.select(current,it,false) };save() }
            val tabW=((right-contentLeft-9)/4).coerceAtLeast(30)
            SafariBiome.entries.forEachIndexed { index,entry ->
                val x=contentLeft+index*(tabW+3)
                card(g,x,actionsTop,tabW,20,entry==biome)
                text(g,entry.label,x+4,actionsTop+6,if(entry==biome) entry.color else SettingsPalette.muted)
                hits.add(Hit(x,actionsTop,tabW,20) { biome=entry;flagScroll=0 })
            }
            val critters=WarpList.critters.filter { SafariRoster.named(it)?.biome==biome && it.contains(filter.value,true) }
            val columns=if(right-contentLeft>=280) 2 else 1
            val cellW=(right-contentLeft-(columns-1)*6)/columns
            val visible=((rowsBottom-rowsTop)/27).coerceAtLeast(1)
            val totalRows=(critters.size+columns-1)/columns
            flagScroll=flagScroll.coerceIn(0,(totalRows-visible).coerceAtLeast(0))
            g.enableScissor(contentLeft,rowsTop,right,rowsBottom)
            critters.drop(flagScroll*columns).take(visible*columns).forEachIndexed { index,critter ->
                val x=contentLeft+(index%columns)*(cellW+6);val y=rowsTop+(index/columns)*27
                val enabled=WarpList.selected(current,critter)
                card(g,x,y,cellW,23,hover=mx in x until x+cellW && my in y until y+23)
                UiIcons.draw(g,x+6,y+7,if(enabled) UiIcons.Kind.CHECK else UiIcons.Kind.CROSS,if(enabled) SettingsPalette.green else SettingsPalette.muted)
                text(g,font.plainSubstrByWidth(critter,cellW-26),x+22,y+8,if(enabled) SettingsPalette.text else SettingsPalette.muted)
                hits.add(Hit(x,y,cellW,23) { WarpList.select(current,critter,!enabled);save() })
            }
            g.disableScissor()
            if(totalRows>visible) {
                val track=rowsBottom-rowsTop;val h=(track*visible/totalRows).coerceAtLeast(12)
                val y=rowsTop+(track-h)*flagScroll/(totalRows-visible).coerceAtLeast(1)
                g.fill(right-3,y,right,y+h,SettingsPalette.accent)
            }
            button(g,"Remove player",contentLeft,rowsBottom+10,100,mx,my) {
                WarpList.removePlayer(current);selected=WarpList.players().firstOrNull();playerScroll=0;save()
            }
        } else {
            text(g,"Choose a player to edit invite flags.",contentLeft,rowsTop+16,SettingsPalette.muted)
        }
        statusLines().forEachIndexed { i,line -> g.text(font,line,16,footerTop+i*10,SettingsPalette.muted,false) }
        button(g,"Done",right-60,height-34,60,mx,my) { onClose() }
        super.extractRenderState(g,mx,my,delta)
    }
    private fun addPlayer() {
        val input=name.value.trim()
        if(!Regex("[A-Za-z0-9_]{1,16}").matches(input)) { status="Enter a valid Minecraft IGN.";return }
        WarpList.addPlayer(input);selected=WarpList.players().first { it.equals(input,true) }
        playerScroll=(WarpList.players().indexOf(selected)-playerRows+1).coerceAtLeast(0)
        name.value="";status="Choose the sparklings that should invite $selected.";save()
    }
    private fun importCsv() {
        if(importing) return
        importing=true;status="Choose CSV. Import replaces the current Warp List."
        NativeCsvDialog.choose().thenApplyAsync { file -> file?.let { Files.newBufferedReader(Path.of(it)).use(WarpListCsv::read) } }
            .whenComplete { values,error -> minecraft.execute {
                importing=false
                if(minecraft.smScreen !== this) return@execute
                status=when {
                    error!=null -> "Import failed. Current list unchanged."
                    values==null -> "Import cancelled. Current list unchanged."
                    else -> {
                        WarpList.replace(values);selected=WarpList.players().firstOrNull();playerScroll=0;flagScroll=0;save()
                        "Imported ${WarpList.players().size} players and ${values.values.sumOf { it.size }} invite flags."
                    }
                }
            } }
    }
    override fun mouseClicked(event: MouseButtonEvent,doubleClick: Boolean): Boolean {
        if(event.button()==InputConstants.MOUSE_BUTTON_LEFT) hits.lastOrNull { it.contains(event.x(),event.y()) }?.let { it.action();return true }
        return super.mouseClicked(event,doubleClick)
    }
    override fun mouseScrolled(mouseX: Double,mouseY: Double,scrollX: Double,scrollY: Double): Boolean {
        if(mouseX<contentLeft) playerScroll=(playerScroll-scrollY.toInt()*2).coerceIn(0,(WarpList.players().size-playerRows).coerceAtLeast(0))
        else flagScroll=(flagScroll-scrollY.toInt()*2).coerceAtLeast(0)
        return true
    }
    override fun keyPressed(event: KeyEvent): Boolean {
        if(event.key() in setOf(InputConstants.KEY_RETURN,InputConstants.KEY_NUMPADENTER) && name.isFocused) { addPlayer();return true }
        return super.keyPressed(event)
    }
    override fun onClose() { save();minecraft.smSetScreen(parent) }
    override fun isPauseScreen()=false
}
