package com.orientsec.easysocket;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.request.Request;


/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 9:13
 * Author: Fredric
 * coding is art not science
 */

public class Packet {

    public Packet(@NonNull PacketType packetType, int taskId, @NonNull Object body) {
        this.taskId = taskId;
        this.packetType = packetType;
        this.body = body;
    }

    /**
     * 消息id
     */
    private int taskId;
    /**
     * 包体
     */
    @NonNull
    private Object body;

    /**
     * 消息类型
     */
    @NonNull
    private PacketType packetType;

    /**
     * 获取任务id
     * 每一个任务的id是唯一的，通过taskId，客户端可以匹配每个请求的返回
     *
     * @return taskId
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * 获取包体内容，类型可以自定义。
     * 在业务层{@link Request#decode(Packet)} (Object)}进行解码、反序列化。
     * 可以根据自定义协议，返回任意类型的结构。
     *
     * @return 协议消息体
     */
    @NonNull
    public Object getBody() {
        return body;
    }

    /**
     * 获取消息类型
     *
     * @return 消息类型
     */
    @NonNull
    public PacketType getPacketType() {
        return packetType;
    }

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
