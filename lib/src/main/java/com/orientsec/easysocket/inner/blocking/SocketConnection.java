package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.inner.AbstractConnection;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2017/12/27 14:35
 * Author: Fredric
 * coding is art not science
 */
public class SocketConnection extends AbstractConnection {

    private Socket socket;

    private BlockingReader reader;

    private BlockingWriter writer;

    private BlockingExecutor executor;

    public SocketConnection(Options options) {
        super(options);
        executor = new BlockingExecutor(this);
    }

    @Override
    public BlockingExecutor taskExecutor() {
        return executor;
    }

    @Override
    public void onPulse(Message message) {
        executor.getMessageQueue().offer(message);
    }

    @Override
    public boolean isConnect() {
        Socket socket = this.socket;
        return state.get() == 2 && socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public <T, R> Task buildTask(Request<T, R> request) {
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
            Logger.i("开始连接 " + connectionInfo.getHost() + ":" + connectionInfo.getPort() + " Socket服务器");
            try {
                socket = new Socket();
                //关闭Nagle算法,无论TCP数据报大小,立即发送
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                socket.setPerformancePreferences(1, 2, 0);
                socket.connect(new InetSocketAddress(connectionInfo.getHost(), connectionInfo.getPort()), options.getConnectTimeOut());
            } catch (IOException e) {
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
                writer = new BlockingWriter(SocketConnection.this);
                reader = new BlockingReader(SocketConnection.this);
                writer.start();
                reader.start();
                if (state.compareAndSet(1, 2)) {
                    if (sleep) {
                        synchronized (lock) {
                            if (isConnect()) {
                                setBackground();
                            }
                        }
                    } else {
                        sendConnectEvent();
                    }
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
    protected void doOnDisconnect() {
        super.doOnDisconnect();
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
            sendDisconnectEvent();
        });
    }
}
