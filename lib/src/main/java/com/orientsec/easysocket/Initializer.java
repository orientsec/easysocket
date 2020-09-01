package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.exception.EasyException;

public interface Initializer {
    void start(@NonNull Connection connection, @NonNull Emitter emitter);

    interface Emitter {
        void success();

        void fail(EasyException e);
    }
}


final class EmptyInitializer implements Initializer {
    @Override
    public void start(@NonNull Connection connection, @NonNull Emitter emitter) {
        emitter.success();
    }
}