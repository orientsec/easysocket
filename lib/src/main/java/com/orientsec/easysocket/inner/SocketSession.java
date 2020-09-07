package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.task.TaskManager;
import com.orientsec.easysocket.utils.Logger;

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
public class SocketSession implements Session, Initializer.Emitter {

    private Socket socket;

    private BlockingReader reader;

    private BlockingWriter writer;

    private final Executor connectExecutor;

    private final EasySocket easySocket;

    private final EventManager eventManager;

    private final Address address;

    private final TaskManager taskManager;

    private final Logger logger;

    SocketSession(EasySocket easySocket, Address address, TaskManager taskManager) {
        this.easySocket = easySocket;
        this.address = address;
        this.taskManager = taskManager;
        eventManager = easySocket.getEventManager();
        connectExecutor = easySocket.getConnectExecutor();
        logger = easySocket.getLogger();
    }


    @Override
    public void open() throws IOException {
        Socket socket = easySocket.getSocketFactoryProvider()
                .get()
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
    public void success() {
        eventManager.publish(Events.AVAILABLE, this);
    }

    @Override
    public void fail() {
        logger.e("Initialize failed.");
        eventManager.publish(Events.INIT_FAILED, this);
    }

    @Override
    public void active() {
        //连接后进行一些前置操作，例如资源初始化
        easySocket.getInitializerProvider()
                .get()
                .start(this);
        writer = new BlockingWriter(socket, easySocket, taskManager);
        reader = new BlockingReader(socket, easySocket);
        writer.start();
        reader.start();
    }

}
