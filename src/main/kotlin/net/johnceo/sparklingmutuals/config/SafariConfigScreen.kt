package net.johnceo.sparklingmutuals.config

import com.mojang.blaze3d.platform.InputConstants
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.hud.UiIcons
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.MultiLineEditBox
import net.minecraft.util.FormattedCharSequence
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import kotlin.math.round

/** SafariUtils workspace layout adapted to our settings/actions (licenses/SafariUtils-MIT.txt). */
class SafariConfigScreen(private val settings: SafariSettings = SafariSettings(), private val initialTarget: SettingsTarget? = null, private val returnTo: Screen? = null) : Screen(Component.literal("Sparkling Mutuals")) {
    private val catalog=SettingsCatalog(settings)
    private data class Area(val name: String,val pages: List<Int>)
    private val areas=listOf(Area("HUDs",listOf(0,7,10)),Area("Safari",listOf(1,6,3,12)),Area("Party",listOf(2,5)),
        Area("ESP",listOf(13,14,15,16,9)),Area("History",listOf(4)),Area("Advanced",listOf(8,11)))
    private val tabNames=mapOf(0 to "Overview",7 to "HUDs",1 to "Modes",6 to "Helpers",3 to "Contest",2 to "Interactions",5 to "Sparkle Warping",9 to "Highlights",10 to "Appearance",4 to "History",8 to "API",11 to "SkyBlock",12 to "Banner")
    private val areaTabs=rememberedNavigation.areaTabs.toMutableMap()
    private var contentTop=88
    private var selected=rememberedNavigation.page.coerceIn(catalog.pages.indices)
    private val expanded=rememberedNavigation.expanded.toMutableSet()
    private val scrolls=rememberedNavigation.scrolls.toMutableMap()
    private var scroll=rememberedNavigation.scroll
    private var skyblockTab=rememberedNavigation.skyblockTab
    private var navScroll=rememberedNavigation.navScroll
    private var initialQuery=rememberedNavigation.query
    private var contentHeight=0
    private lateinit var search: EditBox
    private var contentDrawing=false
    private val hits=mutableListOf<Hit>()
    private var slider: Pair<SettingsCatalog.Row,IntRange>?=null
    private var draggingScroll=false
    private var scrollGrab=0.0
    private var modal: SettingsCatalog.Row?=null
    private var choices: List<String>?=null
    private var choose: ((Int)->Unit)?=null
    private var choiceScroll=0
    private var editor: EditBox?=null
    private var multilineEditor: MultiLineEditBox?=null
    private var targetApplied=false
    private var focusPath: String?=null
    private var scrollToFocus=false
    private var highlightUntil=0L
    private var validation=""
    private var colorDrag: ((Double,Double)->Unit)?=null
    private var hue=0f;private var saturation=0f;private var brightness=1f
    private var opacity=255;private var chroma=0
    private val colorEditing get()=modal?.field?.isAnnotationPresent(ConfigEditorColour::class.java)==true
    private fun colorValue(): String {
        val rgb=java.awt.Color.HSBtoRGB(hue,saturation,brightness)
        return "$chroma:$opacity:${rgb shr 16 and 255}:${rgb shr 8 and 255}:${rgb and 255}"
    }
    private fun updateColorText() { editor?.value=SettingsColor.hex(colorValue()) }
    private var modalRect=Rect(0,0,0,0)
    private val nav get()=if(width<450) 88 else 136
    private val left get()=nav+12
    private val right get()=width-12
    private val top get()=contentTop
    private val bottom get()=height-38
    private val viewport get()=(bottom-top).coerceAtLeast(1)
    private data class Rect(val x: Int,val y: Int,val w: Int,val h: Int) {
        fun contains(mx: Double,my: Double)=mx>=x && mx<x+w && my>=y && my<y+h
    }
    private data class Hit(val rect: Rect,val action: ()->Unit)
    override fun init() {
        if(!targetApplied) {
            targetApplied=true
            initialTarget?.let(catalog::locate)?.let { (page,chain) ->
                initialQuery=""
                selected=page;chain.dropLast(1).forEach { expanded.add(it.path) }
                if(page==11) skyblockTab=if(chain.first().field.name=="endgame") 1 else 0
                focusPath=chain.last().path;scrollToFocus=true;highlightUntil=System.currentTimeMillis()+8000
            }
        }
        val query=if(::search.isInitialized) search.value else initialQuery
        val header=ConfigLayout.header(left,right)
        search=EditBox(font,left+9,19,(header.searchWidth-18).coerceAtLeast(16),18,Component.literal("Search settings"))
        search.setBordered(false);search.setMaxLength(160);search.setHint(Component.empty())
        search.value=query;search.setResponder { scroll=0 };addRenderableWidget(search)
        editor?.let { addRenderableWidget(it) };multilineEditor?.let { previous -> multilineEditor=createTextEditor(modal?.name.orEmpty(),previous.value);addRenderableWidget(multilineEditor!!) };ModUpdates.check()
    }
    override fun tick() { settings.apply(notifyMode=true);ModUpdates.check() }
    override fun removed() {
        rememberedNavigation = navigation()
        settings.apply(notifyMode=true)
        settings.saveTextFields()
        super.removed()
    }
    override fun isPauseScreen()=false
    override fun onClose() {
        if(modal!=null || choices!=null) { closeModal();return }
        minecraft.smSetScreen(returnTo)
    }
    private fun closeModal() {
        editor?.let(::removeWidget);multilineEditor?.let(::removeWidget)
        editor=null;multilineEditor=null;modal=null;choices=null;choose=null;validation=""
        search.active=true;search.visible=true;setFocused(search);slider=null;colorDrag=null
    }
    private fun hit(r: Rect,action: ()->Unit) {
        val clipped=if(contentDrawing) Rect(r.x,maxOf(top,r.y),r.w,(minOf(bottom,r.y+r.h)-maxOf(top,r.y)).coerceAtLeast(0)) else r
        if(clipped.h>0) hits.add(Hit(clipped,action))
    }
    private fun text(g: GuiGraphicsExtractor,s: String,x: Int,y: Int,color: Int=SettingsPalette.text) { g.text(font,s,x,y,color,false) }
    private fun shell(g: GuiGraphicsExtractor,r: Rect,color: Int) {
        g.fill(r.x,r.y,r.x+r.w,r.y+r.h,color)
        g.fill(r.x,r.y,r.x+r.w,r.y+1,SettingsPalette.border);g.fill(r.x,r.y+r.h-1,r.x+r.w,r.y+r.h,SettingsPalette.border)
        g.fill(r.x,r.y,r.x+1,r.y+r.h,SettingsPalette.border);g.fill(r.x+r.w-1,r.y,r.x+r.w,r.y+r.h,SettingsPalette.border)
    }
    private fun button(g: GuiGraphicsExtractor,s: String,r: Rect,mx: Int,my: Int,action: ()->Unit) {
        shell(g,r,if(r.contains(mx.toDouble(),my.toDouble())) SettingsPalette.hover else SettingsPalette.card)
        val lines=font.split(Component.literal(s),(r.w-16).coerceAtLeast(1))
        lines.forEachIndexed { i,line -> g.text(font,line,r.x+8,r.y+(r.h-lines.size*10)/2+i*10,SettingsPalette.text,false) }
        hit(r,action)
    }
    override fun extractRenderState(g: GuiGraphicsExtractor,mx: Int,my: Int,delta: Float) {
        val blocked=modal!=null || choices!=null
        val px=if(blocked) Int.MIN_VALUE else mx;val py=if(blocked) Int.MIN_VALUE else my
        hits.clear();g.fill(0,0,width,height,SettingsPalette.background)
        g.fill(0,0,nav,height,SettingsPalette.surface);g.fill(nav,0,width,48,SettingsPalette.surface)
        g.fill(nav-1,0,nav,height,SettingsPalette.border);g.fill(0,47,width,48,SettingsPalette.border)
        g.text(font,Component.literal("SPARKLING").withStyle { it.withBold(true) },6,11,SettingsPalette.gold,false)
        g.text(font,Component.literal("MUTUALS").withStyle { it.withBold(true) },6,25,SettingsPalette.gold,false)
        val header=ConfigLayout.header(left,right)
        shell(g,Rect(left,10,header.searchWidth,28),SettingsPalette.card)
        if(search.value.isEmpty() && !blocked) text(g,if(header.searchWidth>110) "Search settings…" else "Search…",left+9,21,SettingsPalette.muted)
        val themeRect=Rect(header.themeX,10,header.themeWidth,28)
        shell(g,themeRect,SettingsPalette.card)
        UiIcons.draw(g,themeRect.x+9,themeRect.y+10,UiIcons.Kind.GEM,SettingsPalette.accent)
        if(themeRect.w>=110) {
            text(g,"THEME",themeRect.x+24,themeRect.y+4,SettingsPalette.muted)
            text(g,SettingsPalette.names[ConfigManager.settingsTheme],themeRect.x+24,themeRect.y+15)
        } else text(g,"Theme",themeRect.x+25,themeRect.y+10)
        hit(themeRect) { choices=SettingsPalette.names;choiceScroll=0;choose={ ConfigManager.settingsTheme=it;ConfigManager.save();closeModal() } }
        g.enableScissor(0,54,nav,height-36)
        val activeArea=areas.first { selected in it.pages }
        areas.forEachIndexed { i,area ->
            val y=56+i*36-navScroll
            if(y+32>=54 && y<height-36) {
                val active=area==activeArea
                val color=if(active) SettingsPalette.selected else if(mx in 0 until nav && my in y until y+32 && !blocked) SettingsPalette.hover else SettingsPalette.surface
                g.fill(0,y,nav-1,y+32,color)
                if(active) g.fill(0,y,3,y+32,SettingsPalette.accent)
                val title=Component.literal(area.name).withStyle { it.withBold(true) }
                g.text(font,title,12,y+12,if(active) SettingsPalette.text else SettingsPalette.muted,false)
                hit(Rect(0,maxOf(y,54),nav,(minOf(y+32,height-36)-maxOf(y,54)).coerceAtLeast(0))) {
                    areaTabs[activeArea.name]=selected;scrolls[selected]=scroll
                    selected=areaTabs[area.name]?:area.pages.first();scroll=scrolls[selected]?:0;search.value=""
                }
            }
        }
        g.disableScissor()
        val navHeight=height-94
        if(areas.size*36>navHeight) {
            val thumb=(navHeight.toFloat()*navHeight/(areas.size*36)).toInt().coerceAtLeast(12)
            val y=56+(navHeight-thumb)*navScroll/(areas.size*36-navHeight).coerceAtLeast(1)
            g.fill(nav-4,y,nav-2,y+thumb,SettingsPalette.border)
        }
        val update=Rect(5,height-34,nav-10,30)
        shell(g,update,if(update.contains(px.toDouble(),py.toDouble())) SettingsPalette.hover else SettingsPalette.card)
        if(ModUpdates.label=="Up to Date") UiIcons.draw(g,12,height-23,UiIcons.Kind.CHECK,SettingsPalette.green)
        val status=if(ModUpdates.label=="Up to Date") "Up to date" else ModUpdates.label
        val statusLines=font.split(Component.literal(status),nav-40)
        statusLines.forEachIndexed { i,line -> g.text(font,line,28,update.y+(update.h-statusLines.size*10)/2+i*10,
            if(ModUpdates.label=="Up to Date") SettingsPalette.green else SettingsPalette.muted,false) }
        hit(update) { minecraft.smSetScreen(UpdateDetailsScreen(this)) }
        if(search.value.isBlank()) {
            var tabX=left;var tabY=56
            activeArea.pages.forEach { page ->
                val name=tabNames[page]?:catalog.pages[page].name
                val w=(font.width(name)+24).coerceIn(66,(right-left).coerceAtLeast(66))
                if(tabX>left && tabX+w>right) { tabX=left;tabY+=27 }
                shell(g,Rect(tabX,tabY,w,22),if(selected==page) SettingsPalette.selected else SettingsPalette.card)
                text(g,name,tabX+(w-font.width(name))/2,tabY+7,if(selected==page) SettingsPalette.text else SettingsPalette.muted)
                hit(Rect(tabX,tabY,w,22)) { scrolls[selected]=scroll;selected=page;scroll=scrolls[page]?:0 }
                tabX+=w+6
            }
            if (selected==11) {
                tabY+=29
                var subX=left
                listOf("Helpers","Endgame").forEachIndexed { index,label ->
                    val r=Rect(subX,tabY,font.width(label)+24,22)
                    shell(g,r,if(skyblockTab==index) SettingsPalette.selected else SettingsPalette.card)
                    text(g,label,r.x+12,r.y+7)
                    hit(r) { skyblockTab=index;scroll=0 }
                    subX+=r.w+6
                }
            }
            contentTop=(tabY+34).coerceAtMost(height-70)
        } else { text(g,"Search results",left,58,SettingsPalette.accent);contentTop=80 }
        val cards=measureCards()
        contentHeight=cards.sumOf { it.height+8 }
        if(scrollToFocus) {
            var offset=0
            for(card in cards) { if(card.row.path==focusPath) { scroll=offset;break };offset+=card.height+8 }
            scrollToFocus=false
        }
        scroll=scroll.coerceIn(0,maxScroll())
        var y=top-scroll
        g.enableScissor(left,top,right,bottom)
        contentDrawing=true
        cards.forEach { card ->
            val row=card.row;val x=card.x;val w=card.width;val h=card.height
            if(y+h>top && y<bottom) {
                val r=Rect(x,y,w,h)
                val focused=row.path==focusPath && System.currentTimeMillis()<highlightUntil
                if(!row.standalone) shell(g,r,if(focused) SettingsPalette.selected else if(r.contains(px.toDouble(),py.toDouble())) SettingsPalette.hover else SettingsPalette.card)
                if(row.group) {
                    text(g,if(row.path in expanded) "−" else "+",x+10,y+(h-8)/2,SettingsPalette.accent)
                    card.title.forEachIndexed { i,line -> g.text(font,line,x+26,y+9+i*11,SettingsPalette.text,false) }
                    hit(r) { if(!expanded.add(row.path)) expanded.remove(row.path) }
                } else {
                    card.title.forEachIndexed { i,line -> g.text(font,line,x+12,y+card.textTop+i*11,if(row.standalone) SettingsPalette.accent else SettingsPalette.text,false) }
                    val descY=y+card.textTop+card.title.size*11+if(row.description.isBlank()) 0 else 4
                    if(row.description.isNotBlank()) card.description.forEachIndexed { i,line -> g.text(font,line,x+12,descY+i*11,SettingsPalette.muted,false) }
                    card.control?.let { c -> control(g,row,Rect(c.x,y+c.y,c.w,c.h),px,py) }
                }
            }
            y+=h+8
        }
        contentDrawing=false;g.disableScissor()
        if(maxScroll()>0) {
            val track=scrollTrack();g.fill(right-4,top,right,bottom,SettingsPalette.card)
            g.fill(track.x,track.y,track.x+track.w,track.y+track.h,SettingsPalette.accent)
            hit(Rect(right-6,top,10,viewport)) { draggingScroll=true;scrollGrab=if(track.contains(mx.toDouble(),my.toDouble())) my-track.y.toDouble() else track.h/2.0;dragTo(my.toDouble()) }
        }
        g.fill(nav,height-32,width,height,SettingsPalette.surface)
        text(g,"Changes save automatically",left,height-20,SettingsPalette.muted)
        button(g,"Done",Rect(right-54,height-27,54,21),px,py) { onClose() }
        if(blocked) { hits.clear();drawModal(g,mx,my) }
        search.visible=!blocked;search.active=!blocked;super.extractRenderState(g,mx,my,delta)
    }
    private data class Card(val row: SettingsCatalog.Row,val x: Int,val width: Int,val height: Int,
        val title: List<FormattedCharSequence>,val description: List<FormattedCharSequence>,val control: Rect?,val textTop: Int=12)
    private fun measureCards()=visibleRows().map { (row,depth) ->
        val x=left+(depth*8).coerceAtMost((right-left-180).coerceAtLeast(0));val w=(right-x-14).coerceAtLeast(70)
        val value=row.read();val field=row.field
        val dropdown=field.getAnnotation(ConfigEditorDropdown::class.java)
        val action=field.getAnnotation(ConfigEditorButton::class.java)
        val color=field.isAnnotationPresent(ConfigEditorColour::class.java)
        val label=when { action!=null -> action.buttonText;dropdown!=null -> dropdown.values.getOrNull((value as Number).toInt()).orEmpty();else -> value?.toString().orEmpty().ifBlank { "Edit…" } }
        val preferred=when { action!=null -> maxOf(88,font.width(label)+20);value is Boolean -> 48;field.isAnnotationPresent(ConfigEditorSlider::class.java) || color -> 120;else -> maxOf(120,font.width(label)+20) }
        val columns=ConfigLayout.columns(w,preferred,value is String && !color || dropdown!=null && preferred>w/2)
        val textWidth=if(row.group) w-42 else if(row.standalone) w-24 else columns.textWidth
        val title=font.split(Component.literal(row.name),textWidth.coerceAtLeast(1))
        val desc=font.split(row.descriptionText(),textWidth.coerceAtLeast(1))
        val textHeight=title.size*11+if(row.description.isBlank()) 0 else 4+desc.size*11
        if(row.group) Card(row,x,w,maxOf(28,18+title.size*11),title,desc,null)
        else if(row.standalone) Card(row,x,w,24+textHeight,title,desc,null)
        else if(columns.stacked) {
            val controlHeight=if(action==null && value is Boolean || color || field.isAnnotationPresent(ConfigEditorSlider::class.java)) 24
                else maxOf(24,14+font.split(Component.literal(label),(w-40).coerceAtLeast(1)).size*10)
            val cy=12+textHeight+10
            Card(row,x,w,cy+controlHeight+12,title,desc,Rect(x+12,cy,w-24,controlHeight))
        } else {
            val h=maxOf(48,24+textHeight)
            Card(row,x,w,h,title,desc,Rect(x+w-12-columns.controlWidth,(h-24)/2,columns.controlWidth,24),(h-textHeight)/2)
        }
    }
    private fun navigation() = Navigation(selected, expanded.toSet(), scrolls.toMap(), scroll, skyblockTab,
        navScroll, areaTabs.toMap(), if (::search.isInitialized) search.value else initialQuery)

    fun freshForReturn()=SafariConfigScreen(returnTo=returnTo).also {
        val state = navigation()
        it.selected=state.page
        it.expanded.clear()
        it.expanded.addAll(state.expanded)
        it.scrolls.clear()
        it.scrolls.putAll(state.scrolls)
        it.scroll=state.scroll
        it.skyblockTab=state.skyblockTab
        it.navScroll=state.navScroll
        it.areaTabs.clear()
        it.areaTabs.putAll(state.areaTabs)
        it.initialQuery=state.query
    }
    private fun visibleRows(): List<Pair<SettingsCatalog.Row,Int>> = buildList {
        val query=search.value.trim()
        fun visit(rows: List<SettingsCatalog.Row>,depth: Int) {
            rows.forEach { row ->
                if(query.isNotEmpty()) { if(!row.group && row.matches(query)) add(row to 0);visit(row.children,0) }
                else { add(row to depth);if(row.path in expanded) visit(row.children,depth+1) }
            }
        }
        if(query.isBlank()) {
            if (selected==11) visit(catalog.pages[selected].rows[skyblockTab].children,0)
            else visit(catalog.pages[selected].rows,0)
        } else catalog.pages.forEach { visit(it.rows,0) }
    }
    private fun control(g: GuiGraphicsExtractor,row: SettingsCatalog.Row,r: Rect,mx: Int,my: Int) {
        val value=row.read();val f=row.field
        val range=f.getAnnotation(ConfigEditorSlider::class.java)
        val dropdown=f.getAnnotation(ConfigEditorDropdown::class.java)
        val action=f.getAnnotation(ConfigEditorButton::class.java)
        when {
            action!=null -> button(g,action.buttonText,r,mx,my) { settings.apply();if(value is Runnable) value.run() else settings.executeRunnable(action.runnableId) }
            range!=null -> {
                val fraction=((value as Number).toFloat()-range.minValue)/(range.maxValue-range.minValue)
                shell(g,r,SettingsPalette.surface)
                val x=r.x+6+(fraction.coerceIn(0f,1f)*(r.w-12)).toInt()
                g.fill(r.x+6,r.y+17,r.x+r.w-6,r.y+19,SettingsPalette.border);g.fill(r.x+6,r.y+17,x,r.y+19,SettingsPalette.accent)
                g.fill(x-2,r.y+15,x+2,r.y+21,SettingsPalette.text)
                text(g,if(value.toFloat()%1f==0f) value.toInt().toString() else "%.2f".format(java.util.Locale.ROOT,value.toFloat()),r.x+6,r.y+4,SettingsPalette.accent)
                hit(r) { slider=row to (r.x+6..r.x+r.w-6);setSlider(mx.toDouble()) }
            }
            dropdown!=null -> button(g,dropdown.values.getOrNull((value as Number).toInt())?:"Choose",r,mx,my) {
                modal=row;choices=dropdown.values.toList();choiceScroll=0;choose={ row.write(it);settings.apply(notifyMode=true);closeModal() }
            }
            f.isAnnotationPresent(ConfigEditorColour::class.java) -> {
                shell(g,r,SettingsColor.argb(value as String));text(g,SettingsColor.hex(value),r.x+6,r.y+8,SettingsColor.contrast(value,SettingsPalette.card));hit(r) { edit(row,SettingsColor.hex(value)) }
            }
            value is Boolean -> {
                val t=Rect(r.x+r.w-40,r.y+3,40,18);shell(g,t,if(value) SettingsPalette.green else SettingsPalette.border)
                val x=if(value) t.x+23 else t.x+3;g.fill(x,t.y+3,x+14,t.y+15,SettingsPalette.text)
                hit(r) { row.write(!value);settings.apply(notifyMode=true) }
            }
            else -> button(g,value?.toString().orEmpty().ifEmpty { "Edit…" },r,mx,my) { edit(row,value?.toString().orEmpty()) }
        }
    }
    private fun setSlider(mx: Double) {
        val (row,b)=slider?:return;val r=row.field.getAnnotation(ConfigEditorSlider::class.java)
        val raw=r.minValue+((mx-b.first)/(b.last-b.first).coerceAtLeast(1)).coerceIn(0.0,1.0)*(r.maxValue-r.minValue)
        val n=(r.minValue+round((raw-r.minValue)/r.minStep)*r.minStep).coerceIn(r.minValue.toDouble(),r.maxValue.toDouble())
        row.write(if(row.read() is Int) n.toInt() else n.toFloat());settings.apply(notifyMode=true)
    }
    private fun edit(row: SettingsCatalog.Row,value: String) {
        if(row.field.isAnnotationPresent(ConfigEditorColour::class.java)) {
            val p=(row.read() as String).split(':').map(String::toInt)
            val hsv=java.awt.Color.RGBtoHSB(p[2],p[3],p[4],null)
            hue=hsv[0];saturation=hsv[1];brightness=hsv[2];chroma=p[0];opacity=p[1]
        }
        modal=row;validation=""
        if(!colorEditing) {
            multilineEditor=createTextEditor(row.name,value).also { addRenderableWidget(it);setFocused(it) }
        } else editor=EditBox(font,0,0,100,20,Component.literal(row.name)).also {
            it.setMaxLength(2048);it.value=value;addRenderableWidget(it);setFocused(it)
        }
    }
    private fun createTextEditor(title: String,value: String)=MultiLineEditBox.builder()
        .setTextColor(SettingsPalette.text).setTextShadow(false)
        .build(font,(width-32).coerceAtMost(520)-24,70,Component.literal(title)).also {
            it.setCharacterLimit(2048);it.setValue(value)
        }
    private fun saveEditor() {
        val row=modal?:return;val input=multilineEditor?.value?.replace(Regex("[\\r\\n]+")," ") ?: editor?.value ?: return
        val value=if(row.field.isAnnotationPresent(ConfigEditorColour::class.java)) SettingsColor.fromHex(input,colorValue()) else input
        if(value==null) { validation="Use #RRGGBB, for example #55FF55";return }
        row.write(value);settings.apply(notifyMode=true);settings.saveTextFields();closeModal()
    }
    private fun drawModal(g: GuiGraphicsExtractor,mx: Int,my: Int) {
        g.fill(0,0,width,height,0xB8000000.toInt())
        val options=choices;val w=(width-32).coerceAtMost(if(colorEditing || options!=null) 350 else 520);val h=if(options!=null) (height-42).coerceAtMost(290) else if(colorEditing) 222.coerceAtMost(height-20) else (height-40).coerceAtMost(240)
        val x=(width-w)/2;val y=(height-h)/2;modalRect=Rect(x,y,w,h);shell(g,modalRect,SettingsPalette.surface)
        val heading=font.split(Component.literal(modal?.name?:"Theme"),w-24)
        heading.forEachIndexed { i,line -> g.text(font,line,x+12,y+12+i*11,SettingsPalette.accent,false) }
        val headingHeight=heading.size*11
        if(options!=null) {
            val count=((h-54)/24).coerceAtLeast(1);choiceScroll=choiceScroll.coerceIn(0,(options.size-count).coerceAtLeast(0))
            options.drop(choiceScroll).take(count).forEachIndexed { i,name -> button(g,name,Rect(x+10,y+29+i*24,w-20,22),mx,my) { choose?.invoke(i+choiceScroll) } }
            text(g,"Scroll for more · Esc to return",x+12,y+h-17,SettingsPalette.muted)
        } else {
            if(colorEditing) drawColorControls(g,x,y,w,mx,my)
            editor?.let { it.setX(x+12);it.setY(y+if(colorEditing) h-76 else 24+headingHeight);it.setWidth(w-24) }
            multilineEditor?.let { it.setX(x+12);it.setY(y+24+headingHeight);it.setSize(w-24,(h-headingHeight-75).coerceAtLeast(28)) }
            text(g,validation.ifBlank { if(multilineEditor!=null) "Ctrl+Enter to save · Enter for a new line" else "" },x+12,y+h-45,
                if(validation.isBlank()) SettingsPalette.muted else SettingsPalette.red)
            button(g,"Cancel",Rect(x+w-144,y+h-31,62,22),mx,my) { closeModal() }
            button(g,"Save",Rect(x+w-75,y+h-31,63,22),mx,my) { saveEditor() }
        }
    }
    private fun drawColorControls(g: GuiGraphicsExtractor,x: Int,y: Int,w: Int,mx: Int,my: Int) {
        val pad=Rect(x+12,y+32,w-62,58)
        for(i in 0 until 32) for(j in 0 until 12) {
            val color=java.awt.Color.HSBtoRGB(hue,i/31f,1-j/11f)
            g.fill(pad.x+pad.w*i/32,pad.y+pad.h*j/12,pad.x+pad.w*(i+1)/32,pad.y+pad.h*(j+1)/12,color)
        }
        val markerX=pad.x+(saturation*pad.w).toInt();val markerY=pad.y+((1-brightness)*pad.h).toInt()
        g.fill(markerX-2,markerY-1,markerX+3,markerY+2,-1)
        fun bind(rect: Rect,action: (Double,Double)->Unit) {
            hit(rect) { colorDrag=action;action(mx.toDouble(),my.toDouble());updateColorText() }
        }
        bind(pad) { px,py -> saturation=((px-pad.x)/pad.w).toFloat().coerceIn(0f,1f);brightness=1-((py-pad.y)/pad.h).toFloat().coerceIn(0f,1f) }
        val hueBar=Rect(x+w-39,y+32,20,58)
        for(i in 0 until 29) g.fill(hueBar.x,hueBar.y+i*2,hueBar.x+20,hueBar.y+i*2+2,java.awt.Color.HSBtoRGB(i/29f,1f,1f))
        bind(hueBar) { _,py -> hue=((py-hueBar.y)/hueBar.h).toFloat().coerceIn(0f,1f) }
        fun bar(label: String,top: Int,fraction: Float,change: (Float)->Unit) {
            text(g,label,x+12,top+4,SettingsPalette.muted)
            val track=Rect(x+106,top,w-124,16);shell(g,track,SettingsPalette.card)
            g.fill(track.x+3,top+11,track.x+3+((track.w-6)*fraction).toInt(),top+14,SettingsPalette.accent)
            bind(track) { px,_ -> change(((px-track.x)/track.w).toFloat().coerceIn(0f,1f)) }
        }
        bar("Opacity $opacity",y+97,opacity/255f) { opacity=(it*255).toInt() }
        bar("Chroma $chroma",y+118,chroma/255f) { chroma=(it*255).toInt() }
    }
    private fun maxScroll()=(contentHeight-viewport).coerceAtLeast(0)
    private fun scrollTrack(): Rect {
        val h=(viewport.toDouble()*viewport/contentHeight.coerceAtLeast(1)).toInt().coerceIn(16.coerceAtMost(viewport),viewport)
        return Rect(right-4,top+((viewport-h)*scroll.toDouble()/maxScroll().coerceAtLeast(1)).toInt(),4,h)
    }
    private fun dragTo(y: Double) { val t=scrollTrack();scroll=(((y-top-scrollGrab)/(viewport-t.h).coerceAtLeast(1))*maxScroll()).toInt().coerceIn(0,maxScroll()) }
    override fun mouseClicked(event: MouseButtonEvent,doubleClick: Boolean): Boolean {
        if(event.button()==InputConstants.MOUSE_BUTTON_LEFT) {
            hits.lastOrNull { it.rect.contains(event.x(),event.y()) }?.let { it.action();return true }
            if((modal!=null || choices!=null) && !modalRect.contains(event.x(),event.y())) { closeModal();return true }
        }
        return super.mouseClicked(event,doubleClick)
    }
    override fun mouseDragged(event: MouseButtonEvent,dx: Double,dy: Double): Boolean {
        colorDrag?.let { it(event.x(),event.y());updateColorText();return true }
        if(slider!=null) { setSlider(event.x());return true };if(draggingScroll) { dragTo(event.y());return true };return super.mouseDragged(event,dx,dy)
    }
    override fun mouseReleased(event: MouseButtonEvent): Boolean { slider=null;colorDrag=null;draggingScroll=false;return super.mouseReleased(event) }
    override fun mouseScrolled(mouseX: Double,mouseY: Double,scrollX: Double,scrollY: Double): Boolean {
        if(multilineEditor!=null && modalRect.contains(mouseX,mouseY)) return super.mouseScrolled(mouseX,mouseY,scrollX,scrollY)
        if(choices!=null) choiceScroll=(choiceScroll-scrollY.toInt()*2).coerceAtLeast(0)
        else if(modal==null) {
            if(mouseX<nav) navScroll=(navScroll-scrollY.toInt()*24).coerceIn(0,(areas.size*36-(height-94)).coerceAtLeast(0))
            else scroll=(scroll-scrollY.toInt()*30).coerceIn(0,maxScroll())
        };return true
    }
    override fun keyPressed(event: KeyEvent): Boolean {
        if(event.key()==InputConstants.KEY_ESCAPE && (modal!=null || choices!=null)) { closeModal();return true }
        if(event.key() in setOf(InputConstants.KEY_RETURN,InputConstants.KEY_NUMPADENTER) && (editor!=null || multilineEditor!=null && event.hasControlDown())) { saveEditor();return true }
        if(event.key() in setOf(InputConstants.KEY_RETURN,InputConstants.KEY_NUMPADENTER) && search.isFocused && search.value.isNotBlank()) {
            val found=visibleRows().map { it.first }
            if(found.size==1) found.single().field.getAnnotation(ConfigEditorButton::class.java)?.let { action ->
                settings.apply();val value=found.single().read()
                if(value is Runnable) value.run() else settings.executeRunnable(action.runnableId)
                return true
            }
        }
        return super.keyPressed(event)
    }
    private data class Navigation(
        val page: Int = 0, val expanded: Set<String> = emptySet(), val scrolls: Map<Int,Int> = emptyMap(),
        val scroll: Int = 0, val skyblockTab: Int = 0, val navScroll: Int = 0,
        val areaTabs: Map<String,Int> = emptyMap(), val query: String = ""
    )
    companion object { private var rememberedNavigation=Navigation() }
}
