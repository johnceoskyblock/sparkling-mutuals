package net.johnceo.sparklingmutuals.skyblock

import net.johnceo.sparklingmutuals.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SkillOverflowTest {
    @TempDir lateinit var dir: Path
    @Test fun `overflow follows the normal curve then the doubling slope above sixty`() {
        val sixty=111_672_425L
        assertEquals(OverflowLevel(60,0,7_600_000,0),SkillOverflowMath.calculate(sixty,60))
        assertEquals(OverflowLevel(61,1,8_200_000,7_600_001),SkillOverflowMath.calculate(sixty+7_600_001,60))
        assertEquals(OverflowLevel(70,0,13_600_000,103_000_000),SkillOverflowMath.calculate(sixty+103_000_000,60))
        assertEquals(14_800_000,SkillOverflowMath.calculate(sixty+116_600_000,60).required)
        assertEquals(51,SkillOverflowMath.calculate(59_472_425,50).level)
        assertEquals(4_300_000,SkillOverflowMath.calculate(59_472_425,50).overflow)
        assertTrue(SkillOverflowMath.calculate(Long.MAX_VALUE,60).required>0)
    }
    @Test fun `maxed menu parsing preserves Roman or decimal levels and requires actual XP`() {
        val lines=listOf("Combat LX","Description","Max Skill level reached!","                    120,000,000","Click to view!")
        val plan=OverflowTooltipParsing.plan("Combat LX",lines)!!
        assertEquals("Combat LXI",plan.title);assertEquals(727_575,plan.value.current)
        assertEquals(8_327_575,plan.value.overflow)
        assertEquals(2,plan.progressIndex);assertEquals(3,plan.xpIndex)
        assertEquals("Combat 61",OverflowTooltipParsing.plan("Combat 60",lines)!!.title)
        val farming=OverflowTooltipParsing.plan("Farming L",listOf("Max Skill level reached!","55,172,425"))!!
        assertEquals(50,farming.value.level);assertEquals(0,farming.value.overflow)
        assertNull(OverflowTooltipParsing.plan("Combat LX",listOf("Progress to Level LX: 40%","10,000,000/7,000,000")))
        assertNull(OverflowTooltipParsing.plan("Combat LX",listOf("Max Skill level reached!","Click to view!")))
        assertNull(OverflowTooltipParsing.plan("Social XXV",listOf("Max Skill level reached!","1,000,000")))
        assertNull(OverflowTooltipParsing.plan("Combat LX",lines+"OVERFLOW XP: 8,327,575"))
    }
    @Test fun `abbreviated XP and malformed values are handled safely`() {
        assertEquals(120_000_000,SkillOverflowMath.number("120m"))
        assertEquals(1_500_000_000,SkillOverflowMath.number("1.5B"))
        assertNull(SkillOverflowMath.number("-1"));assertNull(SkillOverflowMath.number("NaN"))
        assertNull(SkillOverflowMath.number("99999999999999999999999999999"))
        assertNull(SkillOverflowMath.menuLevel("IL"));assertEquals(57,SkillOverflowMath.menuLevel("LVII"))
    }
    @Test fun `helper setting persists without changing other toggles`() {
        ConfigManager.init(dir);ContestConfig.init(dir)
        val settings=SafariSettings();assertTrue(settings.skyblock.helpers.skillOverflow)
        settings.skyblock.helpers.skillOverflow=false;settings.apply();ConfigManager.init(dir)
        assertFalse(ConfigManager.skillOverflow)
        assertTrue(SettingsCatalog(SafariSettings()).pages.first { it.name=="SkyBlock" }.rows.first { it.name=="Helpers" }
            .children.any { it.name=="Skills overflow" })
    }
}
