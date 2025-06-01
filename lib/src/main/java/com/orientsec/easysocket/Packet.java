package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.request.Request;

/**
 * Represents a data packet used in the EasySocket library.
 * A `Packet` encapsulates the task ID, message body, and message type,
 * and is used for communication between the client and server.
 */
public class Packet {

    /**
     * Constructs a `Packet` with the specified packet type, task ID, and body.
     *
     * @param packetType The type of the packet, indicating its purpose or category.
     * @param taskId     The unique identifier for the task associated with this packet.
     * @param body       The content of the packet, which can be of any type.
     */
    public Packet(@NonNull PacketType packetType, int taskId, @NonNull Object body) {
        this.taskId = taskId;
        this.packetType = packetType;
        this.body = body;
    }

    /**
     * Constructs a `Packet` with the specified packet type and body.
     * The task ID is set to 0 by default.
     *
     * @param packetType The type of the packet, indicating its purpose or category.
     * @param body       The content of the packet, which can be of any type.
     */
    public Packet(@NonNull PacketType packetType, @NonNull Object body) {
        this.packetType = packetType;
        this.body = body;
        this.taskId = 0;
    }

    /**
     * The unique identifier for the task associated with this packet.
     */
    private final int taskId;

    /**
     * The content of the packet, which can be of any type.
     */
    @NonNull
    private final Object body;

    /**
     * The type of the packet, indicating its purpose or category.
     */
    @NonNull
    private final PacketType packetType;

    /**
     * Retrieves the task ID of the packet.
     * Each task ID is unique, allowing the client to match requests with their responses.
     *
     * @return The task ID of the packet.
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * Retrieves the content of the packet.
     * The content type is customizable and can be decoded or deserialized
     * in the business layer using {@link Request#decode(Packet)}.
     *
     * @return The body of the packet.
     */
    @NonNull
    public Object getBody() {
        return body;
    }

    /**
     * Retrieves the type of the packet.
     *
     * @return The type of the packet.
     */
    @NonNull
    public PacketType getPacketType() {
        return packetType;
    }

    /**
     * Returns a string representation of the packet, including its task ID, body, and type.
     *
     * @return A string representation of the packet.
     */
    @Override
    @NonNull
    public String toString() {
        return "Packet{" +
                "taskId=" + taskId +
                ", body=" + body +
                ", packetType=" + packetType +
                '}';
    }
}
