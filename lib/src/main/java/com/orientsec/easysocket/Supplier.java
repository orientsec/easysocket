package com.orientsec.easysocket;

import androidx.annotation.NonNull;

public interface Supplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    @NonNull
    T get();
}

