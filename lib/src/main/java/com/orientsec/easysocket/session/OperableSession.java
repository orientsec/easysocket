package com.orientsec.easysocket.session;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.task.TaskBuilder;
import com.orientsec.easysocket.utils.Logger;

/**
 * Represents an operable session interface that extends the `Session` and `TaskBuilder` interfaces.
 * Provides methods to open, close, and manage the session state.
 */
public interface OperableSession extends Session, TaskBuilder {

    /**
     * Opens the session.
     * This method must be called on the main thread.
     */
    @MainThread
    void open();

    /**
     * Closes the session.
     * This method must be called on the main thread.
     *
     * @param e The exception describing the reason for closing the session.
     */
    @MainThread
    void close(EasyException e);

    /**
     * Retrieves the `Writer` object, which is created only after a successful connection.
     *
     * @return The `Writer` object, which can be accessed after a successful connection,
     * or `null` if the connection has not been established.
     */
    @Nullable
    Writer getWriter();

    /**
     * Retrieves the logger instance associated with the session.
     *
     * @return The `Logger` instance used for logging.
     */
    @NonNull
    Logger getLogger();

    /**
     * Retrieves the suffix information of the session.
     *
     * @return A string representing the session suffix.
     */
    @NonNull
    String getSuffix();
}
