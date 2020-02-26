package com.orientsec.easysocket.demo.client;

import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.ConnectionInfo;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.adapter.TaskAdapter;

import io.reactivex.Observable;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2018/01/26 13:19
 * Author: Fredric
 * coding is art not science
 */
public class Client {
    private Connection<byte[]> connection;
    private Session session;

    private static class ClientHolder {
        private static Client client = new Client();
    }

    public static Client getInstance() {
        return ClientHolder.client;
    }

    private Client() {
        Options.debug = true;
        session = new Session();
        Options<byte[]> options = new Options.Builder<byte[]>()
                .connectionInfo(new ConnectionInfo("192.168.0.100", 10010))
                .protocol(new MyHeadParser())
                .initializer(new MyInitializer(session))
                .requestTimeOut(60)
                .pulseRate(30)
                .backgroundLiveTime(60)
                .build();
        connection = EasySocket.open(options);
    }

    public Observable<String> request(String param) {
        Task<String> task = connection.buildTask(new SimpleRequest(param, session));
        return TaskAdapter.toObservable(task);
    }


}
