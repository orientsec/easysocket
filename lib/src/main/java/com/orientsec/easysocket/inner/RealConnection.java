package com.orientsec.easysocket.inner;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.EasyManager;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.Errors;
import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.request.Request;
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
public class RealConnection extends AbstractConnection {
    private final Logger logger;
    private final String name;
    private final EasySocket easySocket;
    private final EasyManager easyManager;
    private final Connector connector;
    private final Executor connectExecutor;
    private final Executor callbackExecutor;
    private final TaskManager taskManager;
    private final EventManager eventManager;
    private final PushManager<?, ?> pushManager;
    private final Pulse pulse;

    private final Map<String, PacketHandler> messageHandlerMap = new HashMap<>();
    final Set<ConnectEventListener> connectEventListeners = new CopyOnWriteArraySet<>();
    //连接状态
    volatile State state = State.IDLE;
    //激活时间戳。
    private volatile long activeTimestamp;
    List<Address> addressList;
    Address address;
    private Session session;

    public RealConnection(EasySocket easySocket) {
        this.easySocket = easySocket;
        easyManager = easySocket.getEasyManager();
        logger = easySocket.getLogger();
        name = easySocket.getName();
        connectExecutor = easySocket.getConnectExecutor();
        callbackExecutor = easySocket.getCallbackExecutor();
        eventManager = easySocket.getEventManager();
        taskManager = new RealTaskManager(easySocket);
        pulse = new Pulse(easySocket);
        pushManager = easySocket.getPushManagerProvider().get();
        connector = new Connector(this);

        messageHandlerMap.put(PacketType.RESPONSE.getValue(), taskManager);
        messageHandlerMap.put(PacketType.PULSE.getValue(), pulse);
        messageHandlerMap.put(PacketType.PUSH.getValue(), pushManager);

        eventManager.addListener(this);
        easyManager.addConnection(this);
    }

    @NonNull
    @Override
    public <R extends T, T> Task<R> buildTask(@NonNull Request<R> request,
                                              @NonNull Callback<T> callback) {
        return taskManager.buildTask(request, callback);
    }

    @Override
    public void addConnectEventListener(@NonNull ConnectEventListener listener) {
        connectEventListeners.add(listener);
    }

    @Override
    public void removeConnectEventListener(@NonNull ConnectEventListener listener) {
        connectEventListeners.remove(listener);
    }

    @NonNull
    @Override
    public PushManager<?, ?> getPushManager() {
        return pushManager;
    }

    @Override
    @NonNull
    public EasySocket getEasySocket() {
        return easySocket;
    }

    @Override
    @NonNull
    public String toString() {
        return '[' + name + "]";
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
    protected void onStart() {
        if (state == State.IDLE) {
            state = State.STARTING;
            connectExecutor.execute(new ConnectRunnable());
        }
    }


    @Override
    protected void onSuccess(@NonNull Session session) {
        if (state == State.STARTING) {
            logger.i("Connected.");
            state = State.CONNECT;
            this.session = session;
            session.active();
            //启动心跳及读写线程
            pulse.start();

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

    @Override
    protected void onFailed(@NonNull EasyException e) {
        logger.i("Connect failed.", e);
        if (state == State.STARTING) {
            state = State.IDLE;
        }
        taskManager.reset(e);
        connector.switchServer();
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
    protected void onError(@NonNull EasyException e) {
        if (state == State.CONNECT || state == State.AVAILABLE) {
            logger.i("Disconnected.", e);
            state = State.IDLE;
            session.close();
            session = null;
            pulse.stop();
            taskManager.reset(e);

            if (connectEventListeners.size() > 0) {
                callbackExecutor.execute(() -> {
                    for (ConnectEventListener listener : connectEventListeners) {
                        listener.onDisconnect(e);
                    }
                });
            }

            if (e.getCode() == ErrorCode.SHUT_DOWN) return;
            if (e.getCode() != ErrorCode.PULSE_TIME_OUT) {
                connector.switchServer();
            }
            connector.prepareRestart();
        }
    }

    @Override
    protected void onAvailable() {
        if (state == State.CONNECT) {
            logger.i("Available.");
            state = State.AVAILABLE;
            taskManager.ready();
            connector.ready();
            if (connectEventListeners.size() > 0) {
                callbackExecutor.execute(() -> {
                    for (ConnectEventListener listener : connectEventListeners) {
                        listener.onAvailable();
                    }
                });
            }
        }
    }

    @Override
    protected void onShutdown() {
        if (state == State.SHUTDOWN) return;
        logger.w("Shut down.");
        onError(Errors.shutdown());
        connector.stopRestart();
        state = State.SHUTDOWN;
        easyManager.removeConnection(this);
    }

    @Override
    public void start() {
        activeTimestamp = System.currentTimeMillis();
        if (state == State.IDLE) {
            eventManager.publish(Events.START);
        }
    }

    @Override
    public void shutdown() {
        eventManager.publish(Events.SHUTDOWN);
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
    public boolean isAvailable() {
        return state == State.AVAILABLE;
    }

    @Nullable
    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public void onNetworkAvailable() {
        if (activeTimestamp > 0) {
            connector.prepareRestart();
        }
    }

    boolean isActive() {
        long mills = System.currentTimeMillis();
        long backgroundTimestamp = easyManager.getBackgroundTimestamp();
        return mills - activeTimestamp <= easySocket.getLiveTime()
                || backgroundTimestamp == 0
                || mills - backgroundTimestamp <= easySocket.getLiveTime();
    }


    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        if (eventId < 0) return;
        switch (eventId) {
            case Events.START:
                onStart();
                break;
            case Events.CONNECT_ERROR:
                assert object != null;
                onError((EasyException) object);
                break;
            case Events.SHUTDOWN:
                onShutdown();
                break;
            case Events.AVAILABLE:
                assert object != null;
                Session session = (Session) object;
                if (session == this.session) {
                    onAvailable();
                }
                break;
            case Events.INIT_FAILED:
                assert object != null;
                session = (Session) object;
                if (session == this.session) {
                    onError(Errors.connectError(ErrorCode.INIT_FAILED,
                            "Connection initialize failed."));
                }
                break;
            case Events.CONNECT_SUCCESS:
                assert object != null;
                onSuccess((Session) object);
                break;
            case Events.CONNECT_FAILED:
                assert object != null;
                onFailed((EasyException) object);
                break;
            case Events.ON_PACKET:
                assert object != null;
                dispatchPacket((Packet) object);
                break;
            case Events.RESTART:
                connector.restart();
                break;
            case Events.PULSE:
                pulse.pulse();
                break;
        }
    }

    private class ConnectRunnable implements Runnable {

        @Override
        public void run() {
            setAddressList();
            logger.i("Begin socket connect.");
            try {
                Session session = new SocketSession(easySocket, address, taskManager);
                session.open();
                eventManager.publish(Events.CONNECT_SUCCESS, session);
            } catch (Exception e) {
                logger.i("Socket connect failed.", e);
                eventManager.publish(Events.CONNECT_FAILED,
                        Errors.connectError(ErrorCode.SOCKET_CONNECT, "Socket connect failed."));
            }
        }

        private void setAddressList() {
            if (addressList == null) {
                List<Address> addresses = easySocket.getAddressProvider()
                        .get();
                if (addresses.isEmpty()) {
                    throw new IllegalArgumentException("Address list is empty");
                }
                addressList = addresses;
                address = addressList.get(0);
            }
        }
    }

}
