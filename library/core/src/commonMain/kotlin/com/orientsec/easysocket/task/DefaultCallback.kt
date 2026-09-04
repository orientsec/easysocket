package com.orientsec.easysocket.task

/**
 * A default implementation of the Callback interface with empty method bodies.
 *
 * @param T The type of the response object.
 */
open class DefaultCallback<T> : Callback<T> {

    /**
     * Called when the request starts execution.
     * This implementation does nothing.
     */
    override fun onStart() {

    }

    /**
     * Called when the request completes successfully.
     * This implementation does nothing.
     *
     * @param res The response object.
     */
    override fun onSuccess(res: T) {

    }

    /**
     * Called when the request fails.
     * This implementation does nothing.
     *
     * @param t The exception that caused the failure.
     */
    override fun onFailure(t: Throwable) {

    }

    /**
     * Called when the request is canceled.
     * This implementation does nothing.
     */
    override fun onCanceled() {

    }
}
