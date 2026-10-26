package net.johnceo.sparklingmutuals.commands

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.SafariEntryPresence
import net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput
import net.minecraft.client.Minecraft
import java.util.concurrent.CompletableFuture

/** Suggestions resolve verified party UUIDs, including members absent from the current tab list. */
object PartyNameSuggestions {
    private val wrapper = Regex("""^/(?:pc|pchat|(?:p|party)\s+chat)\s+""", RegexOption.IGNORE_CASE)
    private val lookup = Regex("""^!(?:missing|tickets?)\s+([A-Za-z0-9_]*)$""", RegexOption.IGNORE_CASE)
    private val boss = Regex("""^!pb\s+(?:doom|wumpa|forest|haunted|icy|cavern)\s+([A-Za-z0-9_]*)$""", RegexOption.IGNORE_CASE)

    private data class NameLookup(val future: CompletableFuture<String?>, val expiresAt: Long)
    private val resolved = java.util.concurrent.ConcurrentHashMap<String, NameLookup>()

    fun namesByIdAsync(): CompletableFuture<Map<String, String>> {
        val client = Minecraft.getInstance()
        val self = client.player ?: return CompletableFuture.completedFuture(emptyMap())
        val connection = client.connection ?: return CompletableFuture.completedFuture(emptyMap())
        val ready = CompletableFuture<Unit>()
        PartyManager.withPartyInfo { ready.complete(Unit) }
        return ready.completeOnTimeout(Unit, 5, java.util.concurrent.TimeUnit.SECONDS).thenCompose {
            val result = CompletableFuture<Map<String, String>>()
            client.execute {
                if (client.connection !== connection || client.player !== self) { result.complete(emptyMap()); return@execute }
                val roster = PartyManager.withLocal(self.uuid.toString()).orEmpty()
                val online = connection.onlinePlayers.associate {
                    SafariEntryPresence.normalize(it.profile.id.toString()) to it.profile.name
                }
                val service = client.services().sessionService()
                val futures = roster.map { raw ->
                    val id = SafariEntryPresence.normalize(raw)
                    val cached = if (id == SafariEntryPresence.normalize(self.uuid.toString())) self.name.string
                        else online[id] ?: SafariPartyInput.nameOf(id)
                    if (cached != null) CompletableFuture.completedFuture<String?>(cached)
                    else if (!id.matches(Regex("[0-9a-f]{32}"))) CompletableFuture.completedFuture<String?>(null)
                    else resolved.compute(id) { _, previous ->
                        val now = System.currentTimeMillis()
                        previous?.takeIf { now < it.expiresAt } ?: NameLookup(CompletableFuture.supplyAsync {
                            runCatching {
                                val uuid = java.util.UUID.fromString(id.replace(Regex("(.{8})(.{4})(.{4})(.{4})(.{12})"), "$1-$2-$3-$4-$5"))
                                service.fetchProfile(uuid, false)?.profile()?.takeIf {
                                    SafariEntryPresence.normalize(it.id.toString()) == id
                                }?.name?.takeIf { it.matches(Regex("[A-Za-z0-9_]{1,16}")) }
                            }.getOrNull()
                        }, now + 60_000)
                    }!!.future
                }
                CompletableFuture.allOf(*futures.toTypedArray()).whenComplete { _, _ ->
                    client.execute {
                        if (client.connection !== connection || client.player !== self) result.complete(emptyMap())
                        else {
                            val current = PartyManager.withLocal(self.uuid.toString()).orEmpty().map(SafariEntryPresence::normalize).toSet()
                            result.complete(roster.zip(futures).filter { SafariEntryPresence.normalize(it.first) in current }
                                .mapNotNull { (id, future) -> future.getNow(null)?.let { SafariEntryPresence.normalize(id) to it } }.toMap())
                        }
                    }
                }
            }
            result
        }
    }

    private fun namesAsync(): CompletableFuture<List<String>> = namesByIdAsync().thenApply { it.values.toList() }

    internal fun fill(builder: SuggestionsBuilder, names: Collection<String>, excluded: Set<String> = emptySet()): CompletableFuture<Suggestions> {
        names.distinctBy(String::lowercase).sortedBy(String::lowercase)
            .filter { it.lowercase() !in excluded && it.startsWith(builder.remaining, ignoreCase = true) }
            .forEach(builder::suggest)
        return builder.buildFuture()
    }

    fun sparkling(builder: SuggestionsBuilder): CompletableFuture<Suggestions> =
        namesAsync().thenCompose { sparkling(builder, it) }
    internal fun sparkling(builder: SuggestionsBuilder, names: Collection<String>): CompletableFuture<Suggestions> {
        val args = builder.input.substring(builder.start)
        val offset = args.indexOfLast(Char::isWhitespace) + 1
        val already = args.take(offset).trim().split(Regex("\\s+")).filter(String::isNotEmpty).map(String::lowercase).toSet()
        if ("party" in already) return builder.createOffset(builder.start + offset).buildFuture()
        return fill(builder.createOffset(builder.start + offset), if (already.isEmpty()) names + "party" else names, already)
    }

    fun chat(input: String, cursor: Int): CompletableFuture<Suggestions>? {
        if (chat(input, cursor, emptyList()) == null) return null
        return namesAsync().thenCompose { chat(input, cursor, it)!! }
    }
    internal fun chat(input: String, cursor: Int, names: Collection<String>): CompletableFuture<Suggestions>? {
        val before = input.take(cursor.coerceIn(0, input.length))
        val offset = if (before.startsWith('/')) wrapper.find(before)?.value?.length ?: return null else 0
        val body = before.substring(offset)
        val match = lookup.matchEntire(body) ?: boss.matchEntire(body) ?: return null
        val start = offset + match.groups[1]!!.range.first
        return fill(SuggestionsBuilder(before, start), if (Regex("""^!pb\s+(?:doom|wumpa)\s+""", RegexOption.IGNORE_CASE).containsMatchIn(body)) names + "duo" else names)
    }
}
