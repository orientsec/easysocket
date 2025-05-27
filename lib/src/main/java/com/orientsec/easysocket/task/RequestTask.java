package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.SocketClient;
import com.orientsec.easysocket.client.AbstractSocketClient;
import com.orientsec.easysocket.EasyRunner;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.request.Result;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 17:01
 * Author: Fredric
 * coding is art not science
 */

public class RequestTask<R extends T, T> implements Task<R>, Runnable {

    // Guarded by this.
    private final AtomicBoolean executed = new AtomicBoolean();
    private final AbstractSocketClient socketClient;
    private final Request<R> request;
    private final Callback<T> callback;
    private final Executor callbackExecutor;
    private final Executor codecExecutor;
    private final EasyRunner runner;
    private final Options options;

    /**
     * 每一个任务的id是唯一的，通过taskId，客户端可以匹配每个请求的返回
     */
    final int taskId;

    /**
     * 任务类型
     */
    final TaskType taskType;

    /**
     * 请求任务结束状态
     */
    private volatile State state = null;
    /**
     * 编码后的请求数据
     *
     * @see Request#encode(int)
     */
    private byte[] data = new byte[0];

    private final RealTaskManager taskManager;

    public RequestTask(int taskId,
                       TaskType taskType,
                       Request<R> request,
                       Callback<T> callback,
                       AbstractSocketClient socketClient) {
        this.request = request;
        this.callback = callback;
        this.taskId = taskId;
        this.taskType = taskType;
        this.socketClient = socketClient;
        this.options = socketClient.getOptions();
        this.taskManager = (RealTaskManager) socketClient.getTaskManager();
        this.runner = socketClient.getEasyRunner();
        this.callbackExecutor = options.getCallbackExecutor();
        this.codecExecutor = options.getCodecExecutor();
    }

    @Override
    public int getTaskId() {
        return taskId;
    }

    /**
     * 获取请求的编码数据。
     * 在加入请求队列之前，对request进行编码，编码成功之后data才会有数据。
     *
     * @return 请求的编码数据
     */
    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public void execute() {
        if (executed.compareAndSet(false, true)) {
            runner.post(this::onStart);
        } else {
            throw new IllegalStateException("task is already executed");
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
        runner.post(this::onCancel);
    }

    @Override
    public boolean isCanceled() {
        return state == State.CANCELED;
    }

    @Override
    @NonNull
    public Request<R> request() {
        return request;
    }

    @Override
    public void run() {
        onTimeout();
    }

    private void onStart() {
        if (isFinished()) return;
        SocketClient socketClient = this.socketClient;
        if (socketClient.isShutdown()) {
            onFailure(ErrorCode.SHUTDOWN, ErrorType.SYSTEM, "socket client is shutdown");
        } else if (taskType == TaskType.PULSE) {
            if (socketClient.isAvailable()) {
                taskManager.start(this);
                onEncode();
            }
        } else {
            socketClient.start();
            taskManager.start(this);
            if (socketClient.isAvailable() || taskType == TaskType.INITIALIZE) {
                onEncode();
            } else {
                taskManager.wait(this);
            }
        }
    }

    void onEncode() {
        if (isFinished()) return;
        //对请求消息进行编码, 获取最终写入的字节数组。
        codecExecutor.execute(() -> {
            try {
                Result<byte[]> result = request.encode(taskId);
                if (result.isSuccess()) {
                    data = result.get();
                    if (data.length == 0) {
                        runner.post(() -> {
                            taskManager.remove(this);
                            onFailure(ErrorCode.REQUEST_DATA_EMPTY, ErrorType.TASK,
                                    "request data is empty");
                        });
                    } else {
                        runner.post(this::onEnqueue);
                    }
                } else {
                    runner.post(() -> {
                        taskManager.remove(this);
                        onFailure(result.error());
                    });
                }
            } catch (Throwable t) {
                runner.post(() -> {
                    taskManager.remove(this);
                    onFailure(t);
                });
            }
        });
    }

    private void onEnqueue() {
        if (isFinished()) return;
        if (!taskManager.enqueue(this)) {
            taskManager.remove(this);
            onFailure(ErrorCode.TASK_REFUSED, ErrorType.SYSTEM,
                    "task queue refuse to accept task");
        }
    }

    private void onCancel() {
        if (!isFinished()) {
            taskManager.cancel(this);
            state = State.CANCELED;
            callbackExecutor.execute(callback::onCanceled);
        }
    }

    @Override
    public void onRequestSent() {
        if (!isFinished()) {
            runner.post(this::onSent);
        }
    }

    private void onSent() {
        if (isFinished()) return;
        runner.postDelayed(this, this, options.getRequestTimeOut());
        callbackExecutor.execute(callback::onSent);
    }

    void onReceive(Packet packet) {
        runner.remove(this, this);
        codecExecutor.execute(() -> {
            try {
                Result<R> result = request.decode(packet);
                if (result.isSuccess()) {
                    runner.post(() -> onSuccess(result.get()));
                } else {
                    runner.post(() -> onFailure(result.error()));
                }
            } catch (Throwable t) {
                runner.post(() -> onFailure(t));
            }
        });
    }

    private void onTimeout() {
        taskManager.remove(this);
        onFailure(ErrorCode.RESPONSE_TIME_OUT, ErrorType.TASK, "response time out");
    }

    private void onSuccess(R response) {
        if (!isFinished()) {
            state = State.SUCCESS;
            callbackExecutor.execute(() -> callback.onSuccess(response));
        }
    }

    void onFailure(@NonNull Throwable t) {
        if (!isFinished()) {
            state = State.FAILURE;
            callbackExecutor.execute(() -> callback.onFailure(t));
        }
    }

    private void onFailure(int code, int type, String msg) {
        if (!isFinished()) {
            state = State.FAILURE;
            Throwable t = socketClient.errorBuilder.create(code, type, msg);
            callbackExecutor.execute(() -> callback.onFailure(t));
        }
    }


    protected enum State {
        //收到响应
        SUCCESS,
        //出错
        FAILURE,
        //取消
        CANCELED,
    }
}
