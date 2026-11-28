// Celebration recipes adapted from SafariUtils (c) 2026 Serkosea and MrCloudy2, MIT.
package net.johnceo.sparklingmutuals.config

import com.google.gson.Gson
import net.johnceo.sparklingmutuals.ui.SparklingAlertStyle
import java.util.Base64
import java.util.Properties

internal data class CelebrationRecipe(
    val effects: List<Int>,
    val pulse: Int,
    val frame: Int,
    val callout: Int,
    val shake: Int,
    val gradient: Int,
    val song: Int,
    val instruments: Int,
    val seconds: Int,
    val volume: Int,
    val detail: Int = 2,
    val caption: String = "SPARKLING CAPTURE",
) {
    fun style(): SparklingAlertStyle = SparklingAlertStyle.custom(effects.toIntArray(), pulse, frame, callout, shake,
        gradient, song, instruments, seconds.toFloat(), volume)

    fun validated() = copy(
        effects = List(57) { (effects.getOrNull(it) ?: 0).coerceIn(0, 6) },
        pulse = pulse.coerceIn(0, 6), frame = frame.coerceIn(0, 6), callout = callout.coerceIn(0, 2),
        shake = shake.coerceIn(0, 6), gradient = gradient.coerceIn(0, 6),
        song = song.coerceIn(0, 10), instruments = instruments.coerceIn(0, 12), seconds = seconds.coerceIn(1, 30),
        volume = volume.coerceIn(0, 250), detail = detail.coerceIn(0, 2), caption = caption.filterNot(Char::isISOControl).take(64),
    )

    companion object {
        fun fromPreset(index: Int): CelebrationRecipe = SparklingAlertStyle.preset(index).let {
            CelebrationRecipe(it.effectLevels().toList(), it.pulse(), it.frame(), it.callout(), it.textShake(),
                it.textGradientSpeed(), it.soundSong(), it.soundTheme(), it.durationSeconds().toInt(), it.soundVolumePercent())
        }
    }
}

internal data class NamedCelebration(val name: String, val recipe: CelebrationRecipe, val favorite: Boolean = false)

internal object CustomCatchCelebrations {
    const val MAX_PRESETS = 12
    private const val MAX_CODE = 4096
    private const val PREFIX = "SMA1:"
    private const val KEY = "catchCelebration.custom"
    private val gson = Gson()
    private data class Saved(
        val active: CelebrationRecipe = CelebrationRecipe.fromPreset(0),
        val presets: List<NamedCelebration> = emptyList(),
    )
    private var saved = Saved()
    val active get() = saved.active
    val presets get() = saved.presets

    fun load(properties: Properties) {
        saved = runCatching {
            val text = properties.getProperty(KEY)?.takeIf { it.length <= 32_000 } ?: return@runCatching Saved()
            val loaded = gson.fromJson(text, Saved::class.java)
            Saved(loaded.active.validated(), loaded.presets.take(MAX_PRESETS).mapNotNull { preset ->
                runCatching { NamedCelebration(preset.name.trim().take(32), preset.recipe.validated(), preset.favorite) }
                    .getOrNull()?.takeIf { it.name.isNotBlank() }
            }.distinctBy { it.name.lowercase(java.util.Locale.ROOT) })
        }.getOrDefault(Saved())
    }

    fun save(properties: Properties) {
        properties.setProperty(KEY, gson.toJson(saved))
    }

    fun apply(recipe: CelebrationRecipe, presets: List<NamedCelebration>) {
        val previous = saved
        val previousStyle = ConfigManager.specialCatchStyle
        saved = Saved(recipe.validated(), presets.take(MAX_PRESETS))
        ConfigManager.specialCatchStyle = SparklingAlertStyle.CUSTOM_INDEX
        try {
            ConfigManager.saveSettings()
        } catch (failure: Exception) {
            saved = previous
            ConfigManager.specialCatchStyle = previousStyle
            throw failure
        }
    }

    fun encode(recipe: CelebrationRecipe): String =
        PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(gson.toJson(recipe.validated()).toByteArray(Charsets.UTF_8))

    fun decode(code: String): CelebrationRecipe {
        val input = code.trim()
        require(input.startsWith(PREFIX) && input.length <= MAX_CODE)
        val json = Base64.getUrlDecoder().decode(input.removePrefix(PREFIX)).toString(Charsets.UTF_8)
        return gson.fromJson(json, CelebrationRecipe::class.java).validated()
    }
}
