package com.orientsec.easysocket.impl;

import android.util.SparseArray;

import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
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

    private SocketConnection<T> connection;

    private Executor codecExecutor;

    RequestTaskManager(SocketConnection<T> connection) {
        this.connection = connection;
        codecExecutor = connection.options.getCodecExecutor();
        taskQueue = new LinkedBlockingQueue<>();
        taskArray = new SparseArray<>();
        waitingList = new ArrayList<>();
    }

    @Override
    public int generateTaskId() {
        return taskId.getAndIncrement();
    }

    private void error(RequestTask<T, ?, ?> task, Event event, String message) {
        task.onError(new EasyException(event, message));
    }

    @Override
    public synchronized void add(RequestTask<T, ?, ?> task) {
        if (!task.onPrepare()) {
            error(task, Event.TASK_REFUSED, "Task has already executed.");
        } else if (connection.isShutdown()) {
            error(task, Event.SHUT_DOWN, "Connection is show down.");
        } else if (!ConnectionManager.getInstance().isNetworkAvailable()) {
            error(task, Event.NETWORK_NOT_AVAILABLE, "Network is unavailable!");
        } else {
            connection.start();
            int taskId = task.getTaskId();

            if (task.isSync()) {
                if (taskArray.indexOfKey(taskId) > 0) {
                    error(task, Event.TASK_REFUSED, "A sync task is already running!");
                    return;
                }
            }
            taskArray.put(taskId, task);
            encodeOrSendTask(task);
        }
    }

    private void encodeOrSendTask(RequestTask<T, ?, ?> task) {
        //任务已取消
        if (!task.isPreparing()) return;
        if (connection.isAvailable() || task.isInit()) {
            if (task.getData() == null) {
                encodeTask(task);
            } else {
                if (!taskQueue.offer(task)) {
                    taskArray.remove(task.getTaskId());
                    error(task, Event.TASK_REFUSED, "Task queue refuse to accept task!");
                }
            }
        } else {
            waitingList.add(task);
        }
    }

    private void encodeTask(RequestTask<T, ?, ?> task) {
        codecExecutor.execute(() -> {
            try {
                task.encode();
                synchronized (RequestTaskManager.this) {
                    encodeOrSendTask(task);
                }
            } catch (EasyException e) {
                synchronized (RequestTaskManager.this) {
                    taskArray.remove(task.getTaskId());
                    task.onError(e);
                }
            }
        });
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
            taskArray.remove(task.getTaskId());
            task.onReceive(null);
        } else {
            task.onSend();
        }
    }

    @Override
    public synchronized void remove(RequestTask<T, ?, ?> task, Event event) {
        if (!task.isFinished()) {
            if (event == Event.TASK_CANCELED) {
                task.onCanceled();
            } else {
                error(task, event, event.getMessage());
            }
            int index = taskArray.indexOfKey(task.getTaskId());
            if (index >= 0) {
                taskArray.removeAt(index);
                taskQueue.remove(task);
                waitingList.remove(task);
            }
        }
    }

    @Override
    public synchronized void clear(Event event) {
        for (int i = 0; i < taskArray.size(); i++) {
            RequestTask<T, ?, ?> task = taskArray.valueAt(i);
            error(task, event, "Connection closed.");
        }
        taskArray.clear();
        taskQueue.clear();
        waitingList.clear();
    }

    @Override
    public synchronized void onReady() {
        for (RequestTask<T, ?, ?> task : waitingList) {
            encodeOrSendTask(task);
        }
        waitingList.clear();
    }
}
