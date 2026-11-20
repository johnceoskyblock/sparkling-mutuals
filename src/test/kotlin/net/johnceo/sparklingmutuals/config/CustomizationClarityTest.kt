package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CustomizationClarityTest {
    @TempDir lateinit var dir: Path
    @Test fun sharedNameplatesHaveNoPerCritterTemplates() {
        ConfigManager.init(dir)
        val catalog=SettingsCatalog(SafariSettings())
        assertEquals(listOf("Critter Names","Nameplates","Colors","Other ESP"),catalog.customizationTabs)
        val names=catalog.customizationRows(0)
        assertEquals(37,names.drop(1).sumOf {it.children.size})
        assertTrue(names.drop(1).flatMap {it.children}.all {!it.group && it.read() is String})
        val plates=catalog.customizationRows(1)
        assertTrue(plates.first().description.contains("Critter Names"))
        assertEquals(setOf("enabled","display","distance","pity","eagle","colors","critters"),plates.drop(1).map {it.field.name}.toSet())
        val colors=plates.single {it.field.name=="colors"}
        assertTrue(colors.group)
        assertEquals(setOf("color","uniqueColors","rarityColors","entityColorOverride","fixedColor"),colors.children.map {it.field.name}.toSet())
    }
}
