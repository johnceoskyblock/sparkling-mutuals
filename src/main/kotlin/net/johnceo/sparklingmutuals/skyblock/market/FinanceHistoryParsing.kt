package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Public one-minute ask candles; retrospective references, never invented delivery observations. */
internal object FinanceHistoryParsing {
    private const val MINUTE=60_000L
    private const val MAX_BYTES=2*1024*1024

    fun parse(raw: String, now: Long): List<PriceSample> = candles(raw,now,MINUTE,ShardHistoryParsing.STEP,false)

    fun runtime(raw: String,now: Long,part: HistoryPart): List<PriceSample> = when(part) {
        HistoryPart.RECENT -> candles(raw,now,MINUTE,ShardHistoryParsing.STEP,true)
        HistoryPart.WEEK -> candles(raw,now,900_000,ShardHistoryParsing.COARSE,true)
    }

    private fun candles(raw: String,now: Long,resolution: Long,output: Long,medians: Boolean): List<PriceSample> {
        if(raw.length>MAX_BYTES || raw.toByteArray(Charsets.UTF_8).size>MAX_BYTES) return emptyList()
        return runCatching {
            val batch=JsonParser.parseString(raw).asJsonArray
            if(batch.size()!=1) return emptyList()
            val data=batch[0].asJsonObject.getAsJsonObject("result").getAsJsonObject("data").getAsJsonObject("json")
            val noData=data.getAsJsonPrimitive("noData")
            if(!noData.isBoolean || noData.asBoolean) return emptyList()
            val points=data.getAsJsonArray("bars").mapNotNull { value -> runCatching {
                val bar=value.asJsonObject
                // The source audit found a common 622-minute null-volume/unchanged-price span.
                // Use presence only as a sampling-quality flag; volume is not a trade count.
                val volume=bar.get("volume")
                if(volume==null || !volume.isJsonPrimitive || !volume.asJsonPrimitive.isNumber ||
                    !volume.asDouble.isFinite() || volume.asDouble<0) return@mapNotNull null
                val time=bar.getAsJsonPrimitive("time")
                if(!time.isNumber) return@mapNotNull null
                val start=time.asBigDecimal.longValueExact()
                if(start<=0 || start%resolution!=0L) return@mapNotNull null
                // Candle-start convention is a research assumption; basis/availability stay unqualified.
                val end=Math.addExact(start,resolution)
                val bin=Math.multiplyExact(Math.addExact((end-1)/output,1),output)
                if(bin>now) return@mapNotNull null
                val close=number(bar,"close") ?: return@mapNotNull null
                val high=number(bar,"high") ?: return@mapNotNull null
                val low=number(bar,"low") ?: return@mapNotNull null
                number(bar,"open") ?: return@mapNotNull null
                // Opens may carry the previous close. Highs and volumes never supply replay touches.
                if(low>high || close<low || close>high) return@mapNotNull null
                PriceSample(bin,close,output,end,PriceBasis.UNVERIFIED_REFERENCE,null)
            }.getOrNull() }
            val consistent=points.groupBy {it.observedAt}.values.mapNotNull { group ->
                group.first().takeIf {group.map {it.ask}.distinct().size==1}
            }
            consistent.groupBy {it.at}.values.mapNotNull { group ->
                if(medians && group.size<kotlin.math.ceil(output.toDouble()/resolution*.8).toInt())null
                else group.maxBy {it.observedAt}.let {latest ->
                    if(medians)latest.copy(ask=ShardHistoryParsing.median(group.map {it.ask}))else latest
                }
            }.sortedBy {it.at}
        }.getOrDefault(emptyList())
    }

    private fun number(value: JsonObject, key: String): Double? = runCatching {
        val primitive=value.getAsJsonPrimitive(key)
        if(!primitive.isNumber) return null
        primitive.asDouble.takeIf {it.isFinite() && it>0}
    }.getOrNull()
}
