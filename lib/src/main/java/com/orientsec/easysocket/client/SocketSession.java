package com.orientsec.easysocket.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.PacketType;
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

import javax.net.ssl.SSLSocket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2017/12/27 14:35
 * Author: Fredric
 * coding is art not science
 */
public class SocketSession implements Session, Initializer.Emitter, Runnable, EventListener {
    static final int ERROR = 202;
    static final int START_SUCCESS = 203;
    static final int START_FAILED = 204;
    static final int AVAILABLE = 205;
    static final int INIT_FAILED = 206;
    static final int ON_PACKET = 207;

    private Socket socket;

    private BlockingReader reader;

    private BlockingWriter writer;

    private final Executor connectExecutor;

    private final Options options;

    private final EventManager eventManager;

    private final Logger logger;

    private final AbstractSocketClient socketClient;

    private State state = State.IDLE;

    private Pulse pulse;

    private final Map<String, PacketHandler> messageHandlerMap = new HashMap<>();

    SocketSession(AbstractSocketClient socketClient) {
        this.socketClient = socketClient;
        this.options = socketClient.getOptions();

        connectExecutor = options.getConnectExecutor();
        logger = options.getLogger();

        eventManager = EasySocket.getInstance().newEventManager();
        eventManager.addListener(this);
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void handlePacket(@NonNull Packet packet) {
        eventManager.publish(ON_PACKET, packet);
    }

    private void onPacket(@NonNull Packet packet) {
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
            connectExecutor.execute(this);
            state = State.STARTING;
        }
    }

    @Override
    public void close(EasyException e) {
        if (state == State.IDLE || state == State.STARTING) {
            socketClient.onConnectionFailed(e);
            state = State.DETACH;
        } else {
            onError(e);
        }
    }

    @Override
    public void success() {
        eventManager.publish(AVAILABLE, this);
    }

    @Override
    public void fail(Exception e) {
        logger.e("Session initialize failed.");
        eventManager.publish(INIT_FAILED, e);
    }

    @Override
    public void postError(EasyException e) {
        eventManager.publish(ERROR, e);
    }

    private void onReady(Socket socket) {
        if (state == State.STARTING) {
            this.socket = socket;
            //连接后进行一些前置操作，例如资源初始化
            socketClient.getInitializer().start(this);
            //启动心跳及读写线程
            TaskManager taskManager = socketClient.getTaskManager();
            writer = new BlockingWriter(this, socket, options, taskManager);
            reader = new BlockingReader(this, socket, options, socketClient.getHeadParser());
            writer.start();
            reader.start();

            messageHandlerMap.put(PacketType.RESPONSE.getValue(), taskManager);

            socketClient.onConnected();

            state = State.CONNECT;
            logger.i("Session start success.");
        } else {
            connectExecutor.execute(() -> {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.e("Easy Socket closed.", e);
                }
            });
        }
    }

    private void onFailed(Exception ex) {
        if (state == State.STARTING) {
            EasyException e = Errors.connectError(ErrorCode.SOCKET_CONNECT,
                    "Socket connect failed.", ex);
            socketClient.onConnectionFailed(e);

            state = State.DETACH;
            logger.i("Session start failed.");
        }
    }

    private void onAvailable() {
        if (state == State.CONNECT) {
            //开启心跳
            pulse = new Pulse(socketClient, this, eventManager);
            pulse.start();
            //注册主动心跳及推送消息处理器
            messageHandlerMap.put(PacketType.PULSE.getValue(), pulse);
            messageHandlerMap.put(PacketType.PUSH.getValue(), socketClient.getPushManager());

            socketClient.onConnectionAvailable();

            state = State.AVAILABLE;
            logger.i("Session available.");
        }
    }

    private void onError(EasyException e) {
        if (state == State.CONNECT || state == State.AVAILABLE) {
            if (pulse != null) {
                pulse.stop();
            }
            reader.shutdown();
            writer.shutdown();
            connectExecutor.execute(() -> {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    logger.e("Easy Socket closed.", ioe);
                }
            });
            socketClient.onDisconnected(e);

            state = State.DETACH;
            logger.w("Session end.", e);
        }
    }

    @Override
    public void run() {
        Address address = socketClient.obtainAddress();
        logger.i("Begin socket connect.");
        try {
            Socket socket = socketClient.getSocketFactory().createSocket();
            //关闭Nagle算法,无论TCP数据报大小,立即发送
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setPerformancePreferences(1, 2, 0);
            SocketAddress socketAddress
                    = new InetSocketAddress(address.getHost(), address.getPort());
            socket.connect(socketAddress, options.getConnectTimeOut());

            //STEP3:SSL
            if (socket instanceof SSLSocket) {
                ((SSLSocket) socket).startHandshake();
            }

            eventManager.publish(START_SUCCESS, socket);
        } catch (Exception e) {
            logger.i("Socket connect failed.", e);
            eventManager.publish(START_FAILED, e);
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
        switch (eventId) {
            case ERROR:
                assert object != null;
                onError((EasyException) object);
                break;
            case AVAILABLE:
                onAvailable();
                break;
            case INIT_FAILED:
                assert object != null;
                onError(Errors.connectError(ErrorCode.INIT_FAILED,
                        "Session initialize failed.", (Exception) object));
                break;
            case START_SUCCESS:
                assert object != null;
                onReady((Socket) object);
                break;
            case START_FAILED:
                assert object != null;
                onFailed((Exception) object);
                break;
            case ON_PACKET:
                assert object != null;
                onPacket((Packet) object);
                break;
            case Pulse.PULSE:
                pulse.pulse();
                break;
        }
    }
}
