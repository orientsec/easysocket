package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.session.OperableSession;

import java.util.List;

import javax.net.SocketFactory;

/**
 * Provides the default implementation of a `Provider` for `SocketFactory`.
 * This class supplies the default `SocketFactory` instance.
 */
class DefaultSocketFactoryProvider implements Provider<SocketFactory> {

    /**
     * Retrieves the default `SocketFactory` instance.
     *
     * @param socketClient The `SocketClient` instance requesting the `SocketFactory`.
     * @return The default `SocketFactory` instance.
     */
    @NonNull
    @Override
    public SocketFactory get(SocketClient socketClient) {
        return SocketFactory.getDefault();
    }
}

/**
 * Provides the default implementation of a `Provider` for `SessionInitializer`.
 * This class supplies a new instance of `DefaultSessionInitializer`.
 */
class DefaultInitializerProvider implements Provider<SessionInitializer> {

    /**
     * Retrieves a new instance of `DefaultSessionInitializer`.
     *
     * @param socketClient The `SocketClient` instance requesting the `SessionInitializer`.
     * @return A new instance of `DefaultSessionInitializer`.
     */
    @NonNull
    @Override
    public SessionInitializer get(SocketClient socketClient) {
        return new DefaultSessionInitializer();
    }
}

/**
 * Default implementation of the `SessionInitializer` interface.
 * This class is responsible for marking a session as available.
 */
final class DefaultSessionInitializer implements SessionInitializer {

    /**
     * Starts the session by marking it as available.
     *
     * @param session The `OperableSession` instance to be initialized.
     */
    @Override
    public void start(@NonNull OperableSession session) {
        session.postAvailable();
    }
}

/**
 * Provides a static implementation of a `Provider` for a list of `Address` objects.
 * This class supplies a predefined list of addresses.
 */
class StaticAddressProvider implements Provider<List<Address>> {

    /**
     * Creates a new `StaticAddressProvider` with the specified list of addresses.
     *
     * @param addressList The list of addresses to be provided.
     * @return A new instance of `StaticAddressProvider`.
     * @throws IllegalArgumentException If the address list is empty.
     */
    static StaticAddressProvider build(@NonNull List<Address> addressList) {
        if (addressList.isEmpty()) {
            throw new IllegalArgumentException("Address list is empty.");
        }
        return new StaticAddressProvider(addressList);
    }

    // The list of addresses to be provided.
    private final List<Address> addressList;

    /**
     * Constructs a new `StaticAddressProvider` with the specified list of addresses.
     *
     * @param addressList The list of addresses to be provided.
     */
    StaticAddressProvider(List<Address> addressList) {
        this.addressList = addressList;
    }

    /**
     * Retrieves the predefined list of addresses.
     *
     * @param socketClient The `SocketClient` instance requesting the addresses.
     * @return The predefined list of addresses.
     */
    @NonNull
    @Override
    public List<Address> get(SocketClient socketClient) {
        return addressList;
    }
}