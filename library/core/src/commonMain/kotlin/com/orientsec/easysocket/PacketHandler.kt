package com.orientsec.easysocket

/**
 * The `PacketHandler` interface defines a contract for handling and processing
 * incoming packets in the EasySocket library. Implementations of this interface
 * are responsible for receiving and dispatching packets to the appropriate handlers.
 */
fun interface PacketHandler {

    /**
     * Handles the received packet.
     * This method is invoked when a packet is received, allowing the implementation
     * to process or dispatch the packet as needed.
     *
     * @param packet The received packet to be processed.
     */
    fun handlePacket(packet: Packet)

}