package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal data class OfferEvidence(val productId: String,val unitPrice: Double,val quantity: Long,val completed: Boolean)
internal data class SaleAlert(val productId: String,val message: String)

/** Client-owned, product-level observations. An absent/filled row cannot identify every other offer. */
internal class SellOfferWatch {
    private data class Watch(var seen: Long,var alerted: Long=0)
    private val scopes=linkedMapOf<MarketScope,MutableMap<String,Watch>>()
    private var scope: MarketScope?=null
    private val revalidated=mutableSetOf<String>()
    private val previous=mutableMapOf<String,Recommendation>()

    fun selectScope(next: MarketScope?) {
        val valid=next?.takeIf {SaleProfileEvidence.scope(it.accountId,it.profileId)==it}
        if(valid==scope)return
        scope=valid;revalidated.clear();previous.clear()
        if(valid!=null) {
            val records=scopes.remove(valid) ?: mutableMapOf()
            scopes[valid]=records
            while(scopes.size>4)scopes.remove(scopes.keys.first())
        }
    }
    fun observe(evidence: List<OfferEvidence>,now: Long) {
        val records=scope?.let {scopes[it]} ?: return
        evidence.filter {it.productId in SaleProducts.names && !it.completed && it.quantity>0 && it.unitPrice.isFinite() && it.unitPrice>0}
            .forEach {offer ->
                records.getOrPut(offer.productId) {Watch(now)}.seen=now
                revalidated.add(offer.productId)
            }
        prune(now)
    }
    fun products(now: Long): Set<String> {prune(now);return revalidated.toSet()}
    fun update(advice: Map<String,Recommendation>,now: Long): List<SaleAlert> {
        val records=scope?.let {scopes[it]} ?: return emptyList()
        prune(now)
        val result=mutableListOf<SaleAlert>()
        for(id in revalidated) {
            val watch=records[id] ?: continue
            val current=advice[id]
            if(current==null || current.state==AdviceState.UNKNOWN) {
                previous.remove(id)
                continue
            }
            val before=previous.put(id,current)
            val sameRule=before?.modelKey==current.modelKey
            val drop=before?.state==AdviceState.SELL && current.state==AdviceState.HOLD && sameRule &&
                current.ask?.let {ask -> (current.declineFromAsk ?: before.ask)?.let {ask<it} }==true
            if(!drop || watch.alerted>0 && now-watch.alerted<600_000)continue
            watch.alerted=now
            result.add(SaleAlert(id,BazaarUnlistPresentation.message(setOf(id))!!))
        }
        return result
    }
    private fun prune(now: Long) {
        scopes.values.forEach {records ->records.entries.removeAll {it.value.seen>now+60000 || now-it.value.seen>30*ShardHistoryParsing.DAY}}
        revalidated.retainAll(scope?.let {scopes[it]?.keys}.orEmpty())
        previous.keys.retainAll(revalidated)
    }
    fun export(now: Long): String {
        prune(now)
        return JsonObject().apply {
            addProperty("schema",1)
            add("scopes",JsonArray().apply {scopes.forEach { (owner,records) ->add(JsonObject().apply {
                addProperty("account",owner.accountId);addProperty("profile",owner.profileId)
                add("watches",JsonArray().apply {records.forEach { (id,watch) ->add(JsonArray().apply {
                    add(id);add(watch.seen);add(false);add(watch.alerted) // Retain schema1's obsolete initial-notice field.
                }) }})
            }) }})
        }.toString().also {check(it.toByteArray(Charsets.UTF_8).size<=1024*1024)}
    }
    fun restore(raw: String,now: Long) {
        scope=null;scopes.clear();revalidated.clear();previous.clear()
        if(raw.length>1024*1024 || raw.toByteArray(Charsets.UTF_8).size>1024*1024)return
        runCatching {
            val root=JsonParser.parseString(raw).asJsonObject
            require(root["schema"].asJsonPrimitive.isNumber && root["schema"].asBigDecimal.intValueExact()==1)
            val saved=root.getAsJsonArray("scopes");require(saved.size()<=4)
            saved.forEach {value ->
                val owner=value.asJsonObject
                val selected=SaleProfileEvidence.scope(owner["account"].asString,owner["profile"].asString) ?: error("scope")
                val records=mutableMapOf<String,Watch>()
                val rows=owner.getAsJsonArray("watches");require(rows.size()<=37)
                rows.forEach {row ->
                    val fields=row.asJsonArray;require(fields.size()==4)
                    val id=fields[0].asString;require(id in SaleProducts.names)
                    require(fields[1].asJsonPrimitive.isNumber && fields[2].asJsonPrimitive.isBoolean && fields[3].asJsonPrimitive.isNumber)
                    val seen=fields[1].asBigDecimal.longValueExact();val alerted=fields[3].asBigDecimal.longValueExact()
                    require(seen>0 && alerted>=0 && alerted<=now+60000)
                    records[id]=Watch(seen,alerted) // Legacy initial-notice flag is validated above, then ignored.
                }
                scopes[selected]=records
            }
        }.onFailure {scopes.clear()}
        prune(now)
    }
}
