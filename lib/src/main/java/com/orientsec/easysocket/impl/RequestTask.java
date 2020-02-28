package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Event;

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
    private TaskManager<T, RequestTask<T, ?, ?>> taskManager;
    private ScheduledFuture<?> timeoutFuture;
    /**
     * 消息id
     */
    private int taskId;
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
     * 获取任务id
     * 每一个任务的id是唯一的，通过taskId，客户端可以匹配每个请求的返回
     *
     * @return taskId
     */
    int getTaskId() {
        return taskId;
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

    boolean isInit() {
        return request.isInit();
    }

    boolean isSync() {
        return request.isSync();
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
        taskManager.add(this);
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

    boolean onPrepare() {
        if (state != State.IDLE) {
            return false;
        }
        state = State.PREPARING;
        return true;
    }

    boolean isPreparing() {
        return state == State.PREPARING;
    }

    /**
     * 对请求消息进行编码, 获取最终写入的字节数组。
     *
     * @throws EasyException 编码错误
     */
    void encode() throws EasyException {
        data = getRequest().encode(taskId);
        if (data == null) throw new IllegalStateException("Request data is null.");
    }

    @Override
    public void cancel() {
        taskManager.remove(this, Event.TASK_CANCELED);
    }

    @Override
    public boolean isCanceled() {
        return state == State.CANCELED;
    }

    void onSend() {
        state = State.WAITING;
        startTimer();
        callbackExecutor.execute(() -> callback.onStart());
    }

    void onReceive(T data) {
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

    void onError(EasyException e) {
        state = State.ERROR;
        cancelTimer();
        callbackExecutor.execute(() -> callback.onError(e));
    }

    void onCanceled() {
        state = State.CANCELED;
        cancelTimer();
        callbackExecutor.execute(callback::onCancel);
    }

    private void cancelTimer() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }

    private void startTimer() {
        Runnable timeout = () -> taskManager.remove(this, Event.RESPONSE_TIME_OUT);
        timeoutFuture = options.getScheduledExecutor()
                .schedule(timeout, options.getRequestTimeOut(), TimeUnit.SECONDS);
    }

    @Override
    public Request<T, ?, RESPONSE> getRequest() {
        return request;
    }
}
