package com.orientsec.easysocket;

import androidx.annotation.NonNull;

public interface Initializer {
    void start(@NonNull Emitter emitter);

    interface Emitter {
        void success();

        void fail();
    }
}


final class EmptyInitializer implements Initializer {
    @Override
    public void start(@NonNull Emitter emitter) {
        emitter.success();
    }
}