package com.orientsec.easysocket.inner;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.error.EasyException;

public abstract class AbstractConnection implements Connection, EventListener {

    protected abstract void onStart();

    protected abstract void onShutdown();

    protected abstract void onSuccess(@NonNull Session session);

    protected abstract void onFailed(@NonNull EasyException e);

    protected abstract void onError(@NonNull EasyException e);

    protected abstract void onAvailable();

    public abstract void onNetworkAvailable();

}
