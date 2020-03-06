package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.exception.EasyException;

public interface Initializer<T> {
    interface Emitter {
        void success();

        void fail(EasyException e);
    }

    void start(@NonNull Connection<T> connection, @NonNull Emitter emitter);

    final class EmptyInitializer<T> implements Initializer<T> {
        @Override
        public void start(@NonNull Connection<T> connection, @NonNull Emitter emitter) {
            emitter.success();
        }
    }
}
