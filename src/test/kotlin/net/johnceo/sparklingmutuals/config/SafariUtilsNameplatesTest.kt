package net.johnceo.sparklingmutuals.config

import java.util.Properties
import java.util.UUID
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.esp.EspCaptureMemory
import net.johnceo.sparklingmutuals.safari.esp.EspPityBody
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.minecraft.world.phys.Vec3
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
        val base=CritterNameplates.Options(pity=true)
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
        val staleAim = UUID.randomUUID()
        memory.sighted(staleAim, "Gemzie", 100)
        memory.throwingAt(101, target, "Gemzie")
        memory.threw("Gemzie", 120)
        memory.threw("Gemzie", 121)
        assertEquals(1, memory.pityFor(target))
        assertEquals(0, memory.pityFor(staleAim))

        memory.throwingAt(301, target, "Gemzie")
        memory.threw("Gemzie", 320)
        assertEquals(" (2/5)", CritterNameplates.pityLabel("Gemzie", memory.pityFor(target),
            CritterNameplates.Options(pity=true)))
    }
    @Test fun `Hideon pity survives new body and nametag IDs after each escape`() {
        for (species in listOf("Hideonfloor", "Hideonwall")) {
            val memory = EspCaptureMemory()
            fun body(id: Long, x: Double, named: Boolean = true) = EspPityBody(UUID(0, id), species, Vec3(x, 65.0, 0.0), named)
            val neighbor = body(10, 1.0)
            val first = body(1, 0.0)
            memory.observeBodies(listOf(first, neighbor), 100)
            memory.throwingAt(1000, first.id, species)
            memory.threw(species, 1050)
            val second = body(2, .15)
            // A new named model can arrive before the chat outcome. The old unnamed model may still be loaded.
            memory.observeBodies(listOf(first.copy(named = false), second, neighbor), 3500)
            assertEquals(first.id, memory.escaped(species, 3600))
            memory.observeBodies(listOf(first.copy(named = false), second, neighbor), 3750)
            assertEquals(" (1/3)", CritterNameplates.pityLabel(species, memory.pityFor(second.id), CritterNameplates.Options(pity=true)))
            assertEquals(0, memory.pityFor(first.id))
            assertEquals(0, memory.pityFor(neighbor.id))

            memory.throwingAt(4000, second.id, species)
            memory.threw(species, 4050)
            assertEquals(second.id, memory.escaped(species, 6600))
            val third = body(3, .4)
            memory.observeBodies(listOf(third, neighbor), 10_000)
            assertEquals(2, memory.pityFor(third.id))
            assertEquals(0, memory.pityFor(neighbor.id))
        }
    }
    @Test fun `two possible return bodies cannot inherit escaped pity`() {
        val memory = EspCaptureMemory()
        val initial = EspPityBody(UUID(0, 1), "Hideonfloor", Vec3.ZERO, named = true)
        memory.observeBodies(listOf(initial), 100)
        memory.throwingAt(1000, initial.id, initial.species)
        memory.threw(initial.species, 1050)
        memory.escaped(initial.species, 2000)
        val candidates = listOf(initial.copy(id = UUID(0, 2)), initial.copy(id = UUID(0, 3), position = Vec3(.5, 0.0, 0.0)))
        memory.observeBodies(candidates, 2250)
        assertTrue(candidates.all { memory.pityFor(it.id) == 0 })
        assertEquals(1, memory.pityFor(initial.id))
        memory.reset()
        memory.observeBodies(candidates.take(1), 2500)
        assertEquals(0, memory.pityFor(candidates.first().id))
    }
    @Test fun `a Macaw that inherits nine attempts cannot also inherit a nearby Macaw's escape`() {
        val memory = EspCaptureMemory()
        val first = EspPityBody(UUID(0, 1), "Macaw", Vec3.ZERO, named = true)
        val second = first.copy(id = UUID(0, 2), position = Vec3(1.1, 0.0, 0.0))
        memory.observeBodies(listOf(first, second), 100)

        repeat(9) { attempt ->
            val at = 200L + attempt * 100
            memory.throwingAt(at, first.id, first.species)
            memory.threw(first.species, at + 20)
        }
        memory.escaped(first.species, 1200)
        memory.throwingAt(1300, second.id, second.species)
        memory.threw(second.species, 1350)
        val firstReturn = first.copy(id = UUID(0, 3), position = Vec3(.15, 0.0, 0.0))
        memory.observeBodies(listOf(firstReturn, second.copy(named = false, capturing = true)), 1400)
        assertEquals(9, memory.pityFor(firstReturn.id))

        memory.escaped(second.species, 1700)
        memory.observeBodies(listOf(firstReturn), 1750)
        assertEquals(9, memory.pityFor(firstReturn.id))
        assertEquals(1, memory.pityFor(second.id))

        val secondReturn = second.copy(id = UUID(0, 4), position = Vec3(1.2, 0.0, 0.0))
        memory.observeBodies(listOf(firstReturn, secondReturn), 1800)
        assertEquals(9, memory.pityFor(firstReturn.id))
        assertEquals(1, memory.pityFor(secondReturn.id))
    }

    @Test fun `a delayed label on an existing neighbor is not evidence of a replacement`() {
        val memory = EspCaptureMemory()
        val first = EspPityBody(UUID(0, 1), "Macaw", Vec3.ZERO, named = true)
        val neighbor = first.copy(id = UUID(0, 2), position = Vec3(.5, 0.0, 0.0), named = false)
        val label = UUID(0, 10)
        memory.observeBodies(listOf(first, neighbor), 100)
        memory.bindLabel(first.id, first.species, label, setOf(first.id, neighbor.id))
        memory.throwingAt(200, first.id, first.species)
        memory.threw(first.species, 250)
        memory.escaped(first.species, 900)
        memory.bindLabel(neighbor.id, neighbor.species, label, setOf(neighbor.id))
        memory.observeBodies(listOf(neighbor.copy(named = true)), 950)
        assertEquals(1, memory.pityFor(first.id))
        assertEquals(0, memory.pityFor(neighbor.id))
    }
    @Test fun `every capsule pity species retains a confirmed attempt after its model is replaced`() {
        val options = CritterNameplates.Options(pity = true)
        for (critter in SafariRoster.all.filter { it.name != "Hideyho" && CritterNameplates.threshold(it.name) > 1 }) {
            val memory = EspCaptureMemory()
            val species = critter.name
            val original = EspPityBody(UUID(0, 1), species, Vec3.ZERO, named = true)
            val replacement = original.copy(id = UUID(0, 2), position = Vec3(.25, 0.0, 0.0))
            memory.observeBodies(listOf(original), 100)
            memory.throwingAt(200, original.id, species)
            memory.threw(species, 350)
            assertEquals(original.id, memory.escaped(species, 2700), species)
            memory.observeBodies(listOf(replacement), 2750)
            assertEquals(1, memory.pityFor(replacement.id), species)
            assertFalse(CritterNameplates.label(species, 1, 1.0, option = options, showPity = false).contains("/"), species)
        }
    }
    @Test fun `formatted capture markers suppress pity only near the live capsule`() {
        assertTrue(SafariEspRules.isCapturingLabel("§e§l CAPTURING"))
        assertFalse(SafariEspRules.isCapturingLabel("Rockmite"))
        assertTrue(SafariEspRules.nearCaptureMarker(.15, 1.6, .2))
        assertFalse(SafariEspRules.nearCaptureMarker(.9, 1.6, 0.0))
        assertFalse(SafariEspRules.nearCaptureMarker(0.0, 2.6, 0.0))
    }
    @Test fun missingNameplateSettingsUseEspColorAndHidePity() {
        val defaults = CritterNameplates.Options()
        assertTrue(defaults.enabled)
        assertTrue(CritterNameplates.showsMarker(defaults))
        assertFalse(CritterNameplates.showsVanilla(defaults))
        assertEquals("", CritterNameplates.pityLabel("Troodon", 1, defaults))
        assertEquals(0xFF76BA42.toInt(), CritterNameplates.color("Troodon", false, 0xFF76BA42.toInt(), defaults))
    }
    @Test fun `an escape can confirm one frozen local aim and a dodge cannot`() {
        val memory = EspCaptureMemory()
        val target = UUID(0, 1)
        val escape = SafariEspRules.capsuleEscape("The Hideonfloor escaped your Critter Capsule!")!!
        val dodge = SafariEspRules.capsuleEscape("The Chuckwalla dodged your critter capsule and started running away!")!!
        assertFalse(dodge.brokeOut)
        memory.throwingAt(100, target, dodge.species)
        assertNull(memory.escaped(dodge.species, 150, dodge.brokeOut))
        assertEquals(0, memory.pityFor(target))
        memory.throwingAt(200, target, escape.species)
        assertEquals(target, memory.escaped(escape.species, 3700, escape.brokeOut))
        assertNull(memory.escaped(escape.species, 3750, escape.brokeOut))
        assertEquals(1, memory.pityFor(target))
    }
    @Test fun `confirmed throws without launch aim attach to the unique capture animation`() {
        val memory = EspCaptureMemory()
        val first = EspPityBody(UUID(0, 1), "Hideonfloor", Vec3.ZERO, named = true)
        val neighbor = first.copy(id = UUID(0, 2), position = Vec3(2.0, 0.0, 0.0))
        memory.observeBodies(listOf(first, neighbor), 100)
        memory.threw(first.species, 200)
        assertEquals(0, memory.pityFor(first.id))
        memory.observeBodies(listOf(first.copy(named = false, capturing = true), neighbor), 350)
        assertEquals(1, memory.pityFor(first.id))
        assertEquals(0, memory.pityFor(neighbor.id))

        memory.escaped(first.species, 2000)
        val returned = first.copy(id = UUID(0, 3))
        memory.observeBodies(listOf(returned, neighbor), 2150)
        assertEquals(1, memory.pityFor(returned.id))

        memory.threw(returned.species, 2300)
        assertNull(memory.caught(returned.species, 2400))
        assertEquals(listOf(returned.species to returned.id),
            memory.observeBodies(listOf(returned.copy(capturing = true), neighbor), 2450))
        assertTrue(memory.hidden(returned.id))
        assertFalse(memory.hidden(neighbor.id))
    }

    @Test fun `deferred attempts cannot claim concurrent nearby animations of the same species`() {
        val memory = EspCaptureMemory()
        val first = EspPityBody(UUID(0, 1), "Troodon", Vec3.ZERO, named = false, capturing = true)
        val second = first.copy(id = UUID(0, 2))
        memory.threw(first.species, 200)
        assertTrue(memory.observeBodies(listOf(first, second), 250).isEmpty())
        assertEquals(0, memory.pityFor(first.id))
        assertEquals(0, memory.pityFor(second.id))
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
        fun dialogue(line: String, position: net.minecraft.core.BlockPos) {
            net.johnceo.sparklingmutuals.safari.SafariMessages.lines(line).forEach { marker.chat(it, position) }
        }
        dialogue("Party > Friend: [MOB] Hideyho: No peeking!", start)
        assertEquals("Hideyho (START)", marker.title(start))
        dialogue("[MOB] Hideyho: §fHehe, ok!", start)
        assertNull(marker.title(start))
        dialogue("[MOB] Hideyho: §fNo peeking!", start)
        assertNull(marker.title(start))
        assertEquals("Hideyho (END)", CritterNameplates.label("Hideyho", 0, 0.0, title=marker.title(end)!!))
        dialogue("[MOB] Hideyho: §fAah! You found me!", end)
        assertNull(marker.title(end))
    }
    @Test fun retiringTemplatePropertiesPreservesAliasKeys() {
        val p=Properties();p.setProperty("esp.nameplate.wumpa.text","Old template")
        p.setProperty("appearance.critterName.wumpa","Snow Beast")
        CritterNameplates.load(p);CritterNameplates.save(p)
        assertNull(p.getProperty("esp.nameplate.wumpa.text"))
        assertEquals("Snow Beast",p.getProperty("appearance.critterName.wumpa"))
        assertEquals("2",p.getProperty("esp.nameplates.display"))
        assertEquals("false",p.getProperty("esp.nameplates.pity"))
    }
}
