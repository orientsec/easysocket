package com.orientsec.easysocket.client;

import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;
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
    private final OperableSession session;
    private final Socket socket;
    private final TaskManager taskManager;
    private final BlockingQueue<Task<?>> taskQueue;

    BlockingWriter(OperableSession session, Socket socket, TaskManager taskManager) {
        super(session.getLogger());
        this.session = session;
        this.socket = socket;
        this.taskManager = taskManager;
        taskQueue = taskManager.taskQueue();
    }

    @Override
    public void write() throws IOException {
        try {
            Task<?> task = taskQueue.take();
            mOutputStream.write(task.data());
            mOutputStream.flush();
            taskManager.onTaskSend(task);
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
            session.postClose(ErrorCode.WRITE_EXIT, ErrorType.CONNECT,
                    "Socket write aborted.", error);
        }
    }
}
