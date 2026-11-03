package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.safari.SafariRules
import java.util.Locale
import java.security.MessageDigest

internal data class WholeBoxPage(val shards: List<BoxShard>,val page: Int?,val totalPages: Int?,
    val view: String="",val filtered: Boolean=false,val unsupported: Int=0)
internal object WholeBoxParsing {
    private val product=Regex("^SHARD_[A-Z0-9]+(?:_[A-Z0-9]+)*$")
    private val owned=Regex("^Owned: ([0-9][0-9,]*) Shards?$")
    private val footer=Regex("^(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|VERY SPECIAL) [A-Z]+ SHARD \\(ID [A-Z][0-9]+\\)$")
    private val shortPage=Regex("^\\(([0-9]+)/([0-9]+)\\) Hunting Box$")
    private val longPage=Regex("^Hunting Box \\(Page ([0-9]+)(?:/| of )([0-9]+)\\)$")
    fun validId(id: String)=id.length<=96 && product.matches(id)
    private fun clean(text: String)=SafariRules.strip(text).trim()
    private fun named(name: String)="SHARD_"+clean(name).removeSuffix(" Shard").uppercase(Locale.ROOT).replace(' ','_')
    private fun inner(index: Int)=index in 9..44 && index%9 !in setOf(0,8)
    fun observe(title: String,slots: List<MenuSlot>,catalog: Set<String>): WholeBoxPage? {
        if(!HuntingBoxShardParsing.isBox(title))return null
        var unsupported=0
        val items=slots.groupBy {it.index}.values.mapNotNull {group ->
            if(!inner(group.first().index))return@mapNotNull null
            val slot=group.singleOrNull()
            if(slot==null) {unsupported++;return@mapNotNull null}
            val lore=slot.lore.map(::clean)
            val counts=lore.mapNotNull {owned.matchEntire(it)?.groupValues?.get(1)?.replace(",","")?.toLongOrNull()}
            if(counts.isEmpty()) {
                if(lore.any {it.startsWith("Owned:")} || lore.any {footer.matches(it)})unsupported++
                return@mapNotNull null
            }
            val candidate=named(slot.name)
            val id=when {
                validId(slot.itemId) && (candidate==slot.itemId || candidate !in catalog) -> slot.itemId
                slot.itemId in setOf("","ATTRIBUTE_SHARD") && lore.count {footer.matches(it)}==1 &&
                    validId(candidate) && candidate in catalog -> candidate
                else -> null
            }
            if(id==null || counts.size!=1 || lore.count {it.startsWith("Owned:")}!=1) {unsupported++;return@mapNotNull null}
            BoxShard(slot.index,id,counts.single())
        }.groupBy {it.productId}.values.mapNotNull {group ->
            if(group.map {it.amount}.distinct().size==1)group.first()else {unsupported++;null}
        }
        val controls=slots.filter {!inner(it.index) && it.index in 0..53}.filter {slot ->
            val name=clean(slot.name).lowercase(Locale.ROOT)
            listOf("filter","sort","search").any {it in name}
        }.sortedBy {it.index}.flatMap {listOf(clean(it.name))+it.lore.map(::clean)}
        val filtered=controls.any {it.startsWith("Query: ") && it.removePrefix("Query: ").isNotBlank()} ||
            controls.any {line -> Regex("^(?:Filter|Category|Rarity|Biome|Type)(?: by)?: (.+)$",RegexOption.IGNORE_CASE)
                .matchEntire(line)?.groupValues?.get(1)?.lowercase(Locale.ROOT)?.let {it !in setOf("all","none","any","no filter")}==true}
        val signature=MessageDigest.getInstance("SHA-256").digest(controls.joinToString("\n").toByteArray())
            .joinToString("") {"%02x".format(it)}
        val match=shortPage.matchEntire(clean(title)) ?: longPage.matchEntire(clean(title))
        val count=match?.groupValues?.get(2)?.toIntOrNull()?.takeIf {it in 1..1000}
        val index=match?.groupValues?.get(1)?.toIntOrNull()?.takeIf {count!=null && it in 1..count}
        return WholeBoxPage(items,index,count.takeIf {index!=null},signature,filtered,unsupported)
    }
}
