package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShotInputCyclesTest {
    @Test fun `fallthrough shares one input with two observed returns and rapid invocations remain distinct`() {
        val inputs=mutableListOf<Triple<ShotInputToken,ShotUseRoute,Boolean>>()
        val returns=mutableListOf<Pair<ShotInputToken,ShotUseResult>>()
        val c=ShotInputCycles({ t,_,_,r,standalone -> inputs.add(Triple(t,r,standalone)) }, { t,r,_ -> returns.add(t to r) })
        c.begin()
        val block=c.observe(ShotHand.MAIN,ShotVariant.ORDINARY,ShotUseRoute.BLOCK)
        c.returned(block,ShotUseResult.PASS)
        val air=c.observe(ShotHand.MAIN,ShotVariant.ORDINARY,ShotUseRoute.AIR)
        c.returned(air,ShotUseResult.SUCCESS)
        assertEquals(block,air); assertEquals(1,inputs.size); assertEquals(2,returns.size)
        c.end();c.begin()
        assertNotEquals(block,c.observe(ShotHand.MAIN,ShotVariant.ORDINARY,ShotUseRoute.AIR))
        assertNotEquals(block,c.observe(ShotHand.OFF,ShotVariant.MASTERFUL,ShotUseRoute.AIR))
    }
    @Test fun `standalone uses are distinct and explicitly marked`() {
        val provenance=mutableListOf<Boolean>()
        val c=ShotInputCycles({ _,_,_,_,standalone -> provenance.add(standalone) })
        assertNotEquals(c.observe(ShotHand.MAIN,ShotVariant.ORDINARY,ShotUseRoute.AIR),c.observe(ShotHand.MAIN,ShotVariant.ORDINARY,ShotUseRoute.AIR))
        assertEquals(listOf(true,true),provenance)
    }
}
