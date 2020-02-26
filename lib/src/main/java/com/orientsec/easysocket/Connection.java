package com.orientsec.easysocket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/27 14:03
 * Author: Fredric
 * coding is art not science
 */
public interface Connection<T> {
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
     * 连接是否可达
     *
     * @return 是否可达
     */
    boolean isAvailable();

    /**
     * 创建一个请求任务
     *
     * @param request 发往服务端的请求
     * @return 可执行任务
     */
    <REQUEST, RESPONSE> Task<RESPONSE> buildTask(Request<T, REQUEST, RESPONSE> request);

    /**
     * 创建一个请求任务
     *
     * @param request    请求
     * @param sync       是否同步请求。同步请求即半双工模式，一条请求，一条应答。
     * @param <REQUEST>  请求类型
     * @param <RESPONSE> 响应类型
     * @return 请求任务
     */
    <REQUEST, RESPONSE> Task<RESPONSE>
    buildTask(Request<T, REQUEST, RESPONSE> request, boolean sync);

    /**
     * 创建一个请求任务
     *
     * @param request    请求
     * @param init       是否是初始化请求，只有初始化请求在连接成功之后的初始化过程中
     *                   可以发送。其余请求在等待队列中，初始化成功之后，统一发出。
     * @param sync       是否同步请求，同步请求即半双工模式，一条请求，一条应答。
     * @param <REQUEST>  请求类型
     * @param <RESPONSE> 响应类型
     * @return 请求任务
     */
    <REQUEST, RESPONSE> Task<RESPONSE>
    buildTask(Request<T, REQUEST, RESPONSE> request, boolean init, boolean sync);

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

    /**
     * 获取当前连接站点信息
     *
     * @return 当前连接站点信息
     */
    ConnectionInfo getConnectionInfo();

}
