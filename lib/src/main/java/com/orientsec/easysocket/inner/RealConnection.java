package com.orientsec.easysocket.inner;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.EasyManager;
import com.orientsec.easysocket.EasySocket;
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

import java.util.List;
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
    private final Executor callbackExecutor;
    private final TaskManager taskManager;
    private PushManager<?, ?> pushManager;
    final EventManager eventManager;
    private final Set<ConnectEventListener> connectEventListeners = new CopyOnWriteArraySet<>();
    //激活时间戳。
    private volatile long timestamp;

    Session session;

    private List<Address> addressList;
    private Address address;
    /**
     * 连接失败次数,不包括断开异常
     */
    private int failedTimes = 0;

    /**
     * 备用站点下标
     */
    private int backUpIndex = -1;

    public RealConnection(EasySocket easySocket) {
        this.easySocket = easySocket;
        easyManager = easySocket.getEasyManager();
        logger = easySocket.getLogger();
        name = easySocket.getName();
        callbackExecutor = easySocket.getCallbackExecutor();
        eventManager = easySocket.getEasyManager().newEventManager();
        taskManager = new RealTaskManager(easySocket, eventManager);

        connector = new Connector(this);

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
    public synchronized PushManager<?, ?> getPushManager() {
        if (pushManager == null) {
            pushManager = easySocket.getPushManagerProvider().get();
        }
        return pushManager;
    }

    @Override
    @NonNull
    public EasySocket getEasySocket() {
        return easySocket;
    }

    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    @NonNull
    public String toString() {
        return '[' + name + "]";
    }


    @Override
    protected void onStart() {
        if (isShutdown()) return;
        timestamp = System.currentTimeMillis();
        if (session == null) {
            session = new SocketSession(this);
            session.open();
        }
    }

    @Override
    protected void onStop() {
        if (isShutdown()) return;
        logger.i("Stop.");
        timestamp = 0;
        if (session != null) {
            session.close(Errors.stop());
        }
    }

    @Override
    protected void onShutdown() {
        if (isShutdown()) return;
        logger.w("Shutdown.");
        timestamp = -1;
        if (session != null) {
            session.close(Errors.shutdown());
        }
        easyManager.removeConnection(this);
    }

    @Override
    public void onConnect() {
        logger.i("Connect success.");
        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnect();
                }
            });
        }
    }

    @Override
    public void onConnectFailed(@NonNull EasyException e) {
        logger.i("Connect failed.", e);
        session = null;
        taskManager.reset(e);

        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnectFailed(e);
                }
            });
        }

        switchServer(e.getCode());
        connector.prepareRestart();
    }

    @Override
    public void onDisconnect(@NonNull EasyException e) {
        logger.i("Connect error.", e);
        session = null;
        taskManager.reset(e);

        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onDisconnect(e);
                }
            });
        }


        switchServer(e.getCode());
        connector.prepareRestart();
    }

    @Override
    public void onAvailable() {
        logger.i("Connect available.");
        failedTimes = 0;
        taskManager.ready();
        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onAvailable();
                }
            });
        }
    }


    @Override
    public void start() {
        if (isShutdown()) return;
        eventManager.publish(Events.START);
    }

    @Override
    public void stop() {
        if (isShutdown()) return;
        eventManager.publish(Events.STOP);
    }

    @Override
    public void shutdown() {
        if (isShutdown()) return;
        eventManager.publish(Events.SHUTDOWN);
    }

    @Override
    public boolean isShutdown() {
        return timestamp < 0;
    }

    @Override
    public boolean isConnect() {
        return session != null && session.isConnect();
    }

    @Override
    public boolean isAvailable() {
        return session != null && session.isAvailable();
    }

    @Nullable
    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public void onNetworkAvailable() {
        if (timestamp > 0) {
            connector.prepareRestart();
        }
    }

    @Override
    public Address obtainAddress() {
        if (addressList == null) {
            List<Address> addressList = easySocket.getAddressProvider()
                    .get();
            if (addressList.isEmpty()) {
                throw new IllegalArgumentException("Address list is empty");
            }
            this.addressList = addressList;
            address = addressList.get(0);
        }
        return address;
    }

    /**
     * 切换服务器
     */
    private void switchServer(int code) {
        if (code == ErrorCode.PULSE_TIME_OUT
                || code == ErrorCode.STOP
                || code == ErrorCode.SHUTDOWN) {
            return;
        }
        //连接失败达到阈值,需要切换备用线路
        if (++failedTimes >= easySocket.getRetryTimes()) {
            failedTimes = 0;

            if (++backUpIndex >= addressList.size()) {
                backUpIndex = 0;
                Address address = addressList.get(backUpIndex);
                this.address = address;
                logger.i("Switch to server: " + address);
            }
        }
    }

    boolean isActive() {
        long mills = System.currentTimeMillis();
        long backgroundTimestamp = easyManager.getBackgroundTimestamp();
        return timestamp > 0 &&
                (mills - timestamp <= easySocket.getLiveTime()
                        || backgroundTimestamp == 0
                        || mills - backgroundTimestamp <= easySocket.getLiveTime());
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        if (eventId < 0) return;
        switch (eventId) {
            case Events.START:
                onStart();
                break;
            case Events.STOP:
                onStop();
                break;
            case Events.SHUTDOWN:
                onShutdown();
                break;
            case Events.RESTART:
                connector.restart();
                break;
        }
    }
}
