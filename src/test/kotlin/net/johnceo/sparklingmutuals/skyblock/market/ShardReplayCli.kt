package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path

/** Local research entry point; deliberately in test sources, never packaged with the mod. */
object ShardReplayCli {
    @JvmStatic fun main(args: Array<String>) {
        require(args.size in 2..3) { "Input manifest, output report, optional training selection flag required" }
        val manifest=JsonParser.parseString(Files.readString(Path.of(args[0]))).asJsonObject
        val now=manifest.get("asOf").asLong
        val split=now/ShardHistoryParsing.DAY*ShardHistoryParsing.DAY-ShardReplay.VALIDATION_DAYS*ShardHistoryParsing.DAY
        val exportedSamples=linkedMapOf<String,List<PriceSample>>()
        val reports=manifest.getAsJsonObject("products").entrySet().associate { (id,value) ->
            val samples=value.asJsonObject.getAsJsonArray("files").flatMap { entry ->
                val file=entry.asJsonObject
                val path=Path.of(file.get("path").asString)
                require(Files.size(path)<=2*1024*1024) { "Oversized research input" }
                val source=file.get("source")?.asString ?: "cofl"
                when(source) {
                    "cofl" -> ShardHistoryParsing.parse(Files.readString(path),now,file.get("resolutionMillis").asLong)
                    "skyblock-finance" -> {
                        require(file.get("resolutionMillis").asLong==60_000L) {"Finance parser requires one-minute source bars"}
                        FinanceHistoryParsing.parse(Files.readString(path),now)
                    }
                    else -> throw IllegalArgumentException("Unsupported research source: $source")
                }
            }
            if(args.getOrNull(2)=="export-samples") exportedSamples[id]=samples
            val selected=if(args.getOrNull(2)=="select-training")ShardReplay.selectTrainingParameters(samples,split) else ModelParameters()
            val report=ShardReplay.evaluate(samples,split,selected ?: ModelParameters())
            id to if(selected==null)report.copy(eligible=false,reason="No predeclared candidate met training acceptance")else report
        }
        val result=mutableMapOf<String,Any>("asOf" to now,"splitAt" to split,"modelVersion" to ShardTargetModel.VERSION,"reports" to reports,
            "limits" to "Quote target touches are not fills. Historical price provenance and actual publication availability require independent qualification. Finance null-volume sampling flags are excluded; source candle boundaries remain research assumptions.")
        if(exportedSamples.isNotEmpty()) result["samples"]=exportedSamples
        Files.writeString(Path.of(args[1]),GsonBuilder().setPrettyPrinting().create().toJson(result))
        for((id,report) in reports) println("$id eligible=${report.eligible} windows=${report.windows.size} training=${report.training} holdout=${report.holdout}")
    }
}
