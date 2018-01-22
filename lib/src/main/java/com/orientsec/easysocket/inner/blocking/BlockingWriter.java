package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.WriteException;
import com.orientsec.easysocket.inner.AbstractConnection;
import com.orientsec.easysocket.inner.Looper;
import com.orientsec.easysocket.inner.MessageType;
import com.orientsec.easysocket.inner.SendMessage;
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

    private LinkedBlockingQueue<SendMessage> messageQueue = new LinkedBlockingQueue<>();

    public BlockingWriter(AbstractConnection context) {
        this.connection = (SocketConnection) context;
        options = context.options();
    }

    @Override
    public void write() throws IOException {
        try {
            SendMessage message = connection.taskExecutor().getMessageQueue().take();
            if (!message.isValid()) {
                return;
            }
            if (message.getMessageType() == MessageType.PULSE) {
                mOutputStream.write(message.getBodyBytes());
                mOutputStream.flush();
            } else {
                try {
                    options.getProtocol().encodeMessage(message);
                    options.getProtocol().encodeHead(message);
                } catch (WriteException e) {
                    connection.taskExecutor().onSendError(message, e);
                    return;
                }
                byte[] head = message.getHeadBytes();
                byte[] body = message.getBodyBytes();
                byte[] sendBytes = new byte[head.length + body.length];
                System.arraycopy(head, 0, sendBytes, 0, head.length);
                System.arraycopy(body, 0, sendBytes, head.length, body.length);
                mOutputStream.write(sendBytes);
                mOutputStream.flush();

                connection.taskExecutor().onSend(message);
            }

        } catch (InterruptedException e) {
            //ignore;
        }
    }

    @Override
    protected void beforeLoop() throws IOException {
        mOutputStream = connection.socket().getOutputStream();
    }

    @Override
    protected void runInLoopThread() throws IOException, EasyException {
        write();
    }

    @Override
    protected void loopFinish(Exception e) {
        if (e != null) {
            Logger.e("Blocking write error,thread is dead with exception:" + e.getMessage());
        }
        mOutputStream = null;
        connection.disconnect();
    }
}
