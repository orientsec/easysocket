package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.error.EasyException;

import java.util.concurrent.BlockingQueue;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2018/01/09 15:26
 * Author: Fredric
 * coding is art not science
 */
public interface TaskManager extends PacketHandler, TaskFactory {

    /**
     * 获取任务队列
     *
     * @return 任务队列
     */
    BlockingQueue<Task<?>> taskQueue();

    /**
     * 复位任务管理器。
     *
     * @param e 异常
     */
    void reset(@NonNull EasyException e);

    /**
     * 启动任务管理器。在完成资源初始化、登录之后，连接连接准备就绪，进入可用状态。
     */
    void ready();

    void onTaskSend(@NonNull Task<?> task);
}
