package com.orientsec.easysocket;

import com.orientsec.easysocket.adapter.TaskAdapter;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.impl.PacketType;

import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;

import io.reactivex.Observable;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2018/01/26 13:19
 * Author: Fredric
 * coding is art not science
 */
public class Client {

    private static class MyHeadParser implements HeadParser<byte[]> {
        private int id;

        @Override
        public int headSize() {
            return 12;
        }

        @Override
        public int bodySize(byte[] header) throws EasyException {
            ByteBuffer byteBuffer = ByteBuffer.wrap(header);
            return byteBuffer.getInt();
        }

        @Override
        public Packet<byte[]> decodeMessage(byte[] header, byte[] bodyBytes) throws EasyException {
            ByteBuffer byteBuffer = ByteBuffer.wrap(header);
            Packet<byte[]> packet;
            byteBuffer.getInt();
            int taskId = byteBuffer.getInt();
            int cmd = byteBuffer.getInt();
            if (cmd == 0) {
                packet = new Packet<>(PacketType.PULSE, taskId, bodyBytes);
            } else if (cmd == 1) {
                packet = new Packet<>(PacketType.SINGLE, taskId, bodyBytes);
            } else {
                packet = new Packet<>(PacketType.RESPONSE, taskId, bodyBytes);
            }
            return packet;
        }

        @Override
        public byte[] encodeMessage(Packet<byte[]> packet) throws WriteException {
            byte[] body = packet.getBody();
            if (body == null) {
                body = new byte[0];
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(16);
            byteBuffer.putInt(body.length);
            byteBuffer.putInt(packet.getTaskId());
            if (packet.getPacketType().equals(PacketType.PULSE)) {
                byteBuffer.putInt(0);
            } else if (packet.getPacketType().equals(PacketType.SINGLE)) {
                byteBuffer.putInt(1);
            } else {
                byteBuffer.putInt(2);
            }
            byteBuffer.putInt(id);
            byte[] head = byteBuffer.array();
            byte[] sendBytes = new byte[head.length + body.length];
            System.arraycopy(head, 0, sendBytes, 0, head.length);
            System.arraycopy(body, 0, sendBytes, head.length, body.length);
            return sendBytes;
        }

        @Override
        public boolean authorize(byte[] data) {
            id = ByteBuffer.wrap(data).getInt();
            return true;
        }

        @Override
        public boolean needAuthorize() {
            return true;
        }

        @Override
        public SSLContext sslContext() throws Exception {
            return null;
        }
    }

    Connection<byte[]> connection;

    private static class ClientHolder {
        private static Client client = new Client();
    }

    public static Client getInstance() {
        return ClientHolder.client;
    }

    private Client() {
        Options.debug = true;
        Options<byte[]> options = new Options.Builder<byte[]>()
                .connectionInfo(new ConnectionInfo("192.168.106.129", 10010))
                .protocol(new MyHeadParser())
                .pulseRate(30)
                .backgroundLiveTime(60)
                .build();
        connection = EasySocket.open(options);
    }

    public Observable<String> request(String msg) {
        Task<String> task = connection.buildTask(new Request<byte[], String, String>(msg) {
            @Override
            public byte[] encode(Packet<byte[]> message) {
                return msg.getBytes();
            }

            @Override
            public String decode(byte[] data) {
                return new String(data);
            }
        });
        return TaskAdapter.toObservable(task);
    }
}
