package net.johnceo.sparklingmutuals.config
import java.util.Properties
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import net.johnceo.sparklingmutuals.safari.resources.SafariFloorDrops
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class FullEspControlsTest {
 @Test fun `species styles persist independently and standard ignores them`() {
  val p = Properties().apply {
   setProperty("esp.foxtrot.glow", "false")
   setProperty("esp.foxtrot.box", "true")
  }
  SafariEspConfig.load(p,true)
  assertFalse(SafariEspConfig.showCritterGlow("Foxtrot"));assertTrue(SafariEspConfig.showCritterBox("Foxtrot"))
  assertTrue(SafariEspConfig.showCritterGlow("Bluebird"));assertFalse(SafariEspConfig.showCritterBox("Bluebird"))
  SafariEspConfig.save(p,true);assertEquals("false",p.getProperty("esp.foxtrot.glow"))
  SafariEspConfig.load(p,false)
  assertTrue(SafariEspConfig.showCritterGlow("Foxtrot"));assertFalse(SafariEspConfig.showCritterBox("Foxtrot"))
 }
 @Test fun `legacy global switches migrate once without changing Standard settings`() {
  val p = Properties().apply {
   setProperty("esp.critterGlow", "false")
   setProperty("esp.critterBoxes", "true")
   setProperty("esp.foxtrot.box", "false")
   setProperty("standard.esp.critterGlow", "true")
  }
  assertTrue(SafariEspConfig.migrateCritterStyles(p))
  assertFalse(SafariEspConfig.migrateCritterStyles(p))
  SafariEspConfig.load(p, true)
  assertFalse(SafariEspConfig.showCritterGlow("Bluebird"))
  assertTrue(SafariEspConfig.showCritterBox("Bluebird"))
  assertFalse(SafariEspConfig.showCritterBox("Foxtrot"))
  SafariEspConfig.glowToggles["Foxtrot"] = true
  SafariEspConfig.save(p, true)
  SafariEspConfig.load(p, true)
  assertTrue(SafariEspConfig.showCritterGlow("Foxtrot"))
  SafariEspConfig.load(p, false)
  assertTrue(SafariEspConfig.standardCritterGlow)
  assertFalse(SafariEspConfig.standardCritterBoxes)
 }
 @Test fun `full catalog exposes each style separately and overrides in unique mode`() {
  org.junit.jupiter.api.Assumptions.assumeFalse(SafariSettings.standardEdition)
  val catalog=SettingsCatalog(SafariSettings())
  assertNotNull(catalog.locate(SettingsTarget("ESP General",listOf("glow","Foxtrot"))))
  assertNotNull(catalog.locate(SettingsTarget("ESP General",listOf("boxes","Foxtrot"))))
  assertNull(catalog.locate(SettingsTarget("ESP General",listOf("glow","enabled"))))
  assertNull(catalog.locate(SettingsTarget("ESP General",listOf("boxes","enabled"))))
  assertNull(catalog.locate(SettingsTarget("ESP General", listOf("moundAlwaysOn"))))
  assertNotNull(catalog.locate(SettingsTarget("Always On Toggle", listOf("rockmite"))))
  assertNotNull(catalog.locate(SettingsTarget("Modes",listOf("unique","remainingChecks"))))
 }
 @Test fun `known unpunched nests override remote full clear completion only in Full`() {
  val mode=net.johnceo.sparklingmutuals.safari.SafariMode.FULL_CLEAR
  assertTrue(net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules.showKnownNestMarkers(mode,false,true,false))
  assertFalse(net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules.showKnownNestMarkers(mode,false,false,false))
  assertFalse(net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules.showKnownNestMarkers(mode,false,true,true))
 }
 @Test fun `remaining checks default off`() {
  SafariEspConfig.load(Properties(), true)
  assertFalse(SafariEspConfig.uniqueRemainingChecks)
 }

 @Test fun `linked markers follow the owner's glow and box choices`() {
  val sources = mapOf(FloorDropType.WORM to "parakeet", FloorDropType.BERRY to "bluebird", FloorDropType.SEEDS to "macaw",
      FloorDropType.INCENSE to "doomspiral", FloorDropType.COIN to "gimmiegold", FloorDropType.LIME to "gemzie",
      FloorDropType.PURPLE to "gemzie", FloorDropType.ORANGE to "gemzie")
  val old = Properties().also { SafariEspConfig.save(it, !SafariSettings.standardEdition) }
  try {
   for ((type, owner) in sources) {
    val props = Properties().apply {
     setProperty("esp.$owner.glow", "false")
     setProperty("esp.$owner.box", "false")
    }
    SafariEspConfig.load(props, true)
    assertFalse(SafariFloorDrops.sourceHighlightEnabled(type), type.label)
    props.setProperty("esp.$owner.box", "true")
    SafariEspConfig.load(props, true)
    assertTrue(SafariFloorDrops.sourceHighlightEnabled(type), type.label)
    props.setProperty("esp.$owner.glow", "true")
    props.setProperty("esp.$owner.box", "false")
    SafariEspConfig.load(props, true)
    assertTrue(SafariFloorDrops.sourceHighlightEnabled(type), type.label)
   }
  } finally { SafariEspConfig.load(old, !SafariSettings.standardEdition) }
 }
}
