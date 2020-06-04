package com.orientsec.easysocket.impl;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.ErrorCode;
import com.orientsec.easysocket.exception.ErrorType;

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

public class RequestTask<T, R> implements Task<T, R> {
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
    private Request<T, R> request;
    private Callback<R> callback;
    private Options<T> options;
    private Executor callbackExecutor;
    private Executor codecExecutor;
    private Connection<T> connection;
    private final TaskManager<T, RequestTask<T, ?>> taskManager;
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

    RequestTask(@NonNull Request<T, R> request,
                @NonNull Callback<R> callback,
                @NonNull SocketConnection<T> connection) {
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
            EasyException e = new EasyException(ErrorCode.TASK_REFUSED, ErrorType.TASK,
                    "Task has already executed.");
            onError(e);
            return;
        }
        if (connection.isShutdown()) {
            EasyException e = new EasyException(ErrorCode.SHUT_DOWN, ErrorType.SYSTEM,
                    "Connection is show down.");
            onError(e);
        } else if (!ConnectionManager.getInstance().isNetworkAvailable()) {
            EasyException e = new EasyException(ErrorCode.NETWORK_NOT_AVAILABLE,
                    ErrorType.NETWORK, "Network is unavailable!");
            onError(e);
        } else {
            connection.start();
            try {
                taskManager.add(this);
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
                taskManager.enqueue(this);
            } catch (Exception e) {
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
    public boolean isExecutable() {
        return state == State.PREPARING;
    }

    @Override
    public boolean isExecuted() {
        return state != State.IDLE;
    }


    @Override
    public void cancel() {
        synchronized (this) {
            if (isFinished()) return;
            state = State.CANCELED;
            cancelTimer();
            callbackExecutor.execute(callback::onCancel);
        }
        taskManager.remove(this);
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
                R response = request.decode(data);
                callbackExecutor.execute(() -> callback.onSuccess(response));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    synchronized void onError(Exception e) {
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
                .schedule(this::timeout, options.getRequestTimeOut(), TimeUnit.MILLISECONDS);
    }

    private void timeout() {
        synchronized (this) {
            if (isFinished()) return;
            EasyException e = new EasyException(ErrorCode.RESPONSE_TIME_OUT,
                    ErrorType.RESPONSE, "Response time out.");
            onError(e);
        }
        taskManager.remove(this);
    }

    @Override
    @NonNull
    public Request<T, R> getRequest() {
        return request;
    }
}
