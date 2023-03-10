package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.client.Session;
import com.orientsec.easysocket.error.EasyException;

public class DefaultConnectionListener implements ConnectionListener {
    @Override
    public void onConnectionStart(@NonNull final Session session) {

    }

    @Override
    public void onConnected(@NonNull final Session session) {

    }

    @Override
    public void onConnectionFailed(@NonNull final Session session, @NonNull EasyException e) {

    }

    @Override
    public void onConnectionAvailable(@NonNull final Session session) {

    }

    @Override
    public void onDisconnected(@NonNull final Session session, @NonNull EasyException e) {

    }
}
