package com.orientsec.easysocket.client

import com.orientsec.easysocket.Address

/**
 * 客户端初始化器接口，用于在连接前动态获取服务器地址列表。
 *
 * 当服务器地址不固定或需要通过接口动态获取时，可实现此接口。
 * 在 [com.orientsec.easysocket.Options.Builder.addressList] 未设置的情况下，
 * 库会通过此初始化器获取可用地址。
 *
 * 使用示例：
 * ```kotlin
 * class MyClientInitializer : ClientInitializer {
 *     override suspend fun getAddressList(): Result<List<Address>> {
 *         return try {
 *             val addresses = api.getServerAddresses()
 *             Result.success(addresses)
 *         } catch (e: Exception) {
 *             Result.failure(e)
 *         }
 *     }
 * }
 * ```
 */
interface ClientInitializer {

    /**
     * 获取服务器地址列表。
     * 此方法在协程中调用，可以执行网络请求等耗时操作。
     *
     * @return 包含地址列表的 [Result]，成功时返回非空列表，失败时返回异常
     */
    suspend fun getAddressList(): Result<List<Address>>

}