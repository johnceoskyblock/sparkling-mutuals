package net.johnceo.sparklingmutuals.party

import net.azureaaron.hmapi.events.HypixelPacketEvents
import net.azureaaron.hmapi.network.HypixelNetworking
import net.azureaaron.hmapi.network.packet.s2c.HelloS2CPacket
import net.azureaaron.hmapi.network.packet.s2c.HypixelS2CPacket
import net.azureaaron.hmapi.network.packet.v2.s2c.PartyInfoS2CPacket
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object PartyManager {
    private val roster = PartyRosterState()
    var revision = 0L
        private set
    val hasInfo get() = roster.known
    private val callbacks = mutableListOf<() -> Unit>()
    private var requestedAt = 0L
    private var refreshAgain = false

    fun init() {
        HypixelPacketEvents.HELLO.register(::handlePacket)
        HypixelPacketEvents.PARTY_INFO.register(::handlePacket)
        ClientReceiveMessageEvents.GAME.register { message, _ ->
            if (PartyRosterSignals.changed(message.string)) Minecraft.getInstance().execute {
                roster.invalidate(); revision++
                // A notification can arrive while a packet for the old party is in flight.
                if (requestedAt != 0L) refreshAgain = true else requestPartyInfo()
            }
        }
    }

    private fun handlePacket(packet: HypixelS2CPacket) {
        Minecraft.getInstance().execute {
            when (packet) {
                is HelloS2CPacket -> requestPartyInfo()
                is PartyInfoS2CPacket -> {
                    requestedAt = 0
                    if (refreshAgain) {
                        refreshAgain = false
                        requestPartyInfo()
                        return@execute
                    }
                    roster.update(if (packet.inParty) packet.members?.keys?.map { it.toString() }.orEmpty() else emptyList())
                    revision++
                    val ready = callbacks.toList()
                    callbacks.clear()
                    ready.forEach { it() }
                }
            }
        }
    }

    fun requestPartyInfo() {
        if (requestedAt != 0L) return
        requestedAt = System.currentTimeMillis()
        HypixelNetworking.sendPartyInfoC2SPacket(2)
    }

    fun refreshPartyInfo(onUpdated: () -> Unit) {
        callbacks.add(onUpdated)
        requestPartyInfo()
    }
    fun withPartyInfo(onReady: () -> Unit) { if (hasInfo) onReady() else refreshPartyInfo(onReady) }

    fun onClientTick(client: Minecraft) {
        if (client.player == null || client.connection == null) { reset(); return }
        if (requestedAt != 0L && System.currentTimeMillis() - requestedAt > 5000) {
            requestedAt = 0
            refreshAgain = false
            if (callbacks.isNotEmpty()) client.player?.sendSystemMessage(
                Component.literal("[SM] Party lookup timed out. Try again."))
            callbacks.clear()
        }
    }

    fun reset() { roster.reset(); revision++; callbacks.clear(); requestedAt = 0; refreshAgain = false }
    fun getMembers(): List<String> = roster.members
    fun withLocal(local: String) = roster.withLocal(local)
}
