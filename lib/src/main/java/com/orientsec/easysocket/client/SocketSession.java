package com.orientsec.easysocket.client;

import android.net.TrafficStats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.PacketType;
import com.orientsec.easysocket.Period;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorBuilder;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;
import com.orientsec.easysocket.task.TaskManager;
import com.orientsec.easysocket.utils.LogFactory;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.net.InetAddress;
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
public class SocketSession implements OperableSession, Initializer.Emitter, Runnable, EventListener {
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

    private final Address address;

    private final int addressIndex;

    private State state = State.IDLE;

    private boolean serverAvailable = false;

    private Pulse pulse;

    private final Map<String, PacketHandler> messageHandlerMap = new HashMap<>();

    private final long id;

    private final ErrorBuilder errorBuilder;

    /**
     * 连接各阶段耗时。
     */
    private final Map<Period, Long> connectTimeMap = new HashMap<>();

    SocketSession(AbstractSocketClient socketClient, Address address, int addressIndex, long id) {
        this.socketClient = socketClient;
        this.address = address;
        this.addressIndex = addressIndex;
        this.options = socketClient.getOptions();
        this.id = id;

        connectExecutor = options.getConnectExecutor();

        eventManager = EasySocket.getInstance().newEventManager();
        eventManager.addListener(this);

        String suffix = "  Session(" + id + ")[" + address.getHost() + ":"
                + address.getPort() + "]  Client[" + options.getName() + "]";
        errorBuilder = new ErrorBuilder(suffix);
        logger = LogFactory.getLogger(options, suffix);
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public Logger getLogger() {
        return logger;
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
            logger.i("Session is opening.");

            socketClient.onConnectionStart(this);
        }
    }

    @Override
    public void close(int code, int type, String message) {
        EasyException e = errorBuilder.create(code, type, message);
        if (state == State.IDLE || state == State.STARTING) {
            state = State.DETACH;
            logger.e("Session is closed.", e);
            socketClient.onConnectionFailed(this, e);
        } else {
            onError(e);
        }
    }

    @Override
    public void success() {
        eventManager.publish(AVAILABLE, this);
    }

    @Override
    public void fail(Exception cause) {
        logger.e("Fail to initialize session.");
        eventManager.publish(INIT_FAILED,
                errorBuilder.create(ErrorCode.SESSION_INIT_FAILED, ErrorType.CONNECT,
                        "Session initialize failed.", cause));
    }

    @Override
    public void postClose(int code, int type, String message, Exception cause) {
        eventManager.publish(ERROR, errorBuilder.create(code, type, message, cause));
    }

    private void onReady(Socket socket) {
        if (state == State.STARTING) {
            this.socket = socket;
            //连接后进行一些前置操作，例如资源初始化
            socketClient.getInitializer().start(this);
            //启动心跳及读写线程
            TaskManager taskManager = socketClient.getTaskManager();
            writer = new BlockingWriter(this, socket, taskManager);
            reader = new BlockingReader(this, socket, options, socketClient.getHeadParser());
            writer.start();
            reader.start();

            messageHandlerMap.put(PacketType.RESPONSE.getValue(), taskManager);

            state = State.CONNECT;
            logger.i("Session start success.");

            socketClient.onConnected(this);
        } else {
            //Session已关闭。
            connectExecutor.execute(() -> {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.w("Socket is closed.", e);
                }
            });
        }
    }

    private void onFailed(EasyException e) {
        if (state == State.STARTING) {
            state = State.DETACH;
            logger.e("Session start failed.");

            socketClient.onConnectionFailed(this, e);
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

            state = State.AVAILABLE;
            serverAvailable = true;
            logger.i("Session is available.");

            socketClient.onConnectionAvailable(this);
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
                    logger.w("Socket is closed.", ioe);
                }
            });

            state = State.DETACH;
            logger.e("Session is closed.", e);

            socketClient.onDisconnected(this, e);
        }
    }

    /**
     * 启动socket连接。
     */
    @Override
    public void run() {
        logger.i("Socket connection is starting.");
        TrafficStats.setThreadStatsTag((int) Thread.currentThread().getId());
        try {
            Socket socket = socketClient.getSocketFactory().createSocket();
            //关闭Nagle算法,无论TCP数据报大小,立即发送
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setPerformancePreferences(1, 2, 0);

            long startTimeMill = System.currentTimeMillis();
            long timestamp = startTimeMill;
            //STEP1:DNS
            SocketAddress socketAddress
                    = new InetSocketAddress(address.getHost(), address.getPort());
            long currentTimeMillis = System.currentTimeMillis();
            connectTimeMap.put(Period.DNS, currentTimeMillis - timestamp);
            timestamp = currentTimeMillis;

            //STEP2:CONNECT
            socket.connect(socketAddress, options.getConnectTimeOut());
            currentTimeMillis = System.currentTimeMillis();
            connectTimeMap.put(Period.CONNECT, currentTimeMillis - timestamp);
            timestamp = currentTimeMillis;

            //STEP3:SSL
            if (socket instanceof SSLSocket) {
                ((SSLSocket) socket).startHandshake();
                currentTimeMillis = System.currentTimeMillis();
                connectTimeMap.put(Period.SSL, currentTimeMillis - timestamp);
                timestamp = currentTimeMillis;
            }

            //总时长
            long connectTime = timestamp - startTimeMill;
            connectTimeMap.put(Period.ALL, connectTime);

            eventManager.publish(START_SUCCESS, socket);
            logger.i("Socket connected in " + connectTime + "ms");
        } catch (Exception e) {
            logger.e("Socket connection failed.", e);
            eventManager.publish(START_FAILED,
                    errorBuilder.create(ErrorCode.SOCKET_CONNECT, ErrorType.CONNECT,
                            "Socket connection failed.", e));
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
    public boolean isServerAvailable() {
        return serverAvailable;
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
                onError((EasyException) object);
                break;
            case START_SUCCESS:
                assert object != null;
                onReady((Socket) object);
                break;
            case START_FAILED:
                assert object != null;
                onFailed((EasyException) object);
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

    @NonNull
    @Override
    public Address getAddress() {
        return address;
    }

    @Nullable
    @Override
    public InetAddress getInetAddress() {
        if (socket != null) {
            return socket.getInetAddress();
        }
        return null;
    }

    @Override
    public int getAddressIndex() {
        return addressIndex;
    }

    @Override
    public long connectTime() {
        Long time = connectTimeMap.get(Period.ALL);
        if (time == null) {
            return -1;
        } else {
            return time;
        }
    }

    @Override
    public long connectTime(Period period) {
        Long time = connectTimeMap.get(period);
        if (time == null) {
            return -1;
        } else {
            return time;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "SocketSession[" +
                "id=" + id +
                "address=" + address +
                ']';
    }
}
