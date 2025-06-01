package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.utils.Logger;

import java.util.concurrent.Executor;

/**
 * A wrapper class for `LifecycleCallback` that adds logging and executes callback methods
 * on a specified executor. This class is used to track the lifecycle of a task and log
 * its progress with a unique task ID.
 *
 * @param <T> The type of the result object handled by the callback.
 */
public class LifecycleCallbackWrapper<T> implements LifecycleCallback<T> {

    // The original callback instance to be wrapped
    private final Callback<T> callback;

    // The executor used to run callback methods
    private final Executor executor;

    // Logger instance for logging lifecycle events
    private final Logger logger;

    // Unique identifier for the task
    private final int taskId;

    /**
     * Constructs a `LifecycleCallbackWrapper` instance.
     *
     * @param callback The original callback instance to be wrapped.
     * @param executor The executor used to run callback methods.
     * @param logger   The logger instance for logging lifecycle events.
     * @param taskId   The unique identifier for the task.
     */
    public LifecycleCallbackWrapper(Callback<T> callback, Executor executor, Logger logger, int taskId) {
        this.callback = callback;
        this.executor = executor;
        this.logger = logger;
        this.taskId = taskId;
    }

    @Override
    public void onStart() {
        logger.d("Task " + taskId + ": onStart ");
        executor.execute(callback::onStart);
    }

    @Override
    public void onSuccess(@NonNull T res) {
        logger.d("Task " + taskId + ": onSuccess, result: " + res);
        executor.execute(() -> callback.onSuccess(res));
    }

    @Override
    public void onFailure(@NonNull Throwable t) {
        logger.d("Task " + taskId + ": onFailure, error: " + t.getMessage());
        executor.execute(() -> callback.onFailure(t));
    }

    @Override
    public void onCanceled() {
        logger.d("Task " + taskId + ": onCanceled ");
        executor.execute(callback::onCanceled);
    }

    @Override
    public void onWait() {
        logger.d("Task " + taskId + ": onWait ");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onWait);
        }
    }

    @Override
    public void onResume() {
        logger.d("Task " + taskId + ": onResume ");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onResume);
        }
    }

    @Override
    public void onEncodeStart() {
        logger.d("Task " + taskId + ": onEncodeStart ");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onEncodeStart);
        }
    }

    @Override
    public void onEncodeSuccess() {
        logger.d("Task " + taskId + ": onEncodeSuccess ");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onEncodeSuccess);
        }
    }

    @Override
    public void onEncodeFailure(Throwable t) {
        logger.d("Task " + taskId + ": onEncodeFailure, error: " + t.getMessage());
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onEncodeFailure(t));
        }
    }

    @Override
    public void onSendStart() {
        logger.d("Task " + taskId + ": onSendStart ");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onSendStart);
        }
    }

    @Override
    public void onSendSuccess() {
        logger.d("Task " + taskId + ": onSendSuccess ");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onSendSuccess);
        }
    }

    @Override
    public void onSendFailure(Throwable t) {
        logger.d("Task " + taskId + ": onSendFailure, error: " + t.getMessage());
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onSendFailure(t));
        }
    }

    @Override
    public void onPacketReceived(Packet packet) {
        logger.d("Task " + taskId + ": onPacketReceived, packet: " + packet);
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onPacketReceived(packet));
        }
    }

    @Override
    public void onDecodeStart() {
        logger.d("Task " + taskId + ": onDecodeStart ");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onDecodeStart);
        }
    }

    @Override
    public void onDecodeSuccess() {
        logger.d("Task " + taskId + ": onDecodeSuccess ");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onDecodeSuccess);
        }
    }

    @Override
    public void onDecodeFailure(Throwable t) {
        logger.d("Task " + taskId + ": onDecodeFailure, error: " + t.getMessage());
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onDecodeFailure(t));
        }
    }

    @Override
    public void onComplete() {
        logger.d("Task " + taskId + ": onComplete ");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onComplete);
        }
    }
}
