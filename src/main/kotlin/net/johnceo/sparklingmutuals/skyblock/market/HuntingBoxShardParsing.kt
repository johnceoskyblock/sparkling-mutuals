package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRules
import java.util.Locale

internal data class MenuSlot(val index: Int,val itemId: String,val name: String,val lore: List<String>)
internal data class BoxShard(val slot: Int,val productId: String,val amount: Long)
internal object SaleProducts {
    val names=SafariRoster.all.associate {"SHARD_"+it.name.uppercase(Locale.ROOT).replace(' ','_') to it.name}
    fun identity(itemId: String,name: String): String? {
        if(itemId in names)return itemId
        if(itemId!="ATTRIBUTE_SHARD")return null
        val plain=SafariRules.strip(name).trim()
        return names.entries.firstOrNull {plain=="${it.value} Shard"}?.key
    }
}

/** Narrow identity rules from the archived box parser; no archived valuation or aggregation revived. */
internal object HuntingBoxShardParsing {
    private val titles=Regex("^(?:Hunting Box|\\([1-9][0-9]*/[1-9][0-9]*\\) Hunting Box|Hunting Box \\(Page [1-9][0-9]*(?:/| of )[1-9][0-9]*\\))$")
    private val owned=Regex("^Owned: ([0-9][0-9,]*) Shards?$")
    fun isBox(title: String)=titles.matches(SafariRules.strip(title))
    fun parse(title: String,slots: List<MenuSlot>): List<BoxShard> {
        if(!isBox(title))return emptyList()
        return slots.groupBy {it.index}.values.filter {it.size==1}.mapNotNull {group ->
            val slot=group.single()
            if(slot.index !in 9..44 || slot.index%9 in setOf(0,8))return@mapNotNull null
            val product=SaleProducts.identity(slot.itemId,slot.name) ?: return@mapNotNull null
            val amounts=slot.lore.mapNotNull {owned.matchEntire(SafariRules.strip(it).trim())?.groupValues?.get(1)?.replace(",","")?.toLongOrNull()}
            val amount=amounts.singleOrNull()?.takeIf {it>0} ?: return@mapNotNull null
            BoxShard(slot.index,product,amount)
        }
    }
}

internal object SaleProfileEvidence {
    private fun uuid(raw: String?)=raw?.takeIf {Regex("(?:[0-9a-fA-F]{32}|[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12})").matches(it)}?.replace("-","")?.lowercase(Locale.ROOT)
    fun profile(line: String)=Regex("^Profile ID: (\\S+)$").matchEntire(SafariRules.strip(line))?.groupValues?.get(1)?.let(::uuid)
    fun changing(line: String)=Regex("^You are playing on profile: [A-Za-z]+(?: \\(Co-op\\))?$").matches(SafariRules.strip(line))
    fun scope(account: String?,profile: String?): MarketScope? {
        val owner=uuid(account) ?: return null
        return uuid(profile)?.let {MarketScope(owner,it)}
    }
}
