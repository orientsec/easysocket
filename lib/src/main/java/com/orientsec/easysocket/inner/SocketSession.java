package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.task.TaskManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
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

    private final EasySocket easySocket;


    private final Address address;

    private final TaskManager taskManager;

    SocketSession(EasySocket easySocket, RealConnection connection) {
        this.easySocket = easySocket;
        this.address = connection.getAddress();
        taskManager = connection.taskManager;
        this.connectExecutor = easySocket.getConnectExecutor();
    }


    @Override
    public void open() throws IOException {
        Socket socket = easySocket.getSocketFactoryProvider()
                .get(easySocket)
                .createSocket();
        //关闭Nagle算法,无论TCP数据报大小,立即发送
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setPerformancePreferences(1, 2, 0);
        SocketAddress socketAddress
                = new InetSocketAddress(address.getHost(), address.getPort());
        socket.connect(socketAddress, easySocket.getConnectTimeOut());
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
        writer = new BlockingWriter(socket, easySocket, taskManager);
        reader = new BlockingReader(socket, easySocket);
        writer.start();
        reader.start();
    }

}
