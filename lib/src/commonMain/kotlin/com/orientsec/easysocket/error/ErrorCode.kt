package com.orientsec.easysocket.error

/**
 * This class defines error codes used to represent various types of errors
 * in the EasySocket library. The error codes are categorized into three groups:
 * client errors, request task errors, and connection errors.
 */
object ErrorCode {
    //======> Client Errors <======
    /**
     * Indicates that the client has stopped.
     */
    const val STOP = 1

    /**
     * Indicates that the client has been shut down.
     */
    const val SHUTDOWN = 2

    /**
     * Indicates that the client initialization has failed.
     */
    const val INIT_FAILED = 3

    //======> Request Task Errors <======
    /**
     * Indicates that a response has timed out.
     */
    const val RESPONSE_TIME_OUT = 102

    /**
     * Indicates that the request data is empty.
     */
    const val REQUEST_DATA_EMPTY = 103

    //======> Connection Errors <======
    /**
     * Indicates that the session initialization has failed.
     */
    const val SESSION_INIT_FAILED = 201

    /**
     * Indicates that a heartbeat (pulse) has timed out.
     */
    const val PULSE_TIME_OUT = 202

    /**
     * Indicates a socket connection error.
     */
    const val SOCKET_CONNECT = 203

    /**
     * Indicates that the reader has exited unexpectedly.
     */
    const val READ_EXIT = 204

    /**
     * Indicates that a write operation has failed.
     */
    const val WRITE_ERROR = 205
}
