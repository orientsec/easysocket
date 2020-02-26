package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.ConnectionInfo;
import com.orientsec.easysocket.Initializer;
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

    private BlockingWriter<T> writer;

    private RequestManager<T> taskManager;

    private Map<String, MessageHandler<T>> messageHandlerMap;

    private Initializer.Emitter emitter = new Initializer.Emitter() {
        @Override
        public void success() {
            synchronized (lock) {
                if (state == State.CONNECT) {
                    state = State.AVAILABLE;
                    taskManager.onReady();
                    onAvailable();
                }
            }
        }

        @Override
        public void fail(Event event) {
            Logger.i("Connection initialize failed: " + event);
            disconnect(event);
        }
    };

    public SocketConnection(Options<T> options) {
        super(options);
        taskManager = new RequestManager<>(this);
        pulse = new Pulse<>(this);
        messageHandlerMap = new HashMap<>();
        messageHandlerMap.put(PacketType.RESPONSE.getValue(), taskManager);
        messageHandlerMap.put(PacketType.PULSE.getValue(), pulse);
        messageHandlerMap.put(PacketType.PUSH.getValue(), options.getPushHandler());
    }

    @Override
    public TaskManager<T, RequestTask<T, ?, ?>> taskManager() {
        return taskManager;
    }

    @Override
    public boolean isConnect() {
        return state == State.CONNECT || state == State.AVAILABLE;
    }

    @Override
    public <REQUEST, RESPONSE> Task<RESPONSE>
    buildTask(Request<T, REQUEST, RESPONSE> request) {
        return new RequestTask<>(request, this);
    }

    @Override
    public <REQUEST, RESPONSE> Task<RESPONSE>
    buildTask(Request<T, REQUEST, RESPONSE> request, boolean sync) {
        return new RequestTask<>(request, this, false, sync);
    }

    @Override
    public <REQUEST, RESPONSE> Task<RESPONSE>
    buildTask(Request<T, REQUEST, RESPONSE> request, boolean init, boolean sync) {
        return new RequestTask<>(request, this, init, sync);
    }

    Socket socket() throws IOException {
        if (socket != null) {
            return socket;
        }
        throw new IOException("Socket is unavailable");
    }

    @Override
    public void handleMessage(Packet<T> packet) {
        MessageHandler<T> messageHandler
                = messageHandlerMap.get(packet.getPacketType().getValue());
        if (messageHandler == null) {
            Logger.e("No packet handler for type: " + packet.getPacketType());
        } else {
            messageHandler.handleMessage(packet);
        }
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        if (!isConnect()) {
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
        return new ConnectionInfo(inetAddress.getHostAddress(), address.getPort());

    }

    @Override
    void connectRunnable() {
        //已关闭
        if (isShutdown()) {
            return;
        }
        Logger.i("begin socket connect, " + connectionInfo);

        Socket socket = openSocket();
        boolean closeSocket = false;
        synchronized (lock) {
            if (socket == null) {
                if (state == State.STARTING) {
                    state = State.IDLE;
                }
                stopWorkers(Event.SOCKET_START_ERROR);
                onConnectFailed();
            } else if (state == State.STARTING) {
                state = State.CONNECT;
                SocketConnection.this.socket = socket;
                startWorkers();
                onConnect();
                options.getInitializer().start(this, emitter);
            } else {
                //connection is shutdown
                stopWorkers(Event.SHUT_DOWN);
                closeSocket = true;
            }
        }
        if (closeSocket) {
            closeSocket(socket);
        }
    }

    @Override
    protected void disconnectRunnable(Event event) {
        Socket socket;
        synchronized (lock) {
            if (state == State.STOPPING) {
                state = State.IDLE;
            }
            stopWorkers(event);
            socket = SocketConnection.this.socket;
            SocketConnection.this.socket = null;
            onDisconnect(event);
        }
        if (socket != null) {
            closeSocket(socket);
        }
    }

    private void stopWorkers(Event event) {
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
        taskManager.clear(event);
    }

    private void startWorkers() {
        //启动心跳及读写线程
        pulse.start();
        writer = new BlockingWriter<>(SocketConnection.this, taskManager.taskQueue);
        reader = new BlockingReader<>(SocketConnection.this);
        writer.start();
        reader.start();
    }

    private Socket openSocket() {
        try {
            Socket socket = options.getSocketFactory().createSocket();
            SocketAddress socketAddress
                    = new InetSocketAddress(connectionInfo.getHost(), connectionInfo.getPort());
            socket.connect(socketAddress, options.getConnectTimeOut());
            return socket;
        } catch (Exception e) {
            Logger.e("Socket connect failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}