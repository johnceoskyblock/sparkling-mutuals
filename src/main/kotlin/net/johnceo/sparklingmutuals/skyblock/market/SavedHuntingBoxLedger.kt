package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal data class SavedBoxShard(val productId: String,val amount: Long,val observedAt: Long)
internal data class SavedBoxRow(val shard: SavedBoxShard,val refreshedThisOpening: Boolean,val advice: Recommendation)

/** Client-thread ownership observations only; never persists prices or unverified profile identities. */
internal class SavedHuntingBoxLedger(allowedIds: Set<String>) {
    private val allowed=allowedIds.toSet().also {require(it.size<=37 && it.all {id->id in SaleProducts.names})}
    private data class PageMemory(val shards: MutableMap<String,SavedBoxShard> = linkedMapOf(),var touched: Boolean=false)
    private val scopes=linkedMapOf<MarketScope,PageMemory>()
    private var selected: MarketScope?=null
    private var connection: Any?=null
    private var temporary=PageMemory()
    private val refreshed=mutableSetOf<String>()

    fun select(scope: MarketScope?,connection: Any?) {
        val verified=scope?.let {SaleProfileEvidence.scope(it.accountId,it.profileId)}
        if(verified==selected && connection===this.connection)return
        selected=verified;this.connection=connection;refreshed.clear();temporary=PageMemory()
        if(verified!=null) {
            val memory=scopes.remove(verified) ?: PageMemory()
            scopes[verified]=memory
            trim()
        }
    }
    fun beginOpening() {refreshed.clear()}
    private fun current()=selected?.let {scopes.getValue(it)} ?: temporary
    fun observe(shards: List<BoxShard>,now: Long) {
        if(now<=0)return
        val accepted=shards.filter {it.productId in allowed && it.amount>=0}.groupBy {it.productId}
            .values.mapNotNull {group->group.first().takeIf {group.map {it.amount}.distinct().size==1}}
        if(accepted.isEmpty())return
        val memory=current();memory.touched=true
        for(shard in accepted) {
            if(now<(memory.shards[shard.productId]?.observedAt ?: 0))continue
            if(shard.amount==0L)memory.shards.remove(shard.productId)
            else memory.shards[shard.productId]=SavedBoxShard(shard.productId,shard.amount,now)
            refreshed.add(shard.productId)
        }
    }
    fun rows(): List<Pair<SavedBoxShard,Boolean>> = current().shards.values
        .sortedBy {SaleProducts.names.getValue(it.productId)}.map {it to (it.productId in refreshed)}

    fun export(): String {
        val records=JsonArray()
        for((scope,memory) in scopes)records.add(JsonObject().apply {
            addProperty("account",scope.accountId);addProperty("profile",scope.profileId)
            add("shards",JsonArray().apply {memory.shards.values.forEach {shard->add(JsonArray().apply {
                add(shard.productId);add(shard.amount);add(shard.observedAt)
            })}})
        })
        return JsonObject().apply {addProperty("schema",1);add("scopes",records)}.toString()
    }
    fun restore(raw: String,now: Long) {
        val restored=runCatching {
            require(raw.toByteArray(Charsets.UTF_8).size<=SavedHuntingBoxStore.LIMIT)
            val root=JsonParser.parseString(raw).asJsonObject
            require(root.getAsJsonPrimitive("schema").isNumber && root["schema"].asBigDecimal.intValueExact()==1)
            val records=root.getAsJsonArray("scopes");require(records.size()<=4)
            val result=linkedMapOf<MarketScope,PageMemory>()
            for(record in records) {
                val value=record.asJsonObject
                require(value.getAsJsonPrimitive("account").isString && value.getAsJsonPrimitive("profile").isString)
                val scope=SaleProfileEvidence.scope(value["account"].asString,value["profile"].asString) ?: error("Invalid scope")
                require(scope !in result)
                val rows=value.getAsJsonArray("shards");require(rows.size()<=37)
                val memory=PageMemory()
                for(row in rows) {
                    val fields=row.asJsonArray;require(fields.size()==3 && fields[0].asJsonPrimitive.isString &&
                        fields[1].asJsonPrimitive.isNumber && fields[2].asJsonPrimitive.isNumber)
                    val id=fields[0].asString;val amount=fields[1].asBigDecimal.longValueExact();val at=fields[2].asBigDecimal.longValueExact()
                    require(id in allowed && id !in memory.shards && amount>0 && at>0 && at<=now)
                    memory.shards[id]=SavedBoxShard(id,amount,at)
                }
                result[scope]=memory
            }
            result
        }.getOrNull() ?: return
        // Preserve live observations (including deletions) made while the worker was loading disk data.
        for((scope,memory) in restored)if(scopes[scope]?.touched!=true)scopes[scope]=memory
        selected?.let {scope->scopes.remove(scope)?.let {scopes[scope]=it}}
        trim()
    }
    private fun trim() {while(scopes.size>4)scopes.remove(scopes.keys.first())}
}
