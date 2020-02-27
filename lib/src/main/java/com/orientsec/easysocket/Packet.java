package com.orientsec.easysocket;


import com.orientsec.easysocket.impl.PacketType;


/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 9:13
 * Author: Fredric
 * coding is art not science
 */

public class Packet<T> {

    public Packet(PacketType packetType, T body) {
        this(packetType, Task.SYNC_TASK_ID, body);
    }

    public Packet(PacketType packetType, int taskId, T body) {
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
    private T body;

    /**
     * 消息类型
     */
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
     * 在业务层{@link Request#decode(Object)}进行解码、反序列化。
     * 可以根据自定义协议，返回任意类型的结构。
     *
     * @return 协议消息体
     */
    public T getBody() {
        return body;
    }

    /**
     * 获取消息类型
     *
     * @return 消息类型
     */
    public PacketType getPacketType() {
        return packetType;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "taskId=" + taskId +
                ", body=" + body +
                ", packetType=" + packetType +
                '}';
    }
}
