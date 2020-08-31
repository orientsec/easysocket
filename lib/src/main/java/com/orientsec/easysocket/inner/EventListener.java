package com.orientsec.easysocket.inner;

import androidx.annotation.Nullable;

public interface EventListener {
    void onEvent(int eventId, @Nullable Object object);
}
