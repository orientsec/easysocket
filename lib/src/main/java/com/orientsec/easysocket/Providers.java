package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.push.PushListener;
import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.request.Decoder;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.request.SendOnlyRequest;

import java.util.List;

import javax.net.SocketFactory;

class DefaultSocketFactoryProvider implements Provider<SocketFactory> {

    @NonNull
    @Override
    public SocketFactory get() {
        return SocketFactory.getDefault();
    }
}

class DefaultPulseRequestProvider implements Provider<Request<?>> {

    @NonNull
    @Override
    public Request<?> get() {
        return new SendOnlyRequest();
    }
}

class DefaultPulseDecoderProvider implements Provider<Decoder<?>> {

    @NonNull
    @Override
    public Decoder<?> get() {
        return new DefaultPulseDecoder();
    }
}

class DefaultPulseDecoder implements Decoder<Boolean> {

    @Override
    public Boolean decode(@NonNull Packet data) {
        return true;
    }
}

class DefaultPushManagerProvider implements Provider<PushManager<?, ?>> {

    @NonNull
    @Override
    public PushManager<?, ?> get() {
        return new EmptyPushManager();
    }
}

final class EmptyPushManager implements PushManager<Integer, Integer> {

    @Override
    @NonNull
    public Integer parsePacket(@NonNull Packet packet) {
        return 0;
    }

    @Override
    public void unregisterPushListener(@NonNull PushListener<Integer> pushListener) {

    }

    @Override
    public void unregisterPushListener(@NonNull Integer key,
                                       @NonNull PushListener<Integer> pushListener) {

    }

    @Override
    public void registerPushLister(@NonNull PushListener<Integer> pushListener) {

    }

    @Override
    public void registerPushListener(@NonNull Integer key,
                                     @NonNull PushListener<Integer> pushListener) {

    }

    @Override
    public void handlePacket(@NonNull Packet packet) {
        //logger.i("Unhandled packet. " + packet);
    }
}

class DefaultInitializerProvider implements Provider<Initializer> {

    @NonNull
    @Override
    public Initializer get() {
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

    private List<Address> addressList;

    StaticAddressProvider(List<Address> addressList) {
        this.addressList = addressList;
    }

    @NonNull
    @Override
    public List<Address> get() {
        return addressList;
    }
}