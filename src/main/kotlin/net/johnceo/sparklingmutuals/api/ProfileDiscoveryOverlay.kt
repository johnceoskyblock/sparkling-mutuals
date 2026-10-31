package net.johnceo.sparklingmutuals.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/** Observed catches cannot be lost when a cached or delayed API snapshot is read. */
class ProfileDiscoveryOverlay<K> {
    private data class Earned(val species: MutableSet<String> = linkedSetOf(),var totalFloor: Long=0)
    private val earned=mutableMapOf<K,Earned>()
    @Synchronized fun clear() { earned.clear() }
    @Synchronized fun caught(key: K,species: String,baseline: Long?) {
        val value=earned.getOrPut(key) { Earned() }
        value.species.add(SafariProfile.key(species))
        value.totalFloor=maxOf(value.totalFloor,baseline ?: 0)+1
    }
    @Synchronized fun apply(key: K,uuid: String,body: JsonObject): JsonObject {
        val value=earned[key] ?: return body
        val copy=body.deepCopy()
        val profile=CloudProfiles.selectedProfileId(copy) ?: return copy
        val member=CloudProfiles.member(copy,uuid,profile) ?: return copy
        val safari=member.get("safari")?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject().also { member.add("safari",it) }
        val discoveries=SafariProfile.discoveriesFrom(member).map(SafariProfile::key).toSet()+value.species
        safari.add("discovered_sparkling_critters",JsonArray().also { array -> discoveries.forEach(array::add) })
        val apiTotal=safari.get("total_captured_sparkling_critters")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asLong ?: 0L
        safari.addProperty("total_captured_sparkling_critters",maxOf(apiTotal,value.totalFloor))
        return copy
    }
}
