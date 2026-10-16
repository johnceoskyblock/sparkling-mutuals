package net.johnceo.sparklingmutuals.safari.shots

import com.google.gson.*

object ShotCodec {
    private val gson = GsonBuilder().serializeNulls().disableHtmlEscaping()
        .registerTypeHierarchyAdapter(Enum::class.java, JsonSerializer<Enum<*>> { value, _, _ -> JsonPrimitive(value.name.lowercase()) }).create()
    fun encode(record: ShotRecord): ByteArray {
        val json = gson.toJsonTree(record).asJsonObject
        json.addProperty("type", when (record) { is ShotRecord.Header -> "header"; is ShotRecord.Event -> "event"; is ShotRecord.Footer -> "footer" })
        if (record is ShotRecord.Event) {
            val name = record.observation.javaClass.simpleName.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
            json.addProperty("kind", name)
            json.add("data", json.remove("observation"))
        }
        // Gson rejects NaN/Infinity by default. Recursive validation also protects geometry before writing.
        fun validate(element: JsonElement) {
            when {
                element.isJsonObject -> element.asJsonObject.entrySet().forEach { validate(it.value) }
                element.isJsonArray -> element.asJsonArray.forEach(::validate)
                element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> require(element.asDouble.isFinite())
            }
        }
        validate(json)
        val encoded = (gson.toJson(json) + "\n").toByteArray(Charsets.UTF_8)
        require(encoded.size <= 64 * 1024) { "shot_record_too_large" }
        return encoded
    }
}
