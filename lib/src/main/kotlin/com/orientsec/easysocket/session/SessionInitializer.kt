package com.orientsec.easysocket.session

import com.orientsec.easysocket.task.TaskBuilder

/**
 * The `SessionInitializer` interface defines the contract for initializing a session.
 * It provides a method to start the initialization process and an inner `Emitter` interface
 * for handling session-related events, such as success or failure notifications.
 */
interface SessionInitializer {

    /**
     * Starts the session initialization process.
     *
     * @param taskBuilder The `TaskBuilder` instance used to configure and execute tasks
     *                    required for session initialization.
     */
    suspend fun start(taskBuilder: TaskBuilder):Result<Unit>

}
