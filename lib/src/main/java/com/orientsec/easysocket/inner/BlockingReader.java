package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.ErrorCode;
import com.orientsec.easysocket.exception.ErrorType;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2017/12/27 16:19
 * Author: Fredric
 * coding is art not science
 */
public class BlockingReader extends Looper implements Reader {
    private InputStream inputStream;

    private final HeadParser headParser;

    private final Socket socket;

    private final EventManager eventManager;

    private final Options options;

    BlockingReader(Socket socket, Options options, EventManager eventManager) {
        this.socket = socket;
        this.options = options;
        this.eventManager = eventManager;
        this.headParser = options.getHeadParser();
    }

    @Override
    public void read() throws IOException, EasyException {
        int headLength = headParser.headSize();
        byte[] headBytes = new byte[headLength];
        readInputStream(inputStream, headBytes);
        HeadParser.Head head = headParser.parseHead(headBytes);
        int bodyLength = head.getPacketSize();
        //Logger.i("need read body length: " + bodyLength);
        if (bodyLength > options.getMaxReadDataKB() * 1024) {
            throw new EasyException(ErrorCode.STREAM_SIZE, ErrorType.CONNECT,
                    "InputStream length too long:" + bodyLength);
        } else if (bodyLength >= 0) {
            byte[] data = new byte[bodyLength];
            readInputStream(inputStream, data);
            Packet<?> packet = headParser.decodePacket(head, data);
            eventManager.publish(Events.ON_PACKET, packet);
        } else {
            throw new EasyException(ErrorCode.STREAM_SIZE, ErrorType.CONNECT,
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
        inputStream = socket.getInputStream();
    }

    @Override
    protected void runInLoopThread() throws IOException, EasyException {
        read();
    }

    @Override
    protected void loopFinish(Exception e) {
        EasyException ee;
        if (e != null) {
            Logger.e("Blocking reader error, thread is dead with exception: "
                    + e.getMessage());
            if (e instanceof EasyException) {
                ee = (EasyException) e;
            } else if (e instanceof IOException) {
                ee = new EasyException(ErrorCode.READ_IO, ErrorType.CONNECT, e);
            } else {
                ee = new EasyException(ErrorCode.READ_OTHER, ErrorType.CONNECT, e);
            }
        } else {
            ee = new EasyException(ErrorCode.READ_EXIT, ErrorType.CONNECT, "Read looper exit.");
        }
        synchronized (this) {
            if (!isShutdown()) {
                eventManager.publish(Events.STOP, ee);
            }
        }
    }
}
