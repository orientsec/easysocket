package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Error;
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
        implements PacketHandler<T> {

    private Socket socket;

    private BlockingReader reader;

    private BlockingWriter<T> writer;

    private RequestTaskManager<T> taskManager;

    private Map<String, PacketHandler<T>> messageHandlerMap;

    private Initializer.Emitter emitter = new Initializer.Emitter() {
        @Override
        public void success() {
            synchronized (SocketConnection.this) {
                if (state == State.CONNECT) {
                    state = State.AVAILABLE;
                    taskManager.onReady();
                    onAvailable();
                }
            }
        }

        @Override
        public void fail(EasyException e) {
            Logger.i("Connection initialize failed: " + e);
            disconnect(e);
        }
    };

    public SocketConnection(Options<T> options) {
        super(options);
        taskManager = new RequestTaskManager<>(this);
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
    buildTask(Request<T, REQUEST, RESPONSE> request, Callback<RESPONSE> callback) {
        return new RequestTask<>(request, callback, this);
    }

    Socket socket() throws IOException {
        if (socket != null) {
            return socket;
        }
        throw new IOException("Socket is unavailable");
    }

    @Override
    public void handlePacket(Packet<T> packet) {
        PacketHandler<T> packetHandler
                = messageHandlerMap.get(packet.getPacketType().getValue());
        if (packetHandler == null) {
            Logger.e("No packet handler for type: " + packet.getPacketType());
        } else {
            packetHandler.handlePacket(packet);
        }
    }

    @Override
    public Address getAddress() {
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
        return new Address(inetAddress.getHostAddress(), address.getPort());

    }

    @Override
    void connectRunnable() {
        //已关闭
        if (isShutdown()) {
            return;
        }
        Logger.i("begin socket connect, " + address);

        Socket socket = openSocket();
        boolean closeSocket = false;
        synchronized (SocketConnection.this) {
            if (socket == null) {
                if (state == State.STARTING) {
                    state = State.IDLE;
                }
                stopWorkers(Error.create(Error.Code.SOCKET_CONNECT));
                onConnectFailed();
            } else if (state == State.STARTING) {
                state = State.CONNECT;
                SocketConnection.this.socket = socket;
                startWorkers();
                onConnect();
                options.getInitializer().start(this, emitter);
            } else {
                //connection is shutdown
                stopWorkers(Error.create(Error.Code.SHUT_DOWN));
                closeSocket = true;
            }
        }
        if (closeSocket) {
            closeSocket(socket);
        }
    }

    @Override
    protected void disconnectRunnable(EasyException e) {
        Socket socket;
        synchronized (SocketConnection.this) {
            if (state == State.STOPPING) {
                state = State.IDLE;
            }
            stopWorkers(e);
            socket = SocketConnection.this.socket;
            SocketConnection.this.socket = null;
            onDisconnect(e);
        }
        if (socket != null) {
            closeSocket(socket);
        }
    }

    private void stopWorkers(EasyException e) {
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
        taskManager.clear(e);
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
            Socket socket = options.getSocketFactorySupplier()
                    .get()
                    .createSocket();
            //关闭Nagle算法,无论TCP数据报大小,立即发送
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setPerformancePreferences(1, 2, 0);
            SocketAddress socketAddress
                    = new InetSocketAddress(address.getHost(), address.getPort());
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
