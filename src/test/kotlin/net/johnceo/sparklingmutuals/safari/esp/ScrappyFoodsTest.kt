package net.johnceo.sparklingmutuals.safari.esp

import java.util.UUID
import java.util.Properties
import net.johnceo.sparklingmutuals.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScrappyFoodsTest {
    @Test fun `food ordering waits for three and survives partial sightings and captures`() {
        val foods = ScrappyFoods()
        val wheat = UUID.randomUUID(); val fish = UUID.randomUUID(); val lily = UUID.randomUUID()
        foods.observe(listOf(lily to 121783, wheat to 121777))
        assertNull(foods.food(wheat))
        foods.observe(listOf(fish to 121780))
        assertEquals(ScrappyFood.WHEAT, foods.food(wheat))
        assertEquals(ScrappyFood.FISH, foods.food(fish))
        assertEquals(ScrappyFood.LILY_PAD, foods.food(lily))
        foods.observe(listOf(lily to 121783))
        val extra = UUID.randomUUID()
        foods.observe(listOf(extra to 121790))
        assertNull(foods.food(extra))
        assertEquals(ScrappyFood.FISH, foods.food(fish))
        foods.reset()
        assertNull(foods.food(wheat))
        foods.observe(listOf(lily to 7, fish to 8, wheat to 9))
        assertEquals(ScrappyFood.WHEAT, foods.food(lily))
        assertEquals(ScrappyFood.LILY_PAD, foods.food(wheat))
    }
    @Test fun `independent food colors and shared label size round trip through customization`() {
        val before = Properties().also(SafariEspConfig::save)
        try {
            SafariEspConfig.load(Properties())
            assertEquals(0xFFFFFF00.toInt(), SafariEspConfig.rgb(SafariEspConfig.scrappyFoodColors.getValue(ScrappyFood.WHEAT)))
            assertEquals(0xFFFF0000.toInt(), SafariEspConfig.rgb(SafariEspConfig.scrappyFoodColors.getValue(ScrappyFood.FISH)))
            assertEquals(0xFF00FF00.toInt(), SafariEspConfig.rgb(SafariEspConfig.scrappyFoodColors.getValue(ScrappyFood.LILY_PAD)))
            val settings = CustomizationSettings()
            settings.selectedHex = "#123456"
            val group = settings.cavern
            group.scrappy.wheat.setSelected()
            group.scrappy.textScale = 3f
            assertTrue(settings.apply())
            val saved = Properties().also(SafariEspConfig::save)
            SafariEspConfig.load(saved)
            assertEquals(0xFF123456.toInt(), SafariEspConfig.rgb(SafariEspConfig.scrappyFoodColors.getValue(ScrappyFood.WHEAT)))
            assertEquals(0xFFFF0000.toInt(), SafariEspConfig.rgb(SafariEspConfig.scrappyFoodColors.getValue(ScrappyFood.FISH)))
            assertEquals(3f, SafariEspConfig.scrappyTextScale)
            SafariEspConfig.scrappyTextScale = Float.NaN
            assertTrue(SafariEspConfig.scrappyTextScale.isFinite())
        } finally { SafariEspConfig.load(before) }
    }
}
