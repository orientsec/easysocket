package com.orientsec.easysocket.platform

import com.orientsec.easysocket.session.ByteWriter
import com.orientsec.easysocket.session.CommonQueuedWriter
import com.orientsec.easysocket.session.OperableSession
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Network.nw_connection_send
import platform.Network.nw_connection_t
import platform.Network.nw_content_context_create
import platform.Network.nw_error_t
import platform.darwin.dispatch_data_create

@OptIn(ExperimentalForeignApi::class, InternalCoroutinesApi::class)
class NWWriter(
    session: OperableSession,
    scope: CoroutineScope,
    connection: nw_connection_t,
) : CommonQueuedWriter(session, scope, NWByteWriter(connection)) {

    private class WriteContext(
        val pinned: Pinned<ByteArray>,
        val continuation: CancellableContinuation<Unit>
    )

    private class NWByteWriter(private val connection: nw_connection_t) : ByteWriter {
        override suspend fun write(data: ByteArray) {
            val pinned = data.pin()
            suspendCancellableCoroutine { continuation ->
                val context = WriteContext(pinned, continuation)
                val ref = StableRef.create(context)

                val dispatchData = dispatch_data_create(
                    pinned.addressOf(0),
                    data.size.toULong(),
                    null,
                    null
                )

                // 注意：不要使用 NW_CONNECTION_DEFAULT_MESSAGE_CONTEXT 常量。
                // 该常量底层对象是一个 void 返回的 ObjC block，读取时进入 Kotlin
                // 会触发 K/N blockToKotlinImp 转换并抛出 NSGenericException 导致闪退。
                // 改用 nw_content_context_create 创建等价的发送 context。
                val contentContext = nw_content_context_create("easysocket.message")

                nw_connection_send(
                    connection,
                    dispatchData,
                    contentContext,
                    true
                ) { error: nw_error_t? ->
                    val ctx = ref.get()
                    ref.dispose()

                    ctx.pinned.unpin()
                    if (error != null) {
                        ctx.continuation.tryResumeWithException(Exception("NW write error"))
                            ?.let { ctx.continuation.completeResume(it) }
                    } else {
                        ctx.continuation.tryResume(Unit)
                            ?.let { ctx.continuation.completeResume(it) }
                    }
                }
            }
        }

        override fun close() {
            // Managed by Session
        }
    }
}
