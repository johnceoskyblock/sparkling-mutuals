package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal data class WholeBoxOwned(val productId: String,val amount: Long,val observedAt: Long)
internal data class WholeBoxCoverage(val visited: Int=0,val total: Int?=null,val complete: Boolean=false,
    val older: Int=0,val unsupported: Int=0,val limited: Boolean=false)
internal class WholeBoxLedger {
    private data class Memory(val items: MutableMap<String,WholeBoxOwned> = linkedMapOf(),var touched: Boolean=false)
    private val scopes=linkedMapOf<MarketScope,Memory>()
    private var selected: MarketScope?=null
    private var connection: Any?=null
    private var temporary=Memory()
    private var view: String?=null
    private var filtered=false
    private var total: Int?=null
    private var limited=false
    private var loading=false
    private val refreshed=mutableSetOf<String>()
    private val pages=mutableMapOf<String,Int>()
    private fun current()=selected?.let {scopes.getValue(it)} ?: temporary
    fun select(scope: MarketScope?,connection: Any?) {
        val valid=scope?.let {SaleProfileEvidence.scope(it.accountId,it.profileId)}
        if(valid==selected && this.connection===connection)return
        selected=valid;this.connection=connection;temporary=Memory();beginOpening()
        if(valid!=null) {val memory=scopes.remove(valid) ?: Memory();scopes[valid]=memory;trim()}
    }
    fun beginOpening() {view=null;filtered=false;total=null;limited=false;loading=false;refreshed.clear();pages.clear()}
    fun observe(page: WholeBoxPage,now: Long) {
        if(now<=0)return
        val expected=page.totalPages?.takeIf {it in 1..1000 && page.page!=null && page.page in 1..it}
        if(view!=null && (view!=page.view || total!=expected || filtered!=page.filtered))beginOpening()
        view=page.view;total=expected;filtered=page.filtered
        loading=page.shards.isEmpty() && page.unsupported==0
        if(loading)return // View changes still apply, but packet gaps do not establish zero or scanned coverage.
        val memory=current();memory.touched=true
        for(shard in page.shards) {
            if(!WholeBoxParsing.validId(shard.productId) || shard.amount<0)continue
            if(shard.productId !in memory.items && memory.items.size>=512) {limited=true;continue}
            if(now<(memory.items[shard.productId]?.observedAt ?: 0))continue
            // Keep bounded zero tombstones during this process so older observations cannot resurrect them.
            memory.items[shard.productId]=WholeBoxOwned(shard.productId,shard.amount,now)
            refreshed.add(shard.productId)
        }
        val key=if(expected!=null)"page:${page.page}"else "contents:"+page.shards.map {it.productId}.sorted().joinToString("|").hashCode()
        if(pages.size<1000 || key in pages)pages[key]=page.unsupported.coerceAtLeast(0)else limited=true
    }
    fun rows(): List<WholeBoxOwned> = current().items.values.filter {it.amount>0}.toList()
    fun coverage(): WholeBoxCoverage {
        val older=rows().count {it.productId !in refreshed}
        val unknown=pages.values.sumOf {it.toLong()}.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return WholeBoxCoverage(pages.size,total,total!=null && pages.size==total && !filtered &&
            older==0 && unknown==0 && !limited && !loading,older,unknown,limited)
    }
    fun export(): String = JsonObject().apply {
        addProperty("schema",1)
        add("scopes",JsonArray().apply {scopes.forEach {(scope,memory)->add(JsonObject().apply {
            addProperty("account",scope.accountId);addProperty("profile",scope.profileId)
            add("shards",JsonArray().apply {memory.items.values.filter {it.amount>0}.forEach {add(JsonArray().apply {
                add(it.productId);add(it.amount);add(it.observedAt)
            })}})
        })}})
    }.toString().also {check(it.toByteArray().size<=LIMIT)}
    fun restore(raw: String,now: Long) {
        val restored=runCatching {
            require(raw.length<=LIMIT && raw.toByteArray().size<=LIMIT)
            val root=JsonParser.parseString(raw).asJsonObject
            require(root["schema"].asJsonPrimitive.isNumber && root["schema"].asBigDecimal.intValueExact()==1)
            val records=root.getAsJsonArray("scopes");require(records.size()<=4)
            val result=linkedMapOf<MarketScope,Memory>()
            records.forEach {record ->
                val value=record.asJsonObject
                require(value["account"].asJsonPrimitive.isString && value["profile"].asJsonPrimitive.isString)
                val scope=SaleProfileEvidence.scope(value["account"].asString,value["profile"].asString) ?: error("scope")
                require(scope !in result)
                val entries=value.getAsJsonArray("shards");require(entries.size()<=512)
                val memory=Memory()
                entries.forEach {row ->
                    val fields=row.asJsonArray;require(fields.size()==3 && fields[0].asJsonPrimitive.isString &&
                        fields[1].asJsonPrimitive.isNumber && fields[2].asJsonPrimitive.isNumber)
                    val id=fields[0].asString;val amount=fields[1].asBigDecimal.longValueExact();val at=fields[2].asBigDecimal.longValueExact()
                    require(WholeBoxParsing.validId(id) && id !in memory.items && amount>0 && at>0 && at<=now)
                    memory.items[id]=WholeBoxOwned(id,amount,at)
                }
                result[scope]=memory
            }
            result
        }.getOrNull() ?: return
        restored.forEach {(scope,memory)->if(scopes[scope]?.touched!=true)scopes[scope]=memory}
        selected?.let {scope->scopes.remove(scope)?.let {scopes[scope]=it}}
        trim()
    }
    private fun trim() {while(scopes.size>4)scopes.remove(scopes.keys.first())}
    companion object {const val LIMIT=512*1024}
}
