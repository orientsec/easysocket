package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.Event;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2018/01/09 15:26
 * Author: Fredric
 * coding is art not science
 */
public interface TaskManager<T, TASK extends Task<?>> extends MessageHandler<T> {
    /**
     * 执行任务, 如果connection未启动，执行后开始进行连接
     *
     * @param task 任务
     */
    boolean add(TASK task);

    /**
     * 移除任务
     * 如果任务未开始执行，取消任务执行；如果已经开始执行，但未结束，{@link com.orientsec.easysocket.Callback}不会再收到
     * 回调；如果任务已经完成，无效果
     *
     * @param task 任务
     */
    void remove(TASK task);

    void clear(Event event);

    /**
     * 开始发送消息
     *
     * @param task task
     */
    void onSend(RequestTask<T, ?, ?> task);

    /**
     * 连接准备就绪。在完成资源初始化、登录之后，连接进入可用状态
     */
    void onReady();

    int generateTaskId();
}
