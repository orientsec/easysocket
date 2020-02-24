package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.TaskType;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2018/01/09 15:28
 * Author: Fredric
 * coding is art not science
 */
public class RequestManager<T> implements TaskManager<T, RequestTask<T, ?, ?>> {

    private AtomicInteger taskId = new AtomicInteger();

    private Map<Integer, RequestTask<T, ?, ?>> taskMap = new ConcurrentHashMap<>();

    private SocketConnection<T> connection;

    RequestManager(SocketConnection<T> connection) {
        this.connection = connection;
    }

    @Override
    public int generateTaskId() {
        return taskId.getAndIncrement();
    }

    @Override
    public boolean addTask(RequestTask<T, ?, ?> task) {
        taskMap.put(task.getTaskId(), task);
        if (!connection.writer.addTask(task)) {
            taskMap.remove(task.getTaskId());
            return false;
        }
        return true;
    }

    @Override
    public void handleMessage(Packet<T> packet) {
        RequestTask<T, ?, ?> requestTask = taskMap.remove(packet.getTaskId());
        if (requestTask != null) {
            requestTask.onSuccess(packet.getBody());
        }
    }

    @Override
    public void onSend(RequestTask<T, ?, ?> task) {
        //Release data after send success.
        if (task.getTaskType() == TaskType.SEND_ONLY) {
            task.onSuccess();
            taskMap.remove(task.getTaskId());
        } else {
            task.onStart();
        }
    }

    @Override
    public void removeTask(RequestTask<T, ?, ?> task) {
        if (taskMap.remove(task.getTaskId()) != null) {
            connection.writer.removeTask(task);
        }
    }

    public void onConnectionClosed(Event event) {
        EasyException exception = new EasyException(event, "Connection closed.");
        for (RequestTask<T, ?, ?> task : taskMap.values()) {
            task.onError(exception);
        }
        taskMap.clear();
    }

}
