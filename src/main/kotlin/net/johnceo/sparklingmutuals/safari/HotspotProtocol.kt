package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.commands.PartyChat

/** Party-chat coordination shared with SafariUtils; names are normalized only internally. */
object HotspotProtocol {
    data class Wins(val sender: String,val hotspot: SafariBiome,val counts: Map<String,Int>)
    data class Assignment(val sender: String,val biomes: Map<String,SafariBiome>)
    private val winsHeader=Regex("""^\[(?:SM|SU|SafariUtils)] Hotspot: (Forest|Cavern|Icy|Haunted)\s*\|\s*Hotspot wins:\s*(.+)$""",RegexOption.IGNORE_CASE)
    private val assignmentHeader=Regex("""^\[(?:SM|SU|SafariUtils)] Biomes:\s*(.+)$""",RegexOption.IGNORE_CASE)
    private val member=Regex("""^([A-Za-z0-9_]{1,16})=([A-Za-z]+|\d{1,7})$""")
    private fun entries(body: String): Map<String,String>? {
        val result=linkedMapOf<String,String>()
        for(part in body.split(',')) {
            val match=member.matchEntire(part.trim()) ?: return null
            if(result.put(match.groupValues[1].lowercase(),match.groupValues[2])!=null) return null
        }
        return result.takeIf { it.size in 1..4 }
    }
    private fun biome(label: String)=SafariBiome.entries.firstOrNull { it.label.equals(label,true) }
    fun parseWins(raw: String): Wins? {
        val chat=PartyChat.parse(raw) ?: return null
        val match=winsHeader.matchEntire(chat.body) ?: return null
        val values=entries(match.groupValues[2]) ?: return null
        val counts=values.mapValues { (_,value) -> value.toIntOrNull()?.takeIf { it in 0..1_000_000 } ?: return null }
        return Wins(chat.sender,biome(match.groupValues[1]) ?: return null,counts)
    }
    fun parseAssignment(raw: String): Assignment? {
        val chat=PartyChat.parse(raw) ?: return null
        val match=assignmentHeader.matchEntire(chat.body) ?: return null
        val values=entries(match.groupValues[1]) ?: return null
        val biomes=values.mapValues { (_,value) -> biome(value) ?: return null }
        return Assignment(chat.sender,biomes).takeIf { biomes.values.toSet().size==biomes.size }
    }
    fun winsMessage(hotspot: SafariBiome,counts: Map<String,Int>,names: Map<String,String> = emptyMap())=
        "[SM] Hotspot: ${hotspot.label} | Hotspot wins: "+counts.entries.sortedBy { it.key }.joinToString(", ") {
            "${names[it.key] ?: it.key}=${it.value}"
        }
    fun assignmentMessage(plan: Map<String,SafariBiome>,names: Map<String,String> = emptyMap())=
        "[SM] Biomes: "+plan.entries.sortedBy { it.key }.joinToString(", ") { "${names[it.key] ?: it.key}=${it.value.label}" }
}
