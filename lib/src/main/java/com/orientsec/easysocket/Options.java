package com.orientsec.easysocket;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.SocketFactory;

import io.reactivex.annotations.NonNull;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/27 13:33
 * Author: Fredric
 * coding is art not science
 * <p>
 * 连接配置类
 */
public class Options<T> {
    /**
     * 是否是调试模式
     */
    public static boolean debug;

    private PulseHandler<T> pulseHandler;
    /**
     * 站点信息
     */
    private Address address;
    /**
     * 备用站点信息
     */
    private List<Address> backupAddressList;
    /**
     * Socket factory
     */
    private Supplier<SocketFactory> socketFactorySupplier;
    /**
     * 数据协议
     */
    private HeadParser<T> headParser;
    /**
     * 推送消息处理器
     */
    private PacketHandler<T> pushHandler;
    /**
     * 连接初始化
     */
    private Initializer<T> initializer;
    /**
     * 消息分发执行器
     * 连接状态监听回调，请求回调，都执行在Executor所在线程
     */
    private Executor callbackExecutor;

    /**
     * 连接管理线程池
     * 启动连接、关闭连接的执行线程池
     */
    private Executor managerExecutor;

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
     * 连接的任务执行器
     */
    private ScheduledExecutorService scheduledExecutor;

    /**
     * 失败重连尝试次数
     */
    private int retryTimes;

    /**
     * 连接间隔
     */
    private int connectInterval;

    private Options(Builder<T> builder) {
        pulseHandler = builder.pulseHandler;
        address = builder.address;
        backupAddressList = builder.backupAddressList;
        socketFactorySupplier = builder.socketFactorySupplier;
        headParser = builder.headParser;
        pushHandler = builder.pushHandler;
        callbackExecutor = builder.callbackExecutor;
        managerExecutor = builder.managerExecutor;
        codecExecutor = builder.codecExecutor;
        maxReadDataKB = builder.maxReadDataKB;
        requestTimeOut = builder.requestTimeOut;
        connectTimeOut = builder.connectTimeOut;
        pulseRate = builder.pulseRate;
        pulseLostTimes = builder.pulseLostTimes;
        backgroundLiveTime = builder.backgroundLiveTime;
        livePolicy = builder.livePolicy;
        scheduledExecutor = builder.scheduledExecutor;
        retryTimes = builder.retryTimes;
        connectInterval = builder.connectInterval;
        initializer = builder.initializer;
    }

    public static boolean isDebug() {
        return debug;
    }

    public PulseHandler<T> getPulseHandler() {
        return pulseHandler;
    }

    public Initializer<T> getInitializer() {
        return initializer;
    }

    public HeadParser<T> getHeadParser() {
        return headParser;
    }

    public Supplier<SocketFactory> getSocketFactorySupplier() {
        return socketFactorySupplier;
    }

    public Executor getCallbackExecutor() {
        return callbackExecutor;
    }

    public Executor getManagerExecutor() {
        return managerExecutor;
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

    public PacketHandler<T> getPushHandler() {
        return pushHandler;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    public Address getAddress() {
        return address;
    }

    public List<Address> getBackupAddressList() {
        return backupAddressList;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public int getConnectInterval() {
        return connectInterval;
    }

    public static final class Builder<T> {
        private PulseHandler<T> pulseHandler;
        private Address address;
        private List<Address> backupAddressList;
        private Supplier<SocketFactory> socketFactorySupplier;
        private HeadParser<T> headParser;
        private PacketHandler<T> pushHandler;
        private Initializer<T> initializer;
        private Executor callbackExecutor;
        private Executor managerExecutor;
        private Executor codecExecutor;
        private ScheduledExecutorService scheduledExecutor;
        private int maxReadDataKB = 1024;
        private int requestTimeOut = 5000;
        private int connectTimeOut = 5000;
        private int pulseRate = 60 * 1000;
        private int pulseLostTimes = 2;
        private int backgroundLiveTime = 120 * 1000;
        private LivePolicy livePolicy = LivePolicy.DEFAULT;
        private int retryTimes;
        private int connectInterval = 3000;

        public Builder() {
        }

        public Builder<T> pulseHandler(@NonNull PulseHandler<T> val) {
            pulseHandler = val;
            return this;
        }

        public Builder<T> address(@NonNull Address val) {
            address = val;
            return this;
        }

        public Builder<T> backupAddressList(@NonNull List<Address> val) {
            backupAddressList = val;
            return this;
        }

        public Builder<T> headParser(@NonNull HeadParser<T> val) {
            headParser = val;
            return this;
        }

        public Builder<T> pushHandler(@NonNull PacketHandler<T> val) {
            pushHandler = val;
            return this;
        }

        public Builder<T> initializer(@NonNull Initializer<T> val) {
            initializer = val;
            return this;
        }

        public Builder<T> scheduledExecutor(@NonNull ScheduledExecutorService val) {
            scheduledExecutor = val;
            return this;
        }

        public Builder<T> callbackExecutor(@NonNull Executor val) {
            callbackExecutor = val;
            return this;
        }

        public Builder<T> managerExecutor(@NonNull Executor val) {
            managerExecutor = val;
            return this;
        }

        public Builder<T> codecExecutor(@NonNull Executor val) {
            codecExecutor = val;
            return this;
        }

        public Builder<T> maxReadDataKB(int val) {
            maxReadDataKB = val;
            return this;
        }

        public Builder<T> requestTimeOut(int val) {
            requestTimeOut = val;
            return this;
        }

        public Builder<T> connectTimeOut(int val) {
            connectTimeOut = val;
            return this;
        }

        public Builder<T> pulseRate(int val) {
            pulseRate = val;
            return this;
        }

        public Builder<T> pulseLostTimes(int val) {
            pulseLostTimes = val;
            return this;
        }

        public Builder<T> backgroundLiveTime(int val) {
            backgroundLiveTime = val;
            return this;
        }

        public Builder<T> livePolicy(@NonNull LivePolicy val) {
            livePolicy = val;
            return this;
        }

        public Builder<T> retryTimes(int val) {
            retryTimes = val;
            return this;
        }

        public Builder<T> connectInterval(int val) {
            connectInterval = val;
            return this;
        }

        public Builder<T> socketFactorySupplier(@NonNull Supplier<SocketFactory> val) {
            socketFactorySupplier = val;
            return this;
        }

        public Options<T> build() {
            if (checkParams().isEmpty()) {
                return new Options<>(this);
            }
            throw new IllegalArgumentException();
        }

        @NonNull
        private String checkParams() {
            if (address == null) {
                return "Address not set.";
            }
            if (headParser == null) {
                return "Head parser not set.";
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
            if (socketFactorySupplier == null) {
                socketFactorySupplier = SocketFactory::getDefault;
            }
            if (pulseHandler == null) {
                pulseHandler = new PulseHandler.EmptyPulseHandler<>();
            }
            if (pushHandler == null) {
                pushHandler = new PacketHandler.EmptyPacketHandler<>();
            }
            if (initializer == null) {
                initializer = new Initializer.EmptyInitializer<>();
            }
            if (callbackExecutor == null) {
                callbackExecutor = Executors.mainThreadExecutor;
            }
            if (managerExecutor == null) {
                managerExecutor = Executors.managerExecutor;
            }
            if (codecExecutor == null) {
                codecExecutor = Executors.codecExecutor;
            }
            if (scheduledExecutor == null) {
                scheduledExecutor = Executors.scheduledExecutor;
            }

            return "";
        }
    }
}
