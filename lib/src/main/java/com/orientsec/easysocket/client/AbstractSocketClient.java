package com.orientsec.easysocket.client;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.ConnectionListener;
import com.orientsec.easysocket.EasyRunner;
import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Provider;
import com.orientsec.easysocket.SocketClient;
import com.orientsec.easysocket.error.ErrorBuilder;
import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.request.Decoder;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.task.TaskManager;
import com.orientsec.easysocket.utils.LogFactory;
import com.orientsec.easysocket.utils.Logger;

import javax.net.SocketFactory;

public abstract class AbstractSocketClient implements SocketClient, ConnectionListener {
    protected final Options options;
    protected final EasyRunner runner;
    private PushManager<?, ?> pushManager;
    private HeadParser headParser;
    private Initializer initializer;
    private SocketFactory socketFactory;
    private Decoder<Boolean> pulseDecoder;
    private Request<Boolean> pulseRequest;
    public final ErrorBuilder errorBuilder;
    protected final Logger logger;

    public AbstractSocketClient(Options options, EasyRunner runner) {
        this.options = options;
        this.runner = runner;
        String suffix = "  Client[" + options.getName() + "]";
        this.errorBuilder = new ErrorBuilder(suffix);
        this.logger = LogFactory.getLogger(options, suffix);
    }

    protected abstract void onStart();

    protected abstract void onStop();

    protected abstract void onShutdown();

    public abstract void onNetworkAvailable();

    public abstract TaskManager getTaskManager();

    @Nullable
    @Override
    public synchronized PushManager<?, ?> getPushManager() {
        if (pushManager == null) {
            Provider<PushManager<?, ?>> provider = options.getPushManagerProvider();
            if (provider != null) {
                pushManager = provider.get(this);
            }
        }
        return pushManager;
    }

    @NonNull
    public synchronized HeadParser getHeadParser() {
        if (headParser == null) {
            headParser = options.getHeadParserProvider().get(this);
        }
        return headParser;
    }

    @NonNull
    public synchronized SocketFactory getSocketFactory() {
        if (socketFactory == null) {
            socketFactory = options.getSocketFactoryProvider().get(this);
        }
        return socketFactory;
    }

    @NonNull
    public synchronized Initializer getInitializer() {
        if (initializer == null) {
            initializer = options.getInitializerProvider().get(this);
        }
        return initializer;
    }

    @Nullable
    public synchronized Decoder<Boolean> getPulseDecoder() {
        if (pulseDecoder == null) {
            Provider<Decoder<Boolean>> provider = options.getPulseDecoderProvider();
            if (provider != null) {
                pulseDecoder = provider.get(this);
            }
        }
        return pulseDecoder;
    }

    @Nullable
    public synchronized Request<Boolean> getPulseRequest() {
        if (pulseRequest == null) {
            Provider<Request<Boolean>> provider = options.getPulseRequestProvider();
            if (provider != null) {
                pulseRequest = provider.get(this);
            }
        }
        return pulseRequest;
    }

    @Override
    @NonNull
    public Options getOptions() {
        return options;
    }

    @Override
    @NonNull
    public Logger getLogger() {
        return logger;
    }

    @NonNull
    public EasyRunner getEasyRunner() {
        return runner;
    }
}
