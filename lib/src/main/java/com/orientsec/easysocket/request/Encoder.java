package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

/**
 * An interface for encoding data into a byte array format.
 */
public interface Encoder {

    /**
     * Encodes the given sequence ID into a byte array.
     *
     * @param sequenceId The sequence ID to be encoded.
     * @return A byte array representing the encoded sequence ID.
     */
    @NonNull
    byte[] encode(int sequenceId);
}