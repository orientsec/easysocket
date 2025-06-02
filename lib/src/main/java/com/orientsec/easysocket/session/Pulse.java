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
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.request.Decoder;
import com.orientsec.easysocket.request.DefaultCallback;
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
public class Pulse implements PacketHandler, TaskBuilder, Runnable {
    // The socket client associated with this heartbeat manager
    private final BaseSocketClient socketClient;
    // The maximum number of consecutive heartbeat failures allowed
    // before the session is considered invalid.
    private final int maxLostTimes;
    // The interval in milliseconds between consecutive heartbeat messages.
    private final long intervalInMills;
    // The session associated with this heartbeat manager
    private final OperableSession session;
    // EasyExecutor for scheduling tasks
    private final EasyExecutor mainExecutor;
    // Counter for tracking the number of consecutive heartbeat failures
    private final AtomicInteger lostTimes = new AtomicInteger();
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
    Pulse(BaseSocketClient socketClient, OperableSession session, EasyExecutor mainExecutor) {
        this.socketClient = socketClient;
        this.session = session;
        this.mainExecutor = mainExecutor;
        Options options = socketClient.getOptions();
        this.codecExecutor = options.getCodecExecutor();
        this.intervalInMills = options.getPulseIntervalInSec() * 1000L;
        this.maxLostTimes = options.getPulseMaxLostTimes();
        logger = session.getLogger();
    }

    /**
     * Resets the heartbeat failure counter to zero.
     */
    private void feed() {
        lostTimes.set(0);
    }

    /**
     * Starts the heartbeat mechanism.
     * This method is called after a successful connection is established.
     */
    void start() {
        mainExecutor.schedule(this, intervalInMills);
    }

    /**
     * Stops the heartbeat mechanism.
     * This method is called when the connection is disconnected.
     */
    void stop() {
        mainExecutor.remove(this);
    }

    /**
     * Sends a heartbeat message.
     * If the number of consecutive heartbeat failures exceeds the allowed limit,
     * the session is closed.
     */
    public void run() {
        if (lostTimes.getAndAdd(1) > maxLostTimes) {
            // Close the session if the heartbeat failure count exceeds the limit
            logger.i("pulse failed times up, session invalid");
            EasyException e = EasyException.create(ErrorCode.PULSE_TIME_OUT, ErrorType.CONNECT,
                    "pulse time out", session.getSuffix());
            session.close(e);
        } else {
            Request<Boolean> pulseRequest = socketClient.getPulseRequest();
            if (pulseRequest == null) {
                logger.w("no pulse request");
            } else {
                buildTask(pulseRequest, callback).execute();
                start();
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
            if (res) {
                feed();
            }
        }

        @Override
        public void onFailure(@NonNull Throwable t) {
            logger.w("client pulse failed", t);
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
                if (success) feed();
            } catch (Throwable t) {
                logger.e("server pulse decode failed", t);
            }
        });
    }
}