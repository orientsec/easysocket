package com.orientsec.easysocket;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.inner.EventManager;
import com.orientsec.easysocket.inner.RealConnection;
import com.orientsec.easysocket.utils.AndroidLogger;
import com.orientsec.easysocket.utils.Executors;
import com.orientsec.easysocket.utils.Logger;
import com.orientsec.easysocket.utils.NoLogger;

import java.util.List;
import java.util.concurrent.Executor;

import javax.net.SocketFactory;


/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/25 13:01
 * Author: Fredric
 * coding is art not science
 */

public class EasySocket {

    private static ConnectionManager connectionManager;


    /**
     * 初始化，在应用启动时执行。
     * 注册Activity生命周期监听及网络状态监听。
     *
     * @param application 应用上下文
     */
    public synchronized static void init(@NonNull Application application) {
        if (connectionManager == null) {
            connectionManager = new ConnectionManager(application);
        }
    }

    /**
     * 是否是调试模式
     */
    private boolean debug;

    private String name;

    /**
     * 心跳处理器
     */
    private Provider<PulseHandler> pulseHandlerProvider;
    /**
     * 站点信息
     */
    private Provider<List<Address>> addressProvider;
    /**
     * Socket factory
     */
    private Provider<SocketFactory> socketFactoryProvider;
    /**
     * 数据协议
     */
    private Provider<HeadParser> headParserProvider;
    /**
     * 推送消息处理器
     */
    private Provider<PacketHandler> pushHandlerProvider;
    /**
     * 连接初始化
     */
    private Provider<Initializer> initializerProvider;
    /**
     * 消息分发执行器
     * 连接状态监听回调，请求回调，都执行在Executor所在线程
     */
    private Executor callbackExecutor;

    /**
     * 连接管理线程池
     * 启动连接、关闭连接的执行线程池
     */
    private Executor connectExecutor;

    /**
     * 编解码执行器
     */
    private Executor codecExecutor;
    /**
     * 最大读取数据的K数(KB)<br>
     * 防止服务器返回数据体过大的数据导致前端内存溢出.
     */
    private int maxReadDataKB;

    /**
     * 请求超时时间 单位秒
     */
    private int requestTimeOut;

    /**
     * 连接超时时间 单位秒
     */
    private int connectTimeOut;

    /**
     * 心跳频率 单位秒
     */
    private int pulseRate;
    /**
     * 心跳失败次数
     */
    private int pulseLostTimes;

    /**
     * 后台存活时间
     */
    private int backgroundLiveTime;

    /**
     * 后台策略
     */
    private LivePolicy livePolicy;

    /**
     * 失败重连尝试次数
     */
    private int retryTimes;

    /**
     * 连接间隔
     */
    private int connectInterval;

    protected final EventManager eventManager;

    protected final Logger logger;

    private RealConnection connection;

    private EasySocket(Builder builder) {
        name = builder.name;
        debug = builder.debug;
        callbackExecutor = builder.callbackExecutor;
        connectExecutor = builder.connectExecutor;
        codecExecutor = builder.codecExecutor;
        maxReadDataKB = builder.maxReadDataKB;
        requestTimeOut = builder.requestTimeOut;
        connectTimeOut = builder.connectTimeOut;
        pulseRate = builder.pulseRate;
        pulseLostTimes = builder.pulseLostTimes;
        backgroundLiveTime = builder.backgroundLiveTime;
        livePolicy = builder.livePolicy;
        retryTimes = builder.retryTimes;
        connectInterval = builder.connectInterval;
        addressProvider = builder.addressProvider;
        headParserProvider = builder.headParserProvider;
        initializerProvider = builder.initializerProvider;
        pulseHandlerProvider = builder.pulseHandlerProvider;
        pushHandlerProvider = builder.pushHandlerProvider;
        socketFactoryProvider = builder.socketFactoryProvider;

        eventManager = new EventManager(connectionManager.handlerThread.getLooper());
        if (debug) {
            logger = new AndroidLogger(name);
        } else {
            logger = new NoLogger();
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public String getName() {
        return name;
    }

    public Executor getCallbackExecutor() {
        return callbackExecutor;
    }

    public Executor getConnectExecutor() {
        return connectExecutor;
    }

    public Executor getCodecExecutor() {
        return codecExecutor;
    }

    public int getMaxReadDataKB() {
        return maxReadDataKB;
    }

    public int getRequestTimeOut() {
        return requestTimeOut;
    }

    public int getPulseRate() {
        return pulseRate;
    }

    public int getPulseLostTimes() {
        return pulseLostTimes;
    }

    public int getBackgroundLiveTime() {
        return backgroundLiveTime;
    }

    public LivePolicy getLivePolicy() {
        return livePolicy;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public int getConnectInterval() {
        return connectInterval;
    }

    public Provider<PulseHandler> getPulseHandlerProvider() {
        return pulseHandlerProvider;
    }

    public Provider<List<Address>> getAddressProvider() {
        return addressProvider;
    }

    public Provider<SocketFactory> getSocketFactoryProvider() {
        return socketFactoryProvider;
    }

    public Provider<HeadParser> getHeadParserProvider() {
        return headParserProvider;
    }

    public Provider<PacketHandler> getPushHandlerProvider() {
        return pushHandlerProvider;
    }

    public Provider<Initializer> getInitializerProvider() {
        return initializerProvider;
    }


    public Logger getLogger() {
        return logger;
    }

    public Context getContext() {
        return connectionManager.application;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * 创建一个{@link Connection}
     *
     * @return 连接
     */
    @NonNull
    public synchronized Connection open() {
        if (connection == null) {
            connection = new RealConnection(this);
            return connection;
        } else {
            throw new IllegalStateException("Connection is already open.");
        }
    }

    public synchronized Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Connection is not open.");
        }
        return connection;
    }

    public static final class Builder {
        private String name = "";
        private boolean debug;
        private Provider<PulseHandler> pulseHandlerProvider;
        private Provider<List<Address>> addressProvider;
        private Provider<SocketFactory> socketFactoryProvider;
        private Provider<HeadParser> headParserProvider;
        private Provider<PacketHandler> pushHandlerProvider;
        private Provider<Initializer> initializerProvider;
        private Executor callbackExecutor;
        private Executor connectExecutor;
        private Executor codecExecutor;
        private int maxReadDataKB = 1024;
        private int requestTimeOut = 5000;
        private int connectTimeOut = 5000;
        private int pulseRate = 60 * 1000;
        private int pulseLostTimes = 2;
        private int backgroundLiveTime = 30 * 1000;
        private LivePolicy livePolicy = LivePolicy.DEFAULT;
        private int retryTimes;
        private int connectInterval = 3000;

        public Builder() {
        }

        public Builder name(@NonNull String val) {
            name = val;
            return this;
        }

        public Builder debug(boolean val) {
            debug = val;
            return this;
        }

        public Builder pulseHandlerProvider(@NonNull Provider<PulseHandler> val) {
            pulseHandlerProvider = val;
            return this;
        }

        public Builder addressList(@NonNull List<Address> val) {
            addressProvider = StaticAddressProvider.build(val);
            return this;
        }

        public Builder addressProvider(@NonNull Provider<List<Address>> val) {
            addressProvider = val;
            return this;
        }

        public Builder headParserProvider(@NonNull Provider<HeadParser> val) {
            headParserProvider = val;
            return this;
        }

        public Builder pushHandlerProvider(@NonNull Provider<PacketHandler> val) {
            pushHandlerProvider = val;
            return this;
        }

        public Builder initializerProvider(@NonNull Provider<Initializer> val) {
            initializerProvider = val;
            return this;
        }

        public Builder socketFactoryProvider(@NonNull Provider<SocketFactory> val) {
            socketFactoryProvider = val;
            return this;
        }

        public Builder callbackExecutor(@NonNull Executor val) {
            callbackExecutor = val;
            return this;
        }

        public Builder connectExecutor(@NonNull Executor val) {
            connectExecutor = val;
            return this;
        }

        public Builder codecExecutor(@NonNull Executor val) {
            codecExecutor = val;
            return this;
        }

        public Builder maxReadDataKB(int val) {
            maxReadDataKB = val;
            return this;
        }

        public Builder requestTimeOut(int val) {
            requestTimeOut = val;
            return this;
        }

        public Builder connectTimeOut(int val) {
            connectTimeOut = val;
            return this;
        }

        public Builder pulseRate(int val) {
            pulseRate = val;
            return this;
        }

        public Builder pulseLostTimes(int val) {
            pulseLostTimes = val;
            return this;
        }

        public Builder backgroundLiveTime(int val) {
            backgroundLiveTime = val;
            return this;
        }

        public Builder livePolicy(@NonNull LivePolicy val) {
            livePolicy = val;
            return this;
        }

        public Builder retryTimes(int val) {
            retryTimes = val;
            return this;
        }

        public Builder connectInterval(int val) {
            connectInterval = val;
            return this;
        }

        @NonNull
        public EasySocket build() {
            if (connectionManager == null) {
                throw new IllegalStateException("EasySocket is not initialize.");
            }
            String error = checkParams();
            if (error.isEmpty()) {
                return new EasySocket(this);
            }
            throw new IllegalArgumentException(error);
        }

        @NonNull
        public Connection open() {
            return build().open();
        }

        private String checkParams() {
            if (headParserProvider == null) {
                return "Head parser provider not set.";
            }
            if (addressProvider == null) {
                return "Address provider not set.";
            }
            if (maxReadDataKB <= 0) {
                return "Max read data size in kb must be positive.";
            }
            if (connectTimeOut < 0) {
                return "Connect time out is negative.";
            }
            if (requestTimeOut <= 0) {
                return "Request time out must be positive..";
            }
            if (pulseRate < 30 * 1000) {
                return "Pulse rate must big than 30s.";
            }
            if (pulseLostTimes < 0) {
                return "Pulse lost time is negative.";
            }
            if (backgroundLiveTime <= 15 * 1000) {
                return "Background live time must big than 15s.";
            }
            if (retryTimes < 0) {
                return "Retry time is negative.";
            }
            if (connectInterval <= 1000) {
                return "Connect interval must big than 1000ms.";
            }
            if (socketFactoryProvider == null) {
                socketFactoryProvider = new DefaultSocketFactoryProvider();
            }
            if (pulseHandlerProvider == null) {
                pulseHandlerProvider = new DefaultPulseHandlerProvider();
            }
            if (pushHandlerProvider == null) {
                pushHandlerProvider = new DefaultPacketHandlerProvider();
            }
            if (initializerProvider == null) {
                initializerProvider = new DefaultInitializerProvider();
            }
            if (callbackExecutor == null) {
                callbackExecutor = Executors.mainThreadExecutor;
            }
            if (connectExecutor == null) {
                connectExecutor = Executors.connectExecutor;
            }
            if (codecExecutor == null) {
                codecExecutor = Executors.codecExecutor;
            }

            return "";
        }
    }
}
