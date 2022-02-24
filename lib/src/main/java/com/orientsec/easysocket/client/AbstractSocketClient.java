package com.orientsec.easysocket.client;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.ConnectionListener;
import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.SocketClient;
import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.request.Decoder;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.task.TaskManager;

import javax.net.SocketFactory;

public abstract class AbstractSocketClient implements SocketClient, EventListener,
        ConnectionListener {
    protected final Options options;
    private PushManager<?, ?> pushManager;
    private HeadParser headParser;
    private Initializer initializer;
    private SocketFactory socketFactory;
    private Decoder<Boolean> pulseDecoder;
    private Request<Boolean> pulseRequest;

    public AbstractSocketClient(Options options) {
        this.options = options;
    }

    protected abstract void onStart();

    protected abstract void onStop();

    protected abstract void onShutdown();

    public abstract void onNetworkAvailable();

    public abstract TaskManager getTaskManager();

    protected abstract Address obtainAddress();

    @NonNull
    @Override
    public synchronized PushManager<?, ?> getPushManager() {
        if (pushManager == null) {
            pushManager = options.getPushManagerProvider().get(this);
        }
        return pushManager;
    }

    public synchronized HeadParser getHeadParser() {
        if (headParser == null) {
            headParser = options.getHeadParserProvider().get(this);
        }
        return headParser;
    }

    public synchronized SocketFactory getSocketFactory() {
        if (socketFactory == null) {
            socketFactory = options.getSocketFactoryProvider().get(this);
        }
        return socketFactory;
    }

    public synchronized Initializer getInitializer() {
        if (initializer == null) {
            initializer = options.getInitializerProvider().get(this);
        }
        return initializer;
    }

    public synchronized Decoder<Boolean> getPulseDecoder() {
        if (pulseDecoder == null) {
            pulseDecoder = options.getPulseDecoderProvider().get(this);
        }
        return pulseDecoder;
    }

    public synchronized Request<Boolean> getPulseRequest() {
        if (pulseRequest == null) {
            pulseRequest = options.getPulseRequestProvider().get(this);
        }
        return pulseRequest;
    }
}
