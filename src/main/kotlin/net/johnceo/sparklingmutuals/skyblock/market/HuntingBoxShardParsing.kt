package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRules
import java.util.Locale

internal data class MenuSlot(val index: Int,val itemId: String,val name: String,val lore: List<String>)
internal data class BoxShard(val slot: Int,val productId: String,val amount: Long)
internal object SaleProducts {
    val names=SafariRoster.all.associate {"SHARD_"+it.name.uppercase(Locale.ROOT).replace(' ','_') to it.name}
    private val menuFooter=Regex("^(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC) [A-Z]+ SHARD \\(ID [A-Z][0-9]+\\)$")
    fun identity(itemId: String,name: String,lore: List<String>): String? {
        val plain=SafariRules.strip(name).trim()
        val named=names.entries.firstOrNull {plain==it.value || plain=="${it.value} Shard"} ?: return itemId.takeIf {it in names}
        if(itemId in names)return itemId.takeIf {it==named.key}
        if(itemId!="" && itemId!="ATTRIBUTE_SHARD")return null
        if(itemId=="ATTRIBUTE_SHARD" && plain=="${named.value} Shard")return named.key
        // Box icons use the short creature label and may omit the carried item's custom ID.
        // Require the server's shard footer; the caller also requires box/slot/owned-count evidence.
        return named.key.takeIf {lore.count {menuFooter.matches(SafariRules.strip(it).trim())}==1}
    }
}

/** Narrow identity rules from the archived box parser; no archived valuation or aggregation revived. */
internal object HuntingBoxShardParsing {
    private val titles=Regex("^(?:Hunting Box|\\([1-9][0-9]*/[1-9][0-9]*\\) Hunting Box|Hunting Box \\(Page [1-9][0-9]*(?:/| of )[1-9][0-9]*\\))$")
    private val owned=Regex("^Owned: ([0-9][0-9,]*) Shards?$")
    fun isBox(title: String)=titles.matches(SafariRules.strip(title))
    fun parse(title: String,slots: List<MenuSlot>): List<BoxShard> = observe(title,slots).filter {it.amount>0}
    fun observe(title: String,slots: List<MenuSlot>): List<BoxShard> {
        if(!isBox(title))return emptyList()
        return slots.groupBy {it.index}.values.filter {it.size==1}.mapNotNull {group ->
            val slot=group.single()
            if(slot.index !in 9..44 || slot.index%9 in setOf(0,8))return@mapNotNull null
            val product=SaleProducts.identity(slot.itemId,slot.name,slot.lore) ?: return@mapNotNull null
            val amounts=slot.lore.mapNotNull {owned.matchEntire(SafariRules.strip(it).trim())?.groupValues?.get(1)?.replace(",","")?.toLongOrNull()}
            val amount=amounts.singleOrNull()?.takeIf {it>=0} ?: return@mapNotNull null
            BoxShard(slot.index,product,amount)
        }
    }
}

internal object SaleProfileEvidence {
    fun profile(message: net.minecraft.network.chat.Component): String? {
        profile(message.string)?.let {return it}
        if(SafariRules.strip(message.string) !in setOf("CLICK THIS TO SUGGEST IT IN CHAT [DASHES]",
            "CLICK THIS TO SUGGEST IT IN CHAT [NO DASHES]"))return null
        val ids=mutableSetOf<String>()
        var invalid=false
        message.visit(net.minecraft.network.chat.FormattedText.StyledContentConsumer<Boolean> {style,text ->
            if(text.isNotEmpty()) {
                val click=style.clickEvent
                if(click!=null) {
                    val id=(click as? net.minecraft.network.chat.ClickEvent.SuggestCommand)?.command()?.let(::uuid)
                    if(id==null)invalid=true else ids.add(id)
                }
            }
            java.util.Optional.empty()
        },net.minecraft.network.chat.Style.EMPTY)
        return ids.singleOrNull()?.takeUnless {invalid}
    }
    private fun uuid(raw: String?)=raw?.takeIf {Regex("(?:[0-9a-fA-F]{32}|[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12})").matches(it)}?.replace("-","")?.lowercase(Locale.ROOT)
    fun profile(line: String)=Regex("^Profile ID: (\\S+)$").matchEntire(SafariRules.strip(line))?.groupValues?.get(1)?.let(::uuid)
    fun changing(line: String)=SafariRules.strip(line).let { text ->
        listOf("You are playing on profile: ","Your profile was changed to: ").any {text.startsWith(it,ignoreCase=true)}
    }
    fun scope(account: String?,profile: String?): MarketScope? {
        val owner=uuid(account) ?: return null
        return uuid(profile)?.let {MarketScope(owner,it)}
    }
}
