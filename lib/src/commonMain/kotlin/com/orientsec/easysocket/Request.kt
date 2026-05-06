package com.orientsec.easysocket

interface Request<T> {
    fun encode(sequenceId: Int): ByteArray
    fun decode(packet: Packet): T
}

data class Packet(
    val type: PacketType,
    val taskId: Int = 0,
    val body: Any
)
