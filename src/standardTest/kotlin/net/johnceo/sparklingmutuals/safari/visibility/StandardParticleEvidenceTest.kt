package net.johnceo.sparklingmutuals.safari.visibility
import net.johnceo.sparklingmutuals.safari.sparkling.StandardParticleEvidence
import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class StandardParticleEvidenceTest {
 @Test fun `three matching packets qualify and expire`() {
  val e=StandardParticleEvidence(); val id=UUID.randomUUID()
  e.observe(id,"Hideyho",0); e.observe(id,"Hideyho",100)
  assertFalse(e.confirmed(id,"Hideyho",100))
  e.observe(id,"Hideyho",200); assertTrue(e.confirmed(id,"Hideyho",200))
  assertFalse(e.confirmed(id,"Hideyho",2701))
 }
 @Test fun `species changes long gaps and resets cannot reuse evidence`() {
  val e=StandardParticleEvidence(); val id=UUID.randomUUID()
  e.observe(id,"Hideyho",0); e.observe(id,"Hideyho",100); e.observe(id,"Hideyho",200)
  assertFalse(e.confirmed(id,"Duplico",200)); e.observe(id,"Duplico",300)
  assertFalse(e.confirmed(id,"Duplico",300)); e.observe(id,"Duplico",4000)
  assertFalse(e.confirmed(id,"Duplico",4000)); e.clear(); assertFalse(e.confirmed(id,"Hideyho",200))
 }
 @Test fun `only the exact particle pattern qualifies`() {
  assertTrue(StandardParticleEvidence.pattern(true,5,.3f,.5f,.3f,1f))
  assertFalse(StandardParticleEvidence.pattern(true,1,.3f,.5f,.3f,1f))
  assertFalse(StandardParticleEvidence.pattern(false,5,.3f,.5f,.3f,1f))
  assertFalse(StandardParticleEvidence.pattern(true,5,Float.NaN,.5f,.3f,1f))
 }
}
