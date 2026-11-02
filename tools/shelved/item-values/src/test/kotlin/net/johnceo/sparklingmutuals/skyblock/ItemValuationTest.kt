package net.johnceo.sparklingmutuals.skyblock

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.johnceo.sparklingmutuals.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ItemValuationTest {
    @TempDir lateinit var dir: Path
    private fun json(value: String)=JsonParser.parseString(value).asJsonObject
    private fun engine(vararg quotes: Pair<String,Double>)=ItemValuation(ValueDefinitions.bundled,
        quotes.associate { it.first to MarketQuote(it.second,"Test market") },emptyMap())

    @Test fun `installed upgrades and stack amounts are added exactly once`() {
        val result=engine("HYPERION" to 1000.0,"HOT_POTATO_BOOK" to 10.0,"FUMING_POTATO_BOOK" to 20.0,
            "RECOMBOBULATOR_3000" to 30.0,"IMPLOSION_SCROLL" to 40.0,"ESSENCE_WITHER" to 2.0)
            .estimate(ValueItem("HYPERION","Hyperion",2,json("""{"hot_potato_count":12,"rarity_upgrades":1,"upgrade_level":2,"ability_scroll":["IMPLOSION_SCROLL"]}""")))
        assertEquals(4220.0,result.total) // (1000 + 100 + 40 + 30 + 40 + (150+300)*2) * 2
        assertFalse(result.partial)
        assertEquals(1,result.parts.count { it.product=="IMPLOSION_SCROLL" })
    }
    @Test fun `enchanted books and tier combining do not add a second base item`() {
        val result=engine("ENCHANTMENT_ULTIMATE_CHIMERA_1" to 100.0)
            .estimate(ValueItem("ENCHANTED_BOOK","Enchanted Book",1,json("""{"enchantments":{"ultimate_chimera":5}}""")))
        assertEquals(1600.0,result.total);assertFalse(result.partial)
        assertEquals(1,result.parts.size)
        val unsupported=engine("ENCHANTMENT_SHARPNESS_5" to 10.0)
            .estimate(ValueItem("ENCHANTED_BOOK","Enchanted Book",1,json("""{"enchantments":{"sharpness":7}}""")))
        assertTrue(unsupported.partial,"Unobtainable combinations must not turn missing high-tier quotes into invented prices")
    }
    @Test fun `unknown prices remain partial and Bazaar overrides stale market quotes`() {
        val definitions=ValueDefinitions(JsonObject(),JsonObject(),JsonObject(),JsonObject(),JsonObject())
        val engine=ItemValuation(definitions,mapOf("TEST" to MarketQuote(100.0,"Old market")),mapOf("TEST" to doubleArrayOf(12.0,15.0)))
        val result=engine.estimate(ValueItem("TEST","Test",3,json("""{"rarity_upgrades":1}""")))
        assertEquals(45.0,result.total);assertTrue(result.partial);assertTrue(result.available)
        assertNull(engine.quote("MISSING"))
        assertFalse(engine.estimate(ValueItem("MISSING","Missing")).available)
    }
    @Test fun `pet skin market quote already includes the skin`() {
        val pet=json("""{"petInfo":"{\"type\":\"SHEEP\",\"tier\":\"LEGENDARY\",\"skin\":\"SHEEP_SHLEEPY\",\"heldItem\":\"PET_ITEM_TEXTBOOK\"}"}""")
        val result=engine("LVL_100_LEGENDARY_SHEEP_SKINNED_SHEEP_SHLEEPY" to 2000.0,"PET_SKIN_SHEEP_SHLEEPY" to 500.0,"PET_ITEM_TEXTBOOK" to 10.0)
            .estimate(ValueItem("PET","[Lvl 100] Sheep",1,pet))
        assertEquals(2010.0,result.total);assertFalse(result.partial)
        assertEquals(2,result.parts.size)
    }
    @Test fun `prestiged armor uses base armor and includes earlier tiers rather than counting prestige twice`() {
        val result=engine("CRIMSON_HELMET" to 100.0).estimate(ValueItem("HOT_CRIMSON_HELMET","Hot Crimson Helmet"))
        assertEquals("CRIMSON_HELMET",result.parts.first().product)
        assertEquals(1,result.parts.count { it.label=="hot prestige" && it.product=="SKYBLOCK_COIN" })
        assertTrue(result.parts.any { it.label=="Previous tier stars" })
        assertTrue(result.partial,"Soulbound teeth or other missing materials should remain visible as unpriced")
    }
    @Test fun `storage allowlist excludes shop quest and Hunting Box menus`() {
        listOf("Chest","Large Chest","Ender Chest (2/9)","Jumbo Backpack (Slot #3)","Personal Vault").forEach { assertTrue(StorageValueRules.accepts(it),it) }
        listOf("Hunting Box","Bazaar","Auction House","Critter Safari Entry","Backpack Recipe","Storage").forEach { assertFalse(StorageValueRules.accepts(it),it) }
    }
    @Test fun `market refresh is shared throttled and disk timestamps survive restart`() {
        val window=MarketRefreshWindow()
        assertTrue(window.begin(1000));assertFalse(window.begin(20_000))
        window.finished();assertFalse(window.begin(60_000));assertTrue(window.begin(10_801_000))
        val restored=MarketRefreshWindow();restored.restored(1000)
        assertFalse(restored.begin(60_000));assertTrue(restored.begin(10_801_000))
    }
    @Test fun `both helper toggles persist independently in existing config`() {
        ConfigManager.init(dir);ContestConfig.init(dir)
        val settings=SafariSettings()
        settings.skyblock.helpers.estimatedItemValue=false
        settings.skyblock.helpers.estimatedChestValue=true
        settings.apply()
        ConfigManager.init(dir)
        assertFalse(ConfigManager.estimatedItemValue);assertTrue(ConfigManager.estimatedChestValue)
        val helpers=SettingsCatalog(SafariSettings()).pages.first { it.name=="SkyBlock" }.rows.first { it.name=="Helpers" }
        assertTrue(helpers.children.any { it.name=="Estimated item value" })
        assertTrue(helpers.children.any { it.name=="Estimated chest value" })
    }
}
