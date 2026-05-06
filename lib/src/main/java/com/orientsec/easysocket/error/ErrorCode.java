package com.orientsec.easysocket.error;

/**
 * This class defines error codes used to represent various types of errors
 * in the EasySocket library. The error codes are categorized into three groups:
 * client errors, request task errors, and connection errors.
 */
public class ErrorCode {
    //======> Client Errors <======
    /**
     * Indicates that the client has stopped.
     */
    public static final int STOP = 1;
    /**
     * Indicates that the client has been shut down.
     */
    public static final int SHUTDOWN = 2;
    /**
     * Indicates that the client initialization has failed.
     */
    public static final int INIT_FAILED = 3;

    //======> Request Task Errors <======
    /**
     * Indicates that a response has timed out.
     */
    public static final int RESPONSE_TIME_OUT = 102;
    /**
     * Indicates that the request data is empty.
     */
    public static final int REQUEST_DATA_EMPTY = 103;

    //======> Connection Errors <======
    /**
     * Indicates that the session initialization has failed.
     */
    public static final int SESSION_INIT_FAILED = 201;
    /**
     * Indicates that a heartbeat (pulse) has timed out.
     */
    public static final int PULSE_TIME_OUT = 202;
    /**
     * Indicates a socket connection error.
     */
    public static final int SOCKET_CONNECT = 203;
    /**
     * Indicates that the reader has exited unexpectedly.
     */
    public static final int READ_EXIT = 204;
    /**
     * Indicates that a write operation has failed.
     */
    public static final int WRITE_ERROR = 205;
}