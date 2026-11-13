package net.johnceo.sparklingmutuals.safari.esp

import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.completion.ScannableCritterChecks
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScannableCritterChecksTest {
    @Test fun `owned ordinary critters get optional ESP and checklist entries only until qualified checks finish`() {
        val oldToggle=ConfigManager.allScannableCritterChecks
        val oldMode=ConfigManager.sparklingMode
        val oldFull=ConfigManager.fullClearMode
        try {
            ConfigManager.sparklingMode=true
            ConfigManager.fullClearMode=false
            val party=PartySparklingState().apply {
                select(setOf("me"));accept(setOf("me"),mapOf("me" to SafariRoster.all.map { it.name }.toSet()))
            }
            val run=SafariRun(0).apply { visitBiome(SafariBiome.FOREST) }
            fun names()=SafariPanels.missingCritters(run,SafariBiome.FOREST,party).map { it.name }
            ConfigManager.allScannableCritterChecks=false
            assertTrue(names().isEmpty())
            ConfigManager.allScannableCritterChecks=true
            assertTrue("Foxtrot" in names())
            assertTrue("Foxtrot" in SafariPanels.missing(run,SafariBiome.FOREST,false,0,party=party).rows.map { it.label })
            for(name in listOf("Honeybug","Rockmite","Snoozle","Gemzie","Gazer","Gimmiegold","Wumpa","Doomspiral","Bluebird","Parakeet","Macaw"))
                assertFalse(ScannableCritterChecks.enabled(name),name)
            assertTrue(SafariEspRules.neededForSparkling("Foxtrot",false,party,false,run))
            run.sparklingChecks.scan(SafariBiome.FOREST,3.0,47.0,List(6) { "Foxtrot" to UUID.randomUUID() })
            assertFalse("Foxtrot" in names())
            assertFalse(SafariEspRules.neededForSparkling("Foxtrot",false,party,false,run))
            assertFalse(party.needs("Foxtrot"),"discovery data must not be changed by a visual checklist")
            ConfigManager.sparklingMode=false
            assertFalse(ScannableCritterChecks.enabled("Treefrog"))
            ConfigManager.fullClearMode=true
            assertFalse(ScannableCritterChecks.enabled("Treefrog"))
        } finally {
            ConfigManager.allScannableCritterChecks=oldToggle
            ConfigManager.sparklingMode=oldMode
            ConfigManager.fullClearMode=oldFull
        }
    }
}
