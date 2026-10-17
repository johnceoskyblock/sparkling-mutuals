package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.safari.shots.ShotVariant
import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CapsulePreviewRulesTest {
    @Test fun `preview accepts third person in both narrow and expanded modes`() {
        for(expanded in listOf(false,true)) {
            val s=eligible().copy(firstPerson=false,allowUncalibratedEstimates=expanded)
            val r=CapsulePreviewRules();val w=Any();r.tick(w,s);r.tick(w,s)
            assertEquals(CapsulePreviewAvailability.AVAILABLE,r.evaluate(s))
        }
    }
    @Test fun `third person moving Masterful display does not relax recorder view restriction`() {
        val s=eligible().copy(firstPerson=false,allowUncalibratedEstimates=true,capsule=ShotVariant.MASTERFUL,
            onGround=false,velocity=ShotVector(.2,.3,.1))
        assertEquals(CapsulePreviewAvailability.AVAILABLE,CapsulePreviewRules().tick(Any(),s))
        val strict=CapsulePreviewRules(allowExtendedPreview=false);val w=Any();val ordinary=eligible().copy(allowUncalibratedEstimates=true)
        strict.tick(w,ordinary);strict.tick(w,ordinary)
        assertEquals(CapsulePreviewAvailability.VIEW_UNAVAILABLE,strict.evaluate(ordinary.copy(firstPerson=false)))
        assertEquals(CapsulePreviewAvailability.STAND_STILL,strict.tick(w,ordinary))
        assertEquals(CapsulePreviewAvailability.AVAILABLE,strict.tick(w,ordinary))
    }
    @Test fun `expanded preview remains available across ground and airborne movement`() {
        val s=eligible().copy(allowUncalibratedEstimates=true)
        val r=CapsulePreviewRules();val w=Any();r.tick(w,s)
        for(moving in listOf(s.copy(velocity=ShotVector(.2,0.0,0.0)),s.copy(sprinting=true),
            s.copy(sneaking=true),s.copy(onGround=false,velocity=ShotVector(0.0,.4,.1)),
            s.copy(onGround=false,velocity=ShotVector(.1,-.6,0.0)))) {
            assertEquals(CapsulePreviewAvailability.AVAILABLE,r.evaluate(moving))
            assertEquals(CapsulePreviewAvailability.AVAILABLE,r.tick(w,moving.copy(position=ShotVector(2.0,3.0,4.0))))
        }
    }
    @Test fun `expanded angles require opt in and still reject flight`() {
        for(pitch in listOf(-90.0,35.0,90.0)) {
            val s=eligible().copy(pitch=pitch,allowUncalibratedEstimates=true);val r=CapsulePreviewRules();val w=Any()
            assertEquals(CapsulePreviewAvailability.AVAILABLE,r.tick(w,s))
            assertEquals(CapsulePreviewAvailability.AVAILABLE,r.tick(w,s))
            assertNotEquals(CapsulePreviewAvailability.AVAILABLE,r.evaluate(s.copy(flying=true)))
        }
        val r=ready()
        assertEquals(CapsulePreviewAvailability.TESTED_ANGLE_ONLY,r.evaluate(eligible().copy(pitch=35.0)))
    }
    @Test fun `masterful preview requires expanded opt in and never enables unknown items`() {
        val s=eligible().copy(capsule=ShotVariant.MASTERFUL,allowUncalibratedEstimates=true)
        val r=CapsulePreviewRules();val w=Any()
        assertEquals(CapsulePreviewAvailability.AVAILABLE,r.tick(w,s))
        assertEquals(CapsulePreviewAvailability.HIDDEN,r.evaluate(s.copy(allowUncalibratedEstimates=false)))
        assertEquals(CapsulePreviewAvailability.HIDDEN,r.evaluate(s.copy(capsule=null)))
        assertEquals(CapsulePreviewAvailability.AVAILABLE,r.tick(w,s.copy(sprinting=true,velocity=ShotVector(.3,.2,.1),onGround=false)))
    }
    @Test fun `strict collection remains stationary ordinary even with expanded preview opt in`() {
        val s=eligible().copy(allowUncalibratedEstimates=true,pitch=35.0)
        val r=CapsulePreviewRules(allowExtendedPreview=false);val w=Any()
        assertEquals(CapsulePreviewAvailability.STAND_STILL,r.tick(w,s))
        assertEquals(CapsulePreviewAvailability.AVAILABLE,r.tick(w,s))
        assertEquals(CapsulePreviewAvailability.HIDDEN,r.evaluate(s.copy(capsule=ShotVariant.MASTERFUL)))
        for(moving in listOf(s.copy(velocity=ShotVector(.2,0.0,0.0)),s.copy(sprinting=true),s.copy(sneaking=true),s.copy(onGround=false))) {
            assertEquals(CapsulePreviewAvailability.STAND_STILL,r.tick(w,moving))
            assertEquals(CapsulePreviewAvailability.STAND_STILL,r.tick(w,s))
            assertEquals(CapsulePreviewAvailability.AVAILABLE,r.tick(w,s))
        }
    }
    @Test fun `expanded preview rejects unsupported modes and nonfinite motion`() {
        val s=eligible().copy(allowUncalibratedEstimates=true)
        for(blocked in listOf(s.copy(flying=true),s.copy(riding=true),
            s.copy(position=ShotVector(Double.NaN,0.0,0.0)),s.copy(velocity=ShotVector(0.0,Double.POSITIVE_INFINITY,0.0)))) {
            val r=CapsulePreviewRules();r.tick(Any(),s)
            assertEquals(CapsulePreviewAvailability.STAND_STILL,r.evaluate(blocked))
        }
    }
    @Test fun `expanded water preview allows swimming without relaxing strict collection or opt out`() {
        for(variant in listOf(ShotVariant.ORDINARY,ShotVariant.MASTERFUL)) {
            val s=eligible().copy(allowUncalibratedEstimates=true,capsule=variant,swimming=true,onGround=false,
                velocity=ShotVector(.1,.02,.1))
            assertEquals(CapsulePreviewAvailability.AVAILABLE,CapsulePreviewRules().tick(Any(),s))
            assertNotEquals(CapsulePreviewAvailability.AVAILABLE,CapsulePreviewRules(allowExtendedPreview=false).tick(Any(),s))
            assertNotEquals(CapsulePreviewAvailability.AVAILABLE,CapsulePreviewRules().tick(Any(),s.copy(allowUncalibratedEstimates=false)))
        }
    }
    @Test fun `standing in water stays uncalibrated and cannot produce strict collection stability`() {
        val s=eligible().copy(allowUncalibratedEstimates=true,inWater=true)
        assertEquals(CapsulePreviewAvailability.AVAILABLE,CapsulePreviewRules().tick(Any(),s))
        assertEquals("Water",s.estimateLabel)
        val strict=CapsulePreviewRules(allowExtendedPreview=false);val w=Any()
        repeat(3){assertNotEquals(CapsulePreviewAvailability.AVAILABLE,strict.tick(w,s))}
        assertNotEquals(CapsulePreviewAvailability.AVAILABLE,ready().evaluate(s.copy(allowUncalibratedEstimates=false)))
    }
    @Test fun `lava remains unsupported with expanded water preview enabled`() {
        for(water in listOf(false,true)) {
            val s=eligible().copy(allowUncalibratedEstimates=true,inLava=true,inWater=water)
            assertEquals(CapsulePreviewAvailability.STAND_STILL,CapsulePreviewRules().tick(Any(),s))
        }
    }
    @Test fun `expanded preview still honors hidden view and hiding restrictions`() {
        val s=eligible().copy(capsule=ShotVariant.MASTERFUL,allowUncalibratedEstimates=true,onGround=false)
        val r=CapsulePreviewRules();r.tick(Any(),s)
        for(hidden in listOf(s.copy(enabled=false),s.copy(inSafari=false),s.copy(screenOpen=true),s.copy(hudHidden=true)))
            assertEquals(CapsulePreviewAvailability.HIDDEN,r.evaluate(hidden))
        assertEquals(CapsulePreviewAvailability.AVAILABLE,r.evaluate(s.copy(firstPerson=false)))
        assertEquals(CapsulePreviewAvailability.HIDING_UNAVAILABLE,r.evaluate(s.copy(hideFlying=true)))
        assertEquals(CapsulePreviewAvailability.HIDING_UNAVAILABLE,r.evaluate(s.copy(hideGround=true)))
    }
    @Test fun `extended use cannot bank strict stability or survive world loss`() {
        val s=eligible().copy(allowUncalibratedEstimates=true)
        val r=CapsulePreviewRules();val w=Any()
        assertEquals(CapsulePreviewAvailability.STAND_STILL,r.evaluate(s))
        repeat(3){r.tick(w,s.copy(capsule=ShotVariant.MASTERFUL))}
        assertEquals(CapsulePreviewAvailability.STAND_STILL,r.tick(w,s.copy(allowUncalibratedEstimates=false)))
        assertEquals(CapsulePreviewAvailability.AVAILABLE,r.tick(w,s.copy(allowUncalibratedEstimates=false)))
        r.tick(null,s)
        assertNotEquals(CapsulePreviewAvailability.AVAILABLE,r.evaluate(s))
        assertEquals(CapsulePreviewAvailability.AVAILABLE,r.tick(Any(),s))
    }
    private fun eligible() = CapsulePreviewState(true,true,ShotVariant.ORDINARY,true,true,false,false,false,false,false,
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
        for(blocked in listOf(s.copy(enabled=false),s.copy(inSafari=false),s.copy(capsule=null),
            s.copy(screenOpen=true),s.copy(hudHidden=true)))
            assertEquals(CapsulePreviewAvailability.HIDDEN,ready().evaluate(blocked))
        assertEquals(CapsulePreviewAvailability.HIDDEN,ready().evaluate(null))
        assertEquals(CapsulePreviewAvailability.AVAILABLE,ready().evaluate(s.copy(firstPerson=false)))
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
        for(blocked in listOf(s.copy(enabled=false),s.copy(hideFlying=true),s.copy(capsule=null),s.copy(pitch=2.0))) {
            val r=CapsulePreviewRules();repeat(5){r.tick(w,blocked)}
            r.tick(w,s);assertEquals(CapsulePreviewAvailability.STAND_STILL,r.evaluate(s))
        }
    }
}
