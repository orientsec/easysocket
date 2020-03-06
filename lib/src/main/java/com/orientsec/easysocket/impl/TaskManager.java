package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.EasyException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2018/01/09 15:26
 * Author: Fredric
 * coding is art not science
 */
public interface TaskManager<T, TASK extends Task<T, ?>> extends PacketHandler<T> {

    /**
     * 加入task
     *
     * @param task 任务
     */
    void add(TASK task) throws EasyException;

    void enqueue(TASK task) throws EasyException;

    /**
     * 移除任务
     * 如果任务未开始执行，取消任务执行；如果已经开始执行，但未结束，{@link Callback}不会再收到
     * 回调；如果任务已经完成，无效果
     *
     * @param task 任务
     */
    void remove(TASK task);

    void clear(EasyException e);

    /**
     * 开始发送消息
     *
     * @param task task
     */
    void onSend(RequestTask<T, ?> task);

    /**
     * 连接准备就绪。在完成资源初始化、登录之后，连接进入可用状态
     */
    void onReady();

    int generateTaskId();
}
