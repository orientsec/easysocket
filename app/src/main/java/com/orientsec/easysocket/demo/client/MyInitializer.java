package com.orientsec.easysocket.demo.client;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.task.Task;

public class MyInitializer implements Initializer {
    private final Client client;

    MyInitializer(Client client) {
        this.client = client;
    }

    @Override
    public void start(@NonNull Emitter emitter) {
        SimpleRequest authRequest = new SimpleRequest("test", 1, true, client.session);
        Callback<String> callback = new Callback.EmptyCallback<String>() {
            @Override
            public void onSuccess(@NonNull String res) {
                client.session.setSessionId(Integer.parseInt(res));
                emitter.success();
            }

            @Override
            public void onError(@NonNull Exception e) {
                emitter.fail(e);
            }
        };
        Task<String> task = client.socketClient.buildTask(authRequest, callback);
        task.execute();
    }
}
