package com.orientsec.easysocket;

import com.orientsec.easysocket.utils.Logger;

import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.Executor;

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
    private static boolean debug;
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
    private Protocol protocol;
    /**
     * 推送消息处理器
     */
    private PushHandler pushHandler;
    /**
     * 网络状态监测器
     */
    private NetworkDetector networkDetector;
    /**
     * 消息分发执行器
     * 推送消息，连接状态监听回调，请求回调，都执行在Executor所在线程
     */
    private Executor dispatchExecutor;

    /**
     * 最大读取数据的兆数(MB)<br>
     * 防止服务器返回数据体过大的数据导致前端内存溢出.
     */
    private int maxReadDataMB;

    /**
     * 从服务器读取时单次读取的缓存字节长度,数值越大,读取效率越高.但是相应的系统消耗将越大
     */
    private int singleReadBufferSize;

    private int singleWriteBufferSize;

    private int requestTimeOut;

    private int connectTimeOut = 5000;

    /**
     * 心跳频率 单位秒
     */
    private int pulseRate = 60;

    private int pulseLostTimes = 3;

    /**
     * 后台存活时间
     */
    private int backgroundLiveTime = 120;

    private LivePolicy livePolicy;

    public static boolean isDebug() {
        return debug;
    }

    public NetworkDetector getNetworkDetector() {
        return networkDetector;
    }

    public Protocol getProtocol() {
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

    public int getMaxReadDataMB() {
        return maxReadDataMB;
    }

    public int getSingleReadBufferSize() {
        return singleReadBufferSize;
    }

    public int getSingleWriteBufferSize() {
        return singleWriteBufferSize;
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

    public PushHandler getPushHandler() {
        return pushHandler;
    }

    public static Options defaultOptions() {
        return new Options();
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

    private static class DefaultPushHandler implements PushHandler {
        @Override
        public void onPush(int id, Message message) {
            Logger.i("unhandled push event, cmd:" + id);
        }
    }
}
