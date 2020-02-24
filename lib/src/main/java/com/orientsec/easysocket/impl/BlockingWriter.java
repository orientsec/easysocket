package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Event;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2017/12/27 16:19
 * Author: Fredric
 * coding is art not science
 */
public class BlockingWriter<T> extends Looper implements Writer {
    private OutputStream mOutputStream;
    private SocketConnection<T> connection;
    private LinkedBlockingQueue<RequestTask<T, ?, ?>> packetQueue;

    BlockingWriter(AbstractConnection<T> context) {
        this.connection = (SocketConnection<T>) context;
        packetQueue = new LinkedBlockingQueue<>();
    }

    boolean addTask(RequestTask<T, ?, ?> task) {
        return packetQueue.offer(task);
    }

    void removeTask(RequestTask<T, ?, ?> task) {
        packetQueue.remove(task);
    }

    private void write(RequestTask<T, ?, ?> task) throws IOException {
        TaskManager<T, ?> taskManager = connection.taskManager();
        mOutputStream.write(task.getData());
        mOutputStream.flush();
        taskManager.onSend(task);
    }

    @Override
    public void write() throws IOException {
        try {
            RequestTask<T, ?, ?> task = packetQueue.take();
            write(task);
        } catch (InterruptedException e) {
            //ignore;
        }
    }

    @Override
    protected void beforeLoop() throws IOException {
        mOutputStream = connection.socket().getOutputStream();

        connection.onReady();
    }

    @Override
    protected void runInLoopThread() throws IOException {
        write();
    }

    @Override
    protected void loopFinish(Exception e) {
        Event event = Event.EMPTY;
        if (e != null) {
            //e.printStackTrace();
            Logger.e("Blocking write error, thread is dead with exception: " + e.getMessage());
            if (e instanceof IOException) {
                event = Event.WRITE_IO_ERROR;
            } else if (e instanceof EasyException) {
                event = ((EasyException) e).getEvent();
            }
        }
        mOutputStream = null;

        connection.disconnect(event);
    }
}
