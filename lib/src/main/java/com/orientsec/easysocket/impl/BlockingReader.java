package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Event;
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
public class BlockingReader<T> extends Looper implements Reader {
    private InputStream inputStream;

    private HeadParser<T> headParser;

    private SocketConnection<T> connection;

    private Options<T> options;

    BlockingReader(SocketConnection<T> connection) {
        this.connection = connection;
        this.options = connection.options;
        this.headParser = options.getHeadParser();
    }

    @Override
    public void read() throws IOException, EasyException {
        int headLength = headParser.headSize();
        byte[] headBytes = new byte[headLength];
        readInputStream(inputStream, headBytes);
        HeadParser.Head head = headParser.parseHead(headBytes);
        int bodyLength = head.bodySize();
        //Logger.i("need read body length: " + bodyLength);
        if (bodyLength > options.getMaxReadDataKB() * 1024) {
            throw new EasyException(Event.STREAM_SIZE_ERROR,
                    "InputStream length to long:" + bodyLength);
        } else if (bodyLength >= 0) {
            byte[] data = new byte[bodyLength];
            readInputStream(inputStream, data);
            Packet<T> packet = headParser.decodePacket(head, data);
            connection.handleMessage(packet);
        } else {
            throw new EasyException(Event.STREAM_SIZE_ERROR,
                    "Wrong body length " + bodyLength);
        }
    }

    private void readInputStream(InputStream inputStream, byte[] data) throws IOException {
        int readCount = 0; // 已经成功读取的字节的个数
        int count = data.length;
        while (readCount < count) {
            int len = inputStream.read(data, readCount, count - readCount);
            if (len == -1) {
                throw new IOException("Input stream closed.");
            }
            readCount += len;
        }
    }

    @Override
    protected void beforeLoop() throws IOException {
        inputStream = connection.socket().getInputStream();
    }

    @Override
    protected void runInLoopThread() throws IOException, EasyException {
        read();
    }

    @Override
    protected void loopFinish(Exception e) {
        Event event = Event.EMPTY;
        if (e != null) {
            //e.printStackTrace();
            Logger.e("Blocking read error, thread is dead with exception: "
                    + e.getMessage());
            if (e instanceof IOException) {
                event = Event.READ_IO_ERROR;
            } else if (e instanceof EasyException) {
                event = ((EasyException) e).getEvent();
            }
        }
        inputStream = null;
        connection.disconnect(event);
    }
}
