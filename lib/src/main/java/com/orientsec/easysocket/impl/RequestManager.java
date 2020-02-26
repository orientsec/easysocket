package com.orientsec.easysocket.impl;

import android.util.SparseArray;

import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.TaskType;
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
public class RequestManager<T> implements TaskManager<T, RequestTask<T, ?, ?>> {
    private final byte[] lock = new byte[0];
    private AtomicInteger taskId = new AtomicInteger(1);

    private SparseArray<RequestTask<T, ?, ?>> taskArray;

    LinkedBlockingQueue<RequestTask<T, ?, ?>> taskQueue;

    private List<RequestTask<T, ?, ?>> waitingList;

    private SocketConnection<T> connection;

    private Executor codecExecutor;

    RequestManager(SocketConnection<T> connection) {
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

    @Override
    public void add(RequestTask<T, ?, ?> task) {
        if (ConnectionManager.getInstance().isNetworkAvailable()) {
            connection.start();
            synchronized (lock) {
                int taskId = task.getTaskId();
                if (task.isSyncTask()) {
                    if (taskArray.indexOfKey(taskId) > 0) {
                        task.onError(new EasyException(Event.TASK_REFUSED,
                                "A sync task is already running!"));
                    }
                }
                taskArray.put(taskId, task);

                if (connection.isAvailable() || task.isInitTask()) {
                    executeEncodeTask(task);
                } else {
                    waitingList.add(task);
                }
            }
        } else {
            task.onError(new EasyException(Event.NETWORK_NOT_AVAILABLE,
                    "Network is unavailable!"));
        }
    }

    private void executeEncodeTask(RequestTask<T, ?, ?> task) {
        codecExecutor.execute(() -> {
            try {
                if (!task.encode()) return;
                if (!taskQueue.offer(task)) {
                    throw new EasyException(Event.TASK_REFUSED,
                            "Task queue refuse to accept task!");
                }
            } catch (EasyException e) {
                synchronized (lock) {
                    taskArray.remove(task.getTaskId());
                }
                task.onError(e);
            }
        });
    }

    @Override
    public void handleMessage(Packet<T> packet) {
        int taskId = packet.getTaskId();
        RequestTask<T, ?, ?> requestTask = null;
        synchronized (lock) {
            int index = taskArray.indexOfKey(taskId);
            if (index >= 0) {
                requestTask = taskArray.valueAt(index);
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
            int index = taskArray.indexOfKey(task.getTaskId());
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
            for (RequestTask<T, ?, ?> task : waitingList) {
                executeEncodeTask(task);
            }
            waitingList.clear();
        }
    }
}
