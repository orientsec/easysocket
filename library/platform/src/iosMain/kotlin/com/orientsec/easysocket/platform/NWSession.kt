package com.orientsec.easysocket.platform

import com.orientsec.easysocket.Address
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.session.AbstractSession
import com.orientsec.easysocket.session.Reader
import com.orientsec.easysocket.session.Writer
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.Network.*
import platform.darwin.dispatch_queue_t
import platform.posix.NI_MAXHOST
import platform.posix.NI_NUMERICHOST
import platform.posix.getnameinfo
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalForeignApi::class, InternalCoroutinesApi::class)
class NWSession(
    socketClient: BaseSocketClient,
    address: Address,
    addressIndex: Int,
    id: Long,
    private val queue: dispatch_queue_t
) : AbstractSession(socketClient, address, addressIndex, id) {

    @Suppress("REDUNDANT_NULLABLE")
    private var connection: nw_connection_t? = null

    override suspend fun performConnect(): Boolean {
        return try {
            withTimeout(options.connectTimeoutMills.toLong().milliseconds) {
                doConnect()
            }
        } catch (e: Exception) {
            if ((e is CancellationException) && (e !is TimeoutCancellationException)) {
                throw e
            }
            logger.e("NW connect timeout or failed", e)
            val errorCode = if (e is TimeoutCancellationException) {
                ErrorCode.SOCKET_CONNECT_TIMEOUT
            } else {
                ErrorCode.SOCKET_CONNECT
            }

            socketClient.scope.launch {
                onFailed(
                    EasyException(
                        errorCode,
                        ErrorType.CONNECT,
                        "NW connect failed: ${e.message}",
                        suffix,
                        e
                    )
                )
            }
            closeSocket()
            false
        }
    }

    private suspend fun doConnect(): Boolean {
        val endpoint = nw_endpoint_create_host(
            address.host,
            address.port.toString()
        )
        if (endpoint == null) {
            socketClient.scope.launch {
                onFailed(
                    EasyException(
                        ErrorCode.DNS_ANALYZE,
                        ErrorType.CONNECT,
                        "Invalid host: ${address.host}",
                        suffix
                    )
                )
            }
            return false
        }
        val parameters = createParameters()
        val conn = nw_connection_create(endpoint, parameters)
        if (conn == null) {
            socketClient.scope.launch {
                onFailed(
                    EasyException(
                        ErrorCode.SOCKET_CREATE,
                        ErrorType.CONNECT,
                        "Failed to create connection",
                        suffix
                    )
                )
            }
            return false
        }
        this.connection = conn

        return setupConnectionHandlers(conn)
    }

    private fun createParameters(): nw_parameters_t {
        val tcpOptions = nw_tcp_create_options()
        nw_tcp_options_set_no_delay(tcpOptions, no_delay = true)

        return if (address.isSsl) {
            nw_parameters_create_secure_tcp(
                NW_PARAMETERS_DEFAULT_CONFIGURATION,
                NW_PARAMETERS_DEFAULT_CONFIGURATION
            ).also { params ->
                val stack = nw_parameters_copy_default_protocol_stack(params)
                nw_protocol_stack_set_transport_protocol(stack, tcpOptions)
            }
        } else {
            // Create pure TCP without TLS
            nw_parameters_create().also { params ->
                val stack = nw_parameters_copy_default_protocol_stack(params)
                nw_protocol_stack_set_transport_protocol(stack, tcpOptions)
            }
        }
    }

    private suspend fun setupConnectionHandlers(conn: nw_connection_t): Boolean {
        return suspendCancellableCoroutine { continuation ->
            nw_connection_set_queue(conn, queue)

            nw_connection_set_state_changed_handler(conn)
            { state: nw_connection_state_t, error: nw_error_t ->
                handleStateChange(state, error, continuation)
            }

            nw_connection_start(conn)

            continuation.invokeOnCancellation {
                nw_connection_cancel(conn)
            }
        }
    }

    private fun handleStateChange(
        state: nw_connection_state_t,
        error: nw_error_t,
        continuation: CancellableContinuation<Boolean>
    ) {
        when (state) {
            nw_connection_state_ready -> {
                val token = continuation.tryResume(true)
                if (token != null) {
                    continuation.completeResume(token)
                }
            }

            nw_connection_state_failed -> {
                val nwErrorCode = error?.let { nw_error_get_error_code(it) } ?: 0
                val nwDomain = error?.let { nw_error_get_error_domain(it) } ?: "unknown"
                logger.e("NW connection failed: $nwErrorCode (domain: $nwDomain)")

                val token = continuation.tryResume(false)
                if (token != null) {
                    socketClient.scope.launch {
                        onFailed(
                            EasyException(
                                ErrorCode.SOCKET_CONNECT,
                                ErrorType.CONNECT,
                                "NW connect failed: $nwErrorCode",
                                suffix
                            )
                        )
                    }
                    continuation.completeResume(token)
                } else {
                    // If already ready but failed later, trigger a full session close
                    socketClient.scope.launch {
                        val ex = EasyException(
                            ErrorCode.SOCKET_CONNECT,
                            ErrorType.CONNECT,
                            "NW connection lost: $nwErrorCode",
                            suffix
                        )
                        this@NWSession.close(ex)
                    }
                }
            }

            nw_connection_state_cancelled -> {
                val token = continuation.tryResume(false)
                if (token != null) {
                    continuation.completeResume(token)
                }
            }

            nw_connection_state_waiting -> {
                logger.d("NW connection waiting...")
            }

            else -> {}
        }
    }

    override fun createReader(): Reader {
        val conn = connection ?: throw IllegalStateException("Connection is null")
        return NWReader(this, socketClient, conn)
    }

    override fun createWriter(): Writer {
        val conn = connection ?: throw IllegalStateException("Connection is null")
        return NWWriter(this, socketClient.scope, conn)
    }

    override fun closeSocket() {
        val conn = connection
        connection = null
        conn?.let { nw_connection_cancel(it) }
    }

    override val ipAddress: String?
        get() = connection?.let { conn ->
            val path = nw_connection_copy_current_path(conn) ?: return null
            val endpoint = nw_path_copy_effective_remote_endpoint(path) ?: return null
            val address = nw_endpoint_get_address(endpoint) ?: return null

            memScoped {
                val host = allocArray<ByteVar>(NI_MAXHOST)
                val status = getnameinfo(
                    address,
                    address.pointed.sa_len.toUInt(),
                    host,
                    NI_MAXHOST.toUInt(),
                    null,
                    0u,
                    NI_NUMERICHOST
                )
                if (status == 0) {
                    host.toKString()
                } else null
            }
        }
}
