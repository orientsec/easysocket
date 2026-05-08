package com.orientsec.easysocket.session

import com.orientsec.easysocket.Options
import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.PacketHandler
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.task.*
import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the heartbeat mechanism for maintaining the connection.
 * This class implements `PacketHandler` and `TaskBuilder` interfaces.
 */
class Pulse(
    private val socketClient: BaseSocketClient,
    private val session: OperableSession
) : PacketHandler, TaskBuilder {
    // The maximum number of consecutive heartbeat failures allowed
    // before the session is considered invalid.
    private val maxLostTimes: Int

    // The interval in milliseconds between consecutive heartbeat messages.
    private val intervalMillis: Long

    // Counter for tracking the number of consecutive heartbeat failures
    private var lostTimes = 0

    val scope: CoroutineScope = socketClient.scope

    val options: Options = socketClient.options

    // Logger instance for logging messages
    private val logger: Logger

    // Job for managing the heartbeat coroutine
    private var pulseJob: Job? = null

    init {
        this.intervalMillis = options.pulseIntervalSeconds * 1000L
        this.maxLostTimes = options.pulseMaxLostTimes
        logger = session.logger
    }

    /**
     * Starts the heartbeat mechanism.
     * This method is called after a successful connection is established.
     */
    fun start() {
        stop() // Ensure any existing job is cancelled
        pulseJob = scope.launch {
            while (isActive) {
                delay(intervalMillis)
                runPulse()
            }
        }
    }

    /**
     * Stops the heartbeat mechanism.
     * This method is called when the connection is disconnected.
     */
    fun stop() {
        pulseJob?.cancel()
        pulseJob = null
    }

    /**
     * Sends a heartbeat message.
     * If the number of consecutive heartbeat failures exceeds the allowed limit,
     * the session is closed.
     */
    private fun runPulse() {
        val currentLostTimes = lostTimes++

        if (currentLostTimes > maxLostTimes) {
            // Close the session if the heartbeat failure count exceeds the limit
            logger.i("pulse failed times up, session invalid")
            val e = EasyException(
                ErrorCode.PULSE_TIME_OUT, ErrorType.CONNECT,
                "pulse time out", session.suffix
            )
            session.close(e)
        } else {
            val pulseRequest = socketClient.pulseRequest
            if (pulseRequest == null) {
                logger.w("no pulse request")
            } else {
                buildTask(pulseRequest, callback).execute()
            }
        }
    }

    /**
     * Builds a task for sending a heartbeat request.
     *
     * @param request  The heartbeat request to be sent.
     * @param callback The callback to handle the response of the heartbeat request.
     * @param T      The type of the response object.
     * @return A task for executing the heartbeat request.
     */
    override fun <T> buildTask(request: Request<T>, callback: Callback<T>): Task<T> {
        return TaskImpl(
            TaskType.PULSE, socketClient.getTaskManager().generateTaskId(),
            request, callback, socketClient, session
        )
    }

    // Callback for handling the response of the heartbeat request
    private val callback: Callback<Boolean> = object : DefaultCallback<Boolean>() {
        override fun onSuccess(res: Boolean) {
            logger.i("client pulse result: $res")
            if (res) {
                // Resets the heartbeat failure counter to zero.
                scope.launch { lostTimes = 0 }
            }
        }

        override fun onFailure(t: Throwable) {
            logger.i("client pulse failed ", t)
        }
    }

    /**
     * Handles the received packet and decodes the heartbeat response.
     *
     * @param packet The packet received from the server.
     */
    override fun handlePacket(packet: Packet) {
        val pulseDecoder = socketClient.pulseDecoder ?: return
        scope.launch {
            try {
                val success = withContext(options.codecDispatcher) {
                    pulseDecoder.decode(packet)
                }
                logger.i("server pulse result: $success")
                if (success) lostTimes = 0
            } catch (t: Throwable) {
                logger.i("server pulse decode failed ", t)
            }
        }
    }
}
