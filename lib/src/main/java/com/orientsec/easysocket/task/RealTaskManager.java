package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.client.AbstractSocketClient;
import com.orientsec.easysocket.client.EventListener;
import com.orientsec.easysocket.client.EventManager;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.request.Request;

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
public class RealTaskManager implements TaskManager, EventListener {

    private final AtomicInteger uniqueId = new AtomicInteger(1);

    private final Map<Integer, RequestTask<?, ?>> taskMap = new HashMap<>();

    private final Queue<RequestTask<?, ?>> waitingQueue = new LinkedList<>();

    private final BlockingQueue<Task<?>> writingQueue = new LinkedBlockingQueue<>();

    private final EventManager eventManager;

    private final AbstractSocketClient socketClient;

    public RealTaskManager(AbstractSocketClient socketClient, EventManager eventManager) {
        this.socketClient = socketClient;
        this.eventManager = eventManager;
        eventManager.addListener(this);
    }

    @NonNull
    @Override
    public <R extends T, T> Task<R> buildTask(@NonNull Request<R> request,
                                              @NonNull Callback<T> callback) {
        return new RequestTask<>(uniqueId.getAndIncrement(), request, callback, taskMap,
                waitingQueue, writingQueue, eventManager, socketClient);
    }

    @Override
    public BlockingQueue<Task<?>> taskQueue() {
        return writingQueue;
    }

    @Override
    public void handlePacket(@NonNull Packet packet) {
        RequestTask<?, ?> task = taskMap.get(packet.getTaskId());
        if (task != null) {
            task.onReceive(packet);
        }
    }

    @Override
    public void reset(@NonNull EasyException e) {
        eventManager.remove(RequestTask.TASK_TIME_OUT);
        for (RequestTask<?, ?> task : taskMap.values()) {
            task.onError(e);
        }
        taskMap.clear();
        waitingQueue.clear();
        writingQueue.clear();
    }

    @Override
    public void ready() {
        for (RequestTask<?, ?> task : waitingQueue) {
            task.onEncode();
        }
        waitingQueue.clear();
    }

    @Override
    public void onTaskSend(@NonNull Task<?> task) {
        eventManager.publish(RequestTask.TASK_SEND, task);
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        if (eventId < 300 || eventId > 400) return;
        RequestTask<?, ?> task = (RequestTask<?, ?>) object;
        assert task != null;
        switch (eventId) {
            case RequestTask.TASK_START:
                task.onStart();
                break;
            case RequestTask.TASK_ENQUEUE:
                task.onEnqueue();
                break;
            case RequestTask.TASK_SEND:
                task.onSend();
                break;
            case RequestTask.TASK_SUCCESS:
                task.onSuccess();
                break;
            case RequestTask.TASK_ERROR:
                task.onError();
                break;
            case RequestTask.TASK_CANCEL:
                task.onCancel();
                break;
            case RequestTask.TASK_TIME_OUT:
                task.onTimeout();
                break;
            default:
                break;
        }
    }
}
