package com.orientsec.easysocket;

import androidx.annotation.NonNull;

public interface Provider<T> {
    @NonNull
    T get();
}
