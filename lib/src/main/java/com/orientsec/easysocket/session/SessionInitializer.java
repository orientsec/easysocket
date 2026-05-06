package com.orientsec.easysocket.session;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.task.TaskBuilder;

/**
 * The `SessionInitializer` interface defines the contract for initializing a session.
 * It provides a method to start the initialization process and an inner `Emitter` interface
 * for handling session-related events, such as success or failure notifications.
 */
public interface SessionInitializer {

    /**
     * Starts the session initialization process.
     *
     * @param emitter     The `Emitter` instance used to send session-related events.
     * @param taskBuilder The `TaskBuilder` instance used to configure and execute tasks
     *                    required for session initialization.
     */
    void start(@NonNull Emitter emitter, @NonNull TaskBuilder taskBuilder);

    /**
     * The `Emitter` interface defines methods for sending session-related events.
     * It is used to notify the success or failure of the session initialization process.
     */
    interface Emitter {

        /**
         * Sends an event indicating that the session has been successfully initialized.
         * Notifies that the session is ready for use.
         */
        void postSuccess();

        /**
         * Sends an event indicating that the session initialization has failed.
         * Notifies that an error occurred during the session initialization process.
         *
         * @param t The exception describing the reason for the failure.
         */
        void postFailure(@NonNull Throwable t);
    }
}