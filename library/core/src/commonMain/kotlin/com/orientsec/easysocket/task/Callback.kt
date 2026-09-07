package com.orientsec.easysocket.task

/**
 * A generic callback interface for handling various stages of a task lifecycle.
 *
 * @param T The type of the response object.
 */
interface Callback<T> {

    /**
     * Called when the request starts execution.
     */
    fun onStart()

    /**
     * Called when the request completes successfully.
     *
     * @param res The response object.
     */
    fun onSuccess(res: T)

    /**
     * Called when the request fails.
     *
     * @param t The exception that caused the failure.
     */
    fun onFailure(t: Throwable)

    /**
     * Called when the request is canceled.
     */
    fun onCanceled()

    companion object {

        private val NOOP: Callback<Any?> = object : Callback<Any?> {
            override fun onStart() {}
            override fun onSuccess(res: Any?) {}
            override fun onFailure(t: Throwable) {}
            override fun onCanceled() {}
        }

        /**
         * Returns a callback that does nothing, used as the default when the
         * caller only needs the [Task.await] / [Task.send] suspend path.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> noop(): Callback<T> = NOOP as Callback<T>
    }
}
