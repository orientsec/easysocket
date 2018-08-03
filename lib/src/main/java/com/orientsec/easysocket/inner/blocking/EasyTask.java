package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.TaskType;
import com.orientsec.easysocket.inner.MessageType;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 17:01
 * Author: Fredric
 * coding is art not science
 */

class EasyTask<T, REQUEST, RESPONSE> implements Task<RESPONSE>, Callback<T> {

    /**
     * Task状态，总共有4种状态
     * 0 初始状态
     * 1 等待执行
     * 2 执行中
     * 3 完成
     * 4 取消
     */
    private AtomicInteger state = new AtomicInteger();
    private Message<T> message;
    private Request<T, REQUEST, RESPONSE> request;
    private Callback<RESPONSE> callback;
    private TaskType taskType;
    private SocketConnection<T> connection;
    Future<Void> timeoutFuture;

    EasyTask(Request<T, REQUEST, RESPONSE> request, SocketConnection<T> connection) {
        this.request = request;
        this.connection = connection;
        taskType = request.isSendOnly() ? TaskType.SEND_ONLY : TaskType.NORMAL;
        message = connection.buildMessage(MessageType.REQUEST);
    }

    /**
     * 获取请求消息体
     *
     * @return 请求消息体
     */
    public Message<T> getMessage() {
        return message;
    }

    /**
     * 获取任务类型
     *
     * @return 任务类型
     */
    TaskType getTaskType() {
        return taskType;
    }

    @Override
    public void execute(Callback<RESPONSE> callback) {
        if (!state.compareAndSet(0, 1)) {
            throw new IllegalStateException("Task has already executed!");
        }
        this.callback = callback;
        try {
            message.setBody(request.encode());
            connection.taskExecutor().execute(this);
        } catch (Exception e) {
            onError(e);
        }
    }


    @Override
    public boolean isExecuted() {
        return state.get() > 0;
    }

    @Override
    public void cancel() {
        if (compareAndSet(state, 4, 1, 2)) {
            connection.taskExecutor().remove(this);
            taskEnd();
            onCancel();
        }
    }

    @Override
    public boolean isCanceled() {
        return state.get() == 4;
    }

    @Override
    public void onStart() {
        if (state.compareAndSet(1, 2)) {
            connection.options().getDispatchExecutor().execute(() -> callback.onStart());
        }
    }

    @Override
    public void onSuccess(T data) {
        if (state.compareAndSet(2, 3)) {
            taskEnd();
            try {
                RESPONSE response = request.decode(data);
                connection.options().getDispatchExecutor().execute(() -> callback.onSuccess(response));
            } catch (Exception e) {
                connection.options().getDispatchExecutor().execute(() -> callback.onError(e));
            }
        }
    }

    @Override
    public void onSuccess() {
        if (state.compareAndSet(2, 3)) {
            taskEnd();
            connection.options().getDispatchExecutor().execute(callback::onSuccess);
        }
    }

    @Override
    public void onError(Exception e) {
        if (compareAndSet(state, 3, 1, 2)) {
            taskEnd();
            connection.options().getDispatchExecutor().execute(() -> callback.onError(e));
        }
    }

    @Override
    public void onCancel() {
        connection.options().getDispatchExecutor().execute(callback::onCancel);
    }

    private void taskEnd() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    private boolean compareAndSet(AtomicInteger i, int value, int... range) {
        int prev;
        do {
            prev = i.get();
            if (range.length == 0) {
                return false;
            }
            boolean contains = false;
            for (int aRange : range) {
                if (prev == aRange) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                return false;
            }
        } while (!i.compareAndSet(prev, value));
        return true;
    }
}
