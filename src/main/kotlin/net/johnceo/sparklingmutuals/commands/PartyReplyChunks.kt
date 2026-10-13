package net.johnceo.sparklingmutuals.commands

/** Minecraft's 256-character chat limit includes the /pc command. Never split a species name. */
object PartyReplyChunks {
    private const val LIMIT = 252
    fun split(text: String): List<String> {
        if (text.length <= LIMIT) return listOf(text)
        val separator = text.indexOf(": ")
        if (separator < 0) return listOf(text.take(LIMIT))
        val prefix = text.substring(0, separator)
        val names = text.substring(separator + 2).split(", ")
        // Reserve enough space for the largest possible index and count.
        val digits = names.size.toString().length
        val capacity = LIMIT - prefix.length - (2 * digits + 6)
        val groups = mutableListOf<String>()
        var group = ""
        names.forEach { name ->
            val next = if (group.isEmpty()) name else "$group, $name"
            if (next.length > capacity && group.isNotEmpty()) { groups.add(group); group = name }
            else group = next
        }
        if (group.isNotEmpty()) groups.add(group)
        return groups.mapIndexed { index, body -> "$prefix [${index + 1}/${groups.size}]: $body" }
    }
}
