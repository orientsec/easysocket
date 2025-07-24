package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.client.BaseSocketClient;
import com.orientsec.easysocket.utils.Logger;

import java.util.concurrent.Executor;

public class LifecycleCallbackWrapper<T> implements LifecycleCallback<T> {

    private final Callback<T> callback;
    private final Executor executor;
    private final Logger logger;
    private final Task<?> task;
    private final boolean isDebuggable;

    public LifecycleCallbackWrapper(Callback<T> callback, Task<?> task, BaseSocketClient client) {
        this.callback = callback;
        this.logger = client.getLogger();
        this.task = task;
        Options options = client.getOptions();
        this.executor = options.getCallbackExecutor();
        this.isDebuggable = options.isDebuggable();
    }

    @Override
    public void onStart() {
        if (isDebuggable) logger.d(prefix() + "onStart");
        executor.execute(callback::onStart);
    }

    @Override
    public void onSuccess(@NonNull T res) {
        if (isDebuggable)
            logger.d(prefix() + "onSuccess, type: " + res.getClass().getSimpleName());
        executor.execute(() -> {
            callback.onSuccess(res);
            if (callback instanceof LifecycleCallback) {
                ((LifecycleCallback<T>) callback).onComplete();
            }
        });
        onComplete();
    }

    @Override
    public void onFailure(@NonNull Throwable t) {
        if (isDebuggable) logger.d(prefix() + "onFailure, error: " + t.getMessage());
        executor.execute(() -> {
            callback.onFailure(t);
            if (callback instanceof LifecycleCallback) {
                ((LifecycleCallback<T>) callback).onComplete();
            }
        });
        onComplete();
    }

    @Override
    public void onCanceled() {
        if (isDebuggable) logger.d(prefix() + "onCanceled");
        executor.execute(() -> {
            callback.onCanceled();
            if (callback instanceof LifecycleCallback) {
                ((LifecycleCallback<T>) callback).onComplete();
            }
        });
        onComplete();
    }

    @Override
    public void onWait() {
        if (isDebuggable) logger.d(prefix() + "onWait");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onWait);
        }
    }

    @Override
    public void onResume() {
        if (isDebuggable) logger.d(prefix() + "onResume");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onResume);
        }
    }

    @Override
    public void onEncodeStart() {
        if (isDebuggable) logger.d(prefix() + "onEncodeStart");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onEncodeStart);
        }
    }

    @Override
    public void onEncodeSuccess() {
        if (isDebuggable) logger.d(prefix() + "onEncodeSuccess");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onEncodeSuccess);
        }
    }

    @Override
    public void onEncodeFailure(Throwable t) {
        if (isDebuggable) logger.d(prefix() + "onEncodeFailure, error: " + t.getMessage());
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onEncodeFailure(t));
        }
    }

    @Override
    public void onReset(int failedTimes, Throwable t) {
        if (isDebuggable)
            logger.d(prefix() + "onReset, failedTimes: " + failedTimes
                    + ", error: " + t.getMessage());
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onReset(failedTimes, t));
        }
    }

    @Override
    public void onSendStart() {
        if (isDebuggable) logger.d(prefix() + "onSendStart");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onSendStart);
        }
    }

    @Override
    public void onSendSuccess() {
        if (isDebuggable) logger.d(prefix() + "onSendSuccess");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onSendSuccess);
        }
    }

    @Override
    public void onSendFailure(Throwable t) {
        if (isDebuggable) logger.d(prefix() + "onSendFailure, error: " + t.getMessage());
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onSendFailure(t));
        }
    }

    @Override
    public void onPacketReceived(Packet packet) {
        if (isDebuggable) logger.d(prefix() + "onPacketReceived, packet: " + packet);
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onPacketReceived(packet));
        }
    }

    @Override
    public void onDecodeStart() {
        if (isDebuggable) logger.d(prefix() + "onDecodeStart");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onDecodeStart);
        }
    }

    @Override
    public void onDecodeSuccess() {
        if (isDebuggable) logger.d(prefix() + "onDecodeSuccess");
        if (callback instanceof LifecycleCallback) {
            executor.execute(((LifecycleCallback<T>) callback)::onDecodeSuccess);
        }
    }

    @Override
    public void onDecodeFailure(Throwable t) {
        if (isDebuggable) logger.d(prefix() + "onDecodeFailure, error: " + t.getMessage());
        if (callback instanceof LifecycleCallback) {
            executor.execute(() -> ((LifecycleCallback<T>) callback).onDecodeFailure(t));
        }
    }

    @Override
    public void onComplete() {
        if (isDebuggable) logger.d(prefix() + "onComplete");
    }

    private String prefix() {
        return "Task " + task.getTaskId() + ", type: " + task.getTaskType() + ", ";
    }
}