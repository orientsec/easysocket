package com.orientsec.easysocket.client;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.ConnectListener;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.Errors;
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
public class EasySocketClient extends AbstractSocketClient {
    static final int START = 101;
    static final int STOP = 102;
    static final int SHUTDOWN = 103;
    static final int RESTART = 104;

    private final Logger logger;
    private final String name;
    private final Connector connector;
    private final Executor callbackExecutor;
    private final TaskManager taskManager;
    final EventManager eventManager;
    private final Set<ConnectListener> connectListeners = new CopyOnWriteArraySet<>();

    //激活时间戳。
    private long timestamp;

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

    public EasySocketClient(Options options) {
        super(options);
        logger = options.getLogger();
        name = options.getName();
        callbackExecutor = options.getCallbackExecutor();
        eventManager = EasySocket.getInstance().newEventManager();
        taskManager = new RealTaskManager(this, eventManager);
        connector = new Connector(this);
        eventManager.addListener(this);
        EasySocket.getInstance().addSocketClient(this);
    }

    @NonNull
    @Override
    public <R extends T, T> Task<R> buildTask(@NonNull Request<R> request,
                                              @NonNull Callback<T> callback) {
        return taskManager.buildTask(request, callback);
    }

    @Override
    public void addConnectListener(@NonNull ConnectListener listener) {
        connectListeners.add(listener);
    }

    @Override
    public void removeConnectListener(@NonNull ConnectListener listener) {
        connectListeners.remove(listener);
    }

    @Override
    @NonNull
    public Options getOptions() {
        return options;
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
        onStart(true);
    }

    void onStart(boolean active) {
        if (isShutdown()) return;
        if (active) {
            timestamp = System.currentTimeMillis();
        }
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
        EasySocket.getInstance().removeSocketClient(this);
    }

    @Override
    public void onConnect() {
        if (connectListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectListener listener : connectListeners) {
                    listener.onConnect();
                }
            });
        }
    }

    @Override
    public void onConnectFailed(@NonNull EasyException e) {
        session = null;
        taskManager.reset(e);

        if (connectListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectListener listener : connectListeners) {
                    listener.onConnectFailed(e);
                }
            });
        }

        switchServer(e.getCode());
        connector.prepareRestart();
    }

    @Override
    public void onDisconnect(@NonNull EasyException e) {
        session = null;
        taskManager.reset(e);

        if (connectListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectListener listener : connectListeners) {
                    listener.onDisconnect(e);
                }
            });
        }


        switchServer(e.getCode());
        connector.prepareRestart();
    }

    @Override
    public void onAvailable() {
        failedTimes = 0;
        taskManager.ready();
        if (connectListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectListener listener : connectListeners) {
                    listener.onAvailable();
                }
            });
        }
    }


    @Override
    public void start() {
        if (isShutdown()) return;
        eventManager.publish(START);
    }

    @Override
    public void stop() {
        if (isShutdown()) return;
        eventManager.publish(STOP);
    }

    @Override
    public void shutdown() {
        if (isShutdown()) return;
        eventManager.publish(SHUTDOWN);
    }

    @Override
    public boolean isShutdown() {
        return timestamp < 0;
    }

    @Override
    public boolean isConnect() {
        Session session = this.session;
        return session != null && session.isConnect();
    }

    @Override
    public boolean isAvailable() {
        Session session = this.session;
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
    public synchronized Address obtainAddress() {
        if (addressList == null) {
            List<Address> addressList = options.getAddressProvider()
                    .get(this);
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
    private synchronized void switchServer(int code) {
        if (code == ErrorCode.PULSE_TIME_OUT
                || code == ErrorCode.STOP
                || code == ErrorCode.SHUTDOWN
                || addressList == null) {
            return;
        }
        //连接失败达到阈值,需要切换备用线路
        if (++failedTimes >= options.getRetryTimes()) {
            failedTimes = 0;

            if (++backUpIndex >= addressList.size()) {
                backUpIndex = 0;
            }
            Address address = addressList.get(backUpIndex);
            this.address = address;
            logger.i("Switch to server: " + address);
        }
    }

    boolean isActive() {
        long mills = System.currentTimeMillis();
        long backgroundTimestamp = EasySocket.getInstance().getBackgroundTimestamp();
        return timestamp > 0 &&
                (mills - timestamp <= options.getLiveTime()
                        || backgroundTimestamp == 0
                        || mills - backgroundTimestamp <= options.getLiveTime());
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        switch (eventId) {
            case START:
                onStart();
                break;
            case STOP:
                onStop();
                break;
            case SHUTDOWN:
                onShutdown();
                break;
            case RESTART:
                connector.restart();
                break;
        }
    }
}
