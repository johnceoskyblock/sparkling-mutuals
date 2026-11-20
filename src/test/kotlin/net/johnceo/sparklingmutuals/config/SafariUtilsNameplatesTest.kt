package net.johnceo.sparklingmutuals.config

import java.util.Properties
import java.util.UUID
import net.johnceo.sparklingmutuals.safari.esp.EspCaptureMemory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariUtilsNameplatesTest {
    @Test fun fourDisplayPoliciesAndDisabledMaster() {
        for(mode in 0..3) {
            val option=CritterNameplates.Options(display=mode)
            assertEquals(mode==1 || mode==3,CritterNameplates.showsVanilla(option))
            assertEquals(mode==2 || mode==3,CritterNameplates.showsMarker(option))
        }
        val disabled=CritterNameplates.Options(enabled=false,display=0)
        assertTrue(CritterNameplates.showsVanilla(disabled));assertFalse(CritterNameplates.showsMarker(disabled))
    }
    @Test fun rarityThresholdsAndEagleGuarantees() {
        val base=CritterNameplates.Options()
        assertEquals("",CritterNameplates.pityLabel("Foxtrot",0,base))
        for((name,max) in listOf("Bluebird" to 2,"Scrappy" to 3,"Gemzie" to 5,"Wumpa" to 20))
            assertEquals(" (1/"+max+")",CritterNameplates.pityLabel(name,1,base))
        assertEquals("",CritterNameplates.pityLabel("Bluebird",1,base.copy(eagle=2)))
        assertEquals(" (1/3)",CritterNameplates.pityLabel("Scrappy",1,base.copy(eagle=5)))
        assertEquals("",CritterNameplates.pityLabel("Wumpa",1,base.copy(pity=false)))
        assertEquals("SPARKLING Wumpa (2/20) 42m",CritterNameplates.label("Wumpa",2,41.6,true,base.copy(distance=true)))
    }
    @Test fun identifiedAttemptsDoNotLeakPityToPeers() {
        val memory=EspCaptureMemory();val a=UUID.randomUUID();val b=UUID.randomUUID()
        memory.aimed(a,"Gemzie",100);memory.aimed(a,"Gemzie",101)
        assertEquals(1,memory.pityFor(a));assertEquals(0,memory.pityFor(b))
        assertEquals(a,memory.escaped("Gemzie",200))
        memory.aimed(a,"Gemzie",300);assertEquals(2,memory.pityFor(a))
        memory.escaped("Macaw",400);assertEquals(2,memory.pityFor(a))
        memory.caught("Gemzie",500);assertTrue(memory.hidden(a))
        memory.reset();assertEquals(0,memory.pityFor(a))
    }
    @Test fun exactLabelHandoffsPreservePityButLivePeersDoNotMerge() {
        val memory=EspCaptureMemory();val old=UUID.randomUUID();val next=UUID.randomUUID();val label=UUID.randomUUID()
        memory.bindLabel(old,"Wumpa",label,setOf(old));memory.aimed(old,"Wumpa",100)
        memory.bindLabel(next,"Wumpa",label,setOf(old,next))
        assertEquals(0,memory.pityFor(next));assertEquals(1,memory.pityFor(old))
        memory.bindLabel(old,"Wumpa",label,setOf(old))
        memory.bindLabel(next,"Wumpa",label,setOf(next))
        assertEquals(1,memory.pityFor(next));assertEquals(next,memory.escaped("Wumpa",200))
    }
    @Test fun serverConfirmedRetriesIncreasePityEvenAfterAMissedEscapeLine() {
        val memory = EspCaptureMemory()
        val target = UUID.randomUUID()
        memory.sighted(target, "Gemzie", 100)
        memory.throwing(101)
        memory.threw("Gemzie", 120)
        memory.threw("Gemzie", 121)
        assertEquals(1, memory.pityFor(target))

        memory.sighted(target, "Gemzie", 300)
        memory.throwing(301)
        memory.threw("Gemzie", 320)
        assertEquals(" (2/5)", CritterNameplates.pityLabel("Gemzie", memory.pityFor(target),
            CritterNameplates.Options()))
    }
    @Test fun colorPrecedence() {
        val base=CritterNameplates.Options(uniqueColors=true,rarityColors=true)
        assertEquals(0xFFFF7777.toInt(),CritterNameplates.color("Wumpa",false,0,base))
        assertEquals(0xFF55FF55.toInt(),CritterNameplates.color("Wumpa",true,0,base))
        assertEquals(0xFFFFAA00.toInt(),CritterNameplates.color("Wumpa",false,0,base.copy(uniqueColors=false)))
        assertEquals(123,CritterNameplates.color("Duplico",false,123,base))
        assertEquals(0xFFFF7777.toInt(),CritterNameplates.color("Duplico",false,123,base.copy(entityColorOverride=true)))
        assertEquals(123,CritterNameplates.color("Wumpa",false,123,CritterNameplates.Options()))
        assertEquals(0xFF123456.toInt(),CritterNameplates.color("Wumpa",false,123,
            CritterNameplates.Options(color=AppearanceConfig.color(0x123456),fixedColor=true)))
    }
    @Test fun hideyhoShowsTheEncounterPhaseWithoutCapturePity() {
        val marker = net.johnceo.sparklingmutuals.safari.hideyho.HideyhoMarker()
        val start = net.minecraft.core.BlockPos(0, 0, 0)
        val end = net.minecraft.core.BlockPos(1, 1, 1)
        assertEquals("Hideyho (START)", marker.title(start))
        marker.chat("[MOB] Hideyho: Hehe, you found me!", start)
        assertEquals("Hideyho (START)", marker.title(start))
        marker.chat("[MOB] Hideyho: Hehe, ok!", start)
        assertNull(marker.title(start))
        marker.chat("[MOB] Hideyho: No peeking!", start)
        assertNull(marker.title(start))
        assertEquals("Hideyho (END)", CritterNameplates.label("Hideyho", 0, 0.0, title=marker.title(end)!!))
        marker.chat("[MOB] Hideyho: Aah! You found me!", end)
        assertNull(marker.title(end))
    }
    @Test fun retiringTemplatePropertiesPreservesAliasKeys() {
        val p=Properties();p.setProperty("esp.nameplate.wumpa.text","Old template")
        p.setProperty("appearance.critterName.wumpa","Snow Beast")
        CritterNameplates.load(p);CritterNameplates.save(p)
        assertNull(p.getProperty("esp.nameplate.wumpa.text"))
        assertEquals("Snow Beast",p.getProperty("appearance.critterName.wumpa"))
        assertEquals("2",p.getProperty("esp.nameplates.display"))
    }
}
