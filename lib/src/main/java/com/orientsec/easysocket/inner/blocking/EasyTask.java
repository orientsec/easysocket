package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.TaskType;
import com.orientsec.easysocket.exception.ConnectException;
import com.orientsec.easysocket.exception.DecodeException;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.TimeoutException;
import com.orientsec.easysocket.inner.SendMessage;
import com.orientsec.easysocket.inner.blocking.SocketConnection;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 17:01
 * Author: Fredric
 * coding is art not science
 */

class EasyTask<T, R> implements Task, Callback<Message> {
    private static AtomicInteger id = new AtomicInteger();
    /**
     * Task状态，总共有4种状态
     * 0 初始状态
     * 1 执行中
     * 2 完成
     * 3 取消
     */
    private AtomicInteger state = new AtomicInteger();
    private SendMessage message;
    private Request<T, R> request;
    private TaskType taskType;
    private SocketConnection connection;
    Future<Void> timeoutFuture;

    EasyTask(Request<T, R> request, SocketConnection connection) {
        this.request = request;
        this.connection = connection;
        taskType = request.isSendOnly() ? TaskType.SEND_ONLY : TaskType.NORMAL;
        message = new SendMessage();
        message.setBody(request.getRequest());
        message.setTaskId(id.incrementAndGet());
    }

    /**
     * 获取请求消息体
     *
     * @return 请求消息体
     */
    SendMessage getMessage() {
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
    public void execute() {
        if (!state.compareAndSet(0, 1)) {
            throw new IllegalStateException("Task has already executed!");
        }
        connection.taskExecutor().execute(this);
    }


    @Override
    public boolean isExecuted() {
        return state.get() > 0;
    }

    @Override
    public void cancel() {
        if (state.compareAndSet(1, 3)) {
            connection.taskExecutor().remove(this);
            taskEnd();
            onCancel();
        }

    }

    @Override
    public boolean isCanceled() {
        return state.get() == 3;
    }

    @Override
    public void onSuccess(Message res) {
        if (state.compareAndSet(1, 2)) {
            taskEnd();
            try {
                R r = request.decode(res);
                connection.options().getDispatchExecutor().execute(() -> request.onSuccess(r));
            } catch (DecodeException e) {
                connection.options().getDispatchExecutor().execute(() -> request.onError(e));
            }
        }
    }

    @Override
    public void onSuccess() {
        if (state.compareAndSet(1, 2)) {
            taskEnd();
            connection.options().getDispatchExecutor().execute(request::onSuccess);
        }
    }

    @Override
    public void onError(EasyException e) {
        if (state.compareAndSet(1, 2)) {
            taskEnd();
            connection.options().getDispatchExecutor().execute(() -> request.onError(e));
        }
    }

    @Override
    public void onCancel() {
        connection.options().getDispatchExecutor().execute(request::onCancel);
    }

    private void taskEnd() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }
}
