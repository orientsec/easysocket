package com.orientsec.easysocket.session;

import android.net.TrafficStats;

import com.orientsec.easysocket.EasyExecutor;
import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.client.BaseSocketClient;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * A class responsible for reading data from a socket in a blocking manner.
 * This class extends the `Looper` class and implements the `Reader` interface.
 */
public class BlockingReader extends Looper implements Reader {
    // Input stream for reading data from the socket
    private InputStream inputStream;

    // Parser for handling the packet headers
    private final HeadParser headParser;

    // Session associated with this reader
    private final OperableSession session;

    // Socket used for communication
    private final Socket socket;

    // Configuration options for the reader
    private final Options options;

    // EasyExecutor for executing tasks
    private final EasyExecutor mainExecutor;

    /**
     * Constructs a BlockingReader instance with the specified parameters.
     *
     * @param session The session associated with this reader.
     * @param socket  The socket used for communication.
     * @param client  The BaseSocketClient instance providing options and executors.
     */
    BlockingReader(OperableSession session, Socket socket, BaseSocketClient client) {
        super(session.getLogger());
        this.session = session;
        this.socket = socket;
        this.mainExecutor = client.getMainExecutor();
        this.options = client.getOptions();
        this.headParser = client.getHeadParser();
    }

    /**
     * Reads data from the input stream, parses the packet, and handles it.
     *
     * @throws Exception If an error occurs during reading or parsing.
     */
    @Override
    public void read() throws Exception {
        int headLength = headParser.headSize();
        byte[] headBytes = new byte[headLength];
        readInputStream(inputStream, headBytes);
        HeadParser.Head head = headParser.parseHead(headBytes);
        int bodyLength = head.getPacketSize();
        if (bodyLength > options.getMaxReadSizeInKB() * 1024) {
            throw new Exception("packet size too large: " + bodyLength);
        } else if (bodyLength >= 0) {
            byte[] data = new byte[bodyLength];
            readInputStream(inputStream, data);
            Packet packet = headParser.decodePacket(head, data);
            session.handlePacket(packet);
        } else {
            throw new Exception("negative packet size : " + bodyLength);
        }
    }

    /**
     * Reads data from the input stream into the provided byte array.
     *
     * @param inputStream The input stream to read from.
     * @param data        The byte array to store the read data.
     * @throws IOException If an error occurs during reading.
     */
    private void readInputStream(InputStream inputStream, byte[] data) throws IOException {
        int readCount = 0; // Number of bytes successfully read
        int count = data.length;
        while (readCount < count) {
            int len = inputStream.read(data, readCount, count - readCount);
            if (len == -1) {
                throw new IOException("input stream closed");
            }
            readCount += len;
        }
    }

    /**
     * Prepares the reader before starting the loop.
     *
     * @throws IOException If an error occurs while setting up the input stream.
     */
    @Override
    protected void beforeLoop() throws IOException {
        TrafficStats.setThreadStatsTag(options.getReadStatsTag());
        inputStream = socket.getInputStream();
    }

    /**
     * Executes the reading logic in the loop thread.
     *
     * @throws Exception If an error occurs during reading.
     */
    @Override
    protected void runInLoopThread() throws Exception {
        read();
    }

    /**
     * Cleans up resources and handles errors when the loop finishes.
     */
    @Override
    protected synchronized void loopFinish() {
        TrafficStats.clearThreadStatsTag();
        EasyException e = EasyException.create(ErrorCode.READ_EXIT, ErrorType.CONNECT,
                "socket read aborted", session.getSuffix(), error);
        if (isRunning()) {
            mainExecutor.execute(() -> session.close(e));
        }
    }
}