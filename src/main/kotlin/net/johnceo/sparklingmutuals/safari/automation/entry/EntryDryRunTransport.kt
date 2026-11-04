package net.johnceo.sparklingmutuals.safari.automation.entry

/** Immediate, paced transport. Drops proposals; never owns a resend queue or a gameplay action. */
class EntryDryRunTransport(private val clock: () -> Long) {
    private val time = EntryClock()
    private var lastAttemptCompletedAt: Long? = null
    fun send(output: EntryDryRunOutput.Party, current: () -> EntryDryRunFrame,
             profiles: () -> List<EntryPeer>, send: (String) -> Unit): Boolean {
        val frame = current()
        if (output.message is EntryMessage.Reply) {
            val peer = output.recipient ?: return false
            if (peer.id !in frame.context?.party?.members.orEmpty() ||
                !output.message.requester.equals(peer.name, true)) return false
            val live = profiles().filter { it.name.equals(peer.name, true) }.singleOrNull() ?: return false
            if (live.id != peer.id) return false
        }
        val at = clock()
        if (!time.accept(at) || lastAttemptCompletedAt?.let { at - it < 1500 } == true) return false
        val command = EntryDryRunDispatch.command(output, frame, at) ?: return false
        return try { send(command); true } catch (_: Exception) { false } finally {
            // A pause or uncertain send consumes real wire pacing, regardless of proposal time/context.
            val completed = clock()
            if (time.accept(completed)) lastAttemptCompletedAt = completed
        }
    }
}
