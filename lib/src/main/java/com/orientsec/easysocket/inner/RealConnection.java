package com.orientsec.easysocket.inner;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.Errors;
import com.orientsec.easysocket.task.RealTaskManager;
import com.orientsec.easysocket.task.Task;
import com.orientsec.easysocket.task.TaskManager;
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
public class RealConnection implements Connection, EventListener, Initializer.Emitter {
    private final EasySocket easySocket;
    private final EventManager eventManager;
    final TaskManager taskManager;
    private final String name;
    private final Logger logger;

    private final Executor callbackExecutor;

    private final Executor connectExecutor;

    private final Connector connector;

    private final Pulse pulse;

    private final Map<String, PacketHandler> messageHandlerMap = new HashMap<>();

    private final Set<ConnectEventListener> connectEventListeners = new CopyOnWriteArraySet<>();

    private final PacketHandler pushHandler;
    /**
     * 连接状态
     */
    volatile State state = State.IDLE;

    private volatile long timestamp;

    private Session session;

    List<Address> addressList;

    Address address;

    public RealConnection(EasySocket easySocket) {
        this.easySocket = easySocket;
        name = easySocket.getName();
        logger = easySocket.getLogger();
        eventManager = easySocket.getEventManager();
        callbackExecutor = easySocket.getCallbackExecutor();
        connectExecutor = easySocket.getConnectExecutor();
        taskManager = new RealTaskManager(easySocket);
        pushHandler = easySocket.getPushHandlerProvider().get(easySocket);
        pulse = new Pulse(easySocket);
        connector = new Connector(easySocket);
        eventManager.addListener(new PrepareListener());

        messageHandlerMap.put(PacketType.RESPONSE.getValue(), taskManager);
        messageHandlerMap.put(PacketType.PULSE.getValue(), pulse);
        messageHandlerMap.put(PacketType.PUSH.getValue(), pushHandler);
    }

    @Override
    public void success() {
        eventManager.publish(Events.AVAILABLE);
    }

    @Override
    public void fail() {
        logger.e("Initialize failed.");
        eventManager.publish(Events.STOP,
                Errors.connectError(ErrorCode.INIT_FAILED, "Connection initialize failed."));
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
    public <R> Task<R> buildTask(@NonNull Request<R> request,
                                 @NonNull Callback<R> callback) {
        return taskManager.buildTask(request, callback);
    }

    private void dispatchPacket(@NonNull Packet packet) {
        PacketHandler packetHandler = messageHandlerMap.get(packet.getPacketType().getValue());
        if (packetHandler == null) {
            logger.w("No packet handler for type: " + packet.getPacketType());
        } else {
            packetHandler.handlePacket(packet);
        }
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
            connectExecutor.execute(new ConnectRunnable(easySocket, this));
        }
    }

    private void onSessionCreate(@NonNull Session session) {
        if (state == State.STARTING) {
            logger.i("Connected.");
            state = State.CONNECT;
            this.session = session;
            session.active();
            //启动心跳及读写线程
            pulse.start();
            //连接后进行一些前置操作，例如资源初始化
            easySocket.getInitializerProvider()
                    .get(easySocket)
                    .start(this);
            if (connectEventListeners.size() > 0) {
                callbackExecutor.execute(() -> {
                    for (ConnectEventListener listener : connectEventListeners) {
                        listener.onConnect();
                    }
                });
            }
        } else {
            //connection is shutdown
            session.close();
            taskManager.reset(Errors.shutdown());
        }

    }

    private void onSessionError(EasyException e) {
        logger.i("Connect failed.", e);
        if (state == State.STARTING) {
            state = State.IDLE;
        }
        taskManager.reset(e);
        connector.prepareRestart();
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
        logger.w("Shut down.");
        connector.stopAutoStop();
        connector.stopRestart();
        easySocket.getConnectionManager().removeConnection(this);
        onStop(Errors.shutdown());
        state = State.SHUTDOWN;
    }

    public void stop() {
        eventManager.publish(Events.STOP,
                Errors.systemError(ErrorCode.MANUAL_STOP, "Connection stop."));
    }

    void onStop(@NonNull EasyException e) {
        if (state == State.CONNECT || state == State.AVAILABLE) {
            logger.i("Disconnected.", e);
            state = State.IDLE;
            session.close();
            session = null;
            pulse.stop();
            taskManager.reset(e);
            connector.prepareRestart();

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
            logger.i("Available.");
            state = State.AVAILABLE;
            taskManager.start();
            connector.reset();
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
        connector.prepareRestart();
    }

    public void setBackground() {
        timestamp = System.currentTimeMillis();
        connector.prepareAutoStop();
    }

    public void setForeground() {
        timestamp = 0;
        connector.stopAutoStop();
    }

    boolean isSleep() {
        return timestamp != 0
                && System.currentTimeMillis() - timestamp > easySocket.getBackgroundLiveTime();
    }

    @Override
    public void addConnectEventListener(@NonNull ConnectEventListener listener) {
        connectEventListeners.add(listener);
    }

    @Override
    public void removeConnectEventListener(@NonNull ConnectEventListener listener) {
        connectEventListeners.remove(listener);
    }

    @Nullable
    @Override
    public Address getAddress() {
        return address;
    }

    @NonNull
    @Override
    public PacketHandler getPushHandler() {
        return pushHandler;
    }

    @Override
    @NonNull
    public EasySocket getEasySocket() {
        return easySocket;
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
            case Events.CONNECT_SUCCESS:
                assert object != null;
                onSessionCreate((Session) object);
                break;
            case Events.CONNECT_FAILED:
                onSessionError((EasyException) object);
                break;
            case Events.ON_PACKET:
                assert object != null;
                dispatchPacket((Packet) object);
                break;
        }
    }

    @Override
    @NonNull
    public String toString() {
        return '[' + name + "] " + address;
    }

    private class PrepareListener implements EventListener {
        @Override
        public void onEvent(int eventId, @Nullable Object object) {
            if (eventId == Events.START) {
                connector.attach(RealConnection.this);
                eventManager.removeListener(this);
                eventManager.addListener(RealConnection.this);
                eventManager.addListener(pulse);
                eventManager.addListener(connector);
                eventManager.publish(eventId, object);

                easySocket.getConnectionManager().addConnection(RealConnection.this);
            }
        }
    }

    private static class ConnectRunnable implements Runnable {
        private final EventManager eventManager;

        private final EasySocket easySocket;

        private final RealConnection connection;

        private final Logger logger;

        public ConnectRunnable(EasySocket easySocket, RealConnection connection) {
            this.easySocket = easySocket;
            this.connection = connection;
            eventManager = easySocket.getEventManager();
            logger = easySocket.getLogger();
        }

        @Override
        public void run() {
            setAddressList();
            logger.i("Begin socket connect.");
            try {
                Session session = new SocketSession(easySocket, connection);
                session.open();
                eventManager.publish(Events.CONNECT_SUCCESS, session);
            } catch (Exception e) {
                logger.i("Socket connect failed.", e);
                eventManager.publish(Events.CONNECT_FAILED,
                        Errors.connectError(ErrorCode.SOCKET_CONNECT, "Socket connect failed."));
            }
        }

        private void setAddressList() {
            if (connection.addressList == null) {
                List<Address> addressList = easySocket.getAddressProvider()
                        .get(easySocket);
                if (addressList.isEmpty()) {
                    throw new IllegalArgumentException("Address list is empty");
                }
                connection.addressList = addressList;
                connection.address = addressList.get(0);
            }
        }
    }

}
