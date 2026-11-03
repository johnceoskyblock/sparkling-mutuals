package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.safari.SafariRules

/** Field grammar researched in SkyHanni 07716a4 BazaarOrderApi; independent narrow implementation.
 * Detail/cancel/claim icons are deliberately not interpreted as order ownership or cancellation. */
internal object SellOfferParsing {
    fun parse(title: String,slots: List<MenuSlot>,localName: String): List<OfferEvidence> {
        val screen=SafariRules.strip(title)
        if(screen !in setOf("Your Bazaar Orders","Co-op Bazaar Orders") || localName.isBlank())return emptyList()
        return slots.groupBy {it.index}.values.filter {it.size==1}.mapNotNull { group ->
            val row=group.single()
            if(row.index !in 0..53 || !Regex("^(?:§[0-9a-fk-or])*§lSELL .+$").matches(row.name))return@mapNotNull null
            val name=SafariRules.strip(row.name).removePrefix("SELL ")
            val id=SaleProducts.names.entries.firstOrNull {name=="${it.value} Shard"}?.key ?: return@mapNotNull null
            if(row.itemId.isNotEmpty() && row.itemId!="ATTRIBUTE_SHARD" && row.itemId!=id)return@mapNotNull null
            val lore=row.lore.map {SafariRules.strip(it).trim()}
            val owners=lore.filter {it.startsWith("By:")}
            val owner=owners.singleOrNull()?.let {Regex("^By: (?:\\[[^\\]]+\\] )?([A-Za-z0-9_]{1,16})$").matchEntire(it)?.groupValues?.get(1)}
            if(owners.isNotEmpty() && (owners.size!=1 || owner!=localName) || screen=="Co-op Bazaar Orders" && owner!=localName)return@mapNotNull null
            val amounts=lore.filter {it.startsWith("Offer amount:")}
            val quantity=amounts.singleOrNull()?.let {Regex("^Offer amount: ([0-9][0-9,]*)x$").matchEntire(it)?.groupValues?.get(1)?.replace(",","")?.toLongOrNull()}?.takeIf {it>0} ?: return@mapNotNull null
            val prices=lore.filter {it.startsWith("Price per unit:")}
            val price=prices.singleOrNull()?.let {Regex("^Price per unit: ([0-9][0-9,]*(?:\\.[0-9]+)?) coins$").matchEntire(it)?.groupValues?.get(1)?.replace(",","")?.toDoubleOrNull()}?.takeIf {it.isFinite() && it>0} ?: return@mapNotNull null
            val completed=lore.any {Regex("^Filled: .+ 100%!$").matches(it)}
            OfferEvidence(id,price,quantity,completed)
        }
    }
}
