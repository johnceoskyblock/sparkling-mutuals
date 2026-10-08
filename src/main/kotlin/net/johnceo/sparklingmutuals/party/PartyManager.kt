package net.johnceo.sparklingmutuals.party

import net.azureaaron.hmapi.events.HypixelPacketEvents
import net.azureaaron.hmapi.network.HypixelNetworking
import net.azureaaron.hmapi.network.packet.s2c.HelloS2CPacket
import net.azureaaron.hmapi.network.packet.s2c.HypixelS2CPacket
import net.azureaaron.hmapi.network.packet.v2.s2c.PartyInfoS2CPacket
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object PartyManager {
    private var partyMembers = emptyList<String>()
    var revision = 0L
        private set
    var hasInfo = false
        private set
    private val callbacks = mutableListOf<() -> Unit>()
    private var requestedAt = 0L

    fun init() {
        HypixelPacketEvents.HELLO.register(::handlePacket)
        HypixelPacketEvents.PARTY_INFO.register(::handlePacket)
    }

    private fun handlePacket(packet: HypixelS2CPacket) {
        Minecraft.getInstance().execute {
            when (packet) {
                is HelloS2CPacket -> requestPartyInfo()
                is PartyInfoS2CPacket -> {
                    partyMembers = packet.members?.keys?.map { it.toString() } ?: emptyList()
                    hasInfo = true
                    revision++
                    requestedAt = 0
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

    fun onClientTick(client: Minecraft) {
        if (client.player == null || client.connection == null) { reset(); return }
        if (requestedAt != 0L && System.currentTimeMillis() - requestedAt > 5000) {
            requestedAt = 0
            if (callbacks.isNotEmpty()) client.player?.sendSystemMessage(
                Component.literal("[Sparkling Mutuals] Party information timed out. Please try the command again."))
            callbacks.clear()
        }
    }

    fun reset() { hasInfo = false; revision++; partyMembers = emptyList(); callbacks.clear(); requestedAt = 0 }
    fun getMembers(): List<String> = partyMembers
}
