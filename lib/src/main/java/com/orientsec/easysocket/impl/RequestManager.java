package com.orientsec.easysocket.impl;

import android.util.SparseArray;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.TaskType;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2018/01/09 15:28
 * Author: Fredric
 * coding is art not science
 */
public class RequestManager<T> implements TaskManager<T, RequestTask<T, ?, ?>> {
    private final byte[] lock = new byte[0];
    private AtomicInteger taskId = new AtomicInteger();

    private SparseArray<RequestTask<T, ?, ?>> taskArray;

    LinkedBlockingQueue<RequestTask<T, ?, ?>> taskQueue;

    private List<RequestTask<T, ?, ?>> waitingList;

    private SocketConnection<T> connection;

    RequestManager(SocketConnection<T> connection) {
        this.connection = connection;
        taskQueue = new LinkedBlockingQueue<>();
        taskArray = new SparseArray<>();
        waitingList = new ArrayList<>();
    }

    @Override
    public int generateTaskId() {
        return taskId.getAndIncrement();
    }

    @Override
    public boolean add(RequestTask<T, ?, ?> task) {
        synchronized (lock) {
            taskArray.put(task.getTaskId(), task);
            if (connection.isAvailable() || task.isInitTask()) {
                if (!taskQueue.offer(task)) {
                    taskArray.remove(task.getTaskId());
                    return false;
                }
            } else {
                waitingList.add(task);
            }
            return true;
        }
    }

    @Override
    public void handleMessage(Packet<T> packet) {
        int taskId = packet.getTaskId();
        RequestTask<T, ?, ?> requestTask = null;
        synchronized (lock) {
            int index = taskArray.indexOfKey(taskId);
            if (index >= 0) {
                requestTask = taskArray.valueAt(taskId);
                taskArray.removeAt(index);
            }
        }
        if (requestTask != null) {
            requestTask.onSuccess(packet.getBody());
        }
    }

    @Override
    public void onSend(RequestTask<T, ?, ?> task) {
        //Release data after send success.
        if (task.getTaskType() == TaskType.SEND_ONLY) {
            synchronized (lock) {
                taskArray.remove(task.getTaskId());
            }
            task.onSuccess();
        } else {
            task.onStart();
        }
    }

    @Override
    public void remove(RequestTask<T, ?, ?> task) {
        synchronized (lock) {
            int taskId = task.getTaskId();
            int index = taskArray.indexOfKey(taskId);
            if (index >= 0) {
                taskArray.removeAt(index);
                taskQueue.remove(task);
                waitingList.remove(task);
            }
        }
    }

    @Override
    public void clear(Event event) {
        synchronized (lock) {
            EasyException exception = new EasyException(event, "Connection closed.");
            for (int i = 0; i < taskArray.size(); i++) {
                RequestTask<T, ?, ?> task = taskArray.valueAt(i);
                task.onError(exception);
            }
            taskArray.clear();
            taskQueue.clear();
            waitingList.clear();
        }
    }

    @Override
    public void onReady() {
        synchronized (lock) {
            taskQueue.addAll(waitingList);
            waitingList.clear();
        }
    }
}
