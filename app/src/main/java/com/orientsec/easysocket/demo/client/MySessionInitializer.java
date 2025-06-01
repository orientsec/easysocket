package com.orientsec.easysocket.demo.client;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.SessionInitializer;
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.request.DefaultCallback;
import com.orientsec.easysocket.session.OperableSession;

public class MySessionInitializer implements SessionInitializer {
    private final Client client;

    MySessionInitializer(Client client) {
        this.client = client;
    }

    @Override
    public void start(@NonNull OperableSession session) {
        SimpleRequest authRequest = new SimpleRequest(1, "test", client.session);
        Callback<String> callback = new DefaultCallback<String>() {
            @Override
            public void onSuccess(@NonNull String res) {
                client.session.setSessionId(Integer.parseInt(res));
                session.postAvailable();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                session.postFail(t);
            }
        };
        session.buildTask(authRequest, callback).execute();
    }
}
