package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.exception.AuthorizeException;
import com.orientsec.easysocket.exception.WriteException;
import com.orientsec.easysocket.inner.AbstractConnection;
import com.orientsec.easysocket.inner.Looper;
import com.orientsec.easysocket.inner.MessageType;
import com.orientsec.easysocket.inner.Writer;
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
    private Options<T> options;
    private OutputStream mOutputStream;
    private SocketConnection<T> connection;
    private LinkedBlockingQueue<Message<T>> messageQueue;
    private Authorize authorize;

    BlockingWriter(AbstractConnection<T> context) {
        this(context, null);
    }

    BlockingWriter(AbstractConnection<T> context, Authorize authorize) {
        this.authorize = authorize;
        this.connection = (SocketConnection<T>) context;
        options = context.options();
        messageQueue = connection.taskExecutor().getMessageQueue();
    }

    private void write(Message<T> message) throws IOException {
        try {
            connection.taskExecutor().onSendStart(message);
            byte[] data = options.getProtocol().encodeMessage(message);
            mOutputStream.write(data);
            mOutputStream.flush();
            connection.taskExecutor().onSendSuccess(message);
        } catch (WriteException e) {
            connection.taskExecutor().onSendError(message, e);
        }
    }

    @Override
    public void write() throws IOException {
        try {
            Message<T> message = messageQueue.take();
            write(message);
        } catch (InterruptedException e) {
            //ignore;
        }
    }

    @Override
    protected void beforeLoop() throws IOException, AuthorizeException {
        mOutputStream = connection.socket().getOutputStream();
        if (authorize != null) {
            write(connection.buildMessage(MessageType.AUTH));
            if (!authorize.waitForAuthorize()) {
                throw new AuthorizeException("auth failed!");
            }
        }
        connection.onLogin();
    }

    @Override
    protected void runInLoopThread() throws IOException {
        write();
    }

    @Override
    protected void loopFinish(Exception e) {
        int error = 999;
        if (e != null) {
            //e.printStackTrace();
            Logger.e("Blocking write error, thread is dead with exception: " + e.getMessage());
            if (e instanceof IOException) {
                error = 1;
            } else if (e instanceof AuthorizeException) {
                error = -2;
            }
        }
        mOutputStream = null;

        connection.disconnect(error);
    }
}
