package com.orientsec.easysocket.demo.client;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.task.Task;

public class MyInitializer implements Initializer {
    private Session session;
    private Connection connection;

    MyInitializer(EasySocket easySocket, Session session) {
        this.session = session;
        this.connection = easySocket.getConnection();
    }

    @Override
    public void start(@NonNull Emitter emitter) {
        SimpleRequest authRequest = new SimpleRequest("test", 1, true, session);
        Callback<String> callback = new Callback.EmptyCallback<String>() {
            @Override
            public void onSuccess(@NonNull String res) {
                session.setSessionId(Integer.parseInt(res));
                emitter.success();
            }

            @Override
            public void onError(@NonNull Exception e) {
                emitter.fail();
            }
        };
        Task<String> task = connection.buildTask(authRequest, callback);
        task.execute();
    }
}
