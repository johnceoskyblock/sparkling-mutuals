package net.johnceo.sparklingmutuals

import net.azureaaron.hmapi.events.HypixelPacketEvents
import net.azureaaron.hmapi.network.HypixelNetworking
import net.azureaaron.hmapi.network.packet.s2c.HelloS2CPacket
import net.azureaaron.hmapi.network.packet.s2c.HypixelS2CPacket
import net.azureaaron.hmapi.network.packet.v2.s2c.PartyInfoS2CPacket

object PartyManager {

    private var partyMembers: List<String> = emptyList()

    private var partyInfoCallback: (() -> Unit)? = null

    fun init() {
        HypixelPacketEvents.HELLO.register(::handlePacket)
        HypixelPacketEvents.PARTY_INFO.register(::handlePacket)
    }

    private fun handlePacket(packet: HypixelS2CPacket) {

        when (packet) {

            is HelloS2CPacket -> {
                requestPartyInfo()
            }

            is PartyInfoS2CPacket -> {
                onPartyInfoPacket(packet)
            }
        }
    }

    private fun onPartyInfoPacket(
        packet: PartyInfoS2CPacket
    ) {

        partyMembers = packet.members
            ?.map { it.key.toString() }
            ?: emptyList()

        println(
            "PartyManager: Found ${partyMembers.size} party members."
        )

        partyInfoCallback?.invoke()
        partyInfoCallback = null
    }

    fun requestPartyInfo() {
        HypixelNetworking.sendPartyInfoC2SPacket(2)
    }

    fun refreshPartyInfo(
        onUpdated: () -> Unit
    ) {

        partyInfoCallback = onUpdated

        requestPartyInfo()
    }

    fun getMembers(): List<String> {
        return partyMembers
    }
}