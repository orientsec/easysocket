package com.orientsec.easysocket.client

import com.orientsec.easysocket.*
import com.orientsec.easysocket.push.PushManager
import com.orientsec.easysocket.request.Decoder
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.SessionInitializer
import com.orientsec.easysocket.task.TaskManager
import com.orientsec.easysocket.utils.LogFactory
import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.CoroutineScope

/**
 * BaseSocketClient is an abstract class that provides the base implementation for a socket client.
 * It defines the core structure and behavior for managing socket connections, tasks, and listeners.
 */
abstract class BaseSocketClient(
    override val options: Options, // Configuration options for the socket client.
    override val scope: CoroutineScope // Coroutine scope for running client operations.
) : SocketClient, ConnectionListener {

    val suffix: String = "  Client[${options.name}]" // Suffix used for logging and identification.

    /**
     * Returns the current session associated with the client.
     * This method must be implemented by subclasses.
     *
     * @return The current session or null if no session exists.
     */
    abstract override val session: OperableSession?

    override val logger: Logger = LogFactory.getLogger(options, suffix)

    override val pushManager: PushManager<*, *>? by lazy {
        options.pushManagerProvider?.invoke(this)
    }

    val headParser: HeadParser by lazy {
        options.headParserProvider.invoke(this)
    }

    val clientInitializer: ClientInitializer? by lazy {
        options.clientInitializerProvider?.invoke(this)
    }

    val sessionInitializer: SessionInitializer? by lazy {
        options.sessionInitializerProvider?.invoke(this)
    }

    val pulseDecoder: Decoder<Boolean>? by lazy {
        options.pulseDecoderProvider?.invoke(this)
    }

    val pulseRequest: Request<Boolean>? by lazy {
        options.pulseRequestProvider?.invoke(this)
    }

    /**
     * Handles network availability events. This method must be implemented by subclasses.
     * It is called on the main thread.
     */
    abstract override fun onNetworkAvailable()

    /**
     * Returns the task manager associated with the client.
     *
     * @return The task manager.
     */
    abstract fun getTaskManager(): TaskManager
}
