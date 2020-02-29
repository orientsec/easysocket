package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Error;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 17:01
 * Author: Fredric
 * coding is art not science
 */

public class RequestTask<T, REQUEST, RESPONSE> implements Task<RESPONSE> {
    enum State {
        //初始状态
        IDLE,
        //准备
        PREPARING,
        //等待响应
        WAITING,
        //收到响应
        SUCCESS,
        //出错
        ERROR,
        //取消
        CANCELED,
    }

    private volatile State state = State.IDLE;
    private Request<T, REQUEST, RESPONSE> request;
    private Callback<RESPONSE> callback;
    private Options<T> options;
    private Executor callbackExecutor;
    private Executor codecExecutor;
    private Connection<T> connection;
    private final TaskManager<T, RequestTask<T, ?, ?>> taskManager;
    private ScheduledFuture<?> timeoutFuture;
    /**
     * 每一个任务的id是唯一的，通过taskId，客户端可以匹配每个请求的返回
     */
    int taskId;
    /**
     * 编码后的请求数据
     *
     * @see Request#encode(int)
     */
    private byte[] data;

    RequestTask(Request<T, REQUEST, RESPONSE> request,
                Callback<RESPONSE> callback,
                SocketConnection<T> connection) {
        this.request = request;
        this.connection = connection;
        this.options = connection.options;
        this.callback = callback;
        callbackExecutor = options.getCallbackExecutor();
        codecExecutor = options.getCodecExecutor();
        taskManager = connection.taskManager();
        if (request.isSync()) {
            taskId = SYNC_TASK_ID;
        } else {
            taskId = taskManager.generateTaskId();
        }
    }

    /**
     * 获取请求的编码数据。
     * 在加入请求队列之前，{@link RequestTaskManager}会调用{@link #encode()}
     * 进行编码，编码成功之后data才会有数据。
     *
     * @return 请求的编码数据
     */
    byte[] getData() {
        return data;
    }

    boolean isSync() {
        return request.isSync();
    }

    boolean isInit() {
        return request.isInit();
    }

    /**
     * 是否只发送请求，不接收响应。
     *
     * @return sendOnly
     */
    boolean isSendOnly() {
        return request.isSendOnly();
    }

    @Override
    public void execute() {
        boolean valid;
        synchronized (this) {
            if (state == State.IDLE) {
                state = State.PREPARING;
                valid = true;
            } else {
                valid = false;
            }
        }
        if (!valid) {
            onError(Error.create(Error.Code.TASK_REFUSED, "Task has already executed."));
            return;
        }
        if (connection.isShutdown()) {
            onError(Error.create(Error.Code.SHUT_DOWN, "Connection is show down."));
        } else if (!ConnectionManager.getInstance().isNetworkAvailable()) {
            onError(Error.create(Error.Code.NETWORK_NOT_AVAILABLE,
                    "Network is unavailable!"));
        } else {
            connection.start();
            try {
                synchronized (this) {
                    if (state != State.PREPARING) return;
                    taskManager.add(this);
                }
            } catch (EasyException e) {
                onError(e);
            }
        }
    }

    /**
     * 对请求消息进行编码, 获取最终写入的字节数组。
     * 在编解码线程中执行。
     */
    void encode() {
        codecExecutor.execute(() -> {
            try {
                data = getRequest().encode(taskId);
                if (data == null) throw new IllegalStateException("Request data is null.");

                synchronized (this) {
                    if (state != State.PREPARING) return;
                    taskManager.enqueue(this);
                }
            } catch (EasyException e) {
                onError(e);
            }
        });
    }

    /**
     * 任务是否执行结束
     *
     * @return 是否执行结束
     */
    @Override
    public boolean isFinished() {
        return state == State.SUCCESS
                || state == State.CANCELED
                || state == State.ERROR;
    }

    @Override
    public boolean isExecuted() {
        return state != State.IDLE;
    }


    @Override
    public synchronized void cancel() {
        if (isFinished()) return;
        state = State.CANCELED;
        cancelTimer();
        taskManager.remove(this);
        callbackExecutor.execute(callback::onCancel);
    }

    @Override
    public boolean isCanceled() {
        return state == State.CANCELED;
    }

    synchronized void onSend() {
        if (isCanceled()) return;
        state = State.WAITING;
        startTimer();
        callbackExecutor.execute(() -> callback.onStart());
    }

    synchronized void onReceive(T data) {
        if (isCanceled()) return;
        state = State.SUCCESS;
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

    synchronized void onError(EasyException e) {
        if (isCanceled()) return;
        state = State.ERROR;
        cancelTimer();
        callbackExecutor.execute(() -> callback.onError(e));
    }

    private void cancelTimer() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }

    private void startTimer() {
        timeoutFuture = options.getScheduledExecutor()
                .schedule(this::timeout, options.getRequestTimeOut(), TimeUnit.SECONDS);
    }

    private synchronized void timeout() {
        if (isFinished()) return;
        taskManager.remove(this);
        onError(Error.create(Error.Code.RESPONSE_TIME_OUT));
    }

    @Override
    public Request<T, ?, RESPONSE> getRequest() {
        return request;
    }
}
