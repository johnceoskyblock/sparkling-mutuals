package buildlogic

enum class Edition(val id: String, private val suffix: String, val sourceDirectory: String) {
    STANDARD("standard", "-standard", "standard"),
    FULL("full", "", "main"),
    FULL_NO_AUTO_UPDATE("full-no-auto-update", "-full-no-auto-update", "main");

    fun jarName(version: String, minecraft: String) = "sparkling-mutuals-$version$suffix+mc$minecraft.jar"

    companion object {
        fun parse(id: String) = entries.singleOrNull { it.id == id }
            ?: error("Unknown edition '$id'. Choose standard, full, or full-no-auto-update.")
    }
}
