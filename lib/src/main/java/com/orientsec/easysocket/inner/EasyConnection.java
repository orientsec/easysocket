package com.orientsec.easysocket.inner;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.ErrorCode;
import com.orientsec.easysocket.exception.ErrorType;
import com.orientsec.easysocket.task.Task;
import com.orientsec.easysocket.task.TaskManager;
import com.orientsec.easysocket.task.TaskManagerImpl;
import com.orientsec.easysocket.utils.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;


/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2017/12/27 15:13
 * Author: Fredric
 * coding is art not science
 */
public class EasyConnection<T> implements Connection<T>, EventListener, Initializer.Emitter {

    final EventManager eventManager;

    protected final Options<T> options;

    private final Executor callbackExecutor;

    protected final Executor connectExecutor;

    protected final ReConnector<T> reConnector;

    private final TaskManager<T> taskManager;

    private final Map<String, PacketHandler<T>> messageHandlerMap;

    protected final Pulser<T> pulser;

    protected final Set<ConnectEventListener> connectEventListeners = new CopyOnWriteArraySet<>();

    /**
     * 连接状态
     */
    volatile State state = State.IDLE;

    private volatile long timestamp;

    List<Address> addressList;

    Address address;

    private Session session;

    private final ConnectRunnable<T> connectRunnable;

    public EasyConnection(Options<T> options) {
        this.options = options;
        eventManager = new EventManager();
        reConnector = new ReConnector<>(this);
        callbackExecutor = options.getCallbackExecutor();
        connectExecutor = options.getConnectExecutor();
        taskManager = new TaskManagerImpl<>(this, eventManager, options);
        pulser = new Pulser<>(this, options, eventManager);
        connectRunnable = new ConnectRunnable<>(this);

        messageHandlerMap = new HashMap<>();
        messageHandlerMap.put(PacketType.RESPONSE.getValue(), taskManager);
        messageHandlerMap.put(PacketType.PULSE.getValue(), pulser);
        messageHandlerMap.put(PacketType.PUSH.getValue(), options.getPushHandler());

        eventManager.addListener(this);
    }

    @Override
    public void success() {
        eventManager.publish(Events.AVAILABLE);
    }

    @Override
    public void fail(EasyException e) {
        Logger.i("Connection initialize failed: " + e);
        stop(e);
    }

    @Override
    public boolean isShutdown() {
        return state == State.SHUTDOWN;
    }

    @Override
    public boolean isConnect() {
        return state == State.CONNECT || state == State.AVAILABLE;
    }

    @Override
    @NonNull
    public <R> Task<T, R> buildTask(@NonNull Request<T, R> request,
                                    @NonNull Callback<R> callback) {
        return taskManager.buildTask(request, callback);
    }

    private void dispatchPacket(@NonNull Packet<T> packet) {
        PacketHandler<T> packetHandler = messageHandlerMap.get(packet.getPacketType().getValue());
        if (packetHandler == null) {
            Logger.e("No packet handler for type: " + packet.getPacketType());
        } else {
            packetHandler.handlePacket(packet);
        }
    }

    @Nullable
    @Override
    public Address getAddress() {
        Session session = this.session;
        return session == null ? null : session.getAddress();
    }

    @Override
    public void start() {
        if (state == State.IDLE) {
            eventManager.publish(Events.START);
        }
    }

    void onStart() {
        if (state == State.IDLE) {
            state = State.STARTING;
            connectExecutor.execute(connectRunnable);
        }
    }

    private void onSessionCreate(@NonNull Session session) {
        Logger.i("Connection is established, " + address);
        if (state == State.STARTING) {
            state = State.CONNECT;
            this.session = session;
            session.active();
            //启动心跳及读写线程
            pulser.start();
            //连接后进行一些前置操作，例如资源初始化
            options.getInitializer().start(this, this);
            if (connectEventListeners.size() > 0) {
                callbackExecutor.execute(() -> {
                    for (ConnectEventListener listener : connectEventListeners) {
                        listener.onConnect();
                    }
                });
            }
        } else {
            session.close();
            //connection is shutdown
            EasyException e = new EasyException(ErrorCode.SHUT_DOWN, ErrorType.SYSTEM, "Connection shut down.");
            taskManager.reset(e);
        }

    }

    private void onSessionError(EasyException e) {
        Logger.i("Fail to establish connection, " + address);
        if (state == State.STARTING) {
            state = State.IDLE;
        }
        taskManager.reset(e);
        reConnector.reconnectDelay();
        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnectFailed();
                }
            });
        }
    }


    @Override
    public void shutdown() {
        eventManager.publish(Events.SHUTDOWN);
    }

    private void onShutdown() {
        if (state == State.SHUTDOWN) return;
        reConnector.stopDisconnect();
        reConnector.stopReconnect();
        ConnectionManager.getInstance().removeConnection(this);
        EasyException e = new EasyException(ErrorCode.SHUT_DOWN, ErrorType.SYSTEM, "Connection shut down.");
        onStop(e);
        state = State.SHUTDOWN;
    }

    public void stop(EasyException e) {
        eventManager.publish(Events.STOP, e);
    }

    void onStop(@NonNull EasyException e) {
        if (state == State.CONNECT || state == State.AVAILABLE) {
            Logger.i("Connection is disconnected, " + address);
            state = State.IDLE;
            session.close();
            session = null;
            pulser.stop();
            taskManager.reset(e);
            reConnector.reconnectDelay();

            if (connectEventListeners.size() > 0) {
                callbackExecutor.execute(() -> {
                    for (ConnectEventListener listener : connectEventListeners) {
                        listener.onDisconnect(e);
                    }
                });
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return state == State.AVAILABLE;
    }

    private void onAvailable() {
        if (state == State.CONNECT) {
            Logger.i("Connection is available," + address);
            state = State.AVAILABLE;
            taskManager.start();
            reConnector.reset();
            if (connectEventListeners.size() > 0) {
                callbackExecutor.execute(() -> {
                    for (ConnectEventListener listener : connectEventListeners) {
                        listener.onAvailable();
                    }
                });
            }
        }
    }

    public void onNetworkAvailable() {
        eventManager.publish(Events.NET_AVAILABLE);
    }

    public void setBackground() {
        eventManager.publish(Events.SLEEP);
    }


    public void setForeground() {
        eventManager.publish(Events.WAKEUP);
    }

    boolean isSleep() {
        return timestamp != 0
                && System.currentTimeMillis() - timestamp > options.getBackgroundLiveTime();
    }

    boolean isForeground() {
        return timestamp == 0;
    }

    @Override
    public void addConnectEventListener(@NonNull ConnectEventListener listener) {
        connectEventListeners.add(listener);
    }

    @Override
    public void removeConnectEventListener(@NonNull ConnectEventListener listener) {
        connectEventListeners.remove(listener);
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        if (eventId < 0) return;
        switch (eventId) {
            case Events.START:
                onStart();
                break;
            case Events.STOP:
                assert object != null;
                onStop((EasyException) object);
                break;
            case Events.SHUTDOWN:
                onShutdown();
                break;
            case Events.AVAILABLE:
                onAvailable();
                break;
            case Events.START_DELAY:
                reConnector.reconnect();
                break;
            case Events.STOP_DELAY:
                reConnector.disconnect();
                break;
            case Events.CONNECT_SUCCESS:
                assert object != null;
                onSessionCreate((Session) object);
                break;
            case Events.CONNECT_FAILED:
                onSessionError((EasyException) object);
                break;
            case Events.ON_PACKET:
                assert object != null;
                dispatchPacket((Packet<T>) object);
                break;
            case Events.PULSE:
                pulser.pulse();
                break;
            case Events.NET_AVAILABLE:
                reConnector.reconnectDelay();
                break;
            case Events.SLEEP:
                timestamp = System.currentTimeMillis();
                reConnector.disconnectDelay();
                break;
            case Events.WAKEUP:
                timestamp = 0;
                reConnector.stopDisconnect();
                break;
        }
    }

    private static class ConnectRunnable<T> implements Runnable {
        private final EventManager eventManager;

        private final EasyConnection<T> connection;

        public ConnectRunnable(EasyConnection<T> connection) {
            this.connection = connection;
            this.eventManager = connection.eventManager;
        }

        @Override
        public void run() {
            setAddressList();
            Logger.i("begin socket connect, " + connection.address);
            try {
                Session session = new SocketSession<>(connection, connection.options,
                        eventManager, connection.taskManager.taskQueue());
                session.open();
                eventManager.publish(Events.CONNECT_SUCCESS, session);
            } catch (Exception e) {
                Logger.e("Socket connect failed: " + e.getMessage());
                eventManager.publish(Events.CONNECT_FAILED, e);
            }
        }

        private void setAddressList() {
            if (connection.addressList == null) {
                List<Address> addressList = connection.options.getAddressSupplier().get();
                if (addressList.isEmpty()) {
                    throw new IllegalArgumentException("Address list is empty");
                }
                connection.addressList = addressList;
                connection.address = addressList.get(0);
            }
        }
    }

}
