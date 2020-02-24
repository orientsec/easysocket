package com.orientsec.easysocket;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

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
    private ConnectionInfo connectionInfo;
    /**
     * 备用站点信息
     */
    private List<ConnectionInfo> backupConnectionInfoList;
    /**
     * Socket factory
     */
    private SocketFactory socketFactory;
    /**
     * 数据协议
     */
    private HeadParser<T> headParser;
    /**
     * 推送消息处理器
     */
    private PushHandler<T> pushHandler;
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
        connectionInfo = builder.connectionInfo;
        backupConnectionInfoList = builder.backupConnectionInfoList;
        socketFactory = builder.socketFactory;
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
    }

    public static boolean isDebug() {
        return debug;
    }

    public PulseHandler<T> getPulseHandler() {
        return pulseHandler;
    }

    public HeadParser<T> getHeadParser() {
        return headParser;
    }

    public SocketFactory getSocketFactory() {
        return socketFactory;
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

    public PushHandler<T> getPushHandler() {
        return pushHandler;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public List<ConnectionInfo> getBackupConnectionInfoList() {
        return backupConnectionInfoList;
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
        private ConnectionInfo connectionInfo;
        private List<ConnectionInfo> backupConnectionInfoList;
        private SocketFactory socketFactory;
        private HeadParser<T> headParser;
        private PushHandler<T> pushHandler;
        private Executor callbackExecutor;
        private Executor managerExecutor;
        private Executor codecExecutor;
        private ScheduledExecutorService scheduledExecutor;
        private int maxReadDataKB = 1024;
        private int requestTimeOut = 5;
        private int connectTimeOut = 5000;
        private int pulseRate = 60;
        private int pulseLostTimes = 2;
        private int backgroundLiveTime = 120;
        private LivePolicy livePolicy = LivePolicy.DEFAULT;
        private int retryTimes;
        private int connectInterval = 3000;

        public Builder() {
        }

        public Builder<T> pulseHandler(PulseHandler<T> val) {
            pulseHandler = val;
            return this;
        }

        public Builder<T> connectionInfo(ConnectionInfo val) {
            connectionInfo = val;
            return this;
        }

        public Builder<T> backupConnectionInfoList(List<ConnectionInfo> val) {
            backupConnectionInfoList = val;
            return this;
        }

        public Builder<T> protocol(HeadParser<T> val) {
            headParser = val;
            return this;
        }

        public Builder<T> pushHandler(PushHandler<T> val) {
            pushHandler = val;
            return this;
        }

        public Builder<T> scheduledExecutor(ScheduledExecutorService val) {
            scheduledExecutor = val;
            return this;
        }

        public Builder<T> callbackExecutor(Executor val) {
            callbackExecutor = val;
            return this;
        }

        public Builder<T> managerExecutor(Executor val) {
            managerExecutor = val;
            return this;
        }

        public Builder<T> codecExecutor(Executor val) {
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

        public Builder<T> socketFactory(SocketFactory val) {
            socketFactory = val;
            return this;
        }

        public Options<T> build() {
            if (!checkParams()) {
                throw new IllegalArgumentException();
            }
            return new Options<>(this);
        }

        private boolean checkParams() {
            if (connectionInfo == null) {
                return false;
            }
            if (headParser == null) {
                return false;
            }
            if (maxReadDataKB <= 0) {
                return false;
            }
            if (connectTimeOut < 0) {
                return false;
            }
            if (requestTimeOut <= 0) {
                return false;
            }
            if (pulseRate < 30) {
                return false;
            }
            if (pulseLostTimes < 0) {
                return false;
            }
            if (backgroundLiveTime <= 30) {
                return false;
            }
            if (retryTimes < 0) {
                return false;
            }
            if (connectInterval <= 1000) {
                return false;
            }
            if (socketFactory == null) {
                socketFactory = new DefaultSocketFactory();
            }
            if (pushHandler == null) {
                pushHandler = new EasySocket.DefaultPushHandler<>();
            }
            if (callbackExecutor == null) {
                callbackExecutor = new EasySocket.MainThreadExecutor();
            }
            if (managerExecutor == null) {
                managerExecutor = EasySocket.Executor.managerExecutor;
            }
            if (codecExecutor == null) {
                codecExecutor = EasySocket.Executor.codecExecutor;
            }
            if (scheduledExecutor == null) {
                scheduledExecutor = EasySocket.Executor.scheduledExecutor;
            }

            return true;
        }
    }
}
