package com.orientsec.easysocket;

import com.orientsec.easysocket.inner.MessageType;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 9:13
 * Author: Fredric
 * coding is art not science
 */

public class Message {
    /**
     * 消息id
     */
    private int taskId;
    /**
     * 请求id
     */
    private int cmd;
    /**
     * 原始数据包头字节数组
     */
    private byte[] headBytes;
    /**
     * 包体
     */
    private Object body;

    /**
     * 原始数据包体字节数组
     */
    private byte[] bodyBytes;

    /**
     * 消息类型
     */
    private MessageType messageType;

    /**
     * 数据包体大小
     */
    private int bodySize;

    public int getBodySize() {
        return bodySize;
    }

    public void setBodySize(int bodySize) {
        this.bodySize = bodySize;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public byte[] getHeadBytes() {
        return headBytes;
    }

    public void setHeadBytes(byte[] headBytes) {
        this.headBytes = headBytes;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public void setBodyBytes(byte[] bodyBytes) {
        this.bodyBytes = bodyBytes;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
