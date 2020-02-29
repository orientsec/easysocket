package com.orientsec.easysocket.impl;

import android.util.SparseArray;

import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Error;

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
public class RequestTaskManager<T> implements TaskManager<T, RequestTask<T, ?, ?>> {
    private AtomicInteger taskId = new AtomicInteger(1);

    private SparseArray<RequestTask<T, ?, ?>> taskArray;

    LinkedBlockingQueue<RequestTask<T, ?, ?>> taskQueue;

    private List<RequestTask<T, ?, ?>> waitingList;

    private Connection<T> connection;

    RequestTaskManager(Connection<T> connection) {
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
    public synchronized void add(RequestTask<T, ?, ?> task) throws EasyException {
        if (task.isSync()) {
            if (taskArray.indexOfKey(task.taskId) > 0) {
                throw Error.create(Error.Code.TASK_REFUSED,
                        "A sync task is already running!");
            }
        }
        taskArray.put(task.taskId, task);
        if (connection.isAvailable() || task.isInit()) {
            task.encode();
        } else {
            waitingList.add(task);
        }
    }

    @Override
    public synchronized void enqueue(RequestTask<T, ?, ?> task) throws EasyException {
        if (!taskQueue.offer(task)) {
            taskArray.remove(task.taskId);
            throw Error.create(Error.Code.TASK_REFUSED,
                    "Task queue refuse to accept task!");
        }
    }

    @Override
    public synchronized void handlePacket(Packet<T> packet) {
        int taskId = packet.getTaskId();
        int index = taskArray.indexOfKey(taskId);
        if (index >= 0) {
            RequestTask<T, ?, ?> requestTask = taskArray.valueAt(index);
            taskArray.removeAt(index);
            requestTask.onReceive(packet.getBody());
        }
    }

    @Override
    public synchronized void onSend(RequestTask<T, ?, ?> task) {
        //Release data after send success.
        if (task.isSendOnly()) {
            taskArray.remove(task.taskId);
            task.onReceive(null);
        } else {
            task.onSend();
        }
    }

    @Override
    public synchronized void remove(RequestTask<T, ?, ?> task) {
        int index = taskArray.indexOfKey(task.taskId);
        if (index >= 0) {
            taskArray.removeAt(index);
            taskQueue.remove(task);
            waitingList.remove(task);
        }
    }

    @Override
    public synchronized void clear(EasyException e) {
        for (int i = 0; i < taskArray.size(); i++) {
            RequestTask<T, ?, ?> task = taskArray.valueAt(i);
            task.onError(e);
        }
        taskArray.clear();
        taskQueue.clear();
        waitingList.clear();
    }

    @Override
    public synchronized void onReady() {
        for (RequestTask<T, ?, ?> task : waitingList) {
            task.encode();
        }
        waitingList.clear();
    }
}
