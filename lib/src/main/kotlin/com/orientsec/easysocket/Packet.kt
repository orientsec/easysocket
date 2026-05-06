package com.orientsec.easysocket

/**
 * Represents a data packet used in the EasySocket library.
 * A `Packet` encapsulates the task ID, message body, and message type,
 * and is used for communication between the client and server.
 */
class Packet {
    /**
     * The unique identifier for the task associated with this packet.
     */
    val taskId: Int

    /**
     * The content of the packet, which can be of any type.
     */
    val body: Any

    /**
     * The type of the packet, indicating its purpose or category.
     */
    val packetType: PacketType

    /**
     * Constructs a `Packet` with the specified packet type, task ID, and body.
     *
     * @param packetType The type of the packet, indicating its purpose or category.
     * @param taskId     The unique identifier for the task associated with this packet.
     * @param body       The content of the packet, which can be of any type.
     */
    constructor(packetType: PacketType, taskId: Int, body: Any) {
        this.taskId = taskId
        this.packetType = packetType
        this.body = body
    }

    /**
     * Constructs a `Packet` with the specified packet type and body.
     * The task ID is set to 0 by default.
     *
     * @param packetType The type of the packet, indicating its purpose or category.
     * @param body       The content of the packet, which can be of any type.
     */
    constructor(packetType: PacketType, body: Any) {
        this.packetType = packetType
        this.body = body
        this.taskId = 0
    }

    /**
     * Returns a string representation of the packet, including its task ID, body, and type.
     *
     * @return A string representation of the packet.
     */
    override fun toString(): String {
        return "[Packet: taskId=$taskId, " +
                "bodyType=${body.javaClass.simpleName}, " +
                "packetType=$packetType]"
    }
}
