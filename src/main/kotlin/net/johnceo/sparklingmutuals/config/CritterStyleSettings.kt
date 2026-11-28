package net.johnceo.sparklingmutuals.config

class CritterStyleEntry(val species: String, @JvmField @ConfigEditorBoolean var enabled: Boolean)
class CritterStyleSettings(private val glow: Boolean) {
    val entries = SafariEspConfig.mobs.keys.map { species ->
        CritterStyleEntry(species, if (glow) SafariEspConfig.showCritterGlow(species) else SafariEspConfig.showCritterBox(species))
    }
    @JvmField @ConfigEditorBoolean var moundGlow = SafariEspConfig.rockmiteMoundGlow
    fun apply(): Boolean {
        val target=if(glow) SafariEspConfig.glowToggles else SafariEspConfig.boxToggles
        var changed=false
        entries.forEach { if(target[it.species]!=it.enabled)changed=true;target[it.species]=it.enabled }
        if (glow && SafariEspConfig.rockmiteMoundGlow != moundGlow) {
            SafariEspConfig.rockmiteMoundGlow = moundGlow
            changed = true
        }
        return changed
    }
    fun rows(path: String): List<SettingsCatalog.Row> = buildList {
        if (glow) add(SettingsCatalog.Row(this@CritterStyleSettings, CritterStyleSettings::class.java.getField("moundGlow"),
            "Rockmite mound glow", "Outline the mound model when Rockmite glow is on.", "$path / Rockmite mound glow"))
        for (entry in entries) add(SettingsCatalog.Row(entry, CritterStyleEntry::class.java.getField("enabled"),
            entry.species, "", "$path / ${entry.species}", navigationName = entry.species))
    }
}
