package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShotSessionTest {
    @Test fun `known capsule variant feedback does not attach to incompatible input variants`() {
        val session=session()
        session.accept(at(0),ShotTestFixtures.input("ordinary"))
        session.accept(at(1),ShotTestFixtures.input("masterful").copy(variant=ShotVariant.MASTERFUL))
        session.accept(at(2),ShotObservation.Feedback(ShotFeedbackObservation(ShotFeedbackKind.HIT,"Gemzie",ShotVariant.MASTERFUL),1))
        session.stop(at(3),ShotStopReason.USER_STOP)
        val shots=sink.records().last()["shots"].asJsonArray
        assertEquals(0,shots[0].asJsonObject["feedback"].asJsonArray.size())
        assertEquals(1,shots[1].asJsonObject["feedback"].asJsonArray.size())
    }
    private val budget = ShotTestFixtures.Budget()
    private val sink = ShotTestFixtures.Sink(budget)
    private fun session(limits: ShotLimits = ShotLimits()) = ShotSession(ShotTestFixtures.metadata(), limits, sink, budget)
    private fun at(us: Long) = ShotStamp(us, us/50_000)
    @Test fun `input cap rejects the extra input and records honest termination`() {
        val session = session(ShotLimits(maxInputs=2))
        repeat(3) { session.accept(at(it*10000L), ShotTestFixtures.input()) }
        assertEquals(2,session.status().inputCount); assertFalse(session.status().recording)
        assertEquals(ShotStopReason.INPUT_LIMIT, session.status().stopReason)
    }
    @Test fun `equal timestamps preserve sequence and backwards clock stops instead of reordering`() {
        val session=session()
        session.accept(at(100),ShotTestFixtures.input());session.accept(at(100),ShotTestFixtures.input())
        assertEquals(2,session.status().inputCount)
        session.advance(at(99));assertEquals(ShotStopReason.ERROR,session.status().stopReason)
        val events=sink.records().filter { it["type"].asString=="event" }
        assertEquals(listOf(1L,2L),events.map { it["sequence"].asLong })
    }
    @Test fun `two flying candidates remain ambiguous and disappearance never invents a miss`() {
        val session=session();session.accept(at(0),ShotTestFixtures.input("s1"))
        for (token in listOf("c1","c2")) session.accept(at(100_000),ShotObservation.Capsule(token,1,ShotVariant.ORDINARY,ShotEntityAction.DISCOVERED,ShotTestFixtures.zero,discovery=ShotDiscovery.SPAWN))
        session.advance(at(8_000_001));session.stop(at(8_000_002),ShotStopReason.USER_STOP)
        val shot=sink.records().last()["shots"].asJsonArray[0].asJsonObject
        assertEquals("ambiguous",shot["association"].asString);assertEquals(2,shot["tracks"].asJsonArray.size())
        assertEquals(0,shot["annotations"].asJsonArray.size());assertFalse(sink.lines.joinToString("").contains("\"miss\""))
    }
    @Test fun `preexisting ground and wrong variant capsules are not claimed by a new local input`() {
        val session=session()
        session.accept(at(0),ShotObservation.Capsule("old",1,ShotVariant.ORDINARY,ShotEntityAction.DISCOVERED,ShotTestFixtures.zero))
        session.accept(at(100_000),ShotTestFixtures.input("s1",ShotVariant.MASTERFUL,ShotHand.OFF))
        session.accept(at(120_000),ShotObservation.Capsule("ground",1,ShotVariant.MASTERFUL,ShotEntityAction.DISCOVERED,ShotTestFixtures.zero,flying=false))
        session.accept(at(130_000),ShotObservation.Capsule("other",1,ShotVariant.ORDINARY,ShotEntityAction.DISCOVERED,ShotTestFixtures.zero))
        session.stop(at(200_000),ShotStopReason.USER_STOP)
        assertEquals("unmatched",sink.records().last()["shots"].asJsonArray[0].asJsonObject["association"].asString)
    }
    @Test fun `repeated input windows do not duplicate or reorder preinput samples`() {
        val session=session()
        session.accept(at(100),ShotObservation.Gap(ShotGap.CACHE_BACKLOG))
        session.accept(at(200),ShotTestFixtures.input())
        session.accept(at(300),ShotTestFixtures.input())
        session.stop(at(400),ShotStopReason.USER_STOP)
        val records=sink.records();assertEquals(5,records.size)
        assertEquals(listOf(1L,2L,3L),records.filter { it["type"].asString=="event" }.map { it["sequence"].asLong })
        assertEquals(0,budget.usedBytes)
    }
    @Test fun `shared feedback and manual annotation remain separate from confirmed local outcomes`() {
        val session=session();session.accept(at(0),ShotTestFixtures.input("s1"))
        session.accept(at(1),ShotObservation.Feedback(ShotFeedbackObservation(ShotFeedbackKind.SHARED_CAPTURE,"Gemzie"),1))
        session.accept(at(2),ShotObservation.Annotation("s1",ShotManualOutcome.MISS))
        session.stop(at(3),ShotStopReason.EXPORT)
        val shot=sink.records().last()["shots"].asJsonArray[0].asJsonObject
        assertEquals(0,shot["feedback"].asJsonArray.size());assertEquals("miss",shot["annotations"].asJsonArray[0].asString)
        assertEquals("unmatched",shot["association"].asString)
    }
    @Test fun `queue rejection stops incomplete and releases producer owned bytes`() {
        val session=session();sink.allowed=false
        session.accept(at(0),ShotTestFixtures.input())
        assertEquals(ShotStopReason.QUEUE_LIMIT,session.status().stopReason)
        assertFalse(sink.records().last()["complete"].asBoolean);assertEquals(0,budget.usedBytes)
    }
    @Test fun `duration and event caps include final record without accepting unbounded events`() {
        val session=session(ShotLimits(maxEvents=2));session.accept(at(0),ShotTestFixtures.input());session.accept(at(1),ShotTestFixtures.input())
        assertEquals(ShotStopReason.EVENT_LIMIT,session.status().stopReason)
        assertEquals(1,session.status().inputCount)
        val second=ShotTestFixtures.session();second.advance(at(180_000_000))
        assertEquals(ShotStopReason.DURATION_LIMIT,second.status().stopReason)
    }
    @Test fun `late feedback cannot overtake buffered observations before the next input`() {
        val session=session();session.accept(at(0),ShotTestFixtures.input())
        session.accept(at(9_000_000),ShotObservation.Gap(ShotGap.CACHE_BACKLOG))
        session.accept(at(9_100_000),ShotObservation.Feedback(ShotFeedbackObservation(ShotFeedbackKind.ESCAPE,"Gemzie"),1))
        session.accept(at(9_200_000),ShotTestFixtures.input())
        session.stop(at(9_300_000),ShotStopReason.USER_STOP)
        assertEquals(listOf(1L,2L,3L,4L),sink.records().filter { it["type"].asString=="event" }.map { it["sequence"].asLong })
    }
    @Test fun `byte cap reserves a footer and does not exceed configured file size`() {
        val limits=ShotLimits(maxBytes=66_000)
        val session=session(limits)
        repeat(20) { session.accept(at(it*1000L),ShotTestFixtures.input()) }
        assertEquals(ShotStopReason.BYTE_LIMIT,session.status().stopReason)
        assertTrue(sink.lines.sumOf { it.toByteArray(Charsets.UTF_8).size } <= 66_000)
        assertEquals(0,budget.usedBytes)
    }
}
