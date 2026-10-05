package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Base64

class SafariEspClickerTest {
    @TempDir lateinit var dir: Path
    @Test fun `registry covers every critter and distinguishes fish parrots and shulkers`() {
        assertEquals(SafariRoster.all.map { it.name }.toSet(), SafariEspRules.mobs.map { it.name }.toSet())
        fun find(entity: EspEntity) = SafariEspRules.identify(entity)?.name
        assertEquals("Cavernfish", find(EspEntity("tropical_fish", fish = "CLAYFISH/GRAY/BROWN")))
        assertEquals("Tepid", find(EspEntity("tropical_fish", fish = "SNOOPER/WHITE/WHITE")))
        assertNull(find(EspEntity("tropical_fish", fish = "SNOOPER/GRAY/BROWN")))
        assertEquals("Bluebird", find(EspEntity("parrot", variant = "BLUE")))
        assertEquals("Parakeet", find(EspEntity("parrot", variant = "GREEN")))
        assertEquals("Macaw", find(EspEntity("parrot", variant = "RED_BLUE")))
        assertNull(find(EspEntity("parrot", variant = "GRAY")))
        for (type in listOf("shulker", "item_display", "block_display")) {
            assertEquals("Hideonfloor", find(EspEntity(type, shulker = "GREEN")))
            assertEquals("Hideonwall", find(EspEntity(type, shulker = "PURPLE")))
            assertNull(find(EspEntity(type, shulker = "RED")))
        }
        assertEquals("Rockmite", find(EspEntity("silverfish")))
        assertNull(find(EspEntity("silverfish", invisible = true)))
        assertNull(find(EspEntity("silverfish", passengers = true)))
    }
    @Test fun `all texture and vanilla identifications preserve upstream species`() {
        SafariEspRules.mobs.forEach { mob ->
            mob.identifiers.forEach { assertEquals(mob.name, SafariEspRules.identify(it)?.name) }
        }
        val hash = SafariEspRules.mobs.first { it.name == "Flitter" }.identifiers.single().texture!!
        val encoded = Base64.getEncoder().encodeToString("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/$hash\"}}}".toByteArray())
        assertEquals(hash, SafariEspRules.textureHash(encoded))
        assertEquals("Flitter", SafariEspRules.identify(EspEntity("item_display", texture = hash))?.name)
        assertNull(SafariEspRules.textureHash("invalid"))
        assertNull(SafariEspRules.identify(EspEntity("armor_stand", texture = hash)))
    }
    @Test fun `floor drops need three string displays in one block and render once`() {
        val markers = listOf(EspDrop(9, 1, 2, 3), EspDrop(3, 1, 2, 3), EspDrop(7, 1, 2, 3),
            EspDrop(1, 1, 2, 4), EspDrop(2, 1, 2, 4))
        assertEquals(listOf(EspDrop(3, 1, 2, 3)), SafariEspRules.floorDrops(markers))
    }
    @Test fun `ESP filters permit other biomes only when requested and reject outside Safari`() {
        assertEquals(SafariBiome.FOREST, SafariEspRules.biomeAt(-40.0, 10.0))
        assertEquals(SafariBiome.CAVERN, SafariEspRules.biomeAt(-100.0, 20.0))
        assertEquals(SafariBiome.HAUNTED, SafariEspRules.biomeAt(-20.0, -80.0))
        assertEquals(SafariBiome.ICY, SafariEspRules.biomeAt(-100.0, -30.0))
        assertNull(SafariEspRules.biomeAt(200.0, 20.0))
        assertTrue(SafariEspRules.visible(true, true, false, SafariBiome.FOREST, SafariBiome.ICY))
        assertFalse(SafariEspRules.visible(true, true, true, SafariBiome.FOREST, SafariBiome.ICY))
        assertFalse(SafariEspRules.visible(true, true, true, null, SafariBiome.ICY))
        assertFalse(SafariEspRules.visible(false, true, false, SafariBiome.FOREST, SafariBiome.FOREST))
        assertFalse(SafariEspRules.visible(true, false, false, SafariBiome.FOREST, SafariBiome.FOREST))
        assertFalse(SafariEspRules.visible(true, true, false, SafariBiome.FOREST, null))
    }
    @Test fun `clicker emits twelve clicks per twenty ticks and never carries inactive credit`() {
        val clicker = SafariClickClock()
        assertTrue(clicker.tick(true), "The first held tick must preserve a quick left-click tap")
        clicker.tick(false)
        assertEquals(120, (1..200).count { clicker.tick(true) })
        clicker.tick(false)
        assertTrue(clicker.tick(true))
        assertFalse(clicker.tick(true))
        clicker.tick(false)
        assertTrue(clicker.tick(true))
        for (conditions in listOf(
            ClickConditions(enabled = false), ClickConditions(inSafari = false),
            ClickConditions(mouseHeld = false), ClickConditions(inGame = false),
            ClickConditions(usingItem = true), ClickConditions(breakingBlock = true),
            ClickConditions(targetBlock = true))) assertFalse(conditions.allowed())
        assertTrue(ClickConditions().allowed())
    }
    @Test fun `ESP and auto clicker settings survive saving and existing configuration upgrades`() {
        ConfigManager.init(dir)
        assertTrue(ConfigManager.autoClicker)
        val settings = SafariSettings()
        settings.safari.autoClicker = false
        settings.cavernEsp.enabled = true
        settings.cavernEsp.onlyInBiome = true
        settings.cavernEsp.rockmite.enabled = true
        settings.cavernEsp.rockmite.color = "0:255:255:0:0"
        settings.floorEsp.enabled = true
        settings.apply()
        ConfigManager.init(dir)
        assertFalse(ConfigManager.autoClicker)
        assertTrue(SafariEspConfig.groups.getValue("cavern").enabled)
        assertTrue(SafariEspConfig.groups.getValue("cavern").onlyInBiome)
        assertTrue(SafariEspConfig.mobs.getValue("Rockmite").enabled)
        assertEquals("0:255:255:0:0", SafariEspConfig.mobs.getValue("Rockmite").color)
        assertTrue(SafariEspConfig.groups.getValue("floor").enabled)
    }
}
