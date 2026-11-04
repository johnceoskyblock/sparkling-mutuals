package net.johnceo.sparklingmutuals.safari.automation.entry

import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.safari.SafariRoster
import java.util.Locale

sealed interface EntryMessage {
    val nonce: String
    val realm: String
    val server: String
    data class Query(override val nonce: String, override val realm: String, override val server: String) : EntryMessage {
        init { require(EntryWire.fields(nonce, realm, server)) }
    }
    sealed interface Reply : EntryMessage { val requester: String }
    data class None(override val requester: String, override val nonce: String, override val realm: String, override val server: String) : Reply {
        init { require(EntryWire.ign.matches(requester) && EntryWire.fields(nonce, realm, server)) }
    }
    data class Live(
        override val requester: String, override val nonce: String, override val realm: String,
        override val server: String, val species: String, val ageMs: Long, val event: String
    ) : Reply {
        init {
            require(EntryWire.ign.matches(requester) && EntryWire.fields(nonce, realm, server))
            require(SafariRoster.all.any { it.name == species } && ageMs in 0..5000 && EntryWire.token.matches(event))
        }
    }
}

/** A wire parser, not a chat listener. Membership and context verification are separate. */
object SparklingEntryProtocol {
    private val species = SafariRoster.all.associate { it.name.uppercase(Locale.ROOT).replace(' ', '_') to it.name }
        .also { require(it.size == SafariRoster.all.size) }
    private val sighting = Regex("SPARKLING (.+)!", RegexOption.IGNORE_CASE)
    private fun singleLine(text: String) = text.length <= 1024 && '\n' !in text && '\r' !in text

    fun parse(body: String): EntryMessage? {
        if (!singleLine(body)) return null
        val p = body.split(' ')
        return try {
            when {
                p.size == 5 && p[0] == "SMSE1" && p[1] == "Q" -> EntryMessage.Query(p[2], p[3], p[4])
                p.size == 7 && p[0] == "SMSE1" && p[1] == "R" && p[6] == "NONE" ->
                    EntryMessage.None(p[2], p[3], p[4], p[5])
                p.size == 10 && p[0] == "SMSE1" && p[1] == "R" && p[6] == "LIVE" && p[8].matches(Regex("[0-9]{1,4}")) ->
                    EntryMessage.Live(p[2], p[3], p[4], p[5], species[p[7]] ?: return null, p[8].toLong(), p[9])
                else -> null
            }
        } catch (_: IllegalArgumentException) { null }
    }

    fun encode(message: EntryMessage): String = when (message) {
        is EntryMessage.Query -> "SMSE1 Q ${message.nonce} ${message.realm} ${message.server}"
        is EntryMessage.None -> "SMSE1 R ${message.requester} ${message.nonce} ${message.realm} ${message.server} NONE"
        is EntryMessage.Live -> "SMSE1 R ${message.requester} ${message.nonce} ${message.realm} ${message.server} LIVE ${message.species.uppercase(Locale.ROOT).replace(' ', '_')} ${message.ageMs} ${message.event}"
    }

    /** Only a hint; this text never establishes remote destination or freshness. */
    fun hint(rawPartyMessage: String): Pair<String, String>? {
        if (!singleLine(rawPartyMessage)) return null
        val chat = PartyChat.parse(rawPartyMessage) ?: return null
        val name = sighting.matchEntire(chat.body)?.groupValues?.get(1) ?: return null
        val canonical = SafariRoster.named(name)?.name ?: return null
        return chat.sender to canonical
    }
}
