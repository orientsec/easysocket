package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.inner.EventManager;
import com.orientsec.easysocket.inner.Events;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 17:01
 * Author: Fredric
 * coding is art not science
 */

public class RequestTask<T, R> implements Task<T, R> {
    protected enum State {
        //收到响应
        SUCCESS,
        //出错
        ERROR,
        //取消
        CANCELED,
    }

    // Guarded by this.
    private final AtomicBoolean executed = new AtomicBoolean();
    private volatile State state;
    private final Request<T, R> request;
    private final Callback<R> callback;
    private final Executor callbackExecutor;
    private final Executor codecExecutor;
    private final EventManager eventManager;
    /**
     * 每一个任务的id是唯一的，通过taskId，客户端可以匹配每个请求的返回
     */
    final int taskId;
    /**
     * 编码后的请求数据
     *
     * @see Request#encode(int)
     */
    private byte[] data = new byte[0];

    private R response;

    private Exception exception;

    RequestTask(@NonNull Request<T, R> request,
                @NonNull Callback<R> callback,
                @NonNull EventManager eventManager,
                @NonNull Options<T> options,
                int taskId) {
        this.request = request;
        this.eventManager = eventManager;
        this.callback = callback;
        this.taskId = taskId;
        callbackExecutor = options.getCallbackExecutor();
        codecExecutor = options.getCodecExecutor();
    }

    @Override
    public int taskId() {
        return taskId;
    }

    /**
     * 获取请求的编码数据。
     * 在加入请求队列之前，对request进行编码，编码成功之后data才会有数据。
     *
     * @return 请求的编码数据
     */
    @Override
    public byte[] data() {
        return data;
    }

    boolean isInitialize() {
        return request.isInitialize();
    }

    @Override
    public void execute() {
        if (executed.compareAndSet(false, true)) {
            eventManager.publish(Events.TASK_START, this);
        } else {
            throw new IllegalStateException("Already Executed");
        }
    }

    /**
     * 任务是否执行结束
     *
     * @return 是否执行结束
     */
    @Override
    public boolean isFinished() {
        return state != null;
    }

    @Override
    public void cancel() {
        if (isFinished()) return;
        eventManager.publish(Events.TASK_CANCEL, this);
    }

    @Override
    public boolean isCanceled() {
        return state == State.CANCELED;
    }

    void onStart() {
        //对请求消息进行编码, 获取最终写入的字节数组。
        codecExecutor.execute(() -> {
            try {
                data = request.encode(taskId);
                eventManager.publish(Events.TASK_ENQUEUE, this);
            } catch (Exception e) {
                exception = e;
                eventManager.publish(Events.TASK_ERROR, this);
            }
        });
    }

    void onCancel() {
        state = State.CANCELED;
        callbackExecutor.execute(callback::onCancel);
    }

    void onSend() {
        callbackExecutor.execute(callback::onStart);
    }

    void onReceive(T data) {
        codecExecutor.execute(() -> {
            try {
                response = request.decode(data);
                eventManager.publish(Events.TASK_SUCCESS, this);
            } catch (Exception e) {
                exception = e;
                eventManager.publish(Events.TASK_ERROR, this);
            }
        });
    }

    void onSuccess() {
        state = State.SUCCESS;
        callbackExecutor.execute(() -> callback.onSuccess(response));
    }

    void onError(@NonNull Exception e) {
        state = State.ERROR;
        exception = e;
        callbackExecutor.execute(() -> callback.onError(e));
    }

    void onError() {
        state = State.ERROR;
        callbackExecutor.execute(() -> callback.onError(exception));
    }

    @Override
    @NonNull
    public Request<T, R> request() {
        return request;
    }
}
