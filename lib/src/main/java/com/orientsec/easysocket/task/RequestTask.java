package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.ErrorCode;
import com.orientsec.easysocket.exception.ErrorType;
import com.orientsec.easysocket.inner.EventManager;
import com.orientsec.easysocket.inner.Events;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 17:01
 * Author: Fredric
 * coding is art not science
 */

public class RequestTask<R> implements Task<R> {
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
    private final Request<R> request;
    private final Callback<R> callback;
    private final Executor callbackExecutor;
    private final Executor codecExecutor;
    private final EventManager eventManager;
    private final Connection connection;
    private final Options options;
    private final Map<Integer, RequestTask<?>> taskMap;

    private final LinkedBlockingQueue<Task<?>> writingQueue;

    private final Queue<RequestTask<?>> waitingQueue;
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

    RequestTask(int taskId,
                Request<R> request,
                Callback<R> callback,
                Connection connection,
                Options options,
                EventManager eventManager,
                Map<Integer, RequestTask<?>> taskMap,
                LinkedBlockingQueue<Task<?>> writingQueue,
                Queue<RequestTask<?>> waitingQueue) {
        this.request = request;
        this.connection = connection;
        this.eventManager = eventManager;
        this.callback = callback;
        this.taskId = taskId;
        this.options = options;
        this.taskMap = taskMap;
        this.writingQueue = writingQueue;
        this.waitingQueue = waitingQueue;
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
        if (isFinished()) return;
        if (connection.isShutdown()) {
            EasyException e = new EasyException(ErrorCode.SHUT_DOWN, ErrorType.SYSTEM,
                    "Connection is show down.");
            onError(e);
        } else {
            connection.start();
            taskMap.put(taskId, this);
            if (connection.isAvailable() || isInitialize()) {
                onEncode();
            } else {
                waitingQueue.add(this);
            }
        }

    }

    void onEncode() {
        if (isFinished()) return;
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

    void onEnqueue() {
        if (!isFinished() && !writingQueue.offer(this)) {
            taskMap.remove(taskId);
            EasyException e = new EasyException(ErrorCode.TASK_REFUSED,
                    ErrorType.TASK, "Task queue refuse to accept task!");
            onError(e);
        }
    }

    void onCancel() {
        if (!isFinished()) {
            taskMap.remove(taskId);
            writingQueue.remove(this);
            waitingQueue.remove(this);
            state = State.CANCELED;
            callbackExecutor.execute(callback::onCancel);
        }
    }

    void onSend() {
        if (!isFinished()) {
            eventManager.publish(Events.TASK_TIME_OUT, this, options.getRequestTimeOut());
            callbackExecutor.execute(callback::onStart);
        }
    }

    void onReceive(Packet<?> packet) {
        eventManager.remove(Events.TASK_TIME_OUT, this);
        codecExecutor.execute(() -> {
            try {
                response = request.decode(packet);
                eventManager.publish(Events.TASK_SUCCESS, this);
            } catch (Exception e) {
                exception = e;
                eventManager.publish(Events.TASK_ERROR, this);
            }
        });
    }

    void onSuccess() {
        if (!isFinished()) {
            taskMap.remove(taskId);
            state = State.SUCCESS;
            callbackExecutor.execute(() -> callback.onSuccess(response));
        }
    }

    void onError(@NonNull Exception e) {
        state = State.ERROR;
        exception = e;
        callbackExecutor.execute(() -> callback.onError(e));
    }

    void onError() {
        if (!isFinished()) {
            taskMap.remove(taskId);
            state = State.ERROR;
            callbackExecutor.execute(() -> callback.onError(exception));
        }
    }

    void onTimeout() {
        if (!isFinished()) {
            taskMap.remove(taskId);
            EasyException e = new EasyException(ErrorCode.RESPONSE_TIME_OUT,
                    ErrorType.RESPONSE, "Response time out.");
            onError(e);
        }
    }

    @Override
    @NonNull
    public Request<R> request() {
        return request;
    }
}
