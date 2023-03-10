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
    public SocketFactory get(SocketClient socketClient) {
        return SocketFactory.getDefault();
    }
}

class DefaultPulseRequestProvider implements Provider<Request<Boolean>> {

    @NonNull
    @Override
    public Request<Boolean> get(SocketClient socketClient) {
        return new SendOnlyRequest<>();
    }
}

class DefaultPulseDecoderProvider implements Provider<Decoder<Boolean>> {

    @NonNull
    @Override
    public Decoder<Boolean> get(SocketClient socketClient) {
        return new DefaultPulseDecoder();
    }
}

class DefaultPulseDecoder implements Decoder<Boolean> {

    @Override
    @NonNull
    public Boolean decode(@NonNull Packet data) {
        return true;
    }
}

class DefaultPushManagerProvider implements Provider<PushManager<?, ?>> {

    @NonNull
    @Override
    public PushManager<?, ?> get(SocketClient socketClient) {
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