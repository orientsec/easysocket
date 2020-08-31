package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Request;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/25 17:37
 * Author: Fredric
 * coding is art not science
 */

public interface Task<R> {

    int taskId();

    byte[] data();

    /**
     * Asynchronously send the OUT and notify {@code callback} of its IN or if an error
     * occurred talking to the server, creating the OUT, or processing the IN.
     */
    void execute();

    /**
     * Cancel this call. An attempt will be made to cancel in-flight calls, and
     * if the call has not yet been executed it never will be.
     */
    void cancel();

    /**
     * True if {@link #cancel()} was called.
     */
    boolean isCanceled();

    /**
     * True if lifecycle is end.
     *
     * @return If task is finished.
     */
    boolean isFinished();

    /**
     * Obtain request of this task.
     *
     * @return request
     */
    @NonNull
    Request<R> request();
}
