package com.orientsec.easysocket.client;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.ConnectionListener;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.task.RealTaskManager;
import com.orientsec.easysocket.task.Task;
import com.orientsec.easysocket.task.TaskManager;

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
    static final int INIT_SUCCESS = 105;
    static final int INIT_ERROR = 106;

    private final String name;
    private final Connector connector;
    private final Executor callbackExecutor;
    private final TaskManager taskManager;
    final EventManager eventManager;
    private final Set<ConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();

    //激活时间戳。主动发起请求即为一次激活。
    private long timestamp;

    private SocketSession session;

    private List<Address> addressList;

    /**
     * 是否执行初始化中。
     */
    private boolean initializing;
    /**
     * 连接失败次数,不包括断开异常
     */
    private int failedTimes = 0;

    /**
     * 备用站点下标
     */
    private int backUpIndex;

    private long sessionId;

    public EasySocketClient(Options options) {
        super(options);
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
    public void addConnectListener(@NonNull ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    @Override
    public void removeConnectListener(@NonNull ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Nullable
    @Override
    public Session getSession() {
        return session;
    }

    @Override
    protected void onStart() {
        onStart(true);
    }

    /**
     * 启动Socket client。
     * 1.如果客户端已经关闭（timestamp<0）,不进行任何操作。
     * 2.如果地址列表未设置，启动初始化任务。并且，同一时间只会执行一个初始化任务。
     * 3.如果session已存在，使用当前session；如果session不存在，重新创建一个新的session，并启动。
     *
     * @param active 是否重置激活时间戳。
     */
    void onStart(boolean active) {
        if (isShutdown()) return;
        if (active) {
            timestamp = System.currentTimeMillis();
        }
        if (addressList == null) {
            if (initializing) {
                logger.i("Client is initializing, just wait for the result.");
            } else {
                initializing = true;
                options.getCodecExecutor().execute(new InitializeTask());
            }
        } else if (session == null) {
            session = new SocketSession(this, addressList.get(backUpIndex),
                    sessionId++);
            session.open();
        }
    }

    /**
     * 初始化成功。成功后立即调用onStart()。
     *
     * @param addressList 地址列表。
     */
    void onInitialized(List<Address> addressList) {
        initializing = false;
        this.addressList = addressList;
        onStart();
    }

    /**
     * 初始化失败。
     *
     * @param e 异常。
     */
    void onInitializeFailed(EasyException e) {
        initializing = false;
        taskManager.reset(e);
    }

    @Override
    protected void onStop() {
        if (isShutdown()) return;
        logger.w("Stop socket client.");
        timestamp = 0;
        if (session != null) {
            session.close(ErrorCode.STOP, ErrorType.SYSTEM, "Socket client stop.");
        }
    }

    @Override
    protected void onShutdown() {
        if (isShutdown()) return;
        logger.w("Shutdown socket client.");
        timestamp = -1;
        if (session != null) {
            session.close(ErrorCode.SHUTDOWN, ErrorType.SYSTEM, "Socket client shutdown.");
        }
        EasySocket.getInstance().removeSocketClient(this);
    }

    @Override
    public void onConnectionStart(@NonNull Session session) {
        assert session == this.session;
        if (connectionListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnectionStart(session);
                }
            });
        }
    }

    @Override
    public void onConnected(@NonNull final Session session) {
        assert session == this.session;
        if (connectionListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnected(session);
                }
            });
        }
    }

    @Override
    public void onConnectionFailed(@NonNull final Session session, @NonNull EasyException e) {
        assert session == this.session;
        this.session = null;
        taskManager.reset(e);
        connector.restart(session);

        if (connectionListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnectionFailed(session, e);
                }
            });
        }
    }

    @Override
    public void onDisconnected(@NonNull final Session session, @NonNull EasyException e) {
        assert session == this.session;
        this.session = null;
        taskManager.reset(e);
        connector.restart(session);

        if (connectionListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onDisconnected(session, e);
                }
            });
        }

    }

    @Override
    public void onConnectionAvailable(@NonNull final Session session) {
        assert session == this.session;
        failedTimes = 0;
        taskManager.ready();

        if (connectionListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnectionAvailable(session);
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
    public boolean isConnected() {
        Session session = this.session;
        return session != null && session.isConnect();
    }

    @Override
    public boolean isAvailable() {
        Session session = this.session;
        return session != null && session.isAvailable();
    }

    @Override
    public void onNetworkAvailable() {
        if (timestamp > 0 && session == null) {
            connector.restart();
        }
    }

    /**
     * 切换服务器
     */
    void switchServer() {
        //连接失败达到阈值,需要切换备用线路
        if (++failedTimes >= options.getRetryTimes()) {
            failedTimes = 0;

            if (++backUpIndex >= addressList.size()) {
                backUpIndex = 0;
            }
            logger.i("Switch to server: " + addressList.get(backUpIndex));
        }
    }

    boolean isActive() {
        //stop or shutdown.
        if (timestamp <= 0) return false;

        long backgroundTimestamp = EasySocket.getInstance().getBackgroundTimestamp();
        if (backgroundTimestamp == 0) return true;

        long mills = System.currentTimeMillis();
        long liveMills = options.getLiveTime();
        return mills - backgroundTimestamp <= liveMills && mills - timestamp <= liveMills;
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
            case INIT_SUCCESS:
                onInitialized((List<Address>) object);
                break;
            case INIT_ERROR:
                onInitializeFailed((EasyException) object);
                break;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "EasySocketClient[" +
                "name=" + name +
                ']';
    }

    /**
     * 初始化任务。由于初始化可能是耗时任务，例如：IO操作，所以初始化任务由Connection线程池调度。
     */
    private class InitializeTask implements Runnable {

        @Override
        public void run() {
            try {
                List<Address> addressList = options.getAddressProvider()
                        .get(EasySocketClient.this);
                if (addressList.isEmpty()) {
                    EasyException e = errorBuilder.create(ErrorCode.INIT_FAILED, ErrorType.SYSTEM,
                            "Address list is empty.");
                    eventManager.publish(INIT_ERROR, e);
                } else {
                    eventManager.publish(INIT_SUCCESS, addressList);
                }
            } catch (Exception ex) {
                logger.e("Fail to get address list.", ex);
                EasyException e = errorBuilder.create(ErrorCode.INIT_FAILED, ErrorType.SYSTEM,
                        "Failed to get address list.", ex);
                eventManager.publish(INIT_ERROR, e);
            }

        }
    }
}
