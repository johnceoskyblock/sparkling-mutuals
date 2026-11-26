package net.johnceo.sparklingmutuals.config
import java.util.Properties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class FullEspControlsTest {
 @Test fun `species styles persist independently and standard ignores them`() {
  val p=Properties().apply {setProperty("esp.foxtrot.glow","false");setProperty("esp.foxtrot.box","false");setProperty("esp.critterBoxes","true")}
  SafariEspConfig.load(p,true)
  assertFalse(SafariEspConfig.showCritterGlow("Foxtrot"));assertFalse(SafariEspConfig.showCritterBox("Foxtrot"))
  assertTrue(SafariEspConfig.showCritterGlow("Bluebird"));assertTrue(SafariEspConfig.showCritterBox("Bluebird"))
  SafariEspConfig.save(p,true);assertEquals("false",p.getProperty("esp.foxtrot.glow"))
  SafariEspConfig.load(p,false);assertTrue(SafariEspConfig.showCritterGlow("Foxtrot"))
 }
 @Test fun `full catalog exposes each style separately and overrides in unique mode`() {
  org.junit.jupiter.api.Assumptions.assumeFalse(SafariSettings.standardEdition)
  val catalog=SettingsCatalog(SafariSettings())
  assertNotNull(catalog.locate(SettingsTarget("ESP General",listOf("glow","Foxtrot"))))
  assertNotNull(catalog.locate(SettingsTarget("ESP General",listOf("boxes","Foxtrot"))))
  assertNotNull(catalog.locate(SettingsTarget("Modes",listOf("unique","remainingChecks"))))
 }
 @Test fun `known unpunched nests override remote full clear completion only in Full`() {
  val mode=net.johnceo.sparklingmutuals.safari.SafariMode.FULL_CLEAR
  assertTrue(net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules.showKnownNestMarkers(mode,false,true,false))
  assertFalse(net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules.showKnownNestMarkers(mode,false,false,false))
  assertFalse(net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules.showKnownNestMarkers(mode,false,true,true))
 }
 @Test fun `overrides default off`() { SafariEspConfig.load(Properties(),true);assertFalse(SafariEspConfig.moundAlwaysOn);assertFalse(SafariEspConfig.uniqueRemainingChecks) }
}
