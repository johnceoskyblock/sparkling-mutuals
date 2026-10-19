package net.johnceo.sparklingmutuals.safari.automation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GemzieGemSelectionTest {
    private val orange = GemzieClick(x=-140,y=61,z=46)
    private val empty = GemzieStack(null,"",0)
    private fun hotbar(vararg slots: Pair<Int,GemzieStack>) = MutableList(9) { empty }.apply {
        slots.forEach { (slot,stack) -> this[slot]=stack }
    }
    @Test fun `only the exact support top of each podium supplies its color`() {
        for(gem in GemzieGem.entries) {
            assertEquals(gem,GemzieSelection.target(GemzieClick(x=gem.x,y=gem.y,z=gem.z)))
            assertNull(GemzieSelection.target(GemzieClick(x=gem.x,y=gem.y+1,z=gem.z)))
            assertNull(GemzieSelection.target(GemzieClick(x=gem.x+1,y=gem.y,z=gem.z)))
            assertNull(GemzieSelection.target(GemzieClick(x=gem.x,y=gem.y,z=gem.z,top=false)))
        }
        assertEquals(GemzieGem.ORANGE,GemzieSelection.target(orange))
        assertEquals(GemzieGem.LIME,GemzieSelection.target(GemzieClick(x=-137,y=61,z=51)))
        assertEquals(GemzieGem.PURPLE,GemzieSelection.target(GemzieClick(x=-140,y=61,z=56)))
    }
    @Test fun `disabled and non Safari Cavern contexts retain the ordinary input`() {
        for(context in listOf(orange.copy(enabled=false),orange.copy(safari=false),orange.copy(cavern=false))) {
            assertNull(GemzieSelection.target(context))
            assertFalse(GemzieUseAction().handle(context,0,hotbar(),true,
                { fail("slot changed") },{ fail("item used") },{ fail("notice emitted") }))
        }
    }
    @Test fun `custom identity wins and conflicting IDs cannot use the name fallback`() {
        assertTrue(GemzieSelection.matches(GemzieStack("ORANGE_GEM","Lime Gem"),GemzieGem.ORANGE))
        for(id in listOf("LIME_GEM","CRITTER_CAPSULE","ORANGE_GEM_SHARD","", "orange_gem"))
            assertFalse(GemzieSelection.matches(GemzieStack(id,"Orange Gem"),GemzieGem.ORANGE),id)
    }
    @Test fun `identity absent permits only exact formatting normalized whole gem names`() {
        assertTrue(GemzieSelection.matches(GemzieStack(null,"§6Orange Gem"),GemzieGem.ORANGE))
        for(name in listOf("Orange Gem Shard","Orange Gemstone","Not Orange Gem","orange gem","Lime Gem"))
            assertFalse(GemzieSelection.matches(GemzieStack(null,name),GemzieGem.ORANGE),name)
        assertFalse(GemzieSelection.matches(GemzieStack("ORANGE_GEM","Orange Gem",0),GemzieGem.ORANGE))
    }
    @Test fun `current matching slot wins over earlier duplicates`() {
        val gem=GemzieStack("ORANGE_GEM","Orange Gem")
        assertEquals(6,GemzieSelection.slot(GemzieGem.ORANGE,6,hotbar(1 to gem,6 to gem)))
    }
    @Test fun `first matching hotbar slot is deterministic when current item differs`() {
        val gem=GemzieStack("ORANGE_GEM","Orange Gem")
        assertEquals(1,GemzieSelection.slot(GemzieGem.ORANGE,0,hotbar(6 to gem,1 to gem)))
    }
    @Test fun `exhausted stacks and inventory slots beyond the hotbar are ignored`() {
        val stacks=hotbar(2 to GemzieStack("ORANGE_GEM","Orange Gem",0))+
            GemzieStack("ORANGE_GEM","Orange Gem")
        assertNull(GemzieSelection.slot(GemzieGem.ORANGE,9,stacks))
    }
    @Test fun `fresh click selects before exactly one use and does not restore the slot`() {
        var selected=0; val events=mutableListOf<String>()
        assertTrue(GemzieUseAction().handle(orange,selected,hotbar(4 to GemzieStack("ORANGE_GEM","Orange Gem")),true,
            { selected=it;events.add("select:$it") },{ events.add("use:$selected") },{ fail("missing gem") }))
        assertEquals(listOf("select:4","use:4"),events)
        assertEquals(4,selected)
    }
    @Test fun `selected matching gem dispatches once without a redundant selection`() {
        var used=0
        assertTrue(GemzieUseAction().handle(orange,3,hotbar(3 to GemzieStack("ORANGE_GEM","Orange Gem")),true,
            { fail("selection already correct") },{ used++ },{ fail("missing gem") }))
        assertEquals(1,used)
    }
    @Test fun `missing or nonhotbar gems consume the supported click without using another item`() {
        var missing:GemzieGem?=null
        assertTrue(GemzieUseAction().handle(orange,0,hotbar(),true,
            { fail("slot changed") },{ fail("wrong item used") },{ missing=it }))
        assertEquals(GemzieGem.ORANGE,missing)
    }
    @Test fun `held repeat cannot select use or emit feedback on a supported podium`() {
        assertTrue(GemzieUseAction().handle(orange,0,hotbar(4 to GemzieStack("ORANGE_GEM","Orange Gem")),false,
            { fail("held selection") },{ fail("held use") },{ fail("held notice") }))
        assertFalse(GemzieUseAction().handle(orange.copy(top=false),0,hotbar(),false,
            { fail("slot changed") },{ fail("item used") },{ fail("notice") }))
    }
    @Test fun `reentrant callback cannot duplicate the use and later fresh clicks still work`() {
        val action=GemzieUseAction();var used=0
        val stacks=hotbar(0 to GemzieStack("ORANGE_GEM","Orange Gem"))
        assertTrue(action.handle(orange,0,stacks,true,{}, {
            used++
            assertTrue(action.handle(orange,0,stacks,true,{}, { fail("reentrant use") },{}))
        },{}))
        assertTrue(action.handle(orange,0,stacks,true,{}, { used++ },{}))
        assertEquals(2,used)
    }
}
