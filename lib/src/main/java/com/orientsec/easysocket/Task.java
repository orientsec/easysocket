package com.orientsec.easysocket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/25 17:37
 * Author: Fredric
 * coding is art not science
 */

public interface Task<RESPONSE> {

    /**
     * Asynchronously send the request and notify {@code callback} of its response or if an error
     * occurred talking to the server, creating the request, or processing the response.
     */
    void execute(Callback<RESPONSE> callback);

    void execute();

    /**
     * Returns true if this call has been {@linkplain #execute(Callback callback) executed}.
     * It is an error to execute or enqueue a call more than once.
     */
    boolean isExecuted();

    /**
     * Cancel this call. An attempt will be made to cancel in-flight calls, and if the call has not
     * yet been executed it never will be.
     */
    void cancel();

    /**
     * True if {@link #cancel()} was called.
     */
    boolean isCanceled();

    Request<?, ?, RESPONSE> getRequest();
}
