package com.orientsec.easysocket.client;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Address;

import java.util.List;

/**
 * The `ClientInitializer` interface defines the contract for initializing a client.
 * It provides a method to start the initialization process and an inner `Emitter` interface
 * for sending initialization results.
 */
public interface ClientInitializer {

    /**
     * Starts the client initialization process.
     *
     * @param emitter The `Emitter` instance used to send initialization results.
     */
    void start(@NonNull Emitter emitter);

    /**
     * The `Emitter` interface defines methods for sending client initialization events.
     * It is used to notify the success or failure of the initialization process.
     */
    interface Emitter {

        /**
         * Sends an event indicating that the initialization was successful.
         * Notifies that the client is ready for use with the provided address list.
         *
         * @param addressList The list of addresses associated with the client.
         */
        void postSuccess(@NonNull List<Address> addressList);

        /**
         * Sends an event indicating that the initialization has failed.
         * Notifies that an error occurred during the initialization process.
         *
         * @param t The exception describing the reason for the failure.
         */
        void postFailure(@NonNull Throwable t);
    }
}