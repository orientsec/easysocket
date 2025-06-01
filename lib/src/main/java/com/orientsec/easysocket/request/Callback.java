package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

/**
 * A generic callback interface for handling various stages of a request lifecycle.
 *
 * @param <T> The type of the response object.
 */
public interface Callback<T> {

    /**
     * Called when the request starts execution.
     */
    void onStart();

    /**
     * Called when the request completes successfully.
     *
     * @param res The response object.
     */
    void onSuccess(@NonNull T res);

    /**
     * Called when the request fails.
     *
     * @param t The exception that caused the failure.
     */
    void onFailure(@NonNull Throwable t);

    /**
     * Called when the request is canceled.
     */
    void onCanceled();

}