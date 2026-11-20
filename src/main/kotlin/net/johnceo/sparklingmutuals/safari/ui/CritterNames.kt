package net.johnceo.sparklingmutuals.safari.ui

import java.util.Optional
import java.util.Properties
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence

/** Display text only. Never pass an alias to tracking, persistence of catches, or network code. */
object CritterNames {
    var revision=0L
        private set
    private val names = SafariRoster.all.map { it.name }
    private val canonical = names.associateBy { it.lowercase(java.util.Locale.ROOT) }
    private val pattern = Regex("(?<![\\p{L}\\p{N}_])((?:§.)*)(" + names.sortedByDescending {it.length}.joinToString("|") {Regex.escape(it)} + ")(s?)(?![\\p{L}\\p{N}_])",RegexOption.IGNORE_CASE)
    private val aliases = mutableMapOf<String,String>()
    fun get(name: String) = aliases[name].orEmpty()
    fun set(name: String, value: String): Boolean {
        if (name !in names) return false
        val clean = value.replace(Regex("§."),"").codePoints().filter { !Character.isISOControl(it) && Character.getType(it) != Character.FORMAT.toInt() }
            .limit(48).toArray().let { String(it,0,it.size) }.trim()
        val before = get(name)
        if (clean.isEmpty() || clean == name) aliases.remove(name) else aliases[name] = clean
        val changed=get(name)!=before
        if(changed)revision++
        return changed
    }
    private fun key(name: String) = "appearance.critterName." + name.lowercase(java.util.Locale.ROOT).replace(' ','_')
    fun load(properties: Properties) { aliases.clear();names.forEach { set(it,properties.getProperty(key(it)).orEmpty()) };revision++ }
    fun save(properties: Properties) { names.forEach { if(get(it).isNotEmpty()) properties.setProperty(key(it),get(it)) else properties.remove(key(it)) } }
    fun display(text: String): String = if(aliases.isEmpty()) text else pattern.replace(text) {
        canonical[it.groupValues[2].lowercase(java.util.Locale.ROOT)]?.let(aliases::get)?.let { alias ->
            it.groupValues[1]+alias+it.groupValues[3]
        } ?: it.value
    }
    private class NamedText(private val delegate: Component): Component by delegate {
        override fun getVisualOrderText(): FormattedCharSequence = mark(delegate.visualOrderText)
    }
    private class NamedSequence(private val delegate: FormattedCharSequence): FormattedCharSequence by delegate
    internal fun mark(sequence: FormattedCharSequence): FormattedCharSequence = if(sequence is NamedSequence) sequence else NamedSequence(sequence)
    fun literal(text: Component): Component=NamedText(text)
    fun tooltip(lines: List<Component>): List<Component> = lines.map(::component)
    fun component(text: Component): Component = formatted(text) as Component
    fun formatted(text: FormattedText): FormattedText {
        if(text is NamedText || aliases.isEmpty()) return text
        val raw = StringBuilder();val styles = mutableListOf<Style>()
        text.visit(FormattedText.StyledContentConsumer<Unit> { style,part ->
            raw.append(part);repeat(part.length) {styles.add(style)};Optional.empty()
        },Style.EMPTY)
        return styled(raw.toString(),styles) ?: text
    }
    private fun styled(raw: String, styles: List<Style>): Component? {
        val matches = pattern.findAll(raw).filter {canonical[it.groupValues[2].lowercase(java.util.Locale.ROOT)] in aliases}.toList()
        if(matches.isEmpty()) return null
        val result = Component.empty()
        fun original(from: Int,until: Int) {
            var start=from
            while(start<until) {
                var end=start+1
                while(end<until && styles[end]==styles[start]) end++
                result.append(Component.literal(raw.substring(start,end)).setStyle(styles[start]));start=end
            }
        }
        var cursor=0
        for(match in matches) {
            val nameStart=match.range.first+match.groupValues[1].length
            original(cursor,nameStart)
            result.append(Component.literal(aliases.getValue(canonical.getValue(match.groupValues[2].lowercase(java.util.Locale.ROOT)))+match.groupValues[3]).setStyle(styles[nameStart]))
            cursor=match.range.last+1
        }
        original(cursor,raw.length)
        return NamedText(result)
    }
    fun sequence(text: FormattedCharSequence): FormattedCharSequence {
        if(text is NamedSequence || aliases.isEmpty()) return text
        val raw=StringBuilder();val styles=mutableListOf<Style>()
        text.accept { _,style,codepoint -> raw.appendCodePoint(codepoint);repeat(Character.charCount(codepoint)) {styles.add(style)};true }
        return styled(raw.toString(),styles)?.visualOrderText ?: text
    }
}
