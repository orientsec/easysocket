package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Protocol;
import com.orientsec.easysocket.exception.ReadException;
import com.orientsec.easysocket.inner.Looper;
import com.orientsec.easysocket.inner.Reader;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2017/12/27 16:19
 * Author: Fredric
 * coding is art not science
 */
public class BlockingReader extends Looper implements Reader {
    private InputStream inputStream;

    private Options options;

    private SocketConnection connection;

    public BlockingReader(SocketConnection connection) {
        this.connection = connection;
        options = connection.options();
    }

    @Override
    public void read() throws IOException, ReadException {
        Protocol protocol = options.getProtocol();
        int headLength = protocol.headSize();
        byte[] headBytes = new byte[headLength];
        readInputStream(inputStream, headBytes);
        int bodyLength = protocol.bodySize(headBytes);
        Logger.i("need read body length: " + bodyLength);
        if (bodyLength > options.getMaxReadDataKB() * 1024) {
            throw new ReadException(
                    "this socket input stream has some problem,body length to long:" + bodyLength
                            + ",we'll disconnect");
        } else if (bodyLength > 0) {
            byte[] data = new byte[bodyLength];
            readInputStream(inputStream, data);
            Message message = protocol.decodeMessage(headBytes, data);
            connection.taskExecutor().onReceive(message);
        } else if (bodyLength == 0) {
            Message message = protocol.decodeMessage(headBytes, new byte[0]);
            connection.taskExecutor().onReceive(message);
        } else if (bodyLength < 0) {
            throw new ReadException(
                    "this socket input stream has some problem,wrong body length " + bodyLength
                            + ",we'll disconnect");
        }


    }

    private void readInputStream(InputStream inputStream, byte[] data) throws IOException {
        int readCount = 0; // 已经成功读取的字节的个数
        int count = data.length;
        while (readCount < count) {
            int len = inputStream.read(data, readCount, count - readCount);
            if (len == -1) {
                throw new IOException("input stream closed");
            }
            readCount += len;
        }
    }

    @Override
    protected void beforeLoop() throws IOException {
        inputStream = connection.socket().getInputStream();
    }

    @Override
    protected void runInLoopThread() throws IOException, ReadException {
        read();
    }

    @Override
    protected void loopFinish(Exception e) {
        if (e != null) {
            Logger.e("Blocking read error, thread is dead with exception: " + e.getMessage());
        }
        inputStream = null;
        connection.disconnect();
    }
}
