package com.orientsec.easysocket;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.task.TaskFactory;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/27 14:03
 * Author: Fredric
 * coding is art not science
 */
public interface Connection extends TaskFactory {
    /**
     * 启动连接, 如果连接以经启动，无效果。
     */
    void start();

    void stop(EasyException e);

    /**
     * 关闭连接, 连接关闭之后不再可用
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
     * 添加连接事件监听器
     *
     * @param listener 监听器
     */
    void addConnectEventListener(@NonNull ConnectEventListener listener);

    /**
     * 移除连接事件监听器
     *
     * @param listener 监听器
     */
    void removeConnectEventListener(@NonNull ConnectEventListener listener);

    /**
     * 获取当前连接站点信息
     *
     * @return 当前连接站点信息
     */
    @Nullable
    Address getAddress();

}
