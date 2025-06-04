package com.orientsec.easysocket.demo.client;

import android.util.Log;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.SocketClient;
import com.orientsec.easysocket.task.Callback;

import java.util.ArrayList;
import java.util.List;

public class Client {
    final SocketClient socketClient;
    final Session session;

    private static class ClientHolder {
        private static final Client client = new Client();
    }

    public static Client getInstance() {
        return ClientHolder.client;
    }

    private Client() {
        session = new Session();
        Address address = new Address("192.168.88.153", 10010);
        List<Address> addresses = new ArrayList<>();
        addresses.add(address);
        socketClient = new Options.Builder()
                .debuggable(true)
                .minLogLevel(Log.DEBUG)
                .name("EasySocketDemo")
                .addressList(addresses)
                .headParserProvider((it) -> new MyHeadParser())
                .sessionInitializerProvider((it) -> new MySessionInitializer(this))
                .requestTimeOutInMills(10000)
                .connectTimeOutInMills(5000)
                .connectIntervalInMills(3000)
                .pulseDurationInSec(30)
                .backgroundActiveDurationInSec(20)
                .open();
    }

    public void request(String param, Callback<String> callback) {
        socketClient.buildTask(new SimpleRequest(param, session), callback).execute();
    }

}
