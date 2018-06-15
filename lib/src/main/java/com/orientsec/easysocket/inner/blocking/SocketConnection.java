package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.inner.AbstractConnection;
import com.orientsec.easysocket.inner.MessageType;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2017/12/27 14:35
 * Author: Fredric
 * coding is art not science
 */
public class SocketConnection<T> extends AbstractConnection<T> {
    private AtomicInteger messageId = new AtomicInteger();

    private Socket socket;

    private BlockingReader reader;

    private BlockingWriter writer;

    private BlockingExecutor<T> executor;

    public SocketConnection(Options<T> options) {
        super(options);
        executor = new BlockingExecutor<>(this);
    }

    @Override
    public BlockingExecutor<T> taskExecutor() {
        return executor;
    }

    @Override
    public void onPulse(Message<T> message) {
        executor.getMessageQueue().offer(message);
    }

    @Override
    public boolean isConnect() {
        Socket socket = this.socket;
        return state.get() == 2 && socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public <REQUEST, RESPONSE> Task<RESPONSE> buildTask(Request<T, REQUEST, RESPONSE> request) {
        return new EasyTask<>(request, this);
    }

    Socket socket() throws IOException {
        if (socket != null) {
            return socket;
        }
        throw new IOException("socket is unavailable");
    }

    private void runOnSubThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    @Override
    protected void doOnConnect() {
        runOnSubThread(() -> {
            //已关闭
            if (isShutdown()) {
                return;
            }
            Logger.i("begin socket connect, host:" + connectionInfo.getHost() + ", port:" + connectionInfo.getPort());
            try {
                SSLContext sslContext = options.getProtocol().sslContext();
                if (sslContext == null) {
                    socket = new Socket();
                } else {
                    socket = sslContext.getSocketFactory().createSocket();
                }
                //关闭Nagle算法,无论TCP数据报大小,立即发送
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                socket.setPerformancePreferences(1, 2, 0);
                socket.connect(new InetSocketAddress(connectionInfo.getHost(), connectionInfo.getPort()), options.getConnectTimeOut());
            } catch (Exception e) {
                e.printStackTrace();
                closeSocketAndTasks();
                if (state.compareAndSet(1, 0)) {
                    sendConnectFailedEvent();
                }
                return;
            }
            //再次检查状态
            if (isShutdown()) {
                //关闭socket
                closeSocketAndTasks();
            } else {
                //启动心跳及读写线程
                pulse.start();
                if (options.getProtocol().needAuthorize()) {
                    Authorize authorize = new Authorize(this);
                    writer = new BlockingWriter<>(this, authorize);
                    reader = new BlockingReader<>(this, authorize);
                } else {
                    writer = new BlockingWriter<>(this);
                    reader = new BlockingReader<>(this);
                }
                writer.start();
                reader.start();
                if (state.compareAndSet(1, 2)) {
                    sendConnectEvent();
                } else {
                    //关闭socket
                    closeSocketAndTasks();
                }
            }
        });
    }

    private void closeSocketAndTasks() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            socket = null;
        }
        taskExecutor().onConnectionClosed();
    }

    @Override
    protected void disconnectTask(int error) {
        super.disconnectTask(error);
        if (reader != null) {
            reader.shutdown();
            reader = null;
        }
        if (writer != null) {
            writer.shutdown();
            writer = null;
        }
        runOnSubThread(() -> {
            closeSocketAndTasks();
            state.compareAndSet(3, 0);
            sendDisconnectEvent(error);
        });
    }

    @Override
    protected Message<T> buildMessage(MessageType messageType) {
        return new Message<>(messageType, messageId.incrementAndGet());
    }
}
