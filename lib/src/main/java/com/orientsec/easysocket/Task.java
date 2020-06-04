package com.orientsec.easysocket;

import androidx.annotation.NonNull;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/25 17:37
 * Author: Fredric
 * coding is art not science
 */

public interface Task<T, R> {
    int SYNC_TASK_ID = 0;

    /**
     * Asynchronously send the OUT and notify {@code callback} of its IN or if an error
     * occurred talking to the server, creating the OUT, or processing the IN.
     */
    void execute();

    /**
     * Returns true if this call has been {@linkplain #execute() executed}.
     * It is an error to execute or enqueue a call more than once.
     */
    boolean isExecuted();

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
     * True if task can execute.
     *
     * @return True if task can execute.
     */
    boolean isExecutable();

    /**
     * Obtain request of this task.
     *
     * @return request
     */
    @NonNull
    Request<T, R> getRequest();
}
