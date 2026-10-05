package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.api.ApiFailure

/** A failed lookup has a local message only, so another client can still provide the result. */
data class PartyLookupResult(val text: String, val successful: Boolean) {
    val partyMessage get() = text.takeIf { successful }
    companion object {
        fun fetch(command: PartyCommand, key: String, lookup: () -> String): PartyLookupResult {
            if (command.requiresApiKey && key.isBlank()) return PartyLookupResult(command.failureResult(
                "No Hypixel API key is configured. Set one with /apikey or in settings."), false)
            return try { PartyLookupResult(lookup(), true) }
            catch (failure: ApiFailure) { PartyLookupResult(command.failureResult(failure.message.orEmpty()), false) }
            catch (_: Exception) { PartyLookupResult(command.failureResult("Lookup failed unexpectedly. Please try again."), false) }
        }
    }
}

/** Ordered server party chat; the cursor separates old results from the current request. */
class PartyResponseHistory {
    private data class Entry(val id: Long, val text: String, val senderUuid: String?)
    private val messages = ArrayDeque<Entry>()
    var cursor = 0L
        private set
    fun record(body: String, senderUuid: String?) {
        messages.addLast(Entry(++cursor, body, senderUuid?.lowercase()))
        while (messages.size > 200) messages.removeFirst()
    }
    fun hasReply(command: PartyCommand, after: Long, roster: List<String>) = messages.any {
        it.id > after && (it.senderUuid == null || roster.any { id -> id.equals(it.senderUuid, true) }) && command.matchesResponse(it.text)
    }
    fun clear() { messages.clear(); cursor = 0 }
}
