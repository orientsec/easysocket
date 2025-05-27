package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import java.util.List;

import javax.net.SocketFactory;

class DefaultSocketFactoryProvider implements Provider<SocketFactory> {

    @NonNull
    @Override
    public SocketFactory get(SocketClient socketClient) {
        return SocketFactory.getDefault();
    }
}

class DefaultInitializerProvider implements Provider<Initializer> {

    @NonNull
    @Override
    public Initializer get(SocketClient socketClient) {
        return new DefaultInitializer();
    }
}

final class DefaultInitializer implements Initializer {
    @Override
    public void start(@NonNull Emitter emitter) {
        emitter.success();
    }
}

class StaticAddressProvider implements Provider<List<Address>> {

    static StaticAddressProvider build(@NonNull List<Address> addressList) {
        if (addressList.isEmpty()) {
            throw new IllegalArgumentException("Address list is empty.");
        }
        return new StaticAddressProvider(addressList);
    }

    private final List<Address> addressList;

    StaticAddressProvider(List<Address> addressList) {
        this.addressList = addressList;
    }

    @NonNull
    @Override
    public List<Address> get(SocketClient socketClient) {
        return addressList;
    }
}