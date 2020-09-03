package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import java.util.List;

import javax.net.SocketFactory;

class DefaultSocketFactoryProvider implements Provider<SocketFactory> {

    @NonNull
    @Override
    public SocketFactory get(EasySocket easySocket) {
        return SocketFactory.getDefault();
    }
}

class DefaultPulseHandlerProvider implements Provider<PulseHandler> {

    @NonNull
    @Override
    public PulseHandler get(EasySocket easySocket) {
        return new EmptyPulseHandler();
    }
}

class DefaultPacketHandlerProvider implements Provider<PacketHandler> {

    @NonNull
    @Override
    public PacketHandler get(EasySocket easySocket) {
        return new EmptyPacketHandler(easySocket.getLogger());
    }
}

class DefaultInitializerProvider implements Provider<Initializer> {

    @NonNull
    @Override
    public Initializer get(EasySocket easySocket) {
        return new EmptyInitializer();
    }
}

class StaticAddressProvider implements Provider<List<Address>> {

    static StaticAddressProvider build(@NonNull List<Address> addressList) {
        if (addressList.isEmpty()) {
            throw new IllegalArgumentException("Address list is empty.");
        }
        return new StaticAddressProvider(addressList);
    }

    private List<Address> addressList;

    StaticAddressProvider(List<Address> addressList) {
        this.addressList = addressList;
    }

    @NonNull
    @Override
    public List<Address> get(EasySocket socket) {
        return addressList;
    }
}