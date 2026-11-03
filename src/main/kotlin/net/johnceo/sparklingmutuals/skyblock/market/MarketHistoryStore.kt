package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path

/** Price history only. Final/staging files are each <=8 MiB, together <=16 MiB. */
internal class MarketHistoryStore(
    private val path: Path,
    allowedIds: Set<String>,
    private val modelVersion: Int=ShardTargetModel.VERSION,
) {
    private val allowed=allowedIds.toSet()
    init {
        require(SaleMarketProducts.bounded(allowed) && allowed.all(SaleMarketProducts::historyId))
        require(modelVersion>0)
    }

    @Synchronized fun read(now: Long): Map<String,List<PriceSample>> = runCatching {
        if(!Files.isRegularFile(path) || Files.isSymbolicLink(path) || Files.size(path)>FILE_LIMIT) return emptyMap()
        val bytes=Files.newInputStream(path).use {it.readNBytes(FILE_LIMIT+1)}
        if(bytes.size>FILE_LIMIT) return emptyMap()
        val root=JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
        if(integer(root,"schema")!=1 || integer(root,"model")!=modelVersion) return emptyMap()
        val values=root.getAsJsonObject("products").entrySet().filter {it.key in allowed}.associate { (id,rows) ->
            id to rows.asJsonArray.map { row ->
                val fields=row.asJsonArray
                require(fields.size()==6)
                require((0..3).all {fields[it].isJsonPrimitive && fields[it].asJsonPrimitive.isNumber})
                require(fields[4].isJsonPrimitive && fields[4].asJsonPrimitive.isString)
                require(fields[5].isJsonNull || fields[5].isJsonPrimitive && fields[5].asJsonPrimitive.isNumber)
                PriceSample(fields[0].asBigDecimal.longValueExact(),fields[1].asDouble,
                    fields[2].asBigDecimal.longValueExact(),fields[3].asBigDecimal.longValueExact(),
                    PriceBasis.valueOf(fields[4].asString),if(fields[5].isJsonNull)null else fields[5].asBigDecimal.longValueExact())
            }
        }
        sanitize(values,now)
    }.getOrDefault(emptyMap())

    @Synchronized fun write(values: Map<String,List<PriceSample>>, now: Long,stillCurrent: ()->Boolean={true}): Boolean {
        return runCatching {
            val root=JsonObject().apply {addProperty("schema",1);addProperty("model",modelVersion)}
            val products=JsonObject()
            sanitize(values,now).forEach { (id,points) ->
                val rows=JsonArray()
                points.forEach { point -> rows.add(JsonArray().apply {
                    add(point.at);add(point.ask);add(point.resolutionMillis);add(point.observedAt);add(point.basis.name)
                    if(point.availableAt==null)add(JsonNull.INSTANCE)else add(point.availableAt)
                }) }
                products.add(id,rows)
            }
            root.add("products",products)
            BoundedAtomicCacheFile.write(path,FILE_LIMIT,root.toString().toByteArray(Charsets.UTF_8),stillCurrent)
        }.getOrDefault(false)
    }
    fun sanitize(values: Map<String,List<PriceSample>>, now: Long): Map<String,List<PriceSample>> =
        values.filterKeys {it in allowed}.toSortedMap().mapValues { (_,points) ->
            points.filter { p ->
                p.at>now-8*ShardHistoryParsing.DAY && p.at<=now && p.ask.isFinite() && p.ask>0 &&
                    p.resolutionMillis in setOf(ShardHistoryParsing.STEP,ShardHistoryParsing.COARSE) &&
                    p.at%p.resolutionMillis==0L && p.observedAt>0 && p.observedAt<=p.at &&
                    (p.availableAt==null || p.availableAt>=p.observedAt && p.availableAt<=now)
            }.groupBy {it.resolutionMillis to it.at}.values.mapNotNull { group ->
                val newest=group.maxOf {it.observedAt}
                val current=group.filter {it.observedAt==newest}
                if(current.map {it.ask}.distinct().size!=1)null else current.first().copy(
                    basis=if(current.all {it.basis==PriceBasis.VERIFIED_BEST_ASK})PriceBasis.VERIFIED_BEST_ASK else PriceBasis.UNVERIFIED_REFERENCE,
                    availableAt=if(current.all {it.availableAt!=null})current.maxOf {it.availableAt!!}else null)
            }.groupBy {it.resolutionMillis}.flatMap { (resolution,samples) ->
                samples.sortedBy {it.at}.takeLast(if(resolution==ShardHistoryParsing.STEP)2304 else 96)
            }.sortedWith(compareBy({it.at},{it.resolutionMillis})).toList()
        }.filterValues {it.isNotEmpty()}

    private fun integer(root: JsonObject, key: String): Int? = runCatching {
        val value=root.getAsJsonPrimitive(key)
        if(!value.isNumber)null else value.asBigDecimal.intValueExact()
    }.getOrNull()

    companion object { private const val FILE_LIMIT=8*1024*1024 }
}
