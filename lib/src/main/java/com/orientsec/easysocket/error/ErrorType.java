package com.orientsec.easysocket.error;

/**
 * Defines various error types used in the EasySocket library.
 * These error types categorize different kinds of issues that may occur
 * during the operation of the system, such as system state errors,
 * connection errors, and task-related errors.
 */
public class ErrorType {
    /**
     * Represents system state errors.
     * This includes:
     * 1. Connection actively disconnected, e.g., when the app enters the background
     * for a duration exceeding the configured sleep time, or when shutdown is called explicitly.
     */
    public static final int SYSTEM = 1;

    /**
     * Represents server connection errors.
     * This includes issues such as:
     * - Socket connection failures.
     * - Data validation failures.
     * - Header parsing failures.
     * - Mismatched magic numbers.
     * These issues typically result in the connection being terminated.
     */
    public static final int CONNECT = 2;

    /**
     * Represents task-related errors where no response is received.
     * This error type is used when a task fails to get a response
     * due to issues such as timeouts or unprocessed requests.
     */
    public static final int TASK = 3;
}