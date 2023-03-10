package com.orientsec.easysocket.client;

import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;

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

    private final OperableSession session;

    private final Socket socket;

    private final Options options;

    BlockingReader(OperableSession session, Socket socket, Options options, HeadParser headParser) {
        super(session.getLogger());
        this.session = session;
        this.socket = socket;
        this.options = options;
        this.headParser = headParser;
    }

    @Override
    public void read() throws Exception {
        int headLength = headParser.headSize();
        byte[] headBytes = new byte[headLength];
        readInputStream(inputStream, headBytes);
        HeadParser.Head head = headParser.parseHead(headBytes);
        int bodyLength = head.getPacketSize();
        if (bodyLength > options.getMaxReadDataKB() * 1024) {
            throw new IllegalStateException("Packet size too large: " + bodyLength);
        } else if (bodyLength >= 0) {
            byte[] data = new byte[bodyLength];
            readInputStream(inputStream, data);
            Packet packet = headParser.decodePacket(head, data);
            session.handlePacket(packet);
        } else {
            throw new IllegalStateException("Negative packet size : " + bodyLength);
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
    protected void runInLoopThread() throws Exception {
        read();
    }

    @Override
    protected synchronized void loopFinish() {
        if (isRunning()) {
            session.postClose(ErrorCode.READ_EXIT, ErrorType.CONNECT,
                    "Socket read aborted.", error);
        }
    }
}
