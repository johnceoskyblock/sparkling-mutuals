package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/** Irreversible evidence only. Inventory and positive sightings never cross this interface. */
internal class PartySyncBootstrap {
    companion object { val indices=setOf(23,24,25,26,27,28,29,30,31,32,34,35,36,37) }
    private data class Evidence(val reporter: String, val source: String, var nests: Int=0,
        val facts: IntArray=IntArray(38), val caught: IntArray=IntArray(2))
    private val evidence=linkedMapOf<Pair<String,String>,Evidence>()
    fun observe(reporter: String, kind: String, values: IntArray, source: String="reporter") {
        val e=evidence.getOrPut(reporter to source) { Evidence(reporter,source) }
        when(kind) {
            "A" -> indices.forEach { i -> e.facts[i]=if(i==24) maxOf(e.facts[i],values[i]) else e.facts[i] or values[i] }
            "N" -> e.nests=e.nests or values[0]
            "S" -> for(i in 0..1) e.caught[i]=e.caught[i] or values[i+2]
        }
    }
    fun frames(): List<JsonObject> = evidence.values.map { e -> JsonObject().apply {
        addProperty("reporter",e.reporter);addProperty("source",e.source);addProperty("nests",e.nests)
        add("facts",JsonArray().apply { e.facts.forEach(::add) });add("caught",JsonArray().apply { e.caught.forEach(::add) })
    } }
    fun accept(value: JsonObject, roster: Set<String>): JsonObject? = runCatching {
        val reporter=PartySyncProtocol.text(value.get("reporter"))?.takeIf { it in roster } ?: return null
        val source=PartySyncProtocol.text(value.get("source")) ?: "reporter"
        if(source !in setOf("reporter","room"))return null
        val nests=PartySyncProtocol.integer(value.get("nests"),0,8191)?.toInt() ?: return null
        val raw=value.get("facts")?.takeIf { it.isJsonArray && it.asJsonArray.size()==38 }?.asJsonArray ?: return null
        val facts=raw.map { PartySyncProtocol.integer(it,0,65535)?.toInt() ?: return null }.toIntArray()
        if(facts.indices.any { it !in indices && facts[it]!=0 })return null
        val probe=facts.copyOf().apply {this[17]=-1;this[33]=2}
        if(PartySyncProtocol.numbers("A",probe.joinToString(","))==null)return null
        val c=value.get("caught")?.takeIf { it.isJsonArray && it.asJsonArray.size()==2 }?.asJsonArray ?: return null
        val low=PartySyncProtocol.integer(c[0],0,1073741823)?.toInt() ?: return null
        val high=PartySyncProtocol.integer(c[1],0,127)?.toInt() ?: return null
        observe(reporter,"A",facts,source);observe(reporter,"N",intArrayOf(nests),source);observe(reporter,"S",intArrayOf(0,0,low,high),source)
        frames().single { it.get("reporter").asString==reporter && it.get("source").asString==source }
    }.getOrNull()
}
