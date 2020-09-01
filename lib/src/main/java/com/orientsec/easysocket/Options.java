package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.utils.Executors;

import java.util.List;
import java.util.concurrent.Executor;

import javax.net.SocketFactory;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/27 13:33
 * Author: Fredric
 * coding is art not science
 * <p>
 * 连接配置类
 */
public class Options {
    /**
     * 是否是调试模式
     */
    public static boolean debug;

    private String id;

    private PulseHandler pulseHandler;
    /**
     * 站点信息
     */
    private Supplier<List<Address>> addressSupplier;
    /**
     * Socket factory
     */
    private Supplier<SocketFactory> socketFactorySupplier;
    /**
     * 数据协议
     */
    private HeadParser headParser;
    /**
     * 推送消息处理器
     */
    private PacketHandler pushHandler;
    /**
     * 连接初始化
     */
    private Initializer initializer;
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

    private Options(Builder builder) {
        id = builder.id;
        pulseHandler = builder.pulseHandler;
        addressSupplier = builder.addressSupplier;
        socketFactorySupplier = builder.socketFactorySupplier;
        headParser = builder.headParser;
        pushHandler = builder.pushHandler;
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
        initializer = builder.initializer;
    }

    public static boolean isDebug() {
        return debug;
    }

    public String getId() {
        return id;
    }

    public PulseHandler getPulseHandler() {
        return pulseHandler;
    }

    public Initializer getInitializer() {
        return initializer;
    }

    public HeadParser getHeadParser() {
        return headParser;
    }

    public Supplier<SocketFactory> getSocketFactorySupplier() {
        return socketFactorySupplier;
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

    public PacketHandler getPushHandler() {
        return pushHandler;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    public Supplier<List<Address>> getAddressSupplier() {
        return addressSupplier;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public int getConnectInterval() {
        return connectInterval;
    }

    public static final class Builder {
        private String id = "";
        private PulseHandler pulseHandler;
        private Supplier<List<Address>> addressSupplier;
        private Supplier<SocketFactory> socketFactorySupplier;
        private HeadParser headParser;
        private PacketHandler pushHandler;
        private Initializer initializer;
        private Executor callbackExecutor;
        private Executor connectExecutor;
        private Executor codecExecutor;
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

        public Builder id(@NonNull String val) {
            id = val;
            return this;
        }

        public Builder pulseHandler(@NonNull PulseHandler val) {
            pulseHandler = val;
            return this;
        }

        public Builder addressList(@NonNull List<Address> val) {
            addressSupplier = StaticAddressSupplier.build(val);
            return this;
        }

        public Builder addressSupplier(@NonNull Supplier<List<Address>> val) {
            addressSupplier = val;
            return this;
        }

        public Builder headParser(@NonNull HeadParser val) {
            headParser = val;
            return this;
        }

        public Builder pushHandler(@NonNull PacketHandler val) {
            pushHandler = val;
            return this;
        }

        public Builder initializer(@NonNull Initializer val) {
            initializer = val;
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

        public Builder socketFactorySupplier(@NonNull Supplier<SocketFactory> val) {
            socketFactorySupplier = val;
            return this;
        }

        public Options build() {
            if (checkParams().isEmpty()) {
                return new Options(this);
            }
            throw new IllegalArgumentException();
        }

        @NonNull
        private String checkParams() {
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
                pulseHandler = new EmptyPulseHandler();
            }
            if (pushHandler == null) {
                pushHandler = new EmptyPacketHandler();
            }
            if (initializer == null) {
                initializer = new EmptyInitializer();
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
