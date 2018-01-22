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
     * 执行任务
     *
     * @param task 任务
     */
    void execute(T task);

    /**
     * 接收处理消息
     *
     * @param message 消息体
     */
    void onReceive(Message message);

    void onSend(Message message);

    void onSendError(Message message, WriteException exception);

    void remove(T task);
}
