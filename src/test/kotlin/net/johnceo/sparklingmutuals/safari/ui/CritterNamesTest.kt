package net.johnceo.sparklingmutuals.safari.ui

import java.nio.file.Path
import java.util.Properties
import java.util.UUID
import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingNotice
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GlyphSource
import net.minecraft.client.gui.font.glyphs.BakedGlyph
import net.minecraft.network.chat.FontDescription
import net.minecraft.util.RandomSource
import net.johnceo.sparklingmutuals.hud.CritterDisplayFont
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CritterNamesTest {
    @TempDir lateinit var dir: Path
    @BeforeEach fun reset() {CritterNames.load(Properties())}
    @AfterEach fun clear() {CritterNames.load(Properties())}

    @Test fun `font measures and wraps display names once even when an alias is another critter name`() {
        val glyph=java.lang.reflect.Proxy.newProxyInstance(BakedGlyph::class.java.classLoader,arrayOf(BakedGlyph::class.java)) {_,method,_ ->
            if(method.name=="info") com.mojang.blaze3d.font.GlyphInfo.simple(1f) else error("This test only measures glyphs")
        } as BakedGlyph
        val glyphs=object: GlyphSource {
            override fun getGlyph(codepoint: Int)=glyph
            override fun getRandomGlyph(random: RandomSource,codepoint: Int)=glyph
        }
        val font=CritterDisplayFont(object: Font.Provider {
            override fun glyphs(description: FontDescription)=glyphs
            override fun effect(): net.minecraft.client.gui.font.glyphs.EffectGlyph = error("No effects during measurement")
        })
        CritterNames.set("Wumpa","Macaw")
        CritterNames.set("Macaw","Bird")
        assertEquals(5,font.width("Wumpa"))
        assertEquals(5,font.width(Component.literal("Wumpa")))
        val lines=font.split(Component.literal("Wumpa"),40)
        assertEquals(1,lines.size)
        assertEquals(5,font.width(lines.single()))
    }

    @Test fun `aliases are visual only and preserve sleeping tags and outbound canonical names`() {
        CritterNames.set("Wumpa","Sleepy Beast")
        assertEquals("SPARKLING Sleepy Beast zZz",CritterNames.display("SPARKLING Wumpa zZz"))
        assertEquals("WumpaFan",CritterNames.display("WumpaFan"))
        assertEquals("Wumpa",SafariRules.sparklingSpecies("SPARKLING Wumpa zZz"))
        assertEquals("SPARKLING Wumpa!",SparklingNotice(UUID.randomUUID(),"Wumpa","Icy").message())
        assertEquals("Wumpa",SafariRoster.named("Wumpa")!!.name)
        val original=Component.literal("SPARKLING Wumpa zZz")
        assertEquals("SPARKLING Sleepy Beast zZz",CritterNames.component(original).string)
        assertEquals("SPARKLING Wumpa zZz",original.string)
    }

    @Test fun `styled names can cross spans and aliases are not applied recursively`() {
        CritterNames.set("Mantis Shrimp","Tiny Friend")
        val original=Component.literal("Mantis ").withStyle {it.withColor(0x123456)}.append(Component.literal("Shrimp").withStyle {it.withBold(true)})
        val shown=CritterNames.component(original)
        assertEquals("Tiny Friend",shown.string)
        var color: Int?=null
        shown.visualOrderText.accept {_,style,_ -> color=style.color?.value;true}
        assertEquals(0x123456,color)
        CritterNames.set("Doomspiral","Wumpa")
        CritterNames.set("Wumpa","Other Beast")
        val once=CritterNames.component(Component.literal("Doomspiral"))
        assertEquals("Wumpa",CritterNames.component(once).string)
        val sequence=CritterNames.sequence(once.visualOrderText)
        val text=StringBuilder()
        sequence.accept {_,_,cp -> text.appendCodePoint(cp);true}
        assertEquals("Wumpa",text.toString())
    }

    @Test fun `every critter has a customization dropdown and names persist and reset independently`() {
        ConfigManager.init(dir)
        val settings=SafariSettings()
        val rows=SettingsCatalog(settings).pages.single {it.name=="ESP"}.rows.single {it.name=="Custom critter names"}
        assertEquals(37,rows.children.sumOf {it.children.size})
        assertTrue(rows.children.flatMap {it.children}.all {it.group && it.children.single().name=="Custom name"})
        settings.esp.names.icy.entries.single {it.canonical=="Wumpa"}.name="Snow Beast"
        settings.apply()
        ConfigManager.init(dir)
        assertEquals("Snow Beast",CritterNames.get("Wumpa"))
        assertEquals("",CritterNames.get("Doomspiral"))
        val next=SafariSettings()
        next.esp.names.icy.entries.single {it.canonical=="Wumpa"}.name=""
        next.apply()
        ConfigManager.init(dir)
        assertEquals("Wumpa",CritterNames.display("Wumpa"))
    }
}
