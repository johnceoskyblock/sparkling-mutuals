package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Public one-minute ask candles; retrospective references, never invented delivery observations. */
internal object FinanceHistoryParsing {
    private const val MINUTE=60_000L
    private const val MAX_BYTES=2*1024*1024

    fun parse(raw: String, now: Long): List<PriceSample> {
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
                if(start<=0 || start%MINUTE!=0L) return@mapNotNull null
                // Candle-start convention is a research assumption; basis/availability stay unqualified.
                val end=Math.addExact(start,MINUTE)
                val bin=Math.multiplyExact(Math.addExact((end-1)/ShardHistoryParsing.STEP,1),ShardHistoryParsing.STEP)
                if(bin>now) return@mapNotNull null
                val close=number(bar,"close") ?: return@mapNotNull null
                val high=number(bar,"high") ?: return@mapNotNull null
                val low=number(bar,"low") ?: return@mapNotNull null
                number(bar,"open") ?: return@mapNotNull null
                // Opens may carry the previous close. Highs and volumes never supply replay touches.
                if(low>high || close<low || close>high) return@mapNotNull null
                PriceSample(bin,close,ShardHistoryParsing.STEP,end,PriceBasis.UNVERIFIED_REFERENCE,null)
            }.getOrNull() }
            val consistent=points.groupBy {it.observedAt}.values.mapNotNull { group ->
                group.first().takeIf {group.map {it.ask}.distinct().size==1}
            }
            consistent.groupBy {it.at}.values.map { group -> group.maxBy {it.observedAt} }.sortedBy {it.at}
        }.getOrDefault(emptyList())
    }

    private fun number(value: JsonObject, key: String): Double? = runCatching {
        val primitive=value.getAsJsonPrimitive(key)
        if(!primitive.isNumber) return null
        primitive.asDouble.takeIf {it.isFinite() && it>0}
    }.getOrNull()
}
