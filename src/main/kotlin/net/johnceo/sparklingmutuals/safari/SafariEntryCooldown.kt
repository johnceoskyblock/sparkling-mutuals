package net.johnceo.sparklingmutuals.safari

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.JoinWelcomeState
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

class SafariEntryCountdown {
    private var deadline: Long? = null
    fun remaining(now: Long): Int? {
        val left=(deadline ?: return null)-now
        if(left<=0) { clear();return null }
        return ((left+999)/1000).toInt()
    }
    fun trigger(raw: String, now: Long, safariContext: Boolean, overlay: Boolean=false): Boolean {
        if(overlay || !safariContext || remaining(now)!=null ||
            SafariMessages.lines(raw).none { it=="You cannot join this instance!" }) return false
        deadline=now+30_000
        return true
    }
    fun clear() { deadline=null }
}

/** A display only: this never retries entry, spends tickets or changes guard decisions. */
object SafariEntryCooldown {
    private val countdown=SafariEntryCountdown()
    private fun now()=System.nanoTime()/1_000_000
    fun register() {
        ClientReceiveMessageEvents.GAME.register { message,overlay ->
            if(!overlay) {
                val client=Minecraft.getInstance()
                val connection=client.connection
                client.execute {
                    if(connection==null || client.connection!==connection || !ConfigManager.safariEntryCooldown) return@execute
                    val context=JoinWelcomeState.hypixel(client.currentServer?.ip) &&
                        (SafariIsland.onIsland || SafariIsland.currentMode.equals("foraging_3",true))
                    countdown.trigger(message.string,now(),context)
                }
            }
        }
    }
    fun tick(client: Minecraft) {
        if(!ConfigManager.safariEntryCooldown || client.connection==null) reset()
    }
    fun preview()=panel(30)
    private fun panel(seconds: Int)=HudPanel("Safari Entry Cooldown",0xFF55FFFF.toInt(),
        listOf(HudRow("", "${seconds}s",icon=null)),titleIcon=null)
    fun render(graphics: GuiGraphicsExtractor) {
        val client=Minecraft.getInstance()
        if(!ConfigManager.safariEntryCooldown || client.player==null || client.level==null ||
            client.options.smHideGui || client.smScreen is ContestGui) return
        countdown.remaining(now())?.let { panel(it).draw(graphics,SafariHud.ENTRY_COOLDOWN) }
    }
    fun reset()=countdown.clear()
}
