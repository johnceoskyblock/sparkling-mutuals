// Adapted from SafariUtils EncounterAlerts/BannerPlacement, MIT; see licenses/SafariUtils-MIT.txt.
package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.contest.*
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.ui.AlertSounds
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

object SafariBanners {
    private var activeBanner: ActiveBanner? = null
    private var run: SafariRun?=null
    private val fired=mutableSetOf<String>()
    private val cooldown=mutableMapOf<String,Long>()
    private var chamberTarget: Int?=null
    private var previousBiome: SafariBiome?=null
    private var feeder=0
    private var heldFood=0
    private var tick=0
    private var contestActive: Boolean?=null
    private var contestWindow=-1L
    private var ticket=false
    private var ticketMode=ContestConfig.ticketMode
    private val contestWarnings=mutableSetOf<Int>()
    private fun now()=System.nanoTime()/1_000_000
    private fun sync() {
        val current=SafariTracking.ledger.current
        if(current!==run) { run=current;fired.clear();cooldown.clear();chamberTarget=null;previousBiome=null;feeder=0;heldFood=0 }
    }
    fun fire(kind: BannerKind,biome: SafariBiome?=null,critter: String?=null,text: String?=null,preview: Boolean=false,location: String?=null) {
        if(!kind.supported) return
        if(!preview && kind==BannerKind.HIDEYHO_LOCATION &&
            (!SafariAssist.inSafari || SafariAssist.biome!=SafariBiome.HAUNTED || SafariFullClear.mode==SafariMode.SPARKLING)) return
        if(!preview && kind.encounterBiome?.let { SafariAssist.biome!=it }==true) return
        if(kind==BannerKind.GEMZIE_DONE) { clear(BannerKind.GEMZIE_READY);clear(BannerKind.GEMS) }
        if(kind==BannerKind.WUMPA_DONE) { clear(BannerKind.WUMPA_READY);clear(BannerKind.WUMPA_STARTED) }
        if(kind==BannerKind.DOOM_DONE) { clear(BannerKind.DOOM_READY);clear(BannerKind.DOOM_STARTED) }
        val option=BannerConfig.options.getValue(kind)
        if(!preview && (!BannerConfig.enabled || !option.enabled)) return
        val formatted=(text ?: option.text).replace("<BIOME>",biome?.label ?: "Safari").replace("<CRITTER>",critter ?: "Critter")
            .replace("<LOCATION>",location ?: "Inside").replace("<TICKET>",if(ContestConfig.firstClassTicket) "First-Class" else "Basic")
        val color=if(biome!=null && kind in setOf(BannerKind.HOTSPOT,BannerKind.FLOOR_DROPS,BannerKind.BIOME_DONE)) biome.color
            else if(kind==BannerKind.BIRDS && critter!=null) SafariRoster.named(critter)?.color ?: (0xFF000000.toInt() or kind.rgb)
            else SafariEspConfig.rgb(option.color)
        val notice = BannerNotice(kind,formatted,color,(option.duration*1000).toLong(),option.sound,
            if(option.playback==1) 0f else option.volume/100f,option.pitch,option.playback==2)
        val requestRun = SafariTracking.ledger.current
        val contest = kind.name.startsWith("CONTEST_") || kind == BannerKind.TICKET
        SafariAlertSequence.queue.submit(this, notice.duration, kind to formatted,
            valid = { Minecraft.getInstance().player != null && (preview ||
                BannerConfig.enabled && BannerConfig.options.getValue(kind).enabled &&
                (contest || SafariTracking.ledger.current === requestRun) &&
                (kind != BannerKind.HIDEYHO_LOCATION || SafariAssist.inSafari && SafariAssist.biome==SafariBiome.HAUNTED && SafariFullClear.mode != SafariMode.SPARKLING)) },
            finish = { activeBanner = null }) {
            if (!notice.soundOnly) activeBanner = ActiveBanner(notice, now())
            AlertSounds.play(Minecraft.getInstance(), notice.sound, notice.volume, notice.pitch)
        }
    }
    fun preview(kind: BannerKind=BannerKind.GEMS) {
        val client=Minecraft.getInstance()
        if (BannerConfig.options.getValue(kind).playback != 2) client.smSetScreen(null)
        fire(kind,kind.encounterBiome ?: SafariBiome.CAVERN,"Bluebird",preview=true)
    }
    fun clear(kind: BannerKind)=SafariAlertSequence.queue.clear(this) { (it as? Pair<*, *>)?.first == kind }
    fun silence() { SafariAlertSequence.queue.clear(this);AlertSounds.clearTransientAlerts() }
    private fun once(key: String,kind: BannerKind,biome: SafariBiome?=null) { if(fired.add(key)) fire(kind,biome) }
    private fun encounter(kind: BannerKind,biome: SafariBiome,key: String=kind.name) {
        if(BannerConfig.localEncounters && SafariAssist.biome!=biome) return
        val time=now();if(time-(cooldown[key] ?: Long.MIN_VALUE/2)<20_000) return
        cooldown[key]=time;fire(kind,biome)
    }
    fun chat(text: String) { /* Stage banners were retired; encounters now use witnessed inventory edges. */ }
    fun caught(species: String) { }
    fun tick(client: Minecraft) {
        sync()
        if(client.player==null) { reset();return }
        AlertSounds.tick()
        if(++tick%5!=0) return
        contest(client)
        val current=run ?: return
        if(!SafariAssist.inSafari || current.endedAt!=null) return
        val biome=SafariAssist.biome
        val roster=PartyManager.withLocal(client.player!!.uuid.toString())
        if(PartyManager.hasInfo && roster!=null && roster.size>1 && roster.all { id -> client.connection?.onlinePlayers?.any { it.profile.id.toString()==id }==true })
            once("full-party",BannerKind.FULL_PARTY)
        val party=SafariSparklingMode.state
        if(biome!=null && (SafariFullClear.mode!=SafariMode.SPARKLING || party.ready)) {
            val targets=biome.critters.filter { SafariFullClear.mode!=SafariMode.SPARKLING || party.needs(it.name) }
            if(targets.isNotEmpty() && current.completed(SafariFullClear.mode,targets,party,localOnly=true)==targets.size)
                once("biome:$biome",BannerKind.BIOME_DONE,biome)
        }
        val currentFeeder=current.birds.feederCount
        if(previousBiome==SafariBiome.FOREST && biome==SafariBiome.FOREST && feeder>0 && currentFeeder==0)
            encounter(BannerKind.FEEDER_EMPTY,SafariBiome.FOREST)
        feeder=currentFeeder;previousBiome=biome
    }
    private fun contest(client: Minecraft) {
        if(!ContestConfig.trackContest || !SkyblockSidebar.inSkyblock(client) || !ContestTracker.isActive()) return
        if(ticketMode!=ContestConfig.ticketMode) { ticketMode=ContestConfig.ticketMode;ticket=false }
        val active=!ContestTracker.betweenContests()
        val window=ContestTracker.absoluteWindow(java.time.LocalDateTime.now())
        if(window!=contestWindow) {
            if(contestWindow!=-1L && active) fire(BannerKind.CONTEST_START)
            contestWindow=window;contestWarnings.clear();ticket=false
        }
        if(contestActive==true && !active) fire(BannerKind.CONTEST_END)
        contestActive=active
        if(ContestTracker.isComplete() && !ticket) { ticket=true;fire(BannerKind.TICKET) }
        if(active && !ticket) {
            val remaining=ContestTracker.secondsToEnd()
            val due=listOf(5,3,1).filter { remaining<=it*60 && it !in contestWarnings }
            contestWarnings.addAll(due)
            due.minOrNull()?.let { minute -> fire(when(minute) { 5 -> BannerKind.CONTEST_FIVE;3 -> BannerKind.CONTEST_THREE;else -> BannerKind.CONTEST_ONE }) }
        }
    }
    fun reset() { SafariAlertSequence.queue.clear(this);fired.clear();cooldown.clear();run=null;chamberTarget=null;previousBiome=null;feeder=0;heldFood=0;contestActive=null;contestWindow=-1;contestWarnings.clear();ticket=false;AlertSounds.onConnectionExit() }
    fun previewPanel(measure: (Component) -> Int = { Minecraft.getInstance().font.width(it) }): HudPanel {
        val text=Component.literal("Safari Banner").withStyle { it.withBold(BannerConfig.font in setOf(1,3)).withItalic(BannerConfig.font in setOf(2,3)) }
        return HudPanel("Safari Banner",0xFFFFAA00.toInt(),emptyList(),widthOverride=measure(text)+16,heightOverride=18)
    }
    fun renderPreview(g: GuiGraphicsExtractor,x: Int,y: Int,scale: Float)=draw(g,"Safari Banner",0xFFFFAA00.toInt(),1f,.65f,x.toFloat(),y.toFloat(),scale,160)
    fun render(g: GuiGraphicsExtractor) {
        val client=Minecraft.getInstance()
        if(client.player==null || client.options.smHideGui || client.smScreen is ContestGui) return
        val active=activeBanner ?: return
        val preview=previewPanel();val window=client.window;val width=window.guiScaledWidth;val height=window.guiScaledHeight
        val anchor=preview.bounds(SafariHud.BANNER,width,height)
        val base=SafariHud.BANNER.layout.effectiveScale(width,height,preview.width(client.font::width),preview.height)
        val maxWidth=((width-10)/base).toInt().coerceAtLeast(32)
        val text=Component.literal(active.notice.text).withStyle { it.withBold(BannerConfig.font in setOf(1,3)).withItalic(BannerConfig.font in setOf(2,3)) }
        val lines=client.font.split(text,(maxWidth-16).coerceAtLeast(16))
        val frameWidth=(lines.maxOfOrNull(client.font::width) ?: 0)+16
        val frameHeight=18+(lines.size-1).coerceAtLeast(0)*12
        val scale=minOf(base,(height-10f)/frameHeight)
        val area=SafariHud.BANNER.layout.bounds(width,height,frameWidth,frameHeight)
        val x=area.x.toFloat()
        val y=area.y.toFloat()
        draw(g,active.notice.text,active.notice.color,active.alpha(now()),active.remaining(now()),x,y,scale,maxWidth)
    }
    private fun draw(g: GuiGraphicsExtractor,title: String,color: Int,opacity: Float,remaining: Float,x: Float,y: Float,scale: Float,maxWidth: Int) {
        val font=Minecraft.getInstance().font
        val text=Component.literal(title).withStyle { it.withBold(BannerConfig.font in setOf(1,3)).withItalic(BannerConfig.font in setOf(2,3)) }
        val lines=font.split(text,(maxWidth-16).coerceAtLeast(16));val width=(lines.maxOfOrNull(font::width) ?: 0)+16;val height=18+(lines.size-1).coerceAtLeast(0)*12
        fun blend(a: Int,b: Int,f: Float): Int {
            fun channel(shift: Int)=(((a shr shift and 255)*(1-f)+(b shr shift and 255)*f).toInt() shl shift)
            return channel(16) or channel(8) or channel(0)
        }
        val alpha=(255*opacity).toInt().coerceIn(0,255);val accent=(alpha shl 24) or (color and 0xFFFFFF)
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale)
        if(BannerConfig.background) {
            val configured=SettingsColor.argb(BannerConfig.color,System.currentTimeMillis())
            val top=if(BannerConfig.tint) blend(0x080B11,color,.24f) else configured and 0xFFFFFF
            val bottom=blend(top,0,when(BannerConfig.gradient) { 0 -> 0f;1 -> .2f;else -> .55f })
            val a=(((configured ushr 24)*opacity).toInt() shl 24)
            g.fillGradient(0,0,width,height,a or top,a or bottom)
        }
        val t=BannerConfig.thickness
        if(BannerConfig.border) { g.fill(0,0,width,t,accent);g.fill(0,height-t,width,height,accent);g.fill(0,t,t,height-t,accent);g.fill(width-t,t,width,height-t,accent) }
        val inset=if(BannerConfig.border) t else 1
        fun bar(direction: Int,top: Int) {
            if(direction==0) return
            val length=(width-inset*2)*4*remaining
            val start=if(direction==1) inset*4f else (width-inset)*4f-length
            g.pose().pushMatrix();g.pose().scale(.25f,1f)
            g.fill(start.toInt(),top,(start+length).toInt(),top+1,(alpha shl 24) or blend(color,0xFFFFFF,.32f));g.pose().popMatrix()
        }
        bar(BannerConfig.topBar,inset);bar(BannerConfig.bottomBar,height-inset-1)
        lines.forEachIndexed { i,line -> g.text(font,line,(width-font.width(line))/2,5+i*12,accent,BannerConfig.shadow) }
        g.pose().popMatrix()
    }
}
