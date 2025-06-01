package com.orientsec.easysocket.demo.client;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.SocketClient;
import com.orientsec.easysocket.request.Callback;

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
        Address address = new Address("192.168.0.108", 10010);
        List<Address> addresses = new ArrayList<>();
        addresses.add(address);
        socketClient = new Options.Builder()
                .debug(true)
                .name("EasySocketDemo")
                .addressList(addresses)
                .headParserProvider((it) -> new MyHeadParser())
                .initializerProvider((it) -> new MySessionInitializer(this))
                .requestTimeOutInMills(10000)
                .connectTimeOutInMills(5000)
                .connectIntervalInMills(3000)
                .pulseRate(30)
                .liveTime(10)
                .open();
    }

    public void request(String param, Callback<String> callback) {
        socketClient.buildTask(new SimpleRequest(param, session), callback).execute();
    }

}
