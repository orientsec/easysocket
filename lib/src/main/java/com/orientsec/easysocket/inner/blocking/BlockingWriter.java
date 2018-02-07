package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.exception.AuthorizeException;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.WriteException;
import com.orientsec.easysocket.inner.AbstractConnection;
import com.orientsec.easysocket.inner.Looper;
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
public class BlockingWriter extends Looper implements Writer {
    private Options options;
    private OutputStream mOutputStream;
    private SocketConnection connection;
    private LinkedBlockingQueue<Message> messageQueue;
    private Authorize authorize;

    BlockingWriter(AbstractConnection context) {
        this(context, null);
    }

    BlockingWriter(AbstractConnection context, Authorize authorize) {
        this.authorize = authorize;
        this.connection = (SocketConnection) context;
        options = context.options();
        messageQueue = connection.taskExecutor().getMessageQueue();
    }

    private void write(Message message) throws IOException {
        try {
            byte[] data = options.getProtocol().encodeMessage(message);
            mOutputStream.write(data);
            mOutputStream.flush();
            connection.taskExecutor().onSend(message);
        } catch (WriteException e) {
            connection.taskExecutor().onSendError(message, e);
        }
    }

    @Override
    public void write() throws IOException {
        try {
            Message message = messageQueue.take();
            write(message);
        } catch (InterruptedException e) {
            //ignore;
        }
    }

    @Override
    protected void beforeLoop() throws IOException, EasyException {
        mOutputStream = connection.socket().getOutputStream();
        if (authorize != null) {
            write(authorize.authorizeMessage());
            if (!authorize.waitForAuthorize()) {
                throw new AuthorizeException();
            }
        }
    }

    @Override
    protected void runInLoopThread() throws IOException, EasyException {
        write();
    }

    @Override
    protected void loopFinish(Exception e) {
        if (e != null) {
            Logger.e("Blocking write error, thread is dead with exception: " + e.getMessage());
        }
        mOutputStream = null;
        connection.disconnect();
    }
}
