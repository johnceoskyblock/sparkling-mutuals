package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.SafariHud

/** Menu HUDs are editable for five minutes after their last actual render. Positions always persist. */
internal object HuntingBoxHuds {
    private data class Seen(val panel: HudPanel,val at: Long)
    private val seen=mutableMapOf<SafariHud,Seen>()
    fun shown(hud: SafariHud,panel: HudPanel) { seen[hud]=Seen(panel,System.currentTimeMillis()) }
    fun previews(now: Long=System.currentTimeMillis()): Map<SafariHud,HudPanel> =
        seen.filterValues {now>=it.at && now-it.at<=300_000}.mapValues {it.value.panel}
    fun available(hud: SafariHud)=hud in previews()
}
