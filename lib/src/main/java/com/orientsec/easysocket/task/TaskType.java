package com.orientsec.easysocket.task;

/**
 * Enum representing the types of tasks in the EasySocket framework.
 * Each task type defines a specific purpose or behavior in the system.
 */
public enum TaskType {
    /**
     * Represents a request task.
     * This type is used for general request operations.
     */
    REQUEST,

    /**
     * Represents an initialization task.
     * Non-initialization requests will enter a waiting state until the connection becomes
     * available.
     * Once the connection is available, they will be encoded and sent.
     * Initialization requests can be executed directly after a successful connection.
     */
    INITIALIZE,

    /**
     * Represents a heartbeat task.
     * This type is used for maintaining the connection through periodic heartbeat messages.
     */
    PULSE
}
