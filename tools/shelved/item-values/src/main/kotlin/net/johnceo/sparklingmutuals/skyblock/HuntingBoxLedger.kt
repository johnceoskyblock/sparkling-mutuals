package net.johnceo.sparklingmutuals.skyblock

import net.johnceo.sparklingmutuals.safari.SafariRules

/** Inventory observations only: no profile requests and no changes to the server's box. */
data class BoxShard(val id: String, val name: String, val amount: Long, val slot: Int = -1)
data class BoxPage(val key: String, val totalPages: Int?, val shards: List<BoxShard>)
data class BoxValue(val shard: BoxShard, val sell: Double?, val buy: Double?)
class HuntingBoxLedger {
    private val pages=linkedMapOf<String,BoxPage>()
    val scannedPages get()=pages.size
    val totalPages get()=pages.values.mapNotNull { it.totalPages }.maxOrNull()
    fun observe(page: BoxPage) { pages.remove(page.key);pages[page.key]=page }
    /** A shard belongs once, even if a changed sort/filter places it on another visited page. */
    fun shards(): List<BoxShard> {
        val unique=linkedMapOf<String,BoxShard>()
        pages.values.forEach { page -> page.shards.forEach { unique[it.id]=it } }
        return unique.values.toList()
    }
    fun values(prices: Map<String,DoubleArray>): List<BoxValue> = shards().filter { it.amount>0 }.map { shard ->
        fun quote(index: Int)=prices[shard.id]?.getOrNull(index)?.takeIf { it.isFinite() && it>0 }?.times(shard.amount)
        BoxValue(shard,quote(0),quote(1))
    }.sortedWith(compareByDescending<BoxValue> { it.sell ?: -1.0 }.thenBy { it.shard.name })
    fun clear()=pages.clear()
}
object HuntingBoxParsing {
    private val owned=Regex("""^Owned: ([\d,]+) Shards?$""",RegexOption.IGNORE_CASE)
    private val page=Regex("""(?:^|[ (])Page\s*:?\s*(\d+)\s*(?:/|of)\s*(\d+)(?:[ )]|$)""",RegexOption.IGNORE_CASE)
    private val shortPage=Regex("""^\((\d+)/(\d+)\) Hunting Box$""",RegexOption.IGNORE_CASE)
    fun shard(slot: Int,id: String,name: String,lore: List<String>): BoxShard? {
        if (slot !in 9..44 || slot%9 in setOf(0,8)) return null
        val cleanName=SafariRules.strip(name).trim()
        val product=when {
            Regex("^SHARD_[A-Z0-9_]+$").matches(id) -> id
            id=="ATTRIBUTE_SHARD" && Regex("""^[A-Za-z][A-Za-z0-9 '-]+ Shard$""").matches(cleanName) ->
                "SHARD_"+cleanName.removeSuffix(" Shard").uppercase(java.util.Locale.ROOT).replace(' ','_')
            else -> return null
        }
        val amount=lore.firstNotNullOfOrNull { line -> owned.matchEntire(SafariRules.strip(line).trim())
            ?.groupValues?.get(1)?.replace(",","")?.toLongOrNull() } ?: return null
        if (amount<0) return null
        return BoxShard(product,cleanName,amount,slot)
    }
    fun page(title: String, navigation: List<String>, shards: List<BoxShard>, navigatedPage: Int? = null): BoxPage {
        val labels=listOf(title)+navigation
        val match=labels.firstNotNullOfOrNull { page.find(SafariRules.strip(it)) ?: shortPage.matchEntire(SafariRules.strip(it)) }
        val number=match?.groupValues?.get(1)?.toIntOrNull()
        val total=match?.groupValues?.get(2)?.toIntOrNull()?.takeIf { it in 1..1000 }
        val key=when {
            number!=null && total!=null && number in 1..total -> "page:$number"
            navigatedPage!=null -> "page:$navigatedPage"
            else -> "contents:"+shards.map { it.id }.sorted().joinToString("|")
        }
        return BoxPage(key,total,shards)
    }
    fun isBox(title: String)=SafariRules.strip(title).let { it=="Hunting Box" || shortPage.matches(it) ||
        Regex("""^Hunting Box \(Page \d+(?:/| of )\d+\)$""").matches(it) }
}
