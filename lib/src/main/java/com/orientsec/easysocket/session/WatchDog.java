package com.orientsec.easysocket.session;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.EasyExecutor;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.client.BaseSocketClient;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;
import com.orientsec.easysocket.task.Callback;
import com.orientsec.easysocket.request.Decoder;
import com.orientsec.easysocket.task.DefaultCallback;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.task.Task;
import com.orientsec.easysocket.task.TaskBuilder;
import com.orientsec.easysocket.task.TaskImpl;
import com.orientsec.easysocket.task.TaskType;
import com.orientsec.easysocket.utils.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the heartbeat mechanism for maintaining the connection.
 * This class implements `PacketHandler`, `TaskBuilder`, and `Runnable` interfaces.
 */
public class WatchDog implements PacketHandler, TaskBuilder, Runnable {
    // The socket client associated with this heartbeat manager
    private final BaseSocketClient socketClient;
    // The interval in milliseconds before the watchdog wakes up.
    private final long delayInMills;
    // The interval (in milliseconds) between actions when the watchdog is awake.
    private final long retryIntervalInMills;
    // The maximum number of consecutive heartbeat failures allowed
    // before the session is considered invalid.
    private final int retryTimes;
    // The session associated with this heartbeat manager
    private final OperableSession session;
    // EasyExecutor for scheduling tasks
    private final EasyExecutor mainExecutor;
    // Counter for tracking the number of consecutive heartbeat failures
    private final AtomicInteger barkTimes = new AtomicInteger();
    // Executor for handling codec-related tasks
    private final Executor codecExecutor;
    // Logger instance for logging messages
    private final Logger logger;

    /**
     * Constructs a Pulse instance with the specified parameters.
     *
     * @param socketClient The socket client associated with this heartbeat manager.
     * @param session      The session associated with this heartbeat manager.
     * @param mainExecutor The EasyExecutor for scheduling tasks.
     */
    WatchDog(BaseSocketClient socketClient, OperableSession session, EasyExecutor mainExecutor) {
        this.socketClient = socketClient;
        this.session = session;
        this.mainExecutor = mainExecutor;
        Options options = socketClient.getOptions();
        this.codecExecutor = options.getCodecExecutor();
        this.delayInMills = options.getPulseDelayInSec() * 1000L;
        this.retryIntervalInMills = options.getPulseRetryIntervalInMills() * 1000L;
        this.retryTimes = options.getPulseRetryTimes();
        logger = session.getLogger();
    }

    /**
     * Resets the heartbeat failure counter and restarts the watchdog timer.
     * This method is called whenever data is received.
     */
    void feed() {
        barkTimes.set(0);
        mainExecutor.remove(this);
        mainExecutor.schedule(this, delayInMills);
    }

    /**
     * Starts the heartbeat mechanism.
     * This method is called after a successful connection is established.
     */
    void start() {
        feed();
    }

    /**
     * Stops the heartbeat mechanism.
     * This method is called when the connection is disconnected.
     */
    void stop() {
        mainExecutor.remove(this);
    }

    /**
     * Executes the watchdog action.
     * If the watchdog is awake, it performs actions sequentially:
     * 1. Send first heartbeat.
     * 2. Send second heartbeat.
     * 3. Close the session.
     */
    public void run() {
        int times = barkTimes.getAndIncrement();
        if (times > retryTimes) {
            // Close the session if no data received after 2 heartbeat attempts
            logger.i("watchdog biting, pulse failed " + times + " times, session invalid");
            EasyException e = EasyException.create(ErrorCode.PULSE_TIME_OUT, ErrorType.CONNECT,
                    "pulse time out", session.getSuffix());
            session.close(e);
        } else {
            Request<Boolean> pulseRequest = socketClient.getPulseRequest();
            if (pulseRequest == null) {
                logger.w("no pulse request for watchdog");
            } else {
                logger.i("watchdog sending pulse");
                buildTask(pulseRequest, callback).execute();
                // Schedule the next action
                mainExecutor.schedule(this, retryIntervalInMills);
            }
        }
    }

    /**
     * Builds a task for sending a heartbeat request.
     *
     * @param request  The heartbeat request to be sent.
     * @param callback The callback to handle the response of the heartbeat request.
     * @param <T>      The type of the response object.
     * @return A task for executing the heartbeat request.
     */
    @NonNull
    @Override
    public <T> Task<T> buildTask(@NonNull Request<T> request,
                                 @NonNull Callback<T> callback) {
        return new TaskImpl<>(TaskType.PULSE, socketClient.getTaskManager().generateTaskId(),
                request, callback, socketClient, session);
    }

    // Callback for handling the response of the heartbeat request
    Callback<Boolean> callback = new DefaultCallback<Boolean>() {
        @Override
        public void onSuccess(@NonNull Boolean res) {
            logger.i("pulse result: " + res + ", type: request-response");
        }

        @Override
        public void onFailure(@NonNull Throwable t) {
            logger.i("pulse failed, type: request-response", t);
        }
    };

    /**
     * Handles the received packet and decodes the heartbeat response.
     *
     * @param packet The packet received from the server.
     */
    @Override
    public void handlePacket(@NonNull Packet packet) {
        Decoder<Boolean> pulseDecoder = socketClient.getPulseDecoder();
        if (pulseDecoder == null) return;
        codecExecutor.execute(() -> {
            try {
                boolean success = pulseDecoder.decode(packet);
                logger.i("pulse result: " + success + ", type: ping-pong");
            } catch (Throwable t) {
                logger.i("pulse decode failed, type: ping-pong", t);
            }
        });
    }
}