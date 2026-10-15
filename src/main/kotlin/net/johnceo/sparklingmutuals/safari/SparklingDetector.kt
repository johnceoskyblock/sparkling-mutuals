package net.johnceo.sparklingmutuals.safari

import com.google.gson.JsonParser
import java.util.UUID

/** Immutable hashes supplied by the linked sheet; unresolved/ordinary skins are never negatively cached. */
object SparklingSkins {
    val textures: Map<String, String> by lazy {
        javaClass.getResourceAsStream("/assets/sparkling-mutuals/data/sparkling_skins.json")!!.bufferedReader().use {
            JsonParser.parseReader(it).asJsonObject.getAsJsonObject("textures").entrySet().associate { entry ->
                entry.key to entry.value.asString
            }
        }
    }
    private val byTexture by lazy { textures.entries.associate { it.value to it.key } }
    fun species(texture: String?) = texture?.let(byTexture::get)
}

data class SparklingEntity(val id: Int, val uuid: UUID, val x: Double, val y: Double, val z: Double,
    val name: String? = null, val texture: String? = null, val realPlayer: Boolean = false,
    val skinCarrier: Boolean = false, val body: Boolean = false)
data class SparklingDetection(val species: String, val body: Int, val label: Int, val aliases: Set<UUID>)

/** One positive-identification path feeds alerts, glow, nearby HUD and capture requirements. */
object SparklingDetector {
    fun detect(entities: List<SparklingEntity>): List<SparklingDetection> {
        fun skin(e: SparklingEntity): String? {
            if (!e.skinCarrier || e.realPlayer) return null
            val species = SparklingSkins.species(e.texture) ?: return null
            return e.name?.let(SafariRules::sparklingSpecies) ?: species
        }
        fun distance(a: SparklingEntity, b: SparklingEntity) =
            (a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y)+(a.z-b.z)*(a.z-b.z)
        val skins = entities.mapNotNull { e -> skin(e)?.let { e to it } }.toMap()
        val matches = linkedMapOf<Int, SparklingDetection>()
        entities.filterNot { it.realPlayer }.forEach { label ->
            val species = label.name?.let(SafariRules::sparklingSpecies) ?: return@forEach
            val body = if (label.body || label in skins) label else entities.filter { it.id != label.id && !it.realPlayer &&
                (it.body || it in skins) && (skins[it] == null || skins[it] == species) && distance(label, it) < 9 }
                .minWithOrNull(compareBy<SparklingEntity> { if (skins[it] == species) 0 else 1 }
                    .thenBy { distance(label, it) }.thenBy { it.id }) ?: label
            matches.putIfAbsent(body.id, SparklingDetection(species, body.id, label.id, setOf(body.uuid, label.uuid)))
        }
        skins.forEach { (body, species) ->
            // A real name tag wins if a future skin table no longer agrees with the server label.
            if (body.id !in matches && SafariRoster.named(species)?.biome == SafariEspRules.biomeAt(body.x, body.z))
                matches[body.id] = SparklingDetection(species, body.id, body.id, setOf(body.uuid))
        }
        return matches.values.toList()
    }
}
