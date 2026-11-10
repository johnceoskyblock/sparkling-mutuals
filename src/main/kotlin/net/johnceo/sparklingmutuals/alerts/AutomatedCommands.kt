package net.johnceo.sparklingmutuals.alerts

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.johnceo.sparklingmutuals.safari.SafariTracking

/** One shared interval, rather than separate per-feature throttles. */
class CommandSpacing<T> {
    private val pending=ArrayDeque<T>()
    private var last: Long?=null
    fun add(value: T) { pending.addLast(value) }
    fun next(now: Long): T? {
        if(pending.isEmpty() || last?.let { now-it<500 }==true) return null
        last=now
        return pending.removeFirst()
    }
    fun clear() { pending.clear();last=null }
}

object AutomatedCommands {
    private data class Pending(val connection: ClientPacketListener,val command: String,val expires: Long,val run: Any?)
    private val queue=CommandSpacing<Pending>()
    fun send(connection: ClientPacketListener,command: String) {
        queue.add(Pending(connection,command,System.nanoTime()/1_000_000+5000,SafariTracking.ledger.current))
        tick(Minecraft.getInstance())
    }
    fun tick(client: Minecraft) {
        if(client.connection==null) { reset();return }
        val now=System.nanoTime()/1_000_000
        val pending=queue.next(now) ?: return
        val allowed=when {
            pending.command=="p warp" || pending.command=="p disband" || pending.command.startsWith("p kick ") ->
                net.johnceo.sparklingmutuals.config.ConfigManager.warpAlertsEnabled && !net.johnceo.sparklingmutuals.config.ConfigManager.leechMode
            pending.command.startsWith("p accept ") || pending.command=="p leave" -> net.johnceo.sparklingmutuals.config.ConfigManager.leechMode
            pending.command.startsWith("p ") && pending.command!="p list" ->
                net.johnceo.sparklingmutuals.config.ConfigManager.warpAlertsEnabled && !net.johnceo.sparklingmutuals.config.ConfigManager.leechMode
            else -> true
        }
        if(allowed && client.connection===pending.connection && now<=pending.expires && SafariTracking.ledger.current===pending.run)
            pending.connection.sendCommand(pending.command)
    }
    fun reset()=queue.clear()
}
