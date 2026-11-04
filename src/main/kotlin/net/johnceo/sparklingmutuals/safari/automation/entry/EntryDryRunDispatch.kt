package net.johnceo.sparklingmutuals.safari.automation.entry

import net.johnceo.sparklingmutuals.history.RunTally
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRules

/** The only transport output is a party status message. Never buffers or retries a proposal. */
object EntryDryRunDispatch {
    fun command(output: EntryDryRunOutput.Party, frame: EntryDryRunFrame, now: Long): String? {
        val current = frame.context ?: return null
        val message = output.message
        if (!frame.enabled || current != output.context || now < output.proposedAt ||
            now - output.proposedAt >= 3000 || message.realm != current.realm || message.server != current.server) return null
        when (message) {
            is EntryMessage.Query -> if (!frame.atEntry || frame.activeRun || current.account == current.party.leader ||
                frame.arrivedAt < 0 || now < frame.arrivedAt || now >= entryDeadline(frame.arrivedAt, 12000)) return null
            is EntryMessage.Reply -> if (!frame.activeRun || current.account != current.party.leader ||
                message is EntryMessage.Live && message.ageMs + now - output.proposedAt >= 5000) return null
        }
        return "pc ${SparklingEntryProtocol.encode(message)}".takeIf { it.length <= 256 }
    }
}

object EntryCaptureNotice {
    fun species(text: String): String? {
        if (text.length > 1024 || '\n' in text || '\r' in text) return null
        val match = RunTally.sparkling.matchEntire(SafariRules.strip(text).trim()) ?: return null
        return SafariRoster.named(match.groupValues[2].trim())?.name
    }
}
