package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.ErrorCode;
import com.orientsec.easysocket.exception.ErrorType;
import com.orientsec.easysocket.task.Task;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2017/12/27 16:19
 * Author: Fredric
 * coding is art not science
 */
public class BlockingWriter extends Looper implements Writer {
    private OutputStream mOutputStream;
    private BlockingQueue<Task<?>> taskQueue;
    private final Socket socket;
    private final EventManager eventManager;

    BlockingWriter(Socket socket,
                   EventManager eventManager,
                   BlockingQueue<Task<?>> taskQueue) {
        this.socket = socket;
        this.eventManager = eventManager;
        this.taskQueue = taskQueue;
    }

    @Override
    public void write() throws IOException {
        try {
            Task<?> task = taskQueue.take();
            mOutputStream.write(task.data());
            mOutputStream.flush();
            eventManager.publish(Events.TASK_SEND, task);
        } catch (InterruptedException e) {
            //ignore;
        }
    }

    @Override
    protected void beforeLoop() throws IOException {
        mOutputStream = socket.getOutputStream();
    }

    @Override
    protected void runInLoopThread() throws IOException {
        write();
    }

    @Override
    protected void loopFinish(Exception e) {
        EasyException ee;
        if (e != null) {
            Logger.e("Blocking writer error, thread is dead with exception: " + e.getMessage());
            if (e instanceof EasyException) {
                ee = (EasyException) e;
            } else if (e instanceof IOException) {
                ee = new EasyException(ErrorCode.WRITE_IO, ErrorType.CONNECT, e);
            } else {
                ee = new EasyException(ErrorCode.WRIT_OTHER, ErrorType.CONNECT, e);
            }
        } else {
            ee = new EasyException(ErrorCode.WRITE_EXIT, ErrorType.CONNECT, "Write looper exit.");
        }
        synchronized (this) {
            if (!isShutdown()) {
                eventManager.publish(Events.STOP, ee);
            }
        }
    }
}
