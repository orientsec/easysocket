package com.orientsec.easysocket

/**
 * 数据包，表示从服务端接收到的消息。
 * 每个数据包包含类型、任务ID和消息体。
 *
 * @property type 数据包类型，标识消息的类别（响应/推送/心跳）
 * @property taskId 任务ID，用于将响应与对应的请求任务匹配
 * @property body 消息体内容，具体类型由 [HeadParser.decodePacket] 决定
 */
data class Packet(
    val type: PacketType,
    val taskId: Int = 0,
    val body: Any
)


/**
 * Enum `PacketType` defines the types of packets used in the EasySocket library.
 * Each packet type represents a specific category of communication between the client and server.
 */
enum class PacketType {
    /**
     * Represents a response message.
     * This type is used for handling responses to client requests.
     */
    RESPONSE,

    /**
     * Represents a push message.
     * This type is used for handling server-initiated messages sent to the client.
     */
    PUSH,

    /**
     * Represents a heartbeat message.
     * This type is used for maintaining the connection's liveliness.
     */
    PULSE
}