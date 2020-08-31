package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.task.Task;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2017/12/27 14:35
 * Author: Fredric
 * coding is art not science
 */
public class SocketSession implements Session {

    private Socket socket;

    private BlockingReader reader;

    private BlockingWriter writer;

    private final Executor connectExecutor;

    private final BlockingQueue<Task<?>> taskQueue;

    private final Options options;

    private final Connection connection;

    private final EventManager eventManager;

    SocketSession(Connection connection,
                  Options options,
                  EventManager eventManager,
                  BlockingQueue<Task<?>> taskQueue) {
        this.connection = connection;
        this.options = options;
        this.taskQueue = taskQueue;
        this.eventManager = eventManager;
        this.connectExecutor = options.getConnectExecutor();
    }

    public Address getAddress() {
        if (!connection.isConnect()) {
            return null;
        }
        Socket socket = this.socket;
        if (socket == null) {
            return null;
        }
        InetSocketAddress address = (InetSocketAddress) socket.getRemoteSocketAddress();
        if (address == null) {
            return null;
        }
        InetAddress inetAddress = address.getAddress();
        if (inetAddress == null) {
            return null;
        }
        return new Address(inetAddress.getHostAddress(), address.getPort());

    }


    @Override
    public void open() throws IOException {
        Socket socket = options.getSocketFactorySupplier()
                .get()
                .createSocket();
        //关闭Nagle算法,无论TCP数据报大小,立即发送
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setPerformancePreferences(1, 2, 0);
        Address address = connection.getAddress();
        assert address != null;
        SocketAddress socketAddress
                = new InetSocketAddress(address.getHost(), address.getPort());
        socket.connect(socketAddress, options.getConnectTimeOut());
        this.socket = socket;
    }

    @Override
    public void close() {
        if (reader != null) {
            reader.shutdown();
        }
        if (writer != null) {
            writer.shutdown();
        }
        connectExecutor.execute(() -> {
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
    }

    @Override
    public void active() {
        writer = new BlockingWriter(socket, eventManager, taskQueue);
        reader = new BlockingReader(socket, options, eventManager);
        writer.start();
        reader.start();
    }

}
