package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.exception.EasyException;
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
public class TaskHolder implements TaskManager, EventListener {

    private final AtomicInteger uniqueId = new AtomicInteger(1);

    private final Map<Integer, RequestTask<?>> taskMap = new HashMap<>();

    private final LinkedBlockingQueue<Task<?>> writingQueue = new LinkedBlockingQueue<>();

    private final Queue<RequestTask<?>> waitingQueue = new LinkedList<>();

    private final Connection connection;

    private final EventManager eventManager;

    private final Options options;

    public TaskHolder(EasyConnection connection, EventManager eventManager, Options options) {
        this.connection = connection;
        this.eventManager = eventManager;
        this.options = options;
        eventManager.addListener(this);
    }

    @Override
    public BlockingQueue<Task<?>> taskQueue() {
        return writingQueue;
    }

    @NonNull
    @Override
    public <R> Task<R> buildTask(@NonNull Request<R> request, @NonNull Callback<R> callback) {
        return new RequestTask<>(uniqueId.getAndIncrement(), request, callback, connection,
                options, eventManager, taskMap, writingQueue, waitingQueue);
    }

    @Override
    public void handlePacket(@NonNull Packet packet) {
        RequestTask<?> task = taskMap.get(packet.getTaskId());
        if (task != null) {
            task.onReceive(packet);
        }
    }

    @Override
    public void reset(EasyException e) {
        eventManager.remove(Events.TASK_TIME_OUT);
        for (RequestTask<?> task : taskMap.values()) {
            task.onError(e);
        }
        taskMap.clear();
        waitingQueue.clear();
        writingQueue.clear();
    }

    @Override
    public void start() {
        for (RequestTask<?> task : waitingQueue) {
            task.onEncode();
        }
        waitingQueue.clear();
    }


    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        if (eventId > 0) return;
        RequestTask<?> task = (RequestTask<?>) object;
        assert task != null;
        switch (eventId) {
            case Events.TASK_START:
                task.onStart();
                break;
            case Events.TASK_ENQUEUE:
                task.onEnqueue();
                break;
            case Events.TASK_SEND:
                task.onSend();
                break;
            case Events.TASK_SUCCESS:
                task.onSuccess();
                break;
            case Events.TASK_ERROR:
                task.onError();
                break;
            case Events.TASK_CANCEL:
                task.onCancel();
                break;
            case Events.TASK_TIME_OUT:
                task.onTimeout();
                break;
            default:
                break;
        }
    }
}
