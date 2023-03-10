package com.orientsec.easysocket.client;

import androidx.annotation.Nullable;

public interface EventListener {

    void onEvent(int eventId, @Nullable Object object);

}
