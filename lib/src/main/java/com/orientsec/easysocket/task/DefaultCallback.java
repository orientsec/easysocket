package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

/**
 * A default implementation of the Callback interface with empty method bodies.
 *
 * @param <T> The type of the response object.
 */
public class DefaultCallback<T> implements Callback<T> {

    /**
     * Called when the request starts execution.
     * This implementation does nothing.
     */
    @Override
    public void onStart() {

    }

    /**
     * Called when the request completes successfully.
     * This implementation does nothing.
     *
     * @param res The response object.
     */
    @Override
    public void onSuccess(@NonNull T res) {

    }

    /**
     * Called when the request fails.
     * This implementation does nothing.
     *
     * @param t The exception that caused the failure.
     */
    @Override
    public void onFailure(@NonNull Throwable t) {

    }

    /**
     * Called when the request is canceled.
     * This implementation does nothing.
     */
    @Override
    public void onCanceled() {

    }
}