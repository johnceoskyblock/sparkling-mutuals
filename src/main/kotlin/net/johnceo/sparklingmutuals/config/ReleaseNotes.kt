package net.johnceo.sparklingmutuals.config

/** A small read-only Markdown subset for public GitHub release descriptions. */
object ReleaseNotes {
    enum class Kind { HEADING, BULLET, TEXT, SPACE }
    data class Block(val kind: Kind,val text: String)
    fun parse(notes: String): List<Block> = buildList {
        notes.lineSequence().forEach { raw ->
            val line=raw.trim()
            val heading=Regex("^#{1,6}\\s+(.+)$").matchEntire(line)
            val bullet=Regex("^(?:[-*+] |\\d+[.)] )(.+)$").matchEntire(line)
            val kind=when { line.isEmpty() -> Kind.SPACE;heading!=null -> Kind.HEADING;bullet!=null -> Kind.BULLET;else -> Kind.TEXT }
            val body=heading?.groupValues?.get(1) ?: bullet?.groupValues?.get(1) ?: line
            val text=body.replace(Regex("!?\\[([^]]+)]\\([^)]+\\)"),"$1")
                .replace("`","").replace("**","").replace("__","")
            if(kind!=Kind.SPACE || lastOrNull()?.kind!=Kind.SPACE) add(Block(kind,text))
        }
    }
}
