package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.Errors;

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

    private HeadParser headParser;

    private final Socket socket;

    private final EventManager eventManager;

    private final EasySocket easySocket;

    BlockingReader(Socket socket, EasySocket easySocket) {
        super(easySocket.getLogger());
        this.socket = socket;
        this.easySocket = easySocket;
        this.eventManager = easySocket.getEventManager();
    }

    @Override
    public void read() throws IOException, EasyException {
        int headLength = headParser.headSize();
        byte[] headBytes = new byte[headLength];
        readInputStream(inputStream, headBytes);
        HeadParser.Head head = headParser.parseHead(headBytes);
        int bodyLength = head.getPacketSize();
        if (bodyLength > easySocket.getMaxReadDataKB() * 1024) {
            throw new IllegalStateException("Packet size too large: " + bodyLength);
        } else if (bodyLength >= 0) {
            byte[] data = new byte[bodyLength];
            readInputStream(inputStream, data);
            Packet packet = headParser.decodePacket(head, data);
            eventManager.publish(Events.ON_PACKET, packet);
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
        headParser = easySocket.getHeadParserProvider().get();
    }

    @Override
    protected void runInLoopThread() throws IOException, EasyException {
        read();
    }

    @Override
    protected synchronized void loopFinish() {
        if (isRunning()) {
            eventManager.publish(Events.CONNECT_ERROR,
                    Errors.connectError(ErrorCode.READ_EXIT, "Blocking reader exit."));
        }
    }
}
