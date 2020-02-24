package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.TaskType;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Event;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 17:01
 * Author: Fredric
 * coding is art not science
 */

public class RequestTask<T, REQUEST, RESPONSE> implements Task<RESPONSE>, Callback<T> {

    /**
     * Task状态，总共有4种状态
     * 0 初始状态
     * 1 等待执行
     * 2 执行中
     * 3 完成
     * 4 取消
     */
    private AtomicInteger state = new AtomicInteger();
    private Request<T, REQUEST, RESPONSE> request;
    private Callback<RESPONSE> callback;
    private TaskType taskType;
    private SocketConnection<T> connection;
    private Options<T> options;
    private Executor callbackExecutor;
    private Executor codecExecutor;
    private TaskManager<T, RequestTask<T, ?, ?>> taskManager;
    private ScheduledFuture<?> timeoutFuture;
    /**
     * 消息id
     */
    private int taskId;
    private byte[] data;

    RequestTask(Request<T, REQUEST, RESPONSE> request,
                SocketConnection<T> connection) {
        this.request = request;
        this.connection = connection;
        this.options = connection.options;
        callbackExecutor = options.getCallbackExecutor();
        codecExecutor = options.getCodecExecutor();
        taskManager = connection.taskManager();
        taskType = request.isSendOnly() ? TaskType.SEND_ONLY : TaskType.NORMAL;
    }

    /**
     * 获取任务id
     * 每一个任务的id是唯一的，通过taskId，客户端可以匹配每个请求的返回
     *
     * @return taskId
     */
    int getTaskId() {
        return taskId;
    }

    byte[] getData() {
        return data;
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
        if (connection.isShutdown()) {
            throw new IllegalStateException("Connection is show down!");
        }
        connection.start();
        if (ConnectionManager.getInstance().isNetworkAvailable()) {
            codecExecutor.execute(() -> {
                try {
                    taskId = taskManager.generateTaskId();
                    data = getRequest().encode(taskId);
                    if (!taskManager.addTask(this)) {
                        onError(new EasyException(Event.TASK_REFUSED,
                                "Refuse to execute task!"));
                    }
                } catch (EasyException e) {
                    onError(e);
                }
            });
        } else {
            onError(new EasyException(Event.NETWORK_NOT_AVAILABLE,
                    "network is unavailable!"));
        }
    }

    @Override
    public void execute() {
        execute(new EmptyCallback<>());
    }

    @Override
    public boolean isExecuted() {
        return state.get() > 0;
    }

    @Override
    public void cancel() {
        if (compareAndSet(state, 4, 1, 2)) {
            taskManager.removeTask(this);
            cancelTimer();
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
            startTimer();
            callbackExecutor.execute(() -> callback.onStart());
        }
    }

    @Override
    public void onSuccess(T data) {
        if (state.compareAndSet(2, 3)) {
            cancelTimer();
            codecExecutor.execute(() -> {
                try {
                    RESPONSE response = request.decode(data);
                    callbackExecutor.execute(() -> callback.onSuccess(response));
                } catch (EasyException e) {
                    callbackExecutor.execute(() -> callback.onError(e));
                }
            });
        }
    }

    @Override
    public void onSuccess() {
        if (state.compareAndSet(2, 3)) {
            cancelTimer();
            callbackExecutor.execute(callback::onSuccess);
        }
    }

    @Override
    public void onError(Exception e) {
        if (compareAndSet(state, 3, 1, 2)) {
            cancelTimer();
            callbackExecutor.execute(() -> callback.onError(e));
        }
    }

    @Override
    public void onCancel() {
        callbackExecutor.execute(callback::onCancel);
    }

    private void cancelTimer() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    private void startTimer() {
        timeoutFuture = options.getScheduledExecutor()
                .schedule(() -> {
                            taskManager.removeTask(this);
                            onError(new EasyException(Event.RESPONSE_TIME_OUT, "Response time out."));
                        }
                        , options.getRequestTimeOut()
                        , TimeUnit.SECONDS);
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

    @Override
    public Request<T, ?, RESPONSE> getRequest() {
        return request;
    }
}
