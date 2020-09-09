package com.orientsec.easysocket.demo.client;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.SocketClient;
import com.orientsec.easysocket.demo.adapter.TaskAdapter;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2018/01/26 13:19
 * Author: Fredric
 * coding is art not science
 */
public class Client {
    final SocketClient socketClient;
    final Session session;

    private static class ClientHolder {
        private static Client client = new Client();
    }

    public static Client getInstance() {
        return ClientHolder.client;
    }

    private Client() {
        session = new Session();
        Address address = new Address("192.168.0.107", 10010);
        List<Address> addresses = new ArrayList<>();
        addresses.add(address);
        socketClient = new Options.Builder()
                .addressList(addresses)
                .headParserProvider((it) -> new MyHeadParser())
                .initializerProvider((it) -> new MyInitializer(this))
                .requestTimeOut(6000)
                .pulseRate(30000)
                .connectInterval(3000)
                .liveTime(60000)
                .open();
    }

    public Observable<String> request(String param) {
        return TaskAdapter.buildObservable(socketClient, new SimpleRequest(param, session));
    }

}
