package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.Errors;
import com.orientsec.easysocket.task.Task;
import com.orientsec.easysocket.task.TaskManager;

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
    private final BlockingQueue<Task<?>> taskQueue;
    private final Socket socket;
    private final EventManager eventManager;

    BlockingWriter(Socket socket, EasySocket easySocket, TaskManager taskManager) {
        super(easySocket.getLogger());
        this.socket = socket;
        this.eventManager = easySocket.getEventManager();
        this.taskQueue = taskManager.taskQueue();
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
    protected synchronized void loopFinish() {
        if (isRunning()) {
            eventManager.publish(Events.CONNECT_ERROR,
                    Errors.connectError(ErrorCode.WRITE_EXIT, "Blocking writer exit."));
        }
    }
}
