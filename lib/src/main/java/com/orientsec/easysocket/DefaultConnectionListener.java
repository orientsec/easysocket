package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.session.Session;
import com.orientsec.easysocket.error.EasyException;

/**
 * A default implementation of {@link ConnectionListener} that provides empty implementations
 * for all callback methods.
 * <p>
 * This class can be extended to create custom connection listeners where only specific
 * callback methods need to be overridden.
 */
public class DefaultConnectionListener implements ConnectionListener {
    @Override
    public void onConnectionStart(@NonNull final Session session) {

    }

    @Override
    public void onConnectionSuccess(@NonNull final Session session) {

    }

    @Override
    public void onConnectionFailed(@NonNull final Session session, @NonNull EasyException e) {

    }

    @Override
    public void onConnectionAvailable(@NonNull final Session session) {

    }

    @Override
    public void onConnectionAborted(@NonNull final Session session, @NonNull EasyException e) {

    }
}
