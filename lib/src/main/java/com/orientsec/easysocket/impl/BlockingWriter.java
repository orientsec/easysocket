package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.ErrorCode;
import com.orientsec.easysocket.exception.ErrorType;
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
    private BlockingQueue<RequestTask<T, ?>> taskQueue;

    BlockingWriter(AbstractConnection<T> context,
                   BlockingQueue<RequestTask<T, ?>> taskQueue) {
        this.connection = (SocketConnection<T>) context;
        this.taskQueue = taskQueue;
    }

    @Override
    public void write() throws IOException {
        try {
            RequestTask<T, ?> task = taskQueue.take();
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
            Logger.e("Blocking writer error, thread is dead with exception: "
                    + e.getMessage());
            if (e instanceof EasyException) {
                ee = (EasyException) e;
            } else if (e instanceof IOException) {
                ee = new EasyException(ErrorCode.WRITE_IO, ErrorType.CONNECT, e);
            } else {
                ee = new EasyException(ErrorCode.WRIT_OTHER, ErrorType.CONNECT, e);
            }
        } else {
            ee = new EasyException(ErrorCode.WRITE_EXIT, ErrorType.CONNECT,
                    "Write looper exit.");
        }
        mOutputStream = null;
        connection.disconnect(ee);
    }
}
