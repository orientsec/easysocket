package com.orientsec.easysocket.client;

import android.net.TrafficStats;

import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;
import com.orientsec.easysocket.task.Task;

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
    private final BlockingQueue<Task<?>> taskQueue;
    private final Options options;

    BlockingWriter(OperableSession session, Socket socket, Options options,
                   BlockingQueue<Task<?>> taskQueue) {
        super(session.getLogger());
        this.session = session;
        this.socket = socket;
        this.options = options;
        this.taskQueue = taskQueue;
    }

    @Override
    public void write() throws IOException {
        try {
            Task<?> task = taskQueue.take();
            mOutputStream.write(task.getData());
            mOutputStream.flush();
            task.onRequestSent();
        } catch (InterruptedException e) {
            //ignore;
        }
    }

    @Override
    protected void beforeLoop() throws IOException {
        TrafficStats.setThreadStatsTag(options.getWriteStatsTag());
        mOutputStream = socket.getOutputStream();
    }

    @Override
    protected void runInLoopThread() throws IOException {
        write();
    }

    @Override
    protected synchronized void loopFinish() {
        TrafficStats.clearThreadStatsTag();
        if (isRunning()) {
            session.onError(ErrorCode.WRITE_EXIT, ErrorType.CONNECT,
                    "socket write aborted", error);
        }
    }
}
