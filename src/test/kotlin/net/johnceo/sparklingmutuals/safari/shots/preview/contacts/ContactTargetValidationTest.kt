package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class ContactTargetValidationTest {
    private fun target(key:String="a",generation:Int=1)=ContactTarget(key,generation,"Foxtrot",
        ShotBounds(ShotVector(0.0,0.0,0.0),ShotVector(1.0,1.0,1.0)),"client_body_v1")
    @Test fun `unchanged critter remains available at hundreds of frames per client tick`() {
        val checks=ContactTargetValidation();checks.beginTick();val a=target();var inspected=0
        repeat(100){assertNull(checks.validate(a){inspected++;a})};assertEquals(1,inspected)
    }
    @Test fun `sixteen distinct inspections remain bounded and cached result survives exhaustion`() {
        val checks=ContactTargetValidation();checks.beginTick();var inspected=0
        repeat(16){val a=target("$it");assertNull(checks.validate(a){inspected++;a})}
        assertEquals("identity_budget",checks.validate(target("extra")){inspected++;it})
        assertNull(checks.validate(target("0")){error("already validated")});assertEquals(16,inspected)
    }
    @Test fun `entity mutation invalidates only that identity without resetting budget`() {
        val checks=ContactTargetValidation();checks.beginTick();val a=target();val b=target("b");var inspected=0
        assertNull(checks.validate(a){inspected++;a});assertNull(checks.validate(b){inspected++;b})
        checks.invalidate(a.key);assertEquals("stale_target",checks.validate(a){inspected++;null})
        assertNull(checks.validate(b){error("unrelated entity remains validated")});assertEquals(3,inspected)
        repeat(13){checks.invalidate(a.key);checks.validate(a){inspected++;a}}
        checks.invalidate(a.key);assertEquals("identity_budget",checks.validate(a){error("mutations cannot reset allowance")})
        assertEquals(16,inspected)
    }
    @Test fun `same numeric ID with changed generation or body must be inspected again`() {
        val checks=ContactTargetValidation();checks.beginTick();val a=target();var inspected=0
        assertNull(checks.validate(a){inspected++;a})
        val replacement=target(generation=2);assertNull(checks.validate(replacement){inspected++;replacement})
        val moved=replacement.copy(bounds=ShotBounds(ShotVector(5.0,0.0,0.0),ShotVector(6.0,1.0,1.0)))
        assertEquals("stale_target",checks.validate(replacement.copy(species="Bluebird")){inspected++;moved});assertEquals(3,inspected)
    }
    @Test fun `tick and invalidation require fresh checks while reset disables inspection`() {
        val checks=ContactTargetValidation();val a=target();var inspected=0
        assertEquals("identity_budget",checks.validate(a){inspected++;a})
        checks.beginTick();assertNull(checks.validate(a){inspected++;a})
        checks.invalidate();assertEquals("stale_target",checks.validate(a){inspected++;null})
        checks.beginTick();assertNull(checks.validate(a){inspected++;a})
        checks.reset();assertEquals("identity_budget",checks.validate(a){inspected++;a});assertEquals(3,inspected)
    }
}
