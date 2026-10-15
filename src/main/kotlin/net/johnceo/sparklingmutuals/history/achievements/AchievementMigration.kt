package net.johnceo.sparklingmutuals.history.achievements

import com.google.gson.*
import net.johnceo.sparklingmutuals.history.*

data class LegacyAward(val id: String, val unlockedAt: Long, val originalGoal: String, val originalTier: String, val reason: String)
data class AchievementStateV2(val schemaVersion: Int = 2,
    val currentUnlocks: MutableMap<String, Long> = linkedMapOf(),
    val legacyAwards: MutableMap<String, LegacyAward> = linkedMapOf(),
    val droughtByScope: MutableMap<String, DroughtEvidence> = linkedMapOf(),
    val activity: ActivityEvidence = ActivityEvidence(),
    val eligibleEvents: MutableMap<String, Long> = linkedMapOf(), val originalPayload: JsonObject = JsonObject())
object AchievementMigration {
    private val gson = Gson()
    fun migrate(old: Achievements.Data): AchievementStateV2 = migrate(old, gson.toJsonTree(old).asJsonObject)
    fun migrate(old: Achievements.Data, raw: JsonObject): AchievementStateV2 {
        old.state?.let { require(it.schemaVersion == 2) { "Unsupported achievement schema" }; return it }
        val state = AchievementStateV2(activity = old.activity ?: ActivityEvidence(), originalPayload = raw.deepCopy())
        for ((id, at) in old.unlocked) {
            if (id in LegacyAchievementMetadata.criteria && id !in LegacyAchievementMetadata.replacedIds) state.currentUnlocks[id] = at
            else state.legacyAwards[id] = legacy(id, at)
        }
        for ((id, at) in old.emblems.toSortedMap()) {
            // Prefix until unused: historical IDs can already contain any number of prefixes.
            // Sorted input makes allocated keys stable regardless of source map iteration order.
            var key = id
            while (key in state.legacyAwards) key = "emblem:$key"
            state.legacyAwards[key] = LegacyAward(id, at, "Unknown original criterion", "UNKNOWN", "Removed emblem")
        }
        old.state = state
        old.activity = state.activity
        return state
    }
    private fun legacy(id: String, at: Long): LegacyAward {
        val criterion = LegacyAchievementMetadata.criteria[id]
        return LegacyAward(id, at, criterion?.description ?: "Unknown original criterion", criterion?.tier ?: "UNKNOWN",
            if (criterion == null) "Unsupported historical ID" else "Replaced criterion; retained as a legacy award")
    }
}

/** The actual disk boundary retains JSON before typed conversion and protects failed sources from every save path. */
object AchievementPersistence {
    private val gson = Gson()
    fun load(): Result<Achievements.Data> = runCatching {
        val raw = HistoryStore.readObject("achievements") ?: JsonObject()
        validate(raw)
        val data = gson.fromJson(raw, Achievements.Data::class.java)
        val migrating = data.state == null
        AchievementMigration.migrate(data, raw)
        data.activity = data.state!!.activity
        if (migrating) HistoryStore.backupOnce("achievements", ".pre-v2.bak")
        data
    }.onFailure { HistoryStore.protect("achievements") }

    private fun validate(raw: JsonObject) {
        for (field in listOf("unlocked", "emblems")) if (raw.has(field)) {
            require(raw[field].isJsonObject) { "Invalid $field" }
            for ((_, value) in raw[field].asJsonObject.entrySet()) {
                require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber) { "Invalid award timestamp" }
                value.asLong
            }
        }
        if (raw.has("state")) {
            val state = raw["state"].asJsonObject
            require(state["schemaVersion"]?.asInt == 2) { "Unsupported achievement schema" }
            for (field in listOf("currentUnlocks", "legacyAwards", "droughtByScope", "activity", "originalPayload"))
                require(state[field]?.isJsonObject == true) { "Invalid v2 $field" }
        }
    }
    fun save(data: Achievements.Data): Result<Unit> = HistoryStore.save("achievements", data)
}
