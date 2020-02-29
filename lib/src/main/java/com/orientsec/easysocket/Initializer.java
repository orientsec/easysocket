package com.orientsec.easysocket;

import com.orientsec.easysocket.exception.EasyException;

public interface Initializer<T> {
    interface Emitter {
        void success();

        void fail(EasyException e);
    }

    void start(Connection<T> connection, Emitter emitter);

    final class EmptyInitializer<T> implements Initializer<T> {
        @Override
        public void start(Connection<T> connection, Emitter emitter) {
            emitter.success();
        }
    }
}
