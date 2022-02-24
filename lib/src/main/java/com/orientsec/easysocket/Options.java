package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.request.Decoder;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.utils.Executors;
import com.orientsec.easysocket.utils.LogFactory;
import com.orientsec.easysocket.utils.Logger;

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

public class Options {

    /**
     * 是否是调试模式
     */
    private final boolean debug;

    private final String name;

    private final boolean detailLog;

    /**
     * 心跳解码器
     */
    private final Provider<Decoder<Boolean>> pulseDecoderProvider;
    /**
     * 心跳请求
     */
    private final Provider<Request<Boolean>> pulseRequestProvider;
    /**
     * 站点信息
     */
    private final Provider<List<Address>> addressProvider;
    /**
     * Socket factory
     */
    private final Provider<SocketFactory> socketFactoryProvider;
    /**
     * 数据协议
     */
    private final Provider<HeadParser> headParserProvider;
    /**
     * 推送消息处理器
     */
    private final Provider<PushManager<?, ?>> pushManagerProvider;
    /**
     * 连接初始化
     */
    private final Provider<Initializer> initializerProvider;
    /**
     * 消息分发执行器
     * 连接状态监听回调，请求回调，都执行在Executor所在线程
     */
    private final Executor callbackExecutor;

    /**
     * 连接管理线程池
     * 启动连接、关闭连接的执行线程池
     */
    private final Executor connectExecutor;

    /**
     * 编解码执行器
     */
    private final Executor codecExecutor;
    /**
     * 最大读取数据的K数(KB)<br>
     * 防止服务器返回数据体过大的数据导致前端内存溢出.
     */
    private final int maxReadDataKB;

    /**
     * 请求超时时间 单位秒
     */
    private final int requestTimeOut;

    /**
     * 连接超时时间 单位秒
     */
    private final int connectTimeOut;

    /**
     * 心跳频率 单位秒
     */
    private final int pulseRate;
    /**
     * 心跳失败次数
     */
    private final int pulseLostTimes;

    /**
     * 后台存活时间
     */
    private final int liveTime;

    /**
     * 后台策略
     */
    private final LivePolicy livePolicy;

    /**
     * 失败重连尝试次数
     */
    private final int retryTimes;

    /**
     * 连接间隔
     */
    private final int connectInterval;

    protected final Logger logger;

    private Options(Builder builder) {
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
        liveTime = builder.liveTime;
        livePolicy = builder.livePolicy;
        retryTimes = builder.retryTimes;
        connectInterval = builder.connectInterval;
        addressProvider = builder.addressProvider;
        headParserProvider = builder.headParserProvider;
        initializerProvider = builder.initializerProvider;
        pulseRequestProvider = builder.pulseRequestProvider;
        pulseDecoderProvider = builder.pulseDecoderProvider;
        pushManagerProvider = builder.pushManagerProvider;
        socketFactoryProvider = builder.socketFactoryProvider;
        detailLog = builder.detailLog;
        logger = LogFactory.getLogger(this);
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

    public int getLiveTime() {
        return liveTime;
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

    public Provider<Request<Boolean>> getPulseRequestProvider() {
        return pulseRequestProvider;
    }

    public Provider<Decoder<Boolean>> getPulseDecoderProvider() {
        return pulseDecoderProvider;
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

    public Provider<PushManager<?, ?>> getPushManagerProvider() {
        return pushManagerProvider;
    }

    public Provider<Initializer> getInitializerProvider() {
        return initializerProvider;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isDetailLog() {
        return detailLog;
    }

    public static final class Builder {
        private String name = "";
        private boolean debug;
        private Provider<Request<Boolean>> pulseRequestProvider;
        private Provider<Decoder<Boolean>> pulseDecoderProvider;
        private Provider<List<Address>> addressProvider;
        private Provider<SocketFactory> socketFactoryProvider;
        private Provider<HeadParser> headParserProvider;
        private Provider<PushManager<?, ?>> pushManagerProvider;
        private Provider<Initializer> initializerProvider;
        private Executor callbackExecutor;
        private Executor connectExecutor;
        private Executor codecExecutor;
        private int maxReadDataKB = 1024;
        private int requestTimeOut = 5000;
        private int connectTimeOut = 5000;
        private int pulseRate = 60 * 1000;
        private int pulseLostTimes = 2;
        private int liveTime = 30 * 1000;
        private LivePolicy livePolicy = LivePolicy.DEFAULT;
        private int retryTimes;
        private int connectInterval = 3000;
        private boolean detailLog = true;

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

        public Builder pulseRequestProvider(@NonNull Provider<Request<Boolean>> val) {
            pulseRequestProvider = val;
            return this;
        }

        public Builder pulseDecoderProvider(@NonNull Provider<Decoder<Boolean>> val) {
            pulseDecoderProvider = val;
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

        public Builder pushManagerProvider(@NonNull Provider<PushManager<?, ?>> val) {
            pushManagerProvider = val;
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

        public Builder liveTime(int val) {
            liveTime = val;
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

        public Builder detailLog(boolean val) {
            detailLog = val;
            return this;
        }

        @NonNull
        public Options build() {
            String error = checkParams();
            if (error.isEmpty()) {
                return new Options(this);
            }
            throw new IllegalArgumentException(error);
        }

        @NonNull
        public SocketClient open() {
            return EasySocket.getInstance().open(build());
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
            if (liveTime < 0) {
                return "Live time must be positive.";
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
            if (pulseRequestProvider == null) {
                pulseRequestProvider = new DefaultPulseRequestProvider();
            }
            if (pulseDecoderProvider == null) {
                pulseDecoderProvider = new DefaultPulseDecoderProvider();
            }
            if (pushManagerProvider == null) {
                pushManagerProvider = new DefaultPushManagerProvider();
            }
            if (initializerProvider == null) {
                initializerProvider = new DefaultInitializerProvider();
            }
            if (callbackExecutor == null) {
                callbackExecutor = Executors.defaultMainExecutor();
            }
            if (connectExecutor == null) {
                connectExecutor = Executors.defaultConnectExecutor();
            }
            if (codecExecutor == null) {
                codecExecutor = Executors.defaultCodecExecutor();
            }

            return "";
        }
    }
}
