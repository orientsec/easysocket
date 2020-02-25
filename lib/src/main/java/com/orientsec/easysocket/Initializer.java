package com.orientsec.easysocket;

import com.orientsec.easysocket.exception.Event;

public interface Initializer {
    interface Callback<T> {
        <REQUEST, RESPONSE> Task<RESPONSE> buildTask(Request<T, REQUEST, RESPONSE> request);

        void onSuccess();

        void onFail(Event event);
    }

    void start(Callback callback);

    final class EmptyInitializer implements Initializer {
        @Override
        public void start(Callback callback) {
            callback.onSuccess();
        }
    }
}
