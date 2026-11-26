package net.johnceo.sparklingmutuals.config

class CritterStyleEntry(val species: String, @JvmField @ConfigEditorBoolean var enabled: Boolean)
class CritterStyleSettings(private val glow: Boolean) {
    val entries=SafariEspConfig.mobs.keys.map { CritterStyleEntry(it,(if(glow) SafariEspConfig.glowToggles else SafariEspConfig.boxToggles)[it]!=false) }
    fun apply(): Boolean {
        val target=if(glow) SafariEspConfig.glowToggles else SafariEspConfig.boxToggles
        var changed=false
        entries.forEach { if(target[it.species]!=it.enabled)changed=true;target[it.species]=it.enabled }
        return changed
    }
    fun rows(path: String)=entries.map { entry -> SettingsCatalog.Row(entry,CritterStyleEntry::class.java.getField("enabled"),entry.species,"",path+" / "+entry.species,navigationName=entry.species) }
}
