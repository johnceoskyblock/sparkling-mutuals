package net.johnceo.sparklingmutuals.safari.shots

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShotProtocolTest {
    @Test fun `header is versioned local evidence without a prediction model`() {
        val line=ShotCodec.encode(ShotRecord.Header(ShotTestFixtures.metadata(),ShotLimits()))
        assertTrue(line.isNotEmpty());assertEquals('\n'.code.toByte(),line.last())
        val json=JsonParser.parseString(line.toString(Charsets.UTF_8)).asJsonObject
        assertEquals("header",json["type"].asString)
        assertEquals(1,json["metadata"].asJsonObject["schemaVersion"].asInt)
        assertTrue(json["metadata"].asJsonObject["modelVersion"].isJsonNull)
        assertFalse(json.toString().contains("uuid",true));assertFalse(json.toString().contains("serverAddress"))
    }
    @Test fun `nonfinite positions cannot silently enter an export`() {
        val event=ShotObservation.Capsule("c1",1,ShotVariant.ORDINARY,ShotEntityAction.SAMPLE,ShotVector(Double.NaN,0.0,0.0))
        assertThrows(IllegalArgumentException::class.java) { ShotCodec.encode(ShotRecord.Event(1,0,0,event)) }
    }
    @Test fun `typed feedback exposes species without raw message or catcher identity`() {
        val line=ShotCodec.encode(ShotRecord.Event(1,0,0,ShotObservation.Feedback(ShotFeedbackObservation(ShotFeedbackKind.SHARED_CAPTURE,"Gemzie"),1)))
        assertTrue(line.isNotEmpty());val s=line.toString(Charsets.UTF_8)
        assertTrue(s.contains("\"shared_capture\""));assertTrue(s.contains("Gemzie"))
        assertFalse(s.contains("catcher"));assertFalse(s.contains("raw"))
    }
    @Test fun `producer fixtures retain candidates and unknown outcomes for independent replay`() {
        fun capture(ambiguous: Boolean): String {
            val budget=ShotTestFixtures.Budget();val sink=ShotTestFixtures.Sink(budget)
            val session=ShotSession(ShotTestFixtures.metadata().copy(session=if (ambiguous) "fixture-ambiguous" else "fixture-isolated"),ShotLimits(),sink,budget)
            session.accept(ShotStamp(0,0),ShotTestFixtures.input("s1"))
            repeat(if (ambiguous) 2 else 1) { n ->
                session.accept(ShotStamp(100_000+n*100_000L,2+n*2L),ShotObservation.Capsule("c${n+1}",1,ShotVariant.ORDINARY,ShotEntityAction.DISCOVERED,ShotVector(0.0,0.0,1.0),discovery=ShotDiscovery.SPAWN))
                session.accept(ShotStamp(120_000+n*100_000L,3+n*2L),ShotObservation.Packet("c${n+1}",ShotUpdateKind.POSITION_SYNC,listOf(ShotPathPoint(ShotVector(0.0,0.0,2.0)))))
            }
            session.stop(ShotStamp(500_000,10),ShotStopReason.USER_STOP)
            val footer=sink.records().last();assertEquals(1,footer["inputCount"].asInt)
            assertEquals(if (ambiguous) "ambiguous" else "candidate_unique",footer["shots"].asJsonArray[0].asJsonObject["association"].asString)
            return sink.lines.joinToString("")
        }
        val isolated=capture(false);val ambiguous=capture(true)
        val directory=java.nio.file.Path.of("build/shot-fixtures");java.nio.file.Files.createDirectories(directory)
        java.nio.file.Files.writeString(directory.resolve("isolated.jsonl"),isolated)
        java.nio.file.Files.writeString(directory.resolve("ambiguous.jsonl"),ambiguous)
        java.nio.file.Files.writeString(directory.resolve("partial.jsonl"),isolated.lineSequence().filter { it.isNotBlank() }.toList().dropLast(1).joinToString("\n",postfix="\n"))
    }
}
