package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.WriteException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2018/01/09 15:26
 * Author: Fredric
 * coding is art not science
 */
public interface TaskExecutor<T extends Task> {
    /**
     * 执行任务, 如果connection未启动，执行后开始进行连接
     *
     * @param task 任务
     */
    void execute(T task);

    /**
     * 接收分发消息
     *
     * @param message 消息体
     */
    void onReceive(Message message);

    /**
     * 消息发送回调
     *
     * @param message 消息体
     */
    void onSend(Message message);

    /**
     * 消息发送失败回调
     *
     * @param message   消息体
     * @param exception WriteException
     */
    void onSendError(Message message, WriteException exception);

    /**
     * 移除任务
     * 如果任务未开始执行，取消任务执行；如果已经开始执行，但未结束，{@link com.orientsec.easysocket.Callback}不会再收到
     * 回调；如果任务已经完成，无效果
     *
     * @param task 任务
     */
    void remove(T task);
}
