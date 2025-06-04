package com.orientsec.easysocket.demo.client;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.task.Callback;
import com.orientsec.easysocket.task.DefaultCallback;
import com.orientsec.easysocket.session.SessionInitializer;
import com.orientsec.easysocket.task.TaskBuilder;

public class MySessionInitializer implements SessionInitializer {
    private final Client client;

    MySessionInitializer(Client client) {
        this.client = client;
    }

    @Override
    public void start(@NonNull Emitter emitter, @NonNull TaskBuilder taskBuilder) {
        SimpleRequest authRequest = new SimpleRequest(1, "test", client.session);
        Callback<String> callback = new DefaultCallback<String>() {
            @Override
            public void onSuccess(@NonNull String res) {
                client.session.setSessionId(Integer.parseInt(res));
                emitter.postSuccess();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                emitter.postFailure(t);
            }
        };
        taskBuilder.buildTask(authRequest, callback).execute();
    }
}
