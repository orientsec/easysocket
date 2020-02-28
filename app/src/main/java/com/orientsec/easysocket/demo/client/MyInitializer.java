package com.orientsec.easysocket.demo.client;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.Initializer;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.EasyException;

public class MyInitializer implements Initializer<byte[]> {
    private Session session;

    MyInitializer(Session session) {
        this.session = session;
    }

    @Override
    public void start(Connection<byte[]> connection, Emitter emitter) {
        SimpleRequest authRequest = new SimpleRequest("test", 1, true, session);
        Callback<String> callback = new Callback.EmptyCallback<String>() {
            @Override
            public void onSuccess(String res) {
                session.setSessionId(Integer.parseInt(res));
                emitter.success();
            }

            @Override
            public void onError(EasyException e) {
                emitter.fail(e.getEvent());
            }
        };
        Task<String> task = connection.buildTask(authRequest, callback);
        task.execute();
    }
}
