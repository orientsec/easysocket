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

    //Task instance associated with this callback
    private final Task<?> task;

    /**
     * Constructs a `LifecycleCallbackWrapper` instance.
     *
     * @param callback The original callback instance to be wrapped.
     * @param executor The executor used to run callback methods.
     * @param logger   The logger instance for logging lifecycle events.
     * @param task     Task instance associated with this callback
     */
    public LifecycleCallbackWrapper(Callback<T> callback, Task<?> task,
                                    Executor executor, Logger logger) {
        this.callback = callback;
        this.executor = executor;
        this.logger = logger;
        this.task = task;
    }

    @Override
    public void onStart() {
        logger.d(prefix() + "onStart");
        executor.execute(callback::onStart);
    }

    @Override
    public void onSuccess(@NonNull T res) {
        logger.d(prefix() + "onSuccess, result: " + res);
        executor.execute(() -> callback.onSuccess(res));
    }

    @Override
    public void onFailure(@NonNull Throwable t) {
        logger.d(prefix() + "onFailure, error: " + t.getMessage());
        executor.execute(() -> callback.onFailure(t));
    }

    @Override
    public void onCanceled() {
        logger.d(prefix() + "onCanceled");
        executor.execute(callback::onCanceled);
    }

    @Override
    public void onWait() {
        logger.d(prefix() + "onWait");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onWait);
        }
    }

    @Override
    public void onResume() {
        logger.d(prefix() + "onResume");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onResume);
        }
    }

    @Override
    public void onEncodeStart() {
        logger.d(prefix() + "onEncodeStart");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onEncodeStart);
        }
    }

    @Override
    public void onEncodeSuccess() {
        logger.d(prefix() + "onEncodeSuccess");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onEncodeSuccess);
        }
    }

    @Override
    public void onEncodeFailure(Throwable t) {
        logger.d(prefix() + "onEncodeFailure, error: " + t.getMessage());
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onEncodeFailure(t));
        }
    }

    @Override
    public void onSendStart() {
        logger.d(prefix() + "onSendStart");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onSendStart);
        }
    }

    @Override
    public void onSendSuccess() {
        logger.d(prefix() + "onSendSuccess");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onSendSuccess);
        }
    }

    @Override
    public void onSendFailure(Throwable t) {
        logger.d(prefix() + "onSendFailure, error: " + t.getMessage());
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onSendFailure(t));
        }
    }

    @Override
    public void onPacketReceived(Packet packet) {
        logger.d(prefix() + "onPacketReceived, packet: " + packet);
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onPacketReceived(packet));
        }
    }

    @Override
    public void onDecodeStart() {
        logger.d(prefix() + "onDecodeStart");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onDecodeStart);
        }
    }

    @Override
    public void onDecodeSuccess() {
        logger.d(prefix() + "onDecodeSuccess");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onDecodeSuccess);
        }
    }

    @Override
    public void onDecodeFailure(Throwable t) {
        logger.d(prefix() + "onDecodeFailure, error: " + t.getMessage());
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onDecodeFailure(t));
        }
    }

    @Override
    public void onComplete() {
        logger.d(prefix() + "onComplete");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onComplete);
        }
    }

    private String prefix() {
        return "Task " + task.getTaskId() + ", type: " + task.getTaskType() + ", ";
    }
}