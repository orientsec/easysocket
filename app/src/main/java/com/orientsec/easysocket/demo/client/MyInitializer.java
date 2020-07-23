package com.orientsec.easysocket.demo.client;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.ErrorType;

public class MyInitializer implements Initializer<byte[]> {
    private Session session;

    MyInitializer(Session session) {
        this.session = session;
    }

    @Override
    public void start(@NonNull Connection<byte[]> connection, @NonNull Emitter emitter) {
        SimpleRequest authRequest = new SimpleRequest("test", 1, true, session);
        Callback<String> callback = new Callback.EmptyCallback<String>() {
            @Override
            public void onSuccess(@NonNull String res) {
                session.setSessionId(Integer.parseInt(res));
                emitter.success();
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (e instanceof EasyException) {
                    emitter.fail((EasyException) e);
                } else {
                    EasyException ee = new EasyException(100, ErrorType.CONNECT,
                            e.getMessage(), e);
                    emitter.fail(ee);
                }
            }
        };
        Task<byte[], String> task = connection.buildTask(authRequest, callback);
        task.execute();
    }
}
