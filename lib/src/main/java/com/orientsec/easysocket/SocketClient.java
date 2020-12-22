package com.orientsec.easysocket;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.task.TaskFactory;

import java.net.InetAddress;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/27 14:03
 * Author: Fredric
 * coding is art not science
 */
public interface SocketClient extends TaskFactory {
    /**
     * 启动连接, 如果连接已经启动，无效果。
     */
    void start();

    /**
     * 停止当前连接。
     */
    void stop();

    /**
     * 关闭连接, 连接关闭之后不再可用。
     */
    void shutdown();

    /**
     * 连接是否关闭
     *
     * @return 连接是否关闭
     */
    boolean isShutdown();

    /**
     * 是否连接
     *
     * @return 是否连接
     */
    boolean isConnect();

    /**
     * 连接是否可达
     *
     * @return 是否可达
     */
    boolean isAvailable();

    /**
     * 获取当前连接站点信息
     *
     * @return 当前连接站点信息
     */
    @Nullable
    Address getAddress();

    @Nullable
    InetAddress getInetAddress();

    /**
     * 添加连接事件监听器
     *
     * @param listener 监听器
     */
    void addConnectListener(@NonNull ConnectListener listener);

    /**
     * 移除连接事件监听器
     *
     * @param listener 监听器
     */
    void removeConnectListener(@NonNull ConnectListener listener);

    /**
     * 获取推送管理器
     *
     * @return 推送管理器。
     */
    @NonNull
    PushManager<?, ?> getPushManager();

    /**
     * 获取当前连接所属的EasySocket。
     *
     * @return EasySocket。
     */
    @NonNull
    Options getOptions();
}
