package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.hud.BannerKind

class SafariBannerOptions {
    @JvmField @ConfigOption(name="Full party joined",desc="") @Accordion val fullParty=BannerOption(BannerKind.FULL_PARTY)
    @JvmField @ConfigOption(name="Biome complete",desc="") @Accordion val biome=BannerOption(BannerKind.BIOME_DONE)
    @JvmField @ConfigOption(name="Run complete",desc="") @Accordion val complete=BannerOption(BannerKind.RUN_COMPLETE)
    @JvmField @ConfigOption(name = "Party member kicked", desc = "Alert when a teammate reports a kick in party chat during Safari.")
    @Accordion val partyKicked = BannerOption(BannerKind.PARTY_KICKED)
}

class EncountersBanners : CommonEncounterBanners()
