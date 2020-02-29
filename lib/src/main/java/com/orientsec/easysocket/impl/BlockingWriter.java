package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Error;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;

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
    private BlockingQueue<RequestTask<T, ?, ?>> taskQueue;

    BlockingWriter(AbstractConnection<T> context,
                   BlockingQueue<RequestTask<T, ?, ?>> taskQueue) {
        this.connection = (SocketConnection<T>) context;
        this.taskQueue = taskQueue;
    }

    @Override
    public void write() throws IOException {
        try {
            RequestTask<T, ?, ?> task = taskQueue.take();
            TaskManager<T, ?> taskManager = connection.taskManager();
            mOutputStream.write(task.getData());
            mOutputStream.flush();
            taskManager.onSend(task);
        } catch (InterruptedException e) {
            //ignore;
        }
    }

    @Override
    protected void beforeLoop() throws IOException {
        mOutputStream = connection.socket().getOutputStream();
    }

    @Override
    protected void runInLoopThread() throws IOException {
        write();
    }

    @Override
    protected void loopFinish(Exception e) {
        EasyException ee;
        if (e != null) {
            //e.printStackTrace();
            Logger.e("Blocking writer error, " +
                    "thread is dead with exception: " + e.getMessage());
            if (e instanceof IOException) {
                ee = Error.create(Error.Code.WRITE_IO, e);
            } else if (e instanceof EasyException) {
                ee = (EasyException) e;
            } else {
                ee = Error.create(Error.Code.WRIT_OTHER, e);
            }
        } else {
            ee = Error.create(Error.Code.WRITE_EXIT);
        }
        mOutputStream = null;
        connection.disconnect(ee);
    }
}
