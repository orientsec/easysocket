package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.inner.AbstractConnection;
import com.orientsec.easysocket.inner.SendMessage;
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
    public void onPulse(SendMessage message) {
        executor.getMessageQueue().offer(message);
    }

    @Override
    public boolean isConnect() {
        Socket socket = this.socket;
        return state.get() == 4 && socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public <T, R> Task buildTask(Request<T, R> request) {
        return new EasyTask<>(request, this);
    }

    Socket socket() throws IOException {
        if (isConnect()) {
            return socket;
        }
        throw new IOException("socket is unavailable");
    }

    @Override
    protected void doOnConnect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    //已关闭
                    if (isShutdown()) {
                        return;
                    }
                    Logger.i("开始连接 " + connectionInfo.getHost() + ":" + connectionInfo.getPort() + " Socket服务器");
                    Socket socket = new Socket();
                    //关闭Nagle算法,无论TCP数据报大小,立即发送
                    socket.setTcpNoDelay(true);
                    socket.connect(new InetSocketAddress(connectionInfo.getHost(), connectionInfo.getPort()), options.getConnectTimeOut());
                    SocketConnection.this.socket = socket;

                    //启动心跳及读写线程
                    pulse.start();
                    writer = new BlockingWriter(SocketConnection.this);
                    reader = new BlockingReader(SocketConnection.this);
                    writer.start();
                    reader.start();
                    //再次检查状态
                    if (state.get() == 4) {
                        //已关闭,断开连接
                        doOnDisconnect();
                    } else {
                        synchronized (lock) {
                            if (state.compareAndSet(1, 2)) {
                                if (sleep) {
                                    disconnect();
                                } else {
                                    sendConnectEvent();
                                    if (ConnectionManager.getInstance().isBackground()) {
                                        setBackground();
                                    }
                                }
                            } else {
                                //已关闭,断开连接
                                doOnDisconnect();
                            }
                        }
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                    sendConnectFailedEvent();
                }
            }
        }.start();

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
        new Thread() {
            @Override
            public void run() {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
                sendDisconnectEvent();
            }
        }.start();

    }
}
