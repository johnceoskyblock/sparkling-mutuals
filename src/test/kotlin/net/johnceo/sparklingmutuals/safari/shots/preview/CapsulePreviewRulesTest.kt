package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CapsulePreviewRulesTest {
    private fun eligible() = CapsulePreviewState(true,true,true,true,true,false,false,false,false,false,
        false,false,false,false,0.0,ShotVector(1.0,2.0,3.0),ShotVector(0.0,-.0784,0.0))
    private fun ready(): CapsulePreviewRules = CapsulePreviewRules().also { val w=Any(); it.tick(w,eligible());it.tick(w,eligible()) }

    @Test fun `render evaluation cannot create stationary history`() {
        val rules=CapsulePreviewRules();val w=Any()
        repeat(20) { assertEquals(CapsulePreviewAvailability.STAND_STILL,rules.evaluate(eligible())) }
        rules.tick(w,eligible())
        assertEquals(CapsulePreviewAvailability.STAND_STILL,rules.evaluate(eligible()))
        rules.tick(w,eligible())
        assertEquals(CapsulePreviewAvailability.AVAILABLE,rules.evaluate(eligible()))
    }
    @Test fun `pitch endpoints are inclusive and neighboring values unavailable`() {
        for(pitch in listOf(-10.0,1.0)) assertEquals(CapsulePreviewAvailability.AVAILABLE,ready().evaluate(eligible().copy(pitch=pitch)))
        for(pitch in listOf(-10.00001,1.00001,Double.NaN,Double.POSITIVE_INFINITY))
            assertEquals(CapsulePreviewAvailability.TESTED_ANGLE_ONLY,ready().evaluate(eligible().copy(pitch=pitch)))
    }
    @Test fun `current hidden and view restrictions override saved availability`() {
        val s=eligible()
        for(blocked in listOf(s.copy(enabled=false),s.copy(inSafari=false),s.copy(ordinaryMainHand=false),
            s.copy(screenOpen=true),s.copy(hudHidden=true)))
            assertEquals(CapsulePreviewAvailability.HIDDEN,ready().evaluate(blocked))
        assertEquals(CapsulePreviewAvailability.HIDDEN,ready().evaluate(null))
        assertEquals(CapsulePreviewAvailability.VIEW_UNAVAILABLE,ready().evaluate(s.copy(firstPerson=false)))
        for(blocked in listOf(s.copy(hideFlying=true),s.copy(hideGround=true)))
            assertEquals(CapsulePreviewAvailability.HIDING_UNAVAILABLE,ready().evaluate(blocked))
    }
    @Test fun `physical restrictions immediately hide path and invalidate stability`() {
        val s=eligible();val w=Any()
        for(blocked in listOf(s.copy(onGround=false),s.copy(sneaking=true),s.copy(sprinting=true),
            s.copy(swimming=true),s.copy(flying=true),s.copy(riding=true),
            s.copy(velocity=ShotVector(.00101,0.0,0.0)),s.copy(velocity=ShotVector(0.0,0.0,-.00101)),
            s.copy(velocity=ShotVector(.0008,0.0,.0008)),s.copy(velocity=ShotVector(Double.NaN,0.0,0.0)),
            s.copy(position=ShotVector(Double.NaN,2.0,3.0)))) {
            val rules=CapsulePreviewRules();rules.tick(w,s);rules.tick(w,s)
            assertEquals(CapsulePreviewAvailability.STAND_STILL,rules.evaluate(blocked))
            rules.tick(w,blocked);rules.tick(w,s)
            assertEquals(CapsulePreviewAvailability.STAND_STILL,rules.evaluate(s))
            rules.tick(w,s);assertEquals(CapsulePreviewAvailability.AVAILABLE,rules.evaluate(s))
        }
    }
    @Test fun `position drift on each axis suppresses between ticks and requires settling`() {
        for(p in listOf(ShotVector(1.002,2.0,3.0),ShotVector(1.0,2.002,3.0),ShotVector(1.0,2.0,3.002))) {
            val rules=ready()
            assertEquals(CapsulePreviewAvailability.STAND_STILL,rules.evaluate(eligible().copy(position=p)))
        }
        assertEquals(CapsulePreviewAvailability.AVAILABLE,ready().evaluate(eligible().copy(position=ShotVector(1.0009,2.0009,3.0009),velocity=ShotVector(.001,0.0,0.0))))
    }
    @Test fun `world replacement loss and reset require two new observations`() {
        for(reset in listOf<(CapsulePreviewRules)->Unit>({it.tick(Any(),eligible())},{it.tick(null,eligible())},{it.tick(Any(),null)},{it.reset()})) {
            val r=ready();reset(r)
            assertNotEquals(CapsulePreviewAvailability.AVAILABLE,r.evaluate(eligible()))
            val w=Any();r.tick(w,eligible());assertNotEquals(CapsulePreviewAvailability.AVAILABLE,r.evaluate(eligible()))
            r.tick(w,eligible());assertEquals(CapsulePreviewAvailability.AVAILABLE,r.evaluate(eligible()))
        }
    }
    @Test fun `unsupported states cannot bank stable ticks`() {
        val s=eligible();val w=Any()
        for(blocked in listOf(s.copy(enabled=false),s.copy(firstPerson=false),s.copy(hideFlying=true),s.copy(ordinaryMainHand=false),s.copy(pitch=2.0))) {
            val r=CapsulePreviewRules();repeat(5){r.tick(w,blocked)}
            r.tick(w,s);assertEquals(CapsulePreviewAvailability.STAND_STILL,r.evaluate(s))
        }
    }
}
