package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ShardReplayCliTest {
    @TempDir lateinit var directory: Path
    private val now=1_793_700_300_000L/300_000*300_000
    private fun manifest(source: String, body: String): Path {
        val raw=directory.resolve("response.json")
        Files.writeString(raw,body)
        val file=JsonObject().apply {addProperty("source",source);addProperty("path",raw.toString());addProperty("resolutionMillis",60_000)}
        val root=JsonObject().apply {
            addProperty("asOf",now)
            add("products",JsonObject().apply {add("SHARD_HIDEONWALL",JsonObject().apply {add("files",JsonArray().apply {add(file)})})})
        }
        return directory.resolve("manifest.json").also {Files.writeString(it,root.toString())}
    }
    @Test fun `finance manifest reaches production parser while retaining unknown source and delivery evidence`() {
        val raw="""[{"result":{"data":{"json":{"noData":false,"bars":[{"time":${now-60_000},"open":100,"high":100,"low":100,"close":100,"volume":10}]}}}}]"""
        val input=manifest("skyblock-finance",raw)
        val output=directory.resolve("result.json")
        ShardReplayCli.main(arrayOf(input.toString(),output.toString(),"export-samples"))
        val result=JsonParser.parseString(Files.readString(output)).asJsonObject
        val points=result.getAsJsonObject("samples").getAsJsonArray("SHARD_HIDEONWALL")
        assertEquals(1,points.size())
        assertEquals("UNVERIFIED_REFERENCE",points[0].asJsonObject.get("basis").asString)
        assertFalse(result.getAsJsonObject("reports").getAsJsonObject("SHARD_HIDEONWALL").get("eligible").asBoolean)
    }
    @Test fun `unknown input source is rejected instead of silently interpreted as another provider`() {
        val input=manifest("unsupported-source","[]")
        assertThrows(IllegalArgumentException::class.java) {
            ShardReplayCli.main(arrayOf(input.toString(),directory.resolve("result.json").toString()))
        }
    }
}
