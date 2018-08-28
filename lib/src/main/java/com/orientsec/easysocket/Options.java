package com.orientsec.easysocket;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.orientsec.easysocket.utils.Logger;

import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
    /**
     * 站点信息
     */
    private ConnectionInfo connectionInfo;
    /**
     * 备用站点信息
     */
    private List<ConnectionInfo> backupConnectionInfoList;
    /**
     * 写入Socket管道中给服务器的字节序
     */
    private ByteOrder writeOrder;
    /**
     * 从Socket管道中读取字节序时的字节序
     */
    private ByteOrder readByteOrder;
    /**
     * 数据协议
     */
    private Protocol<T> protocol;
    /**
     * 推送消息处理器
     */
    private PushHandler<T> pushHandler;
    /**
     * 消息分发执行器
     * 推送消息，连接状态监听回调，请求回调，都执行在Executor所在线程
     */
    private Executor dispatchExecutor;

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
    private ScheduledExecutorService executorService;

    /**
     * 失败重连尝试次数
     */
    private int retryTimes;

    private Options(Builder<T> builder) {
        connectionInfo = builder.connectionInfo;
        backupConnectionInfoList = builder.backupConnectionInfoList;
        writeOrder = builder.writeOrder;
        readByteOrder = builder.readByteOrder;
        protocol = builder.protocol;
        pushHandler = builder.pushHandler;
        dispatchExecutor = builder.dispatchExecutor;
        maxReadDataKB = builder.maxReadDataKB;
        requestTimeOut = builder.requestTimeOut;
        connectTimeOut = builder.connectTimeOut;
        pulseRate = builder.pulseRate;
        pulseLostTimes = builder.pulseLostTimes;
        backgroundLiveTime = builder.backgroundLiveTime;
        livePolicy = builder.livePolicy;
        executorService = builder.executorService;
        retryTimes = builder.retryTimes;
    }

    public static boolean isDebug() {
        return debug;
    }

    public Protocol<T> getProtocol() {
        return protocol;
    }

    public ByteOrder getWriteOrder() {
        return writeOrder;
    }

    public ByteOrder getReadByteOrder() {
        return readByteOrder;
    }

    public Executor getDispatchExecutor() {
        return dispatchExecutor;
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

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    private static class DefaultPushHandler<T> implements PushHandler<T> {
        @Override
        public void onPush(T message) {
            Logger.i("unhandled push event");
        }
    }

    private static class MainThreadExecutor implements Executor {
        private Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            handler.post(command);
        }
    }

    private static class ExecutorHolder {
        private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    }


    public static final class Builder<T> {
        private ConnectionInfo connectionInfo;
        private List<ConnectionInfo> backupConnectionInfoList;
        private ByteOrder writeOrder = ByteOrder.BIG_ENDIAN;
        private ByteOrder readByteOrder = ByteOrder.BIG_ENDIAN;
        private Protocol<T> protocol;
        private PushHandler<T> pushHandler;
        private Executor dispatchExecutor;
        private int maxReadDataKB = 1024;
        private int requestTimeOut = 5;
        private int connectTimeOut = 5000;
        private int pulseRate = 60;
        private int pulseLostTimes = 2;
        private int backgroundLiveTime = 120;
        private LivePolicy livePolicy = LivePolicy.DEFAULT;
        private ScheduledExecutorService executorService;
        private int retryTimes = 3;

        public Builder() {
        }

        public Builder<T> connectionInfo(ConnectionInfo val) {
            connectionInfo = val;
            return this;
        }

        public Builder<T> backupConnectionInfoList(List<ConnectionInfo> val) {
            backupConnectionInfoList = val;
            return this;
        }

        public Builder<T> writeOrder(@NonNull ByteOrder val) {
            writeOrder = val;
            return this;
        }

        public Builder<T> readByteOrder(@NonNull ByteOrder val) {
            readByteOrder = val;
            return this;
        }

        public Builder<T> protocol(Protocol<T> val) {
            protocol = val;
            return this;
        }

        public Builder<T> pushHandler(PushHandler<T> val) {
            pushHandler = val;
            return this;
        }

        public Builder<T> executorService(ScheduledExecutorService val) {
            executorService = val;
            return this;
        }

        public Builder<T> dispatchExecutor(Executor val) {
            dispatchExecutor = val;
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
            if (protocol == null) {
                return false;
            }
            if (dispatchExecutor == null) {
                dispatchExecutor = new MainThreadExecutor();
            }
            if (maxReadDataKB < 100 || maxReadDataKB > 1024) {
                maxReadDataKB = 1024;
            }
            if (connectTimeOut < 500) {
                connectTimeOut = 5000;
            }
            if (requestTimeOut <= 0) {
                requestTimeOut = 5;
            }
            if (pulseRate < 30 || pulseRate > 300) {
                pulseRate = 60;
            }
            if (pulseLostTimes < 0) {
                return false;
            }
            if (backgroundLiveTime < 30) {
                backgroundLiveTime = 120;
            }
            if (pushHandler == null) {
                pushHandler = new DefaultPushHandler<>();
            }
            if (executorService == null) {
                executorService = ExecutorHolder.executorService;
            }
            if (retryTimes < 0) {
                retryTimes = 3;
            }
            return true;
        }
    }
}
