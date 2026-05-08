package com.orientsec.easysocket.session

import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.task.TaskBuilder
import com.orientsec.easysocket.utils.Logger

/**
 * Represents an operable session interface that extends the `Session` and `TaskBuilder` interfaces.
 * Provides methods to open, close, and manage the session state.
 */
interface OperableSession : Session, TaskBuilder {

    /**
     * Opens the session.
     * This method must be called on the main thread.
     */
    fun open()

    /**
     * Closes the session.
     * This method must be called on the main thread.
     *
     * @param e The exception describing the reason for closing the session.
     */
    fun close(e: EasyException)

    /**
     * Retrieves the `Writer` object, which is created only after a successful connection.
     *
     * @return The `Writer` object, which can be accessed after a successful connection,
     * or `null` if the connection has not been established.
     */
    val writer: Writer?

    /**
     * Retrieves the logger instance associated with the session.
     *
     * @return The `Logger` instance used for logging.
     */
    val logger: Logger

    /**
     * Retrieves the suffix information of the session.
     *
     * @return A string representing the session suffix.
     */
    val suffix: String
}
