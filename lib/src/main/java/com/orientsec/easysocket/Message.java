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

    public Message(MessageType messageType) {
        this(messageType, 0);
    }

    public Message(MessageType messageType, int taskId) {
        this.taskId = taskId;
        this.messageType = messageType;
    }

    /**
     * 消息id
     */
    private int taskId;
    /**
     * 请求id
     */
    private int cmd;

    /**
     * 包体
     */
    private Object body;

    /**
     * 消息类型
     */
    private MessageType messageType;

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
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

    public MessageType getMessageType() {
        return messageType;
    }
}
