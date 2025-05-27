package com.orientsec.easysocket.demo.client;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.task.TaskType;

public class MyInitializer implements Initializer {
    private final Client client;

    MyInitializer(Client client) {
        this.client = client;
    }

    @Override
    public void start(@NonNull Emitter emitter) {
        SimpleRequest authRequest = new SimpleRequest(1, "test", client.session);
        Callback<String> callback = new Callback.EmptyCallback<String>() {
            @Override
            public void onSuccess(@NonNull String res) {
                client.session.setSessionId(Integer.parseInt(res));
                emitter.success();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                emitter.fail(t);
            }
        };
        client.socketClient
                .buildTask(authRequest, callback, TaskType.INITIALIZE)
                .execute();
    }
}
