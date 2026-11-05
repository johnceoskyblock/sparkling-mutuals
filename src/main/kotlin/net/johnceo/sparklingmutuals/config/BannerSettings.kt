package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.hud.*
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.compat.smSetScreen

/** SafariUtils banners, using our persisted options and native settings widgets. */
class BannerSettings {
    @JvmField @ConfigOption(name="Banner alerts",desc="") @ConfigEditorBoolean var enabled=BannerConfig.enabled
    @JvmField @ConfigOption(name="Appearance",desc="") @Accordion val appearance=BannerAppearance()
    @JvmField @ConfigOption(name="Position and size",desc="") @ConfigEditorButton(buttonText="Edit HUDs")
    val placement=Runnable { val client=net.minecraft.client.Minecraft.getInstance();client.execute { client.smSetScreen(net.johnceo.sparklingmutuals.contest.ContestGui(client.smScreen)) } }
    @JvmField @ConfigOption(name="Preview banner",desc="") @ConfigEditorButton(buttonText="Preview")
    val preview=Runnable { appearance.apply();ConfigManager.save();SafariBanners.preview() }
    @JvmField @ConfigOption(name="Safari",desc="") @Accordion val safari=SafariBannerOptions()
    @JvmField @ConfigOption(name="Encounters",desc="") @Accordion val encounters=EncountersBanners()
    @JvmField @ConfigOption(name="Contest",desc="") @Accordion val contest=ContestBanners()
    fun apply(): Boolean {
        var changed=appearance.apply() || enabled!=BannerConfig.enabled
        if(BannerConfig.enabled && !enabled) SafariBanners.silence()
        BannerConfig.enabled=enabled
        javaClass.declaredFields.filter { it.isAnnotationPresent(Accordion::class.java) && it.name!="appearance" }.forEach { field ->
            val group=field.get(this)
            group.javaClass.declaredFields.filter { it.type==BannerOption::class.java }.forEach { changed=(it.get(group) as BannerOption).apply() || changed }
        }
        return changed
    }
}
class SafariBannerOptions {
    @JvmField @ConfigOption(name="Full party joined",desc="") @Accordion val fullParty=BannerOption(BannerKind.FULL_PARTY)
    @JvmField @ConfigOption(name="Biome complete",desc="") @Accordion val biome=BannerOption(BannerKind.BIOME_DONE)
    @JvmField @ConfigOption(name="Run complete",desc="") @Accordion val complete=BannerOption(BannerKind.RUN_COMPLETE)
    @JvmField @ConfigOption(name="Warping now",desc="") @Accordion val warp=BannerOption(BannerKind.WARP)
}
class EncountersBanners {
    @JvmField @ConfigOption(name="Hideyho inside/outside",desc="Unique and Full Clear only. Use <LOCATION> for Inside or Outside.")
    @Accordion val hideyho=BannerOption(BannerKind.HIDEYHO_LOCATION)
    @JvmField @ConfigOption(name="All bird food collected",desc="") @Accordion val food=BannerOption(BannerKind.BIRD_FOOD)
    @JvmField @ConfigOption(name="Birdfeeder empty",desc="") @Accordion val feeder=BannerOption(BannerKind.FEEDER_EMPTY)
    @JvmField @ConfigOption(name="All gems ready",desc="") @Accordion val gems=BannerOption(BannerKind.GEMS)
    @JvmField @ConfigOption(name="All incense ready",desc="") @Accordion val incense=BannerOption(BannerKind.INCENSE)
    @JvmField @ConfigOption(name="All coins collected",desc="") @Accordion val coins=BannerOption(BannerKind.COINS)
}
class ContestBanners {
    @JvmField @ConfigOption(name="Contest started",desc="") @Accordion val start=BannerOption(BannerKind.CONTEST_START)
    @JvmField @ConfigOption(name="Contest: five minutes",desc="") @Accordion val five=BannerOption(BannerKind.CONTEST_FIVE)
    @JvmField @ConfigOption(name="Contest: three minutes",desc="") @Accordion val three=BannerOption(BannerKind.CONTEST_THREE)
    @JvmField @ConfigOption(name="Contest: one minute",desc="") @Accordion val one=BannerOption(BannerKind.CONTEST_ONE)
    @JvmField @ConfigOption(name="Contest ended",desc="") @Accordion val end=BannerOption(BannerKind.CONTEST_END)
    @JvmField @ConfigOption(name="Ticket earned",desc="") @Accordion val ticket=BannerOption(BannerKind.TICKET)
}
class BannerOption(private val kind: BannerKind) {
    private val saved=BannerConfig.options.getValue(kind)
    @JvmField @ConfigOption(name="Enabled",desc="") @ConfigEditorBoolean var enabled=saved.enabled
    @JvmField @ConfigOption(name="Show",desc="") @ConfigEditorDropdown(values=["Banner + sound","Banner only","Sound only"]) var playback=saved.playback
    @JvmField @ConfigOption(name="Text",desc="") @ConfigEditorText var text=saved.text
    @JvmField @ConfigOption(name="Color",desc="") @ConfigEditorColour var color=saved.color
    @JvmField @ConfigOption(name="Duration",desc="Seconds.") @ConfigEditorSlider(minValue=1f,maxValue=15f,minStep=.5f) var duration=saved.duration
    @JvmField @ConfigOption(name="Sound",desc="") @ConfigEditorDropdown(values=["Challenge Complete","Player Level Up","Experience Orb","Amethyst Chime","Note Block Pling","Note Block Bell","Beacon Activate","Button Click","Totem Used","Note Block Chime","Note Block Xylophone","Note Block Iron Xylophone","Note Block Cow Bell","Note Block Flute","Note Block Harp","Note Block Banjo","Note Block Didgeridoo","Enchanting Table","Ender Chest Open","Firework Twinkle","Note Block Bass","Note Block Bass Drum","Note Block Bit","Note Block Guitar","Note Block Hi-Hat","Note Block Snare","Melody: Ascending Chime","Melody: Celebration","Melody: Gentle Arpeggio","Melody: Major Fanfare","Melody: Mystery","Melody: Safari Adventure","Melody: Success","Melody: Warning Pulse","Anvil Land","Arrow Hit","Bell Ring","Brewing Complete","Dragon Growl","Evoker Summon","Firework Blast","Item Pickup","Lightning Thunder","Portal Travel","Melody: Bright Discovery","Melody: Canyon Call","Melody: Enchanted Waltz","Melody: Rare Find","Melody: Sparkle Cascade","Melody: Victory March","Melody: Clockwork Waltz","Melody: Desert Caravan","Melody: Moonlit Lullaby","Melody: Ocean Voyage","Melody: Playful Steps","Melody: Royal Arrival"]) var sound=saved.sound
    @JvmField @ConfigOption(name="Volume",desc="") @ConfigEditorSlider(minValue=0f,maxValue=100f,minStep=1f) var volume=saved.volume
    @JvmField @ConfigOption(name="Pitch",desc="") @ConfigEditorSlider(minValue=.5f,maxValue=2f,minStep=.1f) var pitch=saved.pitch
    @JvmField @ConfigOption(name="Preview",desc="") @ConfigEditorButton(buttonText="Preview") val preview=Runnable { apply();ConfigManager.save();SafariBanners.preview(kind) }
    fun apply(): Boolean {
        val value=BannerPreference(enabled,playback.coerceIn(0,2),text.take(160).ifBlank { kind.text },SafariEspConfig.validColor(color,AppearanceConfig.color(kind.rgb)),
            duration.coerceIn(1f,15f),sound.coerceIn(0,55),volume.coerceIn(0,100),pitch.coerceIn(.5f,2f))
        if(value==BannerConfig.options[kind]) return false
        BannerConfig.options[kind]=value;if(!value.enabled) SafariBanners.clear(kind);return true
    }
}
class BannerAppearance {
    @JvmField @ConfigOption(name="Background",desc="") @ConfigEditorBoolean var background=BannerConfig.background
    @JvmField @ConfigOption(name="Background color",desc="") @ConfigEditorColour var color=BannerConfig.color
    @JvmField @ConfigOption(name="Background style",desc="") @ConfigEditorDropdown(values=["Flat","Soft gradient","Deep gradient"]) var gradient=BannerConfig.gradient
    @JvmField @ConfigOption(name="Match alert color",desc="") @ConfigEditorBoolean var tint=BannerConfig.tint
    @JvmField @ConfigOption(name="Border",desc="") @ConfigEditorBoolean var border=BannerConfig.border
    @JvmField @ConfigOption(name="Border thickness",desc="") @ConfigEditorSlider(minValue=1f,maxValue=4f,minStep=1f) var thickness=BannerConfig.thickness
    @JvmField @ConfigOption(name="Font",desc="") @ConfigEditorDropdown(values=["Normal","Bold","Italic","Bold italic"]) var font=BannerConfig.font
    @JvmField @ConfigOption(name="Text shadow",desc="") @ConfigEditorBoolean var shadow=BannerConfig.shadow
    @JvmField @ConfigOption(name="Top timer bar",desc="") @ConfigEditorDropdown(values=["Off","Left to right","Right to left"]) var topBar=BannerConfig.topBar
    @JvmField @ConfigOption(name="Bottom timer bar",desc="") @ConfigEditorDropdown(values=["Off","Left to right","Right to left"]) var bottomBar=BannerConfig.bottomBar
    fun apply(): Boolean {
        val before=BannerConfig.appearanceSnapshot()
        BannerConfig.background=background;BannerConfig.color=SafariEspConfig.validColor(color,"0:155:18:25:37");BannerConfig.gradient=gradient.coerceIn(0,2)
        BannerConfig.tint=tint;BannerConfig.border=border;BannerConfig.thickness=thickness.coerceIn(1,4);BannerConfig.font=font.coerceIn(0,3)
        BannerConfig.shadow=shadow;BannerConfig.topBar=topBar.coerceIn(0,2);BannerConfig.bottomBar=bottomBar.coerceIn(0,2)
        BannerConfig.localEncounters=true
        return before!=BannerConfig.appearanceSnapshot()
    }
}
