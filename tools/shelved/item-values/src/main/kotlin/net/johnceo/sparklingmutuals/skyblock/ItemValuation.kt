/* LGPL-2.1: SkyHanni-style upgrade accounting. See licenses/SkyHanni-Value-NOTICE.txt. */
package net.johnceo.sparklingmutuals.skyblock

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Locale

internal fun JsonObject.obj(key: String)=get(key)?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
internal fun JsonObject.str(key: String)=get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString.orEmpty()
internal fun JsonObject.num(key: String)=runCatching { get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble?.takeIf(Double::isFinite) }.getOrNull() ?: 0.0
internal fun JsonObject.flag(key: String)=num(key)>0 || get(key)?.let { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean && it.asBoolean }==true
internal fun JsonObject.elements(key: String)=get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.toList().orEmpty()
internal fun JsonElement.stringOrNull()=takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

data class ValueItem(val id: String,val name: String,val count: Int=1,val extra: JsonObject=JsonObject(),val lore: List<String> = emptyList())
data class ValuePart(val label: String,val product: String,val amount: Double,val quote: MarketQuote?) {
    val coins get()=quote?.coins?.times(amount)
}
data class ItemEstimate(val item: ValueItem,val parts: List<ValuePart>,val notes: List<String>) {
    val knownUnit get()=parts.sumOf { it.coins ?: 0.0 }
    val total get()=knownUnit*item.count.coerceAtLeast(0)
    val partial get()=parts.any { it.quote==null } || notes.any { it.startsWith("Not priced:") }
    val available get()=parts.any { it.quote!=null }
}
class ValueDefinitions(val items: JsonObject,val reforges: JsonObject,val gems: JsonObject,val essence: JsonObject,val rules: JsonObject) {
    companion object {
        val bundled by lazy {
            fun read(name: String)=ValueDefinitions::class.java.getResourceAsStream("/assets/sparkling-mutuals/valuation/$name")
                ?.bufferedReader()?.use { JsonParser.parseReader(it).asJsonObject } ?: JsonObject()
            ValueDefinitions(read("items.json"),read("reforgestones.json"),read("gemstonecosts.json"),read("essencecosts.json"),read("skyhanni-items.json"))
        }
    }
}
/** Adds replacement costs, rather than pretending that upgrades guarantee the same sale premium. */
class ItemValuation(private val definitions: ValueDefinitions,private val market: Map<String,MarketQuote>,private val bazaar: Map<String,DoubleArray>) {
    private val rarity=listOf("COMMON","UNCOMMON","RARE","EPIC","LEGENDARY","MYTHIC","DIVINE","SPECIAL","VERY_SPECIAL")
    private fun cleanId(id: String)=id.uppercase(Locale.ROOT).replace(' ','_')
    fun quote(id: String): MarketQuote? {
        if(id=="SKYBLOCK_COIN") return MarketQuote(1.0,"Coins")
        val candidates=listOf(id,id.replace(':','-'))
        for(key in candidates) bazaar[key]?.getOrNull(1)?.takeIf { it.isFinite() && it>0 }?.let { return MarketQuote(it,"Bazaar buy") }
        for(key in candidates) market[key]?.let { return it }
        val npc=definitions.items.obj(id).num("npc_sell_price")
        return npc.takeIf { it>0 }?.let { MarketQuote(it,"NPC fallback") }
    }
    fun estimate(item: ValueItem): ItemEstimate {
        val parts=mutableListOf<ValuePart>();val notes=mutableListOf<String>();val tag=item.extra
        fun add(label: String,product: String,amount: Double=1.0) {
            if(product.isBlank() || amount<=0 || !amount.isFinite()) return
            parts.add(ValuePart(label,product,amount,quote(product)))
        }
        fun integer(key: String,max: Int=100)=tag.num(key).toInt().coerceIn(0,max)
        val crimsonTiers=listOf("HOT","BURNING","FIERY","INFERNAL")
        val tier=crimsonTiers.indexOfFirst { item.id.startsWith(it+"_") && definitions.rules.elements("crimson_armors").any { armor -> armor.stringOrNull().equals(item.id.substringAfter('_').substringBefore('_'),true) } }
        val baseId=if(tier>=0) item.id.substringAfter('_') else item.id
        val metadata=definitions.items.obj(item.id)
        val pet=runCatching { JsonParser.parseString(tag.str("petInfo")).asJsonObject }.getOrNull()
        val isBook=item.id=="ENCHANTED_BOOK" || item.id.startsWith("ENCHANTED_BOOK_BUNDLE_")
        if(pet!=null) {
            val type=pet.str("type");val tierName=pet.str("tier");val tierIndex=rarity.indexOf(tierName)
            val level=Regex("""\[Lvl (\d+)]""").find(item.name)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val bracket=if(level>=200) 200 else if(level>=100) 100 else 1
            val neu=type+";"+tierIndex+(if(bracket>1) "+$bracket" else "")
            val generic="LVL_${bracket}_${tierName}_$type"
            val skinned=generic+"_SKINNED_"+pet.str("skin")
            val product=when { pet.str("skin").isNotEmpty() && market.containsKey(skinned) -> skinned
                market.containsKey(neu) -> neu;else -> generic }
            add("Base pet",product)
            if(level !in setOf(1,100,200)) notes.add("Not priced: exact pet level premium (uses level $bracket market floor)")
            if(pet.str("skin").isNotEmpty() && product!=skinned) add("Pet skin","PET_SKIN_"+pet.str("skin"))
            pet.str("heldItem").takeIf { it.isNotEmpty() }?.let { add("Held pet item",it) }
            val candy=pet.num("candyUsed").toInt()
            if(candy>0) notes.add("Not priced: candy / collector discount")
        } else if(!isBook) {
            val base=when {
                item.id=="RUNE" -> tag.obj("runes").entrySet().firstOrNull()?.let { "${it.key.uppercase(Locale.ROOT)}_RUNE;${it.value.asInt}" } ?: item.id
                item.id=="ATTRIBUTE_SHARD" -> "SHARD_"+item.name.replace(Regex("§."),"").removeSuffix(" Shard").uppercase(Locale.ROOT).replace(' ','_')
                else -> baseId
            }
            add("Base item",base)
        }
        if(tier>=0) {
            for(index in 0..tier) {
                val prestigeCosts=definitions.rules.obj("crimson_prestige_costs").obj(crimsonTiers[index])
                prestigeCosts.entrySet().forEach { (id,amount) -> if(amount.isJsonPrimitive && amount.asJsonPrimitive.isNumber) add("${crimsonTiers[index].lowercase()} prestige",id,amount.asDouble) }
                val prior=if(index==0) baseId else crimsonTiers[index-1]+"_"+baseId
                val priorCosts=definitions.items.obj(prior).elements("upgrade_costs")
                priorCosts.forEach { cost -> costs(cost,parts,"Previous tier stars") }
            }
        }
        val modifier=tag.str("modifier")
        if(modifier.isNotEmpty()) {
            val stone=definitions.reforges.entrySet().firstOrNull { (_,value) -> value.isJsonObject && value.asJsonObject.str("reforgeName").equals(modifier,true) }
            if(stone!=null) {
                add("Reforge stone",stone.key)
                val baseTier=definitions.items.obj(baseId).str("tier")
                val currentTier=rarity.getOrNull((rarity.indexOf(baseTier).coerceAtLeast(0)+integer("rarity_upgrades",1)).coerceAtMost(rarity.lastIndex)) ?: baseTier
                val fee=stone.value.asJsonObject.obj("reforgeCosts").num(currentTier)
                add("Reforge application","SKYBLOCK_COIN",fee)
            }
        }
        val once=mapOf("rarity_upgrades" to "RECOMBOBULATOR_3000","jalapeno_count" to "JALAPENO_BOOK",
            "wood_singularity_count" to "WOOD_SINGULARITY","divan_powder_coating" to "DIVAN_POWDER_COATING",
            "mithril_infusion" to "MITHRIL_INFUSION","free_will" to "FREE_WILL","art_of_war_count" to "THE_ART_OF_WAR",
            "artOfPeaceApplied" to "THE_ART_OF_PEACE")
        once.forEach { (field,id) -> if(tag.flag(field)) add(label(id),id) }
        if(tag.has("stats_book")) add("Book of Stats","BOOK_OF_STATS")
        if(tag.flag("ethermerge")) { add("Etherwarp conduit","ETHERWARP_CONDUIT");add("Etherwarp merger","ETHERWARP_MERGER") }
        val hpb=integer("hot_potato_count",15)
        add("Hot Potato Books","HOT_POTATO_BOOK",minOf(10,hpb).toDouble())
        add("Fuming Potato Books","FUMING_POTATO_BOOK",(hpb-10).coerceAtLeast(0).toDouble())
        mapOf("wet_book_count" to "WET_BOOK","farming_for_dummies_count" to "FARMING_FOR_DUMMIES",
            "levelable_overclocks" to "OVERCLOCKER_3000","polarvoid" to "POLARVOID_BOOK","sack_pss" to "POCKET_SACK_IN_A_SACK",
            "bookworm_books" to "BOOKWORM_BOOK","tuned_transmission" to "TRANSMISSION_TUNER","mana_disintegrator_count" to "MANA_DISINTEGRATOR").forEach { (key,id) -> add(label(id),id,integer(key).toDouble()) }
        val starCount=if(tag.has("upgrade_level")) integer("upgrade_level",100) else integer("dungeon_item_level",10)
        if(starCount>0) {
            val dungeon=metadata.flag("dungeon_item") || item.lore.any { it.contains("DUNGEON ") }
            val normal=if(dungeon) minOf(5,starCount) else starCount
            val prices=metadata.elements("upgrade_costs")
            if(prices.isNotEmpty()) prices.take(normal).forEach { costs(it,parts,"Stars") }
            else {
                val legacy=definitions.essence.obj(item.id)
                if(legacy.entrySet().isEmpty()) notes.add("Not priced: star costs for ${item.id}")
                else for(star in 1..normal) {
                    add("Star essence","ESSENCE_"+legacy.str("type").uppercase(Locale.ROOT),legacy.num(star.toString()))
                    legacy.obj("items").elements(star.toString()).mapNotNull { it.stringOrNull() }.forEach { ingredient(it,"Star materials",parts) }
                }
            }
            if(dungeon) listOf("FIRST","SECOND","THIRD","FOURTH","FIFTH").take((starCount-5).coerceIn(0,5)).forEach { add("Master star","${it}_MASTER_STAR") }
        }
        for(key in listOf("power_ability_scroll","drill_part_upgrade_module","drill_part_engine","drill_part_fuel_tank","dye_item"))
            tag.str(key).takeIf { it.isNotEmpty() }?.let { add(label(key),cleanId(it)) }
        tag.str("skin").takeIf { it.isNotEmpty() }?.let { add("Skin",if(it.startsWith("PET_SKIN_")) it else if(quote(it)!=null) it else "${item.id}_SKIN_"+it) }
        tag.str("talisman_enrichment").takeIf { it.isNotEmpty() }?.let { add("Enrichment","TALISMAN_ENRICHMENT_"+cleanId(it)) }
        for(part in listOf("hook","line","sinker")) tag.obj(part).str("part").takeIf(String::isNotEmpty)?.let { add("Rod $part",it) }
        tag.elements("ability_scroll").mapNotNull { it.stringOrNull() }.forEach { add("Ability scroll",it) }
        tag.elements("boosters").mapNotNull { it.stringOrNull() }.forEach { add("Booster",if(it.endsWith("_BOOSTER")) it else it+"_BOOSTER") }
        val runes=tag.obj("runes")
        if(item.id!="RUNE") runes.entrySet().forEach { (rune,level) -> if(level.isJsonPrimitive && level.asJsonPrimitive.isNumber) add("Rune","${rune.uppercase(Locale.ROOT)}_RUNE;${level.asInt}") }
        val gemTags=tag.obj("gems")
        val types=setOf("RUBY","AMETHYST","JADE","SAPPHIRE","AMBER","TOPAZ","JASPER","OPAL","AQUAMARINE","CITRINE","ONYX","PERIDOT")
        gemTags.entrySet().filter { it.key!="unlocked_slots" && !it.key.endsWith("_gem") }.forEach { (slot,value) ->
            val quality=value.stringOrNull() ?: value.takeIf { it.isJsonObject }?.asJsonObject?.str("quality").orEmpty()
            val type=slot.substringBefore('_').takeIf { it in types } ?: gemTags.str(slot+"_gem")
            if(quality in setOf("ROUGH","FLAWED","FINE","FLAWLESS","PERFECT") && type in types) add("$quality $type gem","${quality}_${type}_GEM")
        }
        val unlocked=gemTags.elements("unlocked_slots").mapNotNull { it.stringOrNull() }.toMutableSet()
        gemTags.str("unlocked_slots").split(',').map(String::trim).filter(String::isNotEmpty).forEach(unlocked::add)
        val slotCosts=definitions.gems.obj(item.id)
        unlocked.forEach { slot -> slotCosts.elements(slot).mapNotNull { it.stringOrNull() }.forEach { ingredient(it,"Gem slot unlock",parts) } }
        if(unlocked.isNotEmpty() && slotCosts.entrySet().isEmpty()) notes.add("Not priced: gemstone slot unlock costs")
        enchantments(item,parts,notes)
        if(tag.obj("attributes").entrySet().isNotEmpty() && item.id!="ATTRIBUTE_SHARD") notes.add("Not priced: attribute roll premiums")
        if(item.lore.any { it.contains("SOULBOUND",true) } || tag.flag("donated_museum")) notes.add("Soulbound: replacement cost, not sale proceeds")
        if(parts.any { it.quote?.source=="NPC fallback" }) notes.add("Not priced: market quote unavailable for NPC fallback components")
        return ItemEstimate(item,parts,notes.distinct())
    }
    private fun enchantments(item: ValueItem,parts: MutableList<ValuePart>,notes: MutableList<String>) {
        val enchants=item.extra.obj("enchantments")
        val rules=definitions.rules.obj("value_calculation_data")
        val tierOne=rules.elements("only_tier_one_prices").mapNotNull { it.stringOrNull() }.toSet()
        val tierFive=rules.elements("only_tier_five_prices").mapNotNull { it.stringOrNull() }.toSet()
        val stacking=setOf("cultivating","champion","compact","expertise","hecatomb","toxophilite")
        enchants.entrySet().forEach { (name,raw) ->
            val level=runCatching { raw.asInt }.getOrNull()?.coerceIn(1,100) ?: return@forEach
            if(name=="efficiency" && item.id!="ENCHANTED_BOOK") {
                val silex=(level-if(item.id=="STONK_PICKAXE") 6 else 5).coerceIn(0,5)
                if(silex>0) parts.add(ValuePart("Silex","SIL_EX",silex.toDouble(),quote("SIL_EX")))
                return@forEach
            }
            val innate=rules.obj("always_active_enchants").obj(name)
            if(innate.num("level").toInt()==level && innate.elements("items").any { it.stringOrNull()==item.id }) return@forEach
            var paidLevel=if(name in stacking) 1 else level
            var count=1.0
            val caps=rules.obj("endcap_enchants_new").elements(name)
            caps.mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }.forEach { cap ->
                if(level>cap.num("required_level")) {
                    val product=cap.str("endcap_item")
                    parts.add(ValuePart("Enchantment endcap",product,1.0,quote(product)))
                    paidLevel=minOf(paidLevel,cap.num("required_level").toInt())
                }
            }
            when {
                name in tierOne && paidLevel in 2..5 -> { count=Math.pow(2.0,(paidLevel-1).toDouble());paidLevel=1 }
                name in tierFive && paidLevel in 6..10 -> { count=Math.pow(2.0,(paidLevel-5).toDouble());paidLevel=5 }
            }
            if(item.id.startsWith("ENCHANTED_BOOK_BUNDLE_")) count=definitions.rules.obj("book_bundle_amount").num(name).takeIf { it>0 } ?: 5.0
            var product="ENCHANTMENT_${name.uppercase(Locale.ROOT)}_$paidLevel"
            var price=quote(product)
            // Legal anvil combinations are available when the precise tier has no order book.
            if(price==null && paidLevel<=5) for(lower in paidLevel-1 downTo 1) {
                val candidate="ENCHANTMENT_${name.uppercase(Locale.ROOT)}_$lower"
                quote(candidate)?.let { product=candidate;price=it;count*=Math.pow(2.0,(paidLevel-lower).toDouble()) }
                if(price!=null) break
            }
            parts.add(ValuePart("${label(name)} $level",product,count,price))
        }
    }
    private fun costs(costs: JsonElement,parts: MutableList<ValuePart>,label: String) {
        if(!costs.isJsonArray) return
        costs.asJsonArray.filter { it.isJsonObject }.forEach { raw ->
            val cost=raw.asJsonObject
            val product=when(cost.str("type")) { "COINS" -> "SKYBLOCK_COIN";"ESSENCE" -> "ESSENCE_"+cost.str("essence_type");else -> cost.str("item_id") }
            val count=if(cost.str("type")=="COINS") cost.num("coins") else cost.num("amount")
            if(product.isNotEmpty() && count>0) parts.add(ValuePart(label,product,count,quote(product)))
        }
    }
    private fun ingredient(raw: String,label: String,parts: MutableList<ValuePart>) {
        val index=raw.lastIndexOf(':');if(index<1) return
        val amount=raw.substring(index+1).toDoubleOrNull()?.takeIf { it>0 && it.isFinite() } ?: return
        val id=raw.substring(0,index);parts.add(ValuePart(label,id,amount,quote(id)))
    }
    private fun label(id: String)=id.lowercase(Locale.ROOT).replace('_',' ').replaceFirstChar(Char::uppercase)
}
