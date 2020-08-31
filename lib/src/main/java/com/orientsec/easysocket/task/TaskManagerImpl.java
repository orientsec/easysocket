package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.ErrorCode;
import com.orientsec.easysocket.exception.ErrorType;
import com.orientsec.easysocket.inner.EasyConnection;
import com.orientsec.easysocket.inner.EventListener;
import com.orientsec.easysocket.inner.EventManager;
import com.orientsec.easysocket.inner.Events;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2018/01/09 15:28
 * Author: Fredric
 * coding is art not science
 */
public class TaskManagerImpl<T> implements TaskManager<T>, EventListener {

    private final AtomicInteger uniqueId = new AtomicInteger(1);

    private final Map<Integer, RequestTask<T, ?>> taskMap = new HashMap<>();

    private final LinkedBlockingQueue<Task<T, ?>> taskQueue = new LinkedBlockingQueue<>();

    private final Queue<RequestTask<T, ?>> waitingQueue = new LinkedList<>();

    private final Connection<T> connection;

    private final EventManager eventManager;

    private final Options<T> options;

    public TaskManagerImpl(EasyConnection<T> connection, EventManager eventManager, Options<T> options) {
        this.connection = connection;
        this.eventManager = eventManager;
        this.options = options;
        eventManager.addListener(this);
    }

    @Override
    public BlockingQueue<Task<T, ?>> taskQueue() {
        return taskQueue;
    }

    @NonNull
    @Override
    public <RE> Task<T, RE> buildTask(@NonNull Request<T, RE> request, @NonNull Callback<RE> callback) {
        return new RequestTask<>(request, callback, eventManager, options, uniqueId.getAndIncrement());
    }

    @Override
    public void handlePacket(@NonNull Packet<T> packet) {
        RequestTask<T, ?> task = taskMap.remove(packet.getTaskId());
        if (task != null) {
            eventManager.remove(Events.TASK_TIME_OUT, task);
            task.onReceive(packet.getBody());
        }
    }

    @Override
    public void reset(EasyException e) {
        eventManager.remove(Events.TASK_TIME_OUT);
        for (RequestTask<T, ?> task : taskMap.values()) {
            task.onError(e);
        }
        taskMap.clear();
        taskQueue.clear();
        waitingQueue.clear();
    }

    @Override
    public void start() {
        for (RequestTask<T, ?> task : waitingQueue) {
            if (!task.isCanceled()) {
                task.onStart();
            }
        }
        waitingQueue.clear();
    }

    private void onTaskStart(RequestTask<T, ?> task) {
        if (task.isCanceled()) return;
        if (connection.isShutdown()) {
            EasyException e = new EasyException(ErrorCode.SHUT_DOWN, ErrorType.SYSTEM,
                    "Connection is show down.");
            task.onError(e);
        } else {
            connection.start();
            taskMap.put(task.taskId, task);
            if (connection.isAvailable() || task.isInitialize()) {
                task.onStart();
            } else {
                waitingQueue.add(task);
            }
        }
    }

    private void onTaskEnqueue(int taskId) {
        RequestTask<T, ?> task = taskMap.get(taskId);
        if (task == null) return;
        if (!taskQueue.offer(task)) {
            taskMap.remove(taskId);
            EasyException e = new EasyException(ErrorCode.TASK_REFUSED,
                    ErrorType.TASK, "Task queue refuse to accept task!");
            task.onError(e);
        }
    }

    private void onTaskError(int taskId) {
        RequestTask<T, ?> task = taskMap.remove(taskId);
        if (task != null) {
            task.onError();
        }
    }

    private void onTaskCancel(int taskId) {
        RequestTask<T, ?> task = taskMap.remove(taskId);
        if (task != null) {
            task.onCancel();
        }
    }

    private void onTaskSend(int taskId) {
        RequestTask<T, ?> task = taskMap.get(taskId);
        if (task != null) {
            eventManager.publish(Events.TASK_TIME_OUT, task, options.getRequestTimeOut());
            task.onSend();
        }
    }

    private void onTaskTimeout(int taskId) {
        RequestTask<T, ?> task = taskMap.remove(taskId);
        if (task != null) {
            EasyException e = new EasyException(ErrorCode.RESPONSE_TIME_OUT,
                    ErrorType.RESPONSE, "Response time out.");
            task.onError(e);
        }
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        if (eventId > 0) return;
        RequestTask<T, ?> task = (RequestTask<T, ?>) object;
        assert task != null;
        switch (eventId) {
            case Events.TASK_START:
                onTaskStart(task);
                break;
            case Events.TASK_ENQUEUE:
                onTaskEnqueue(task.taskId);
                break;
            case Events.TASK_SEND:
                onTaskSend(task.taskId);
                break;
            case Events.TASK_ERROR:
                onTaskError(task.taskId);
                break;
            case Events.TASK_CANCEL:
                onTaskCancel(task.taskId);
                break;
            case Events.TASK_TIME_OUT:
                onTaskTimeout(task.taskId);
                break;
            default:
                break;
        }
    }
}
