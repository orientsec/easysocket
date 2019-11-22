package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.PushHandler;
import com.orientsec.easysocket.TaskType;
import com.orientsec.easysocket.exception.ConnectException;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.NetworkException;
import com.orientsec.easysocket.exception.TimeoutException;
import com.orientsec.easysocket.exception.WriteException;
import com.orientsec.easysocket.inner.MessageType;
import com.orientsec.easysocket.inner.TaskExecutor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2018/01/09 15:28
 * Author: Fredric
 * coding is art not science
 */
public class BlockingExecutor<T> implements TaskExecutor<T, EasyTask<T, ?, ?>> {

    private Map<Integer, EasyTask<T, ?, ?>> taskMap = new ConcurrentHashMap<>();

    private LinkedBlockingQueue<Message<T>> messageQueue = new LinkedBlockingQueue<>();

    private SocketConnection connection;

    private PushHandler<T> pushHandler;

    LinkedBlockingQueue<Message<T>> getMessageQueue() {
        return messageQueue;
    }

    BlockingExecutor(SocketConnection<T> connection) {
        this.connection = connection;
        pushHandler = connection.options().getPushHandler();
    }

    @Override
    public void execute(EasyTask<T, ?, ?> task) {
        if (connection.isShutdown()) {
            throw new IllegalStateException("connection is show down!");
        }
        connection.connect();
        if (ConnectionManager.getInstance().isNetworkAvailable()) {
            Message<T> message = task.getMessage();
            taskMap.put(message.getTaskId(), task);
            if (!messageQueue.offer(message)) {
                taskMap.remove(message.getTaskId());
                task.onError(new EasyException("task refuse to execute!"));
            }
        } else {
            task.onError(new NetworkException("network is unavailable!"));
        }
    }

    @Override
    public void onReceive(Message<T> message) {
        if (message.getMessageType() == MessageType.PULSE) {
            connection.pulse().feed();
        } else if (message.getMessageType() == MessageType.PUSH) {
            connection.options().getDispatchExecutor().execute(() -> pushHandler.onPush(message.getBody()));
        } else {
            EasyTask<T, ?, ?> easyTask = taskMap.remove(message.getTaskId());
            if (easyTask != null) {
                easyTask.onSuccess(message.getBody());
            }
        }
    }

    @Override
    public void onSendStart(Message message) {
        EasyTask easyTask = taskMap.get(message.getTaskId());
        if (easyTask != null) {
            easyTask.onStart();
        }
    }

    @Override
    public void onSendSuccess(Message message) {
        EasyTask easyTask = taskMap.get(message.getTaskId());
        if (easyTask != null) {
            if (easyTask.getTaskType() == TaskType.SEND_ONLY) {
                easyTask.onSuccess();
                taskMap.remove(message.getTaskId());
            } else {
                easyTask.timeoutFuture = connection.options().getExecutorService()
                        .schedule(() -> {
                            taskMap.remove(message.getTaskId());
                            easyTask.onError(new TimeoutException("request time out"));
                        }, connection.options().getRequestTimeOut(), TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public void onSendError(Message message, WriteException exception) {
        EasyTask easyTask = taskMap.remove(message.getTaskId());
        if (easyTask != null) {
            easyTask.onError(exception);
        }
    }

    @Override
    public void remove(EasyTask task) {
        if (taskMap.remove(task.getMessage().getTaskId()) != null) {
            messageQueue.remove(task.getMessage());
        }
    }

    void onConnectionClosed() {
        ConnectException exception = new ConnectException("connect failed");
        messageQueue.clear();
        Set<Map.Entry<Integer, EasyTask<T, ?, ?>>> entrySet = taskMap.entrySet();
        for (Map.Entry<Integer, EasyTask<T, ?, ?>> entry : entrySet) {
            entry.getValue().onError(exception);
        }
        taskMap.clear();
    }


}
