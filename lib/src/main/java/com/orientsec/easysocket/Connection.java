package com.orientsec.easysocket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/27 14:03
 * Author: Fredric
 * coding is art not science
 */
public interface Connection {
    /**
     * 启动连接, 如果连接以经启动，无效果。
     */
    void start();

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
     * 创建一个请求任务
     *
     * @param request 发往服务端的请求
     * @return 可执行任务
     */
    <T, R> Task<R> buildTask(Request<T, R> request);

    /**
     * 添加连接事件监听器
     *
     * @param listener 监听器
     */
    void addConnectEventListener(ConnectEventListener listener);

    /**
     * 移除连接事件监听器
     *
     * @param listener 监听器
     */
    void removeConnectEventListener(ConnectEventListener listener);

}
