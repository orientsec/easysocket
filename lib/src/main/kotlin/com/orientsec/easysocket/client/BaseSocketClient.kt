package com.orientsec.easysocket.client

import androidx.annotation.MainThread
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
import javax.net.SocketFactory

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
        options.pushManagerProvider?.get(this)
    }

    val headParser: HeadParser by lazy {
        options.headParserProvider.get(this)
    }

    val clientInitializer: ClientInitializer? by lazy {
        options.clientInitializerProvider?.get(this)
    }

    val sessionInitializer: SessionInitializer? by lazy {
        options.sessionInitializerProvider?.get(this)
    }

    val socketFactory: SocketFactory by lazy {
        options.socketFactoryProvider.get(this)
    }

    val pulseDecoder: Decoder<Boolean>? by lazy {
        options.pulseDecoderProvider?.get(this)
    }

    val pulseRequest: Request<Boolean>? by lazy {
        options.pulseRequestProvider?.get(this)
    }

    /**
     * Handles network availability events. This method must be implemented by subclasses.
     * It is called on the main thread.
     */
    @MainThread
    abstract override fun onNetworkAvailable()

    /**
     * Returns the task manager associated with the client.
     *
     * @return The task manager.
     */
    abstract fun getTaskManager(): TaskManager


}
