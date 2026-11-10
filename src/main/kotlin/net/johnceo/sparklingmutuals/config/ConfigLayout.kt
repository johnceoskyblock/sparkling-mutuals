package net.johnceo.sparklingmutuals.config

/** Logical-pixel geometry shared by text measuring and widget placement. */
object ConfigLayout {
    data class Columns(val textWidth: Int,val controlWidth: Int,val stacked: Boolean)
    fun columns(width: Int,preferredControl: Int,forceStack: Boolean=false): Columns {
        val inner=(width-24).coerceAtLeast(24)
        val control=preferredControl.coerceAtLeast(40).coerceAtMost(inner)
        val stack=forceStack || inner-control-16<(if(control<=48) 90 else 130)
        return Columns(if(stack) inner else inner-control-16,control,stack)
    }
    data class Header(val searchWidth: Int,val themeX: Int,val themeWidth: Int)
    fun header(left: Int,right: Int): Header {
        val available=(right-left).coerceAtLeast(100)
        val theme=minOf(if(available>=330) 126 else 86,available-34)
        return Header((available-theme-10).coerceAtLeast(24),right-theme,theme)
    }
}

data class SettingsTarget(val page: String,val fields: List<String>)

object HudConfigLinks {
    fun target(hud: net.johnceo.sparklingmutuals.hud.SafariHud): SettingsTarget {
        val huds="HUDs";val safari="Safari helpers";val banner="Banner"
        return when(hud) {
            net.johnceo.sparklingmutuals.hud.SafariHud.HOTSPOT -> SettingsTarget(huds,listOf("hotspotHud","appearance"))
            net.johnceo.sparklingmutuals.hud.SafariHud.ENTRY_COOLDOWN -> SettingsTarget(safari,listOf("entryCooldown","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.DEVELOPER -> SettingsTarget(huds, emptyList())
            net.johnceo.sparklingmutuals.hud.SafariHud.PROGRESS -> SettingsTarget(huds,listOf("progressHud","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.PROFITABLE -> SettingsTarget(huds,listOf("profitableHud","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.LAKE_CLEARED -> SettingsTarget(huds,listOf("lakeClearedHud","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.MISSING -> SettingsTarget(huds,listOf("missingHud","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.OBJECTIVES -> SettingsTarget(huds,listOf("objectivesHud","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.CAPTURES -> SettingsTarget(huds,listOf("capturesHud","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.SPARKLINGS -> SettingsTarget(safari,listOf("alerts","shiny"))
            net.johnceo.sparklingmutuals.hud.SafariHud.DOOM_THROW -> SettingsTarget(safari,listOf("capture","doomTimer"))
            net.johnceo.sparklingmutuals.hud.SafariHud.BANNER -> SettingsTarget(banner,listOf("enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.GEMS -> SettingsTarget(banner,listOf("encounters","gems","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.BIRD_FOOD -> SettingsTarget(banner,listOf("encounters","food","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.COINS -> SettingsTarget(banner,listOf("encounters","coins","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.INCENSE -> SettingsTarget(banner,listOf("encounters","incense","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.RUN_COMPLETE -> SettingsTarget(banner,listOf("safari","complete","enabled"))
            net.johnceo.sparklingmutuals.hud.SafariHud.WARP -> SettingsTarget(banner,listOf("safari","warp","enabled"))
        }
    }
    val contest=SettingsTarget("HUDs",listOf("contestHud","enabled"))
}
