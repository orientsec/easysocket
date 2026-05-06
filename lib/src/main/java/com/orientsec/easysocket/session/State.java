package com.orientsec.easysocket.session;

/**
 * Represents the connection state of a session.
 *
 * <p>
 * IDLE: The idle state. Transition: IDLE -> STARTING.
 * </p>
 *
 * <p>
 * STARTING: The starting state.
 * 1. Transition: STARTING -> DETACHED (connection failed).
 * 2. Transition: STARTING -> CONNECTED (connection succeeded).
 * </p>
 *
 * <p>
 * CONNECTED: The connected state.
 * 1. Transition: CONNECTED -> AVAILABLE (initialization succeeded).
 * 2. Transition: CONNECTED -> DETACHED (initialization failed).
 * </p>
 *
 * <p>
 * AVAILABLE: The available state. Transition: AVAILABLE -> DETACHED (connection lost).
 * </p>
 *
 * <p>
 * DETACHED: The detached state, indicating the session is no longer active.
 * </p>
 */
enum State {
    IDLE, STARTING, CONNECTED, AVAILABLE, DETACHED
}