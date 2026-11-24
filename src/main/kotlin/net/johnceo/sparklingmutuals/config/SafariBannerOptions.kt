package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.hud.BannerKind

class SafariBannerOptions {
    @JvmField @ConfigOption(name="Full party joined",desc="") @Accordion val fullParty=BannerOption(BannerKind.FULL_PARTY)
    @JvmField @ConfigOption(name="Biome complete",desc="") @Accordion val biome=BannerOption(BannerKind.BIOME_DONE)
    @JvmField @ConfigOption(name="Run complete",desc="") @Accordion val complete=BannerOption(BannerKind.RUN_COMPLETE)
    @JvmField @ConfigOption(name="Warping now",desc="") @Accordion val warp=BannerOption(BannerKind.WARP)
}

class EncountersBanners : CommonEncounterBanners() {
    @JvmField @ConfigOption(name = "Hideyho location", desc = "Announce inside or outside while you are in Haunted. Unique and Full Clear modes only.")
    @Accordion val hideyho = BannerOption(BannerKind.HIDEYHO_LOCATION)
}
