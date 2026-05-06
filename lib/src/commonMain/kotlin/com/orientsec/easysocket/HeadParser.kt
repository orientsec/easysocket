package com.orientsec.easysocket

interface HeadParser {
    open class Head(val packetSize: Int)

    fun headSize(): Int
    
    @Throws(Exception::class)
    fun parseHead(bytes: ByteArray): Head
    
    @Throws(Exception::class)
    fun decodePacket(head: Head, bodyBytes: ByteArray): Packet
}
