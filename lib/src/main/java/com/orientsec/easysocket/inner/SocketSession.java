package com.orientsec.easysocket.inner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.Errors;
import com.orientsec.easysocket.task.TaskManager;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2017/12/27 14:35
 * Author: Fredric
 * coding is art not science
 */
public class SocketSession implements Session, Initializer.Emitter, Runnable,
        EventListener, PacketHandler {

    private Socket socket;

    private BlockingReader reader;

    private BlockingWriter writer;

    private final Executor connectExecutor;

    private final EasySocket easySocket;

    private final EventManager eventManager;

    private final TaskManager taskManager;

    private final Logger logger;

    private AbstractConnection connection;

    private State state = State.IDLE;

    private Pulse pulse;

    private final Map<String, PacketHandler> messageHandlerMap = new HashMap<>();

    SocketSession(AbstractConnection connection) {
        this.connection = connection;
        this.easySocket = connection.getEasySocket();
        this.taskManager = connection.getTaskManager();

        connectExecutor = easySocket.getConnectExecutor();
        logger = easySocket.getLogger();

        eventManager = easySocket.getEasyManager().newEventManager();
        eventManager.addListener(this);

    }

    @Override
    public void handlePacket(@NonNull Packet packet) {
        if (state == State.DETACH) return;
        PacketHandler packetHandler = messageHandlerMap.get(packet.getPacketType().getValue());
        if (packetHandler == null) {
            logger.w("No packet handler for type: " + packet.getPacketType());
        } else {
            packetHandler.handlePacket(packet);
        }
    }


    @Override
    public void open() {
        if (state == State.IDLE) {
            state = State.STARTING;
            connectExecutor.execute(this);
        }
    }

    @Override
    public void close(EasyException e) {
        if (state == State.IDLE || state == State.STARTING) {
            state = State.DETACH;
            connection.onConnectFailed(e);
        } else {
            onError(e);
        }
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

    private void onConnect(Socket socket) {
        if (state == State.STARTING) {
            state = State.CONNECT;
            this.socket = socket;
            //连接后进行一些前置操作，例如资源初始化
            easySocket.getInitializerProvider()
                    .get()
                    .start(this);
            //启动心跳及读写线程
            writer = new BlockingWriter(socket, easySocket, taskManager, eventManager);
            reader = new BlockingReader(socket, easySocket, eventManager);
            writer.start();
            reader.start();
            pulse = new Pulse(easySocket, eventManager);
            pulse.start();

            messageHandlerMap.put(PacketType.RESPONSE.getValue(), taskManager);
            messageHandlerMap.put(PacketType.PULSE.getValue(), pulse);
            messageHandlerMap.put(PacketType.PUSH.getValue(), connection.getPushManager());

            connection.onConnect();
        } else {
            connectExecutor.execute(() -> {
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
        }
    }

    private void onConnectFailed() {
        if (state == State.STARTING) {
            state = State.DETACH;
            EasyException e = Errors.connectError(ErrorCode.SOCKET_CONNECT,
                    "Socket connect failed.");
            connection.onConnectFailed(e);
        }
    }

    private void onAvailable() {
        if (state == State.CONNECT) {
            state = State.AVAILABLE;
            connection.onAvailable();
        }
    }

    private void onError(EasyException e) {
        if (state == State.CONNECT || state == State.AVAILABLE) {
            state = State.DETACH;
            pulse.stop();
            reader.shutdown();
            writer.shutdown();
            connectExecutor.execute(() -> {
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
            connection.onDisconnect(e);
        }
    }

    @Override
    public void run() {
        Address address = connection.obtainAddress();
        logger.i("Begin socket connect.");
        try {
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
            eventManager.publish(Events.CONNECT_SUCCESS, socket);
        } catch (Exception e) {
            logger.i("Socket connect failed.", e);
            eventManager.publish(Events.CONNECT_FAILED);
        }
    }

    @Override
    public boolean isConnect() {
        return state == State.CONNECT || state == State.AVAILABLE;
    }

    @Override
    public boolean isAvailable() {
        return state == State.AVAILABLE;
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        if (eventId < 0) return;
        switch (eventId) {
            case Events.CONNECT_ERROR:
                assert object != null;
                onError((EasyException) object);
                break;
            case Events.AVAILABLE:
                onAvailable();
                break;
            case Events.INIT_FAILED:
                onError(Errors.connectError(ErrorCode.INIT_FAILED,
                        "Connection initialize failed."));
                break;
            case Events.CONNECT_SUCCESS:
                assert object != null;
                onConnect((Socket) object);
                break;
            case Events.CONNECT_FAILED:
                onConnectFailed();
                break;
            case Events.ON_PACKET:
                assert object != null;
                handlePacket((Packet) object);
                break;
            case Events.PULSE:
                pulse.pulse();
                break;
        }
    }
}
