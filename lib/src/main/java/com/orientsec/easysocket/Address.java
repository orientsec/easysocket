package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * The `Address` class represents connection information for a server.
 * It includes the server's IP address or domain name and port number.
 * This class is immutable and implements `Serializable` and `Cloneable` interfaces.
 */
public final class Address implements Serializable, Cloneable {
    /**
     * The IP address or domain name of the server.
     */
    private final String host;

    /**
     * The port number of the server.
     */
    private final int port;

    /**
     * Constructs an `Address` instance with the specified host and port.
     *
     * @param host The IP address or domain name of the server.
     * @param port The port number of the server.
     */
    public Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Retrieves the IP address or domain name of the server.
     *
     * @return The IP address or domain name as a `String`.
     */
    public String getHost() {
        return host;
    }

    /**
     * Retrieves the port number of the server.
     *
     * @return The port number as an `int`.
     */
    public int getPort() {
        return port;
    }

    /**
     * Compares this `Address` instance with another object for equality.
     * Two `Address` instances are considered equal if their host and port are the same.
     *
     * @param o The object to compare with.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Address)) {
            return false;
        }

        Address connectInfo = (Address) o;

        return port == connectInfo.port && host.equals(connectInfo.host);
    }

    /**
     * Computes the hash code for this `Address` instance.
     * The hash code is based on the host and port values.
     *
     * @return The hash code as an `int`.
     */
    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }

    /**
     * Returns a string representation of this `Address` instance.
     * The string includes the host and port values.
     *
     * @return A string representation of the `Address`.
     */
    @NonNull
    @Override
    public String toString() {
        return "Address[" +
                "host=" + host +
                ", port=" + port +
                ']';
    }

    /**
     * Creates and returns a copy of this `Address` instance.
     *
     * @return A clone of this `Address` instance.
     * @throws CloneNotSupportedException If the cloning operation is not supported.
     */
    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}