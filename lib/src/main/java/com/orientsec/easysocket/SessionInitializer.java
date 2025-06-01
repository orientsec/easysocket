package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.session.OperableSession;

/**
 * The `SessionInitializer` interface defines a contract for initializing a session
 * after a successful connection. Implementations of this interface can perform
 * tasks such as authentication or other setup operations required for the session.
 */
public interface SessionInitializer {

    /**
     * Starts the initialization process for the given session.
     * <p>
     * This method is called after the connection has been established.
     * * After successful initialization, the session should be marked as available by
     * * call {@code OperableSession.postAvailable()}.
     *
     * @param session The session that has been successfully connected and is ready
     *                for initialization.
     */
    void start(@NonNull OperableSession session);

}