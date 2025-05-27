package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.client.AbstractSocketClient;
import com.orientsec.easysocket.error.EasyException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2018/01/09 15:28
 * Author: Fredric
 * coding is art not science
 */
public class RealTaskManager implements TaskManager {

    private final Map<Integer, RequestTask<?, ?>> taskMap = new HashMap<>();

    private final Queue<RequestTask<?, ?>> waitingQueue = new LinkedList<>();

    private final BlockingQueue<Task<?>> writingQueue = new LinkedBlockingQueue<>();

    private final AbstractSocketClient socketClient;

    public RealTaskManager(AbstractSocketClient socketClient) {
        this.socketClient = socketClient;
    }

    @Override
    public BlockingQueue<Task<?>> getTaskQueue() {
        return writingQueue;
    }

    @Override
    public void handlePacket(@NonNull Packet packet) {
        RequestTask<?, ?> task = taskMap.remove(packet.getTaskId());
        if (task != null) {
            task.onReceive(packet);
        }
    }

    @Override
    public void reset(@NonNull EasyException e) {
        for (RequestTask<?, ?> task : taskMap.values()) {
            task.onFailure(e);
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

    void wait(@NonNull RequestTask<?, ?> task) {
        waitingQueue.add(task);
    }

    void start(@NonNull RequestTask<?, ?> task) {
        taskMap.put(task.getTaskId(), task);
    }

    boolean enqueue(@NonNull Task<?> task) {
        return writingQueue.add(task);
    }

    void remove(@NonNull Task<?> task) {
        taskMap.remove(task.getTaskId());
    }

    void cancel(@NonNull RequestTask<?, ?> task) {
        boolean removeFromTaskMap = taskMap.remove(task.getTaskId()) != null;
        boolean removeFromWritingQueue = writingQueue.remove(task);
        boolean removeFromWaitingQueue = waitingQueue.remove(task);

        socketClient.getLogger().i("cancel task:" + task.getTaskId() +
                " removed from task map:" + removeFromTaskMap +
                " removed from writing queue:" + removeFromWritingQueue +
                " removed from waiting queue:" + removeFromWaitingQueue);
    }
}
