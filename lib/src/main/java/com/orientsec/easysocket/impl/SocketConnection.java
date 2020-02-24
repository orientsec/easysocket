package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.ConnectionInfo;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.Event;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2017/12/27 14:35
 * Author: Fredric
 * coding is art not science
 */
public class SocketConnection<T> extends AbstractConnection<T>
        implements MessageHandler<T> {

    private Socket socket;

    private BlockingReader reader;

    BlockingWriter<T> writer;

    private TaskManager<T, RequestTask<T, ?, ?>> taskManager;

    private Map<String, MessageHandler<T>> messageHandlerMap;

    public SocketConnection(Options<T> options) {
        super(options);
        taskManager = new RequestManager<>(this);
        pulse = new Pulse<>(this);
        messageHandlerMap = new HashMap<>();
        messageHandlerMap.put(PacketType.RESPONSE, taskManager);
        messageHandlerMap.put(PacketType.PULSE, pulse);
        messageHandlerMap.put(PacketType.PUSH, options.getPushHandler());
    }

    @Override
    public TaskManager<T, RequestTask<T, ?, ?>> taskManager() {
        return taskManager;
    }

    @Override
    public boolean isConnect() {
        Socket socket = this.socket;
        return state.get() == 2
                && socket != null
                && socket.isConnected()
                && !socket.isClosed();
    }

    @Override
    public <REQUEST, RESPONSE> Task<RESPONSE> buildTask(Request<T, REQUEST, RESPONSE> request) {
        return new RequestTask<>(request, this);
    }

    Socket socket() throws IOException {
        if (socket != null) {
            return socket;
        }
        throw new IOException("Socket is unavailable");
    }

    private void release(Event event) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            socket = null;
        }
        //停止心跳
        pulse.stop();
        if (reader != null) {
            reader.shutdown();
            reader = null;
        }
        if (writer != null) {
            writer.shutdown();
            writer = null;
        }
        taskManager.onConnectionClosed(event);
    }

    @Override
    public void handleMessage(Packet<T> packet) {
        MessageHandler<T> messageHandler = messageHandlerMap.get(packet.getPacketType());
        if (messageHandler == null) {
            Logger.e("No packet handler for type: " + packet.getPacketType());
        } else {
            messageHandler.handleMessage(packet);
        }
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        if (isConnect()) {
            Socket socket = this.socket;
            if (socket != null) {
                InetSocketAddress address = (InetSocketAddress) socket.getRemoteSocketAddress();
                if (address != null) {
                    InetAddress inetAddress = address.getAddress();
                    if (inetAddress != null) {
                        return new ConnectionInfo(inetAddress.getHostAddress(), address.getPort());
                    }
                }
            }
        }
        return null;
    }

    @Override
    Runnable connectRunnable() {
        return new ConnectRunnable();
    }

    @Override
    protected Runnable disconnectRunnable(Event event) {
        return new DisconnectRunnable(event);
    }

    private class ConnectRunnable implements Runnable {

        @Override
        public void run() {
            //已关闭
            if (isShutdown()) {
                return;
            }
            Logger.i("begin socket connect, " + connectionInfo);

            if (!connectSocket()) {
                release(Event.SOCKET_START_ERROR);
                if (state.compareAndSet(1, 0)) {
                    onConnectFailed();
                }
                return;
            }

            //再次检查状态
            if (isShutdown()) {
                //关闭socket
                release(Event.SHUT_DOWN);
            } else {
                startComponents();
                if (state.compareAndSet(1, 2)) {
                    onConnect();
                } else {
                    //关闭socket
                    release(Event.SHUT_DOWN);
                }
            }
        }

        private boolean connectSocket() {
            try {
                Socket socket = options.getSocketFactory().createSocket();
                SocketAddress socketAddress = new InetSocketAddress(connectionInfo.getHost(), connectionInfo.getPort());
                socket.connect(socketAddress, options.getConnectTimeOut());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        private void startComponents() {
            //启动心跳及读写线程
            pulse.start();
            writer = new BlockingWriter<>(SocketConnection.this);
            reader = new BlockingReader<>(SocketConnection.this);
            writer.start();
            reader.start();
        }
    }

    private class DisconnectRunnable implements Runnable {
        Event event;

        DisconnectRunnable(Event event) {
            this.event = event;
        }

        @Override
        public void run() {
            release(event);
            state.compareAndSet(3, 0);
            onDisconnect(event);
        }
    }
}
