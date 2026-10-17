package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShywormCrowdedContactTest {
    private fun target(key:String,x:Double=0.0,species:String="Foxtrot",part:Boolean=false)=ContactTarget(key,1,species,
        ShotBounds(ShotVector(x,0.0,0.0),ShotVector(x+.6,.6,.6)),"armor_stand_body_v1",bodyPart=part)
    @Test fun `finished bounded discovery is distinct from complete target coverage`() {
        val cache=ContactTargetCache();val world=Any()
        val slots=(0..89).map{ContactDiscovery("$it",1)}.iterator()
        cache.tick(world,slots){target(it.key)}
        val (targets,coverage)=cache.snapshot()
        assertEquals(64,targets.size)
        assertFalse(coverage.complete);assertTrue("target_limit" in coverage.reasons)
        assertTrue(cache.sweepFinished,"overflow must not make a completed scan look unfinished")
        cache.invalidate();assertFalse(cache.sweepFinished)
    }
    @Test fun `discovery finishes with unrelated metadata churn while full coverage stays provisional`() {
        val cache=ContactTargetCache();val world=Any()
        val slots=(0..399).map{ContactDiscovery("$it",1)}.iterator()
        cache.tick(world,slots){target(it.key)};assertFalse(cache.sweepFinished)
        cache.metadataChanged()
        cache.tick(world,slots){target(it.key)}
        assertTrue(cache.sweepFinished);assertFalse(cache.snapshot().second.complete)
    }
    @Test fun `support includes a witnessed head outside the retained target slots`() {
        val retained=listOf(target("head",species="Shyworm"),target("piece",1.0,"Shyworm",true))+
            (0..61).map{target("other$it",20.0)}
        val support=ShywormContactParts.support(retained+target("other-head",2.0,"Shyworm"))
        assertFalse("piece" in support,"a head displaced by global retention still establishes ambiguous membership")
    }
    @Test fun `reserved witness budget bounds discovery and reset clears completion`() {
        val cache=ContactTargetCache();val world=Any();var inspections=0
        val slots=(0..499).map{ContactDiscovery("$it",1)}.iterator()
        cache.tick(world,slots,160){inspections++;target(it.key)}
        assertEquals(160,inspections);assertFalse(cache.sweepFinished)
        inspections=0
        cache.tick(world,slots,160){inspections++;target(it.key)}
        assertEquals(160,inspections);assertFalse(cache.sweepFinished)
        cache.tick(world,slots,160){target(it.key)};assertFalse(cache.sweepFinished)
        cache.tick(world,slots,160){target(it.key)};assertFalse(cache.sweepFinished)
        cache.tick(world,slots,160){target(it.key)};assertTrue(cache.sweepFinished)
        cache.reset();assertFalse(cache.sweepFinished)
    }
    @Test fun `over-budget association cannot silently discard connectors`() {
        val graph=listOf(target("head",species="Shyworm"))+(1..64).map{target("piece$it",it*.4,"Shyworm",true)}
        assertTrue(ShywormContactParts.support(graph).isEmpty(),"a truncated association component cannot establish unique ownership")
    }
}
