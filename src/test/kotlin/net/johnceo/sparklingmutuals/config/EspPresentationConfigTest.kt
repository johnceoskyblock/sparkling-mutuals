package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class EspPresentationConfigTest {
    @TempDir lateinit var dir: Path
    private fun init() {ConfigManager.init(dir);ContestConfig.init(dir)}

    @Test fun `organized ESP controls edit the same saved colors sizes and visibility`() {
        init()
        val settings=SafariSettings()
        val catalog=SettingsCatalog(settings)
        fun control(page: String,vararg fields: String)=catalog.locate(SettingsTarget(page,fields.toList()))!!.second.last()
        assertNotNull(control("ESP General","biomes"))
        if (SafariSettings.standardEdition) {
            assertNull(catalog.locate(SettingsTarget("ESP General", listOf("floorVisibility"))))
            assertNull(catalog.locate(SettingsTarget("ESP General", listOf("rewardDrops"))))
            assertEquals(listOf("generic"), control("ESP", "floorDrops").children.map { it.navigationName })
        } else {
            assertNotNull(control("ESP General","floorVisibility"))
            control("ESP General","rewardDrops").write(true)
        }
        val floorType = if (SafariSettings.standardEdition) FloorDropType.GENERIC else FloorDropType.COIN
        val color = if (SafariSettings.standardEdition) control("ESP", "floorDrops", "generic")
            else control("ESP", "floorDrops", "coin", "color")
        color.write("0:255:12:34:56")
        if (!SafariSettings.standardEdition) control("ESP","floorDrops","coin","textScale").write(3f)
        val critter=control("ESP","critterColors","forest","foxtrot")
        assertFalse(critter.group)
        critter.write("0:255:65:43:21")
        settings.apply();init()
        if (!SafariSettings.standardEdition) assertTrue(SafariEspConfig.rewardDrops)
        assertEquals("0:255:12:34:56",SafariEspConfig.dropColor(floorType))
        if (!SafariSettings.standardEdition) assertEquals(3f,SafariEspConfig.dropTextScale(floorType))
        assertEquals("0:255:65:43:21",SafariEspConfig.mobs.getValue("Foxtrot").color)
    }

    @Test fun `glow and boxes are independent and the new General tab saves their choices`() {
        init()
        val settings=SafariSettings()
        val catalog = SettingsCatalog(settings)
        val glow = catalog.locate(SettingsTarget("ESP General", listOf("glow")))?.second?.last()
        val boxes = catalog.locate(SettingsTarget("ESP General", listOf("boxes")))?.second?.last()
        assertTrue(SafariEspConfig.critterGlow)
        if (SafariSettings.standardEdition) {
            assertNull(boxes)
            glow!!.write(false)
            settings.apply()
            init()
            assertFalse(SafariEspConfig.critterGlow)
            return
        }
        assertFalse(SafariEspConfig.critterBoxes)
        glow!!.write(false)
        boxes!!.write(true)
        settings.apply()
        init()
        assertFalse(SafariEspConfig.critterGlow)
        assertTrue(SafariEspConfig.critterBoxes)
    }

    @Test fun `biome override covers every species without overwriting individual or mound colors`() {
        init()
        val original=SafariEspConfig.mobs.mapValues {it.value.color}
        val mound=SafariEspConfig.rockmiteMoundColor
        val settings=SafariSettings()
        settings.esp.biomeColors.enabled=true
        settings.esp.biomeColors.forest="0:255:1:2:3"
        settings.apply();init()
        assertTrue(SafariEspConfig.biomeCritterColors)
        SafariEspRules.mobs.forEach {mob ->
            assertEquals(SafariEspConfig.biomeColors.getValue(mob.biome.name.lowercase()),SafariEspConfig.entityColor(mob.name),mob.name)
        }
        assertEquals(mound,SafariEspConfig.entityColor("Rockmite",true))
        SafariSettings().apply {esp.biomeColors.cavernEnabled=false}.apply()
        init()
        assertFalse(SafariEspConfig.usesBiomeColor("Scrappy"))
        assertEquals(original.getValue("Scrappy"),SafariEspConfig.entityColor("Scrappy"))
        assertEquals("0:255:1:2:3",SafariEspConfig.entityColor("Foxtrot"))
        assertTrue(SafariEspConfig.biomeCritterColors)
        SafariSettings().apply {esp.biomeColors.enabled=false}.apply()
        init()
        original.forEach {(name,color)->assertEquals(color,SafariEspConfig.entityColor(name),name)}
    }

    @Test fun `ticket announcement toggle and separate mode messages survive switching and reload`() {
        init()
        assertTrue(ContestConfig.announceTicket)
        assertEquals("Ticket earned!",ContestConfig.ticketMessage(false))
        assertEquals("First-Class Ticket earned!",ContestConfig.ticketMessage(true))
        val settings=SafariSettings()
        settings.miria.ticketMessage="Basic ready!";settings.apply()
        settings.miria.ticketMode=1;settings.apply()
        assertEquals("First-Class Ticket earned!",settings.miria.ticketMessage)
        settings.miria.ticketMessage="First-Class ready!";settings.apply()
        settings.miria.ticketMode=0;settings.apply()
        assertEquals("Basic ready!",settings.miria.ticketMessage)
        settings.miria.announceTicket=false;settings.apply();init()
        assertFalse(ContestConfig.announceTicket)
        assertEquals("Basic ready!",ContestConfig.ticketMessage(false))
        assertEquals("First-Class ready!",ContestConfig.ticketMessage(true))
    }
}
