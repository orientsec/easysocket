package com.orientsec.easysocket.platform

import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.session.ByteReader
import com.orientsec.easysocket.session.CommonReader
import com.orientsec.easysocket.session.OperableSession
import kotlinx.cinterop.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Network.nw_connection_receive
import platform.Network.nw_connection_t
import platform.Network.nw_content_context_t
import platform.Network.nw_error_t
import platform.darwin.dispatch_data_apply
import platform.darwin.dispatch_data_get_size
import platform.darwin.dispatch_data_t
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NWReader(
    session: OperableSession,
    client: BaseSocketClient,
    connection: nw_connection_t
) : CommonReader(session, client, NWByteReader(connection))

@OptIn(ExperimentalForeignApi::class)
private class NWByteReader(private val connection: nw_connection_t) : ByteReader {

    private class ReadContext(
        val continuation: CancellableContinuation<Pair<dispatch_data_t?, Boolean>>
    )

    override suspend fun readFully(data: ByteArray) {
        val length = data.size.toUInt()
        var totalRead = 0u

        data.usePinned { pinnedData ->
            while (totalRead < length) {
                val (content, isComplete) = readInto(length - totalRead, length - totalRead)

                content?.let { dispatchData ->
                    val size = dispatch_data_get_size(dispatchData).toInt()
                    dispatch_data_apply(dispatchData) { _, offset, buffer, bufferSize ->
                        val target = pinnedData.addressOf(totalRead.toInt() + offset.toInt())
                        memcpy(target, buffer, bufferSize)
                        true
                    }
                    totalRead += size.toUInt()
                }

                if (isComplete && totalRead < length) {
                    throw Exception("Connection closed before reading fully")
                } else if (isComplete) {
                    break
                }
            }
        }
    }

    private suspend fun readInto(
        min: UInt,
        max: UInt
    ): Pair<dispatch_data_t?, Boolean> = suspendCancellableCoroutine { continuation ->
        val context = ReadContext(continuation)
        val ref = StableRef.create(context)

        nw_connection_receive(
            connection,
            min,
            max
        ) { content: dispatch_data_t?, _: nw_content_context_t?, isComplete: Boolean, error: nw_error_t? ->
            val ctx = ref.get()
            ref.dispose()

            if (error != null) {
                ctx.continuation.resumeWithException(Exception("NW read error"))
            } else {
                ctx.continuation.resume(Pair(content, isComplete))
            }
        }
    }

    override fun close() {
        // Managed by Session
    }
}
