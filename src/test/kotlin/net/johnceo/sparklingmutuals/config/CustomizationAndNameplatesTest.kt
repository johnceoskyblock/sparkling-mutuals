package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import net.johnceo.sparklingmutuals.hud.*
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CustomizationAndNameplatesTest {
    @TempDir lateinit var dir: Path
    @Test fun `all 37 nameplates default off and each critter keeps independent settings`() {
        ConfigManager.init(dir)
        assertEquals(37,CritterNameplates.options.size)
        assertFalse(CritterNameplates.anyEnabled)
        val settings=SafariSettings()
        val wumpa=settings.esp.nameplates.icy.entries.single {it.canonical=="Wumpa"}
        wumpa.enabled=true;wumpa.replaceTag=false;wumpa.text="{name} · {distance}";wumpa.scale=3f;wumpa.range=100
        wumpa.useEspColor=false;wumpa.color=AppearanceConfig.color(0x123456);wumpa.background=false
        settings.apply();ConfigManager.init(dir)
        val saved=CritterNameplates.options.getValue("Wumpa")
        assertTrue(saved.enabled);assertFalse(saved.replaceTag);assertFalse(saved.background)
        assertEquals(100,saved.range);assertEquals(3f,saved.textScale)
        assertEquals("Wumpa · 42m",CritterNameplates.label("Wumpa",saved.text,42))
        assertEquals("SPARKLING Wumpa · 42m",CritterNameplates.label("Wumpa",saved.text,42,true))
        assertEquals(1,CritterNameplates.options.values.count {it.enabled})
    }
    @Test fun `world customization has its own route and HUD edit links retain their owners`() {
        ConfigManager.init(dir)
        val catalog=SettingsCatalog(SafariSettings())
        val esp=catalog.pages.single {it.name=="ESP"}
        val plate=esp.rows.single {it.name=="Critter nameplates"}
        assertEquals(37,plate.children.sumOf {it.children.size})
        assertEquals("Customization → World ESP",catalog.location(plate))
        val visibility=catalog.pages.single {it.name=="Safari helpers"}.rows.single {it.name=="Party visibility"}
        assertFalse(visibility.children.any {it.field.name in setOf("color","textScale")})
        for(hud in listOf(SafariHud.PROGRESS,SafariHud.MISSING,SafariHud.OBJECTIVES,SafariHud.CAPTURES,SafariHud.HOTSPOT))
            assertNotNull(catalog.locate(HudConfigLinks.target(hud)))
    }
    @Test fun `moved teammate appearance still uses existing saved keys`() {
        ConfigManager.init(dir)
        val settings=SafariSettings()
        settings.esp.teammates.color=AppearanceConfig.color(0x123456)
        settings.esp.teammates.textScale=3f
        settings.apply();ConfigManager.init(dir)
        assertEquals(AppearanceConfig.color(0x123456),SafariEspConfig.teammateColor)
        assertEquals(3f,SafariEspConfig.teammateTextScale)
    }
    @Test fun `HUD text styles persist and protect rarity and biome colors`() {
        ConfigManager.init(dir)
        val settings=SafariSettings()
        val text=settings.tracking.objectivesHud.appearance.text
        text.headerText="My Objectives";text.header.enabled=true;text.header.color=AppearanceConfig.color(0x112233)
        text.labels.enabled=true;text.labels.color=AppearanceConfig.color(0x445566)
        text.values.enabled=true;text.values.color=AppearanceConfig.color(0x778899)
        settings.apply();ConfigManager.init(dir)
        val style=AppearanceConfig.panels.getValue("objectives")
        assertEquals("My Objectives",style.title("Biome Objectives"))
        assertEquals(0xFF112233.toInt(),style.header.resolve(HudRow.GOLD))
        assertEquals(0xFF445566.toInt(),HudRow("Bee nests").labelColor(style))
        assertEquals(0xFF778899.toInt(),HudRow("Bee nests","2").numberColor(style))
        assertEquals(0xFFAA00AA.toInt(),HudRow("Macaw",color=0xFFAA00AA.toInt()).labelColor(style))
        assertEquals(0xFF55FF55.toInt(),HudRow("Forest",color=0xFF55FF55.toInt()).labelColor(style))
        val panel=HudPanel("X",rows=emptyList())
        assertTrue(panel.width(String::length,style)>panel.width(String::length))
    }
    @Test fun `objective reminder toggles only hide their text and retain the missing panel`() {
        ConfigManager.init(dir);SafariFullClear.select(SafariMode.UNIQUE)
        val run=SafariRun(0);val party=PartySparklingState();val biome=SafariBiome.CAVERN
        fun rows()=SafariPanels.objectives(run,biome,true,0,party=party).rows.map {it.label}
        assertTrue("Capture all uniques" in rows());assertTrue("Finish shiny checks" in rows())
        val missing=SafariPanels.missingCritters(run,biome,party).map {it.name}
        val settings=SafariSettings()
        settings.tracking.objectivesHud.captureReminder=false;settings.tracking.objectivesHud.shinyReminder=false
        settings.apply();ConfigManager.init(dir)
        assertFalse("Capture all uniques" in rows());assertFalse("Finish shiny checks" in rows())
        assertEquals(missing,SafariPanels.missingCritters(run,biome,party).map {it.name})
        assertFalse(ConfigManager.objectiveCaptureReminder);assertFalse(ConfigManager.objectiveShinyReminder)
    }
}
